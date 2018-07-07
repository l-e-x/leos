/*
 * Copyright 2015 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
; // jshint ignore:line
define(function transformationConfigResolverModule(require) {
    "use strict";
    var STAMPIT = require("stampit");
    var LOG = require("logger");
    var PATH = "pathForResolver";
    var TRANSFORMATION_DIRECTION_FROM = "from";
    var TRANSFORMATION_DIRECTION_TO = "to";
    var DATA_AKN_NAME = "data-akn-name";
    var FIRST_LEVEL_ELEMENT = 0;
    /*
     * Helper function which check if the only single config is resolved
     */
    var _isConfigMatched = function _isConfigMatched(configs) {
        if (configs && configs.length == 1) {
            return true;
        }
        return false;
    }

    // Stamp representing the match type
    var matchTypeStamp = STAMPIT().methods({
        init : function init(params) {
            this.matchName = params.matchName;
            this.priority = params.priority;
            return this;
        },
        hasGreaterPriorityThanInElement : function hasGreaterPriorityThanInElement(element) {
            if (this.priority >= element.matchedBy.priority) {
                element.matchedBy = this;
                return true;
            }
            return false;
        }
    });

    var MATCH_TYPE_ELEMENT_NAME = matchTypeStamp().init({
        matchName : "ELEMENT_NAME",
        priority : 1
    });

    var MATCH_TYPE_ATTRIBUTE_NAME = matchTypeStamp().init({
        matchName : "ATTRIBUTE_NAME",
        priority : 2
    });

    var MATCH_TYPE_ATTRIBUTE_NAME_AND_VALUE = matchTypeStamp().init({
        matchName : "ATTRIBUTE_NAME_AND_VALUE",
        priority : 3
    });

    var MATCH_TYPE_DATA_AKN_NAME = matchTypeStamp().init({
        matchName : "DATA_AKN_NAME",
        priority : 4
    });

    /*
     * Container storing data specific to one iteration of the element at specific level.
     */
    var configResolverContextStamp = STAMPIT().methods({
        init : function init(params) {
            this.elementLevel = params.elementLevel;
            this.element = params.element;
            this.direction = params.direction;
            this.matchedConfigs = params.configs;
            return this;
        },
        /*
         * Iterator of the transformation configs
         */
        forEachFirstLevelConfig : function forEachConfig(onEachConfigCallback) {
            if (!this.matchedConfigs) {
                return;
            }
            var newMatchedConfigs = [];
            var that = this;
            this.matchedConfigs.forEach(function(config) {
                var firstLevelConfigForPathAndDirection = config[that.direction][FIRST_LEVEL_ELEMENT];
                if (onEachConfigCallback(firstLevelConfigForPathAndDirection)) {
                    newMatchedConfigs.push(config);
                }
            });
            this.matchedConfigs = newMatchedConfigs;

        },
        /*
         * Return false - if current iteration should finish or true if iteration should go to the child element
         */
        shouldGoToChildElement : function shouldGoToChildElement() {

            if (this.matchedConfigs) {
                if (this.matchedConfigs.length > 1) {
                    // should go to the child element only if there is one then more config
                    return true;
                } else if (this.matchedConfigs.length === 1) {
                    return false;
                }
            } 
            else if (this.element.elementLevel > FIRST_LEVEL_ELEMENT) {
                // because there could be nested white spaces if the first level is not matched try to go as deep as possible
                return true;
            }
            return false;
        }

    });

    var transformationConfigResolverStamp = STAMPIT().methods(
            {

                init : function init(params) {
                    this._initPrivate();
                    this._validateRequired("transformationConfigs", params);
                    params["transformationConfigs"].forEach(function(transformationConfigs) {
                        this._addTransformationConfigs(transformationConfigs);
                    }, this);

                },
                /*
                 * Resolve the transformation config for the given fragment and direction.
                 */
                getConfig : function getConfig(fragment, direction) {
                    var elementLevel = FIRST_LEVEL_ELEMENT - 1;

                    var matchedConfigs, matchedConfig;
                    if (fragment.forEach) {
                        var that = this;
                        fragment.forEach(function(element) {
                            elementLevel += 1;
                            /*
                             * there is no way to stop iterator when there are siblings so this if statement make sure that no sibling are considered for
                             * matched when there is one already
                             */
                            if (_isConfigMatched(matchedConfigs)) {
                                return false;
                            }
                            that._setPath(element, fragment);
                            matchedConfigs = that._resolveByElementName(that._.resolverConfigs, element, direction);
                            var context = configResolverContextStamp().init({
                                direction : direction,
                                element : element,
                                elementLevel : elementLevel,
                                configs : matchedConfigs
                            });
                            that._resolveConfigsForFromAttribute(context);
                            matchedConfigs = context.matchedConfigs;
                            return context.shouldGoToChildElement();
                        });

                    }
                    matchedConfig = this._resolveSingleConfig(matchedConfigs, direction);
                    return matchedConfig;

                },
                _resolveSingleConfig : function _resolveSingleConfig(matchedConfigs, direction, fragment) {
                    var matchedConfig;
                    if (matchedConfigs) {
                        if (matchedConfigs.length > 1) {
                            LOG.error("Not able to uniquely identify config for: '", fragment, "' . Try to adjust your transformation configs.");
                        } else if (matchedConfigs.length === 1) {
                            matchedConfig = matchedConfigs[0][direction];
                        }
                    }
                    return matchedConfig;
                },
                _resolveByElementName : function _resolveByElementName(configs, element, direction) {
                    var matchedConfigs = configs[direction][element[PATH]];
                    if (matchedConfigs) {
                        element.matchedBy = MATCH_TYPE_ELEMENT_NAME;
                    }
                    return matchedConfigs;
                },
                /*
                 * Resolve the transformation config in case of the presence of the from attribute
                 */
                _resolveConfigsForFromAttribute : function _resolveConfigsForFromAttribute(context) {
                    if (context.elementLevel !== FIRST_LEVEL_ELEMENT) {
                        return;
                    }
                    context.forEachFirstLevelConfig(function(firstLevelConfig) {
                        if (!firstLevelConfig.fromAttribute) {
                            return true;
                        } else {
                            if (!!firstLevelConfig.fromAttributeValue
                                    && context.element.attributes[firstLevelConfig.fromAttribute] === firstLevelConfig.fromAttributeValue) {
                                // if branched here it means that in order to resolver config, the element name needs to be matched as well as its attribute and
                                // attribute value
                                return MATCH_TYPE_ATTRIBUTE_NAME_AND_VALUE.hasGreaterPriorityThanInElement(context.element);
                            } else if (!firstLevelConfig.fromAttributeValue && !!context.element.attributes[firstLevelConfig.fromAttribute]) {
                                // if branched here it means that in order to resolver config, the element name needs to be matched as well as its attribute
                                return MATCH_TYPE_ATTRIBUTE_NAME.hasGreaterPriorityThanInElement(context.element);
                            }
                        }
                        return false;
                    });
                },

                /*
                 * Set path for current iteration element
                 */
                _setPath : function _setPath(element, fragment) {
                    var path = this._getNameForElement(element);
                    if (element !== fragment) {
                        path = [ element.parent[PATH], "/", path ].join("");
                    }
                    // store calculated path in current element
                    element[PATH] = path;

                },
                /*
                 * Get element name for current iteration element
                 */
                _getNameForElement : function _getNameForElement(element) {
                    var elementName = null;
                    if (element instanceof CKEDITOR.htmlParser.element) {
                        elementName = element.name;
                    } else if (element instanceof CKEDITOR.htmlParser.text) {
                        elementName = "text";

                    }
                    return elementName;
                },

                _initPrivate : function _initPrivate() {
                    if (this._) {
                        var errorMessage = "Instance already initialized. Probably method called more than once.";
                        LOG.error(errorMessage);
                        throw errorMessage;
                    } else {
                        this._ = {};
                        this._.resolverConfigs = {};
                        this._.resolverConfigs['to'] = {};
                        this._.resolverConfigs['from'] = {};
                    }
                },
                _validateRequired : function _validateRequired(paramName, params) {
                    if (!params || !params[paramName]) {
                        var errorMessage = [ "Param with name: '", paramName, "' is required, please provide it." ].join("");
                        throw new Error(errorMessage);
                    }
                },
                /*
                 * Adds transformation configs representing exactly one element
                 */
                _addTransformationConfigs : function _addTransformationConfigs(transformationConfigs) {
                    if (transformationConfigs) {
                        this._addTransformationConfigsForDirection({
                            direction : "to",
                            transformationConfigs : transformationConfigs
                        });
                        this._addTransformationConfigsForDirection({
                            direction : "from",
                            transformationConfigs : transformationConfigs
                        });
                    }

                },
                /*
                 * Adds transformation configs representing exactly one element and one of the to possible direction: 'to' or 'from'
                 */
                _addTransformationConfigsForDirection : function _addTransformationConfigs(params) {
                    var transformationConfigsForDirection = params.transformationConfigs[params.direction];
                    var resolverConfigsForDirection = this._.resolverConfigs[params.direction];
                    for ( var ii in transformationConfigsForDirection) {
                        this._pushConfigToResolver({
                            direction : params.direction,
                            transformationConfigs : params.transformationConfigs,
                            transformationConfigsIndex : ii,
                            resolverConfigsForDirection : resolverConfigsForDirection
                        });
                    }
                },
                /*
                 * Push configs for element to the resolver configs map, where map is storing path to its transformation config
                 */
                _pushConfigToResolver : function _pushConfigToResolver(params) {
                    var configLevelElement = params.transformationConfigs[params.direction][params.transformationConfigsIndex];
                    // if the config with the fromPath attribute don't exists yet, add it
                    if (!params.resolverConfigsForDirection[configLevelElement.fromPath]) {
                        params.resolverConfigsForDirection[configLevelElement.fromPath] = [];
                        params.resolverConfigsForDirection[configLevelElement.fromPath].push(params.transformationConfigs);
                    } else {
                        // if the config with the fromPath attribute already exists just add it
                        if (params.resolverConfigsForDirection[configLevelElement.fromPath].length > 0
                                && !this._isIncludedInConfigResolver(params.resolverConfigsForDirection[configLevelElement.fromPath],
                                        params.transformationConfigs)) {
                            params.resolverConfigsForDirection[configLevelElement.fromPath].push(params.transformationConfigs);
                        }
                        // if the config with the fromPath attribute already exists but represents the same element just do nothing
                    }
                },
                _isIncludedInConfigResolver : function _isIncludedInConfigResolver(resolverConfigs, configsForElement) {
                    for ( var key in resolverConfigs) {
                        if (resolverConfigs[key] === configsForElement) {
                            return true;
                        }
                    }
                    return false;

                }

            });

    return transformationConfigResolverStamp;
});
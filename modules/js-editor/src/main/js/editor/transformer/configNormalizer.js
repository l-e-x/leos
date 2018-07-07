/*
 * Copyright 2016 European Commission
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
define(function configNormalizerModule(require) {
    "use strict";
    var STAMPIT = require("stampit");
    var LODASH = require("lodash");
    var LOG = require("logger");
    var attributeTransformerFactory = require("transformer/attributeTransformer");
    function _initializeRegexForConfigNormalizer() {
        var matchThePath = "(?:\\w*\\/)+";
        var matchTheElement="\\w*";
        var matchTheAttributeWithValue="([\\w-_\:]+)(?:=([\~?\\w-_\*\(\)\\s]+)?)?";
        var matchTheEnclosedAttributeWithValue="\\[matchTheAttributeWithValue\\]".replace("matchTheAttributeWithValue", matchTheAttributeWithValue);
        var matchThePathWithElementAndAttribute = "(matchThePath)?(?:(matchTheElement)?(?:matchTheEnclosedAttributeWithValue)?)?".replace("matchThePath",matchThePath).replace("matchTheElement",matchTheElement).replace("matchTheEnclosedAttributeWithValue",matchTheEnclosedAttributeWithValue);
        var matchPossibleSeparatorsInConfig = /[\/\[\]=]/g;
        var matchElementWithNameAndAttribute = new RegExp(matchThePathWithElementAndAttribute);
        var matchAttributeWithValue = new RegExp(matchTheAttributeWithValue);
        return {
            matchElementWithNameAndAttribute:matchElementWithNameAndAttribute,
            matchAttributeWithValue:matchAttributeWithValue,
            matchPossibleSeparatorsInConfig:matchPossibleSeparatorsInConfig
        };
    }
    var configNormalizerMatcher = _initializeRegexForConfigNormalizer();
        
    
    
    

    /*
     * ConfigNormalizer is used to normalize configuration used to describe the transformation to be performed. E.g.: config to be normalize: { akn: 'mp', html:
     * 'p', sub: { akn: "text", html: "p/text" } } normalized config(result): E.g: { to: { [ { from: "mp", fromPath: "mp", to: "p", toPath: "p" },{ from:
     * "text", fromPath: "mp/text", to: "text", toPath: "p/text" } }, from: { [ { from: "p", fromPath: "p", to: "mp", toPath: "mp" },{ from: "text", fromPath:
     * "p/text", to: "text", toPath: "mp/text" } },
     * 
     * 
     * 
     * @param config - config to be normalized
     * 
     */
    var ConfigNormalizerStamp = STAMPIT().methods(
            {
                /*
                 * Return normalized config. @return the normalized config.
                 */
                getNormalizedConfig: function getNormalizedConfig(params) {
                    this._init();
                    this._.rawConfig = params.rawConfig;
                    this._normalizeRawConfig(params.rawConfig);
                    this._.normalizedConfig.transformer = params.rawConfig.transformer;
                    return this._.normalizedConfig;

                },
                _init: function _init() {
                    if (this._) {
                        var errorMessage = "Instance already initialized. Probably method called more than once.";
                        LOG.error(errorMessage);
                        throw errorMessage;
                    } else {
                        this._ = {};
                        this._.normalizedConfig = {};
                        this._.normalizedConfig.to = [];
                        this._.normalizedConfig.from = [];
                    }
                },
                /*
                 * 
                 * Normalize one level of the raw config @param rawConfig - raw config in the form of: { akn: 'mp', html: 'p',.. } to be normalized
                 */
                _normalizeRawConfig: function _normalizeRawConfig(rawConfig) {
                    var normalizedConfigForAknToHtml = this._normalizeRawConfigForAknToHtml(rawConfig);
                    var normalizedConfigForHtmlToAkn = this._normalizeRawConfigForHtmlToAkn(normalizedConfigForAknToHtml);
                    this._.normalizedConfig.to.push(normalizedConfigForAknToHtml);
                    this._.normalizedConfig.from.push(normalizedConfigForHtmlToAkn);
                    this._normalizeRawConfigSubElements(rawConfig, normalizedConfigForAknToHtml.fromPath);

                },
                /*
                 * Normalize the children of the given rawConfig @rawConfig - given config whose children are to be normalized @fromPath - the location of the
                 * current rawConfig
                 */
                _normalizeRawConfigSubElements: function _normalizeRawConfigSubElements(rawConfig, fromPath) {
                    rawConfig.fromPath = fromPath;
                    var parentForNextRawConfig = rawConfig;
                    var nextRawConfig = rawConfig.sub;
                    if (nextRawConfig) {
                        this._setParentOnRawConfig(nextRawConfig, parentForNextRawConfig);
                        if (LODASH.isArray(nextRawConfig)) {
                            for (var ii = 0, length = nextRawConfig.length; ii < length; ii++) {
                                this._normalizeRawConfig(nextRawConfig[ii]);
                            }

                        } else {
                            this._normalizeRawConfig(nextRawConfig);
                        }
                    }

                },
                /*
                 * Create the 'from' part of the normalized config. With some exception 'from' part is created using the 'to' part. @param normalizeConfigTo -
                 * 'to' part of normalized config used to build the 'from' part @return the normalized config for the 'from' part of one level element
                 * transformation
                 */
                _normalizeRawConfigForHtmlToAkn: function _normalizeRawConfigForHtmlToAkn(normalizeConfigTo) {
                    var normalizeConfigFrom = {};
                    normalizeConfigFrom.attrs = LODASH.cloneDeep((this._normalizeAttrsFrom(normalizeConfigTo.attrs)));
                    normalizeConfigFrom.fromPath = LODASH.cloneDeep(normalizeConfigTo.toPath);
                    normalizeConfigFrom.fromParentPath = LODASH.cloneDeep(normalizeConfigTo.toParentPath);
                    normalizeConfigFrom.from = LODASH.cloneDeep(normalizeConfigTo.to);
                    normalizeConfigFrom.fromAttribute = LODASH.cloneDeep(normalizeConfigTo.toAttribute);
                    //TODO consider to wrap all assignment with: _assignIfNotUndefined 
                    //assign value to the fromAttribute value only if it is not undefined in order not to brak tests
                    this._assignIfNotUndefined(normalizeConfigFrom, "fromAttributeValue", LODASH.cloneDeep(normalizeConfigTo.toAttributeValue));
                    normalizeConfigFrom.toPath = LODASH.cloneDeep(normalizeConfigTo.fromPath);
                    normalizeConfigFrom.toParentPath = LODASH.cloneDeep(normalizeConfigTo.fromParentPath);
                    normalizeConfigFrom.to = LODASH.cloneDeep(normalizeConfigTo.from);
                    normalizeConfigFrom.toAttribute = LODASH.cloneDeep(normalizeConfigTo.fromAttribute);
                    this._assignIfNotUndefined(normalizeConfigFrom, "toAttributeValue", LODASH.cloneDeep(normalizeConfigTo.toAttributeValue));
                    return normalizeConfigFrom;
                },
                _assignIfNotUndefined: function _assignIfNotUndefined(object, property, value) {
                    if (value) {
                        object[property]=value;
                    }
                },
                
                /*
                 * Create the 'to' part of the normalized config. @param rawConfig - raw config to be normalized @param return the normalized config for 'to'
                 * part of one level element transformation
                 */
                _normalizeRawConfigForAknToHtml: function _normalizeRawConfigForAknToHtml(rawConfig) {
                    var fullFromPath = this._getPathForFrom(rawConfig);
                    var configPathAttributeResolverForFrom = this._configPathAttributeResolver(fullFromPath);
                    var normalizedConfigTo = {};
                    normalizedConfigTo.attrs = this._normalizeAttrsTo(rawConfig.attr);
                    normalizedConfigTo.fromPath = configPathAttributeResolverForFrom.path;
                    normalizedConfigTo.fromPath = normalizedConfigTo.fromPath && normalizedConfigTo.fromPath.toLowerCase();
                    normalizedConfigTo.fromParentPath = this._getParentPath(normalizedConfigTo.fromPath);
                    normalizedConfigTo.fromParentPath = normalizedConfigTo.fromParentPath && normalizedConfigTo.fromParentPath.toLowerCase();
                    normalizedConfigTo.from = configPathAttributeResolverForFrom.name;
                    normalizedConfigTo.fromAttribute = configPathAttributeResolverForFrom.attributeName;
                    this._assignIfNotUndefined(normalizedConfigTo, "fromAttributeValue", configPathAttributeResolverForFrom.attributeValue);
                    this.__normalizeRawConfigForAknToHtmlForToSide(normalizedConfigTo, rawConfig);
                    return normalizedConfigTo;
                },
                __normalizeRawConfigForAknToHtmlForToSide: function __normalizeRawConfigForAknToHtmlForToSide(normalizedConfigTo, rawConfig) {
                    var fullToPath = rawConfig.html;
                    var configPathAttributeResolverForTo = this._configPathAttributeResolver(fullToPath);
                    normalizedConfigTo.toPath = configPathAttributeResolverForTo.path;
                    normalizedConfigTo.toParentPath = this._getParentPath(normalizedConfigTo.toPath);
                    normalizedConfigTo.to = configPathAttributeResolverForTo.name;
                    normalizedConfigTo.toAttribute = configPathAttributeResolverForTo.attributeName;
                    this._assignIfNotUndefined(normalizedConfigTo, "toAttributeValue", configPathAttributeResolverForTo.attributeValue);
                    normalizedConfigTo.toParentPath = normalizedConfigTo.toParentPath && normalizedConfigTo.toParentPath.toLowerCase();
                    normalizedConfigTo.toPath = normalizedConfigTo.toPath && normalizedConfigTo.toPath.toLowerCase();
                    if (normalizedConfigTo.from==="text"&&normalizedConfigTo.toAttribute) {
                        // content of text node is moved to attribute, this means that the parent is able to holds only text node
                        this._.normalizedConfig["from"].forEach(function(config) {
                            if (config.toPath ===  normalizedConfigTo.fromParentPath) {
                                config.noNestedAllowed = true;
                            }
                        });
                        
                    }
                },
                
                
                _getPathForFrom: function(rawConfig) {
                	var path = "";
                	if (rawConfig.parent && rawConfig.parent.fromPath) {
                		path = rawConfig.parent.fromPath ;
                		//only add current akn as part of path if it is different than previous(parent one)
                		if (rawConfig.parent.akn !== rawConfig.akn) {
                			path+="/"+rawConfig.akn;
                		}
                	} else {
                		path+=rawConfig.akn;
                	}
                	return path;
                	
                },
                /*
                 * Extract path, name and attribute from following form: path/nodeName[attributeName] @param fullPath - given fullPath param to be parsed, could
                 * be in the form of: path/elementName[attributeName], path/elementName, elementName @return the map containing: path+name, name, attribute
                 * 
                 */
                _configPathAttributeResolver: function(fullPath) {
                    var path, elementName, attributeName, attributeValue;
                    var result = fullPath.match(configNormalizerMatcher.matchElementWithNameAndAttribute)
                    if (!result) {
                        LOG.error("Invalid config transformation part: "+fullPath+" for transformation config: " + JSON.stringify(this._.rawConfig).replace(/"/g,''));
                    } else {
                        var whatsLeftFromFullPath = fullPath.replace(configNormalizerMatcher.matchPossibleSeparatorsInConfig,'');
                        var whatsLeftFromWholeMatch = result[0].replace(configNormalizerMatcher.matchPossibleSeparatorsInConfig,'');
                        if (whatsLeftFromFullPath!==whatsLeftFromWholeMatch) {
                            LOG.error("Invalid config transformation part: "+fullPath+" for transformation config: " + JSON.stringify(this._.rawConfig).replace(/"/g,''));
                        } else {
                            path = result[1]||"";
                            elementName = result[2];
                            attributeName = result[3];
                            attributeValue = result[4];
                            
                        }
                    }
                    return {
                        'path': path + elementName,
                        'name': elementName,
                        'attributeName': attributeName,
                        'attributeValue': attributeValue
                    };

                },

                /*
                 * Extract the attribute name and its default value from following form: attributeName=value
                 * 
                 * @param attrWithValue - given attrWithValue param to be parsed, could be in the form of: attributeName, attributeName=value @return the map
                 * containing: attributeName and attributeValue
                 */
                _configAttributeResolver: function(attrWithValue) {
                    var result,attributeName,attributeValue;
                    if (!attrWithValue) {
                        result = [];
                    } else {
                        result = attrWithValue.match(configNormalizerMatcher.matchAttributeWithValue);
                        if (!result) {
                            LOG.error("Invalid attribute value: '"+attrWithValue+"'"+"for config: "+ JSON.stringify(this._.rawConfig).replace(/"/g,''));
                        } else {
                            var whatsLeftFromNameAndValue = result[0].replace(configNormalizerMatcher.matchPossibleSeparatorsInConfig,'');
                            var whatsLeftFromPassedNameAndValue = attrWithValue.replace(configNormalizerMatcher.matchPossibleSeparatorsInConfig,'');
                            if (whatsLeftFromNameAndValue!==whatsLeftFromPassedNameAndValue) {
                                LOG.error("Invalid attribute value: '"+attrWithValue+"'"+"for config: "+JSON.stringify(this._.rawConfig).replace(/"/g,''));
                            } else {
                               attributeName = result[1];
                               attributeValue = result[2]
                                
                            }
                            
                            
                        }
                    }

                    return {
                        name: attributeName,
                        value: attributeValue
                    };
                },
                /*
                 * Calculates the parent path for given path @path given path @return parent path
                 * 
                 */
                _getParentPath: function(path) {
                    var splittedPath = path.split("/");
                    var parentPath;
                    if (splittedPath.length === 1) {
                        parentPath = splittedPath[0];
                    } else {
                        splittedPath.pop();
                        parentPath = splittedPath.join("/");
                    }
                    return parentPath;

                },
                /*
                 * Normalize attribute metadata from following form: { akn : "attributeName=value", html : "attributeName=value" }
                 * 
                 * @param attr - attribute to be normalized @return normalized attribute, the map containing following: { from - attribute name for the 'from'
                 * side, fromValue - attribute value which needs to be matched, to - attribute name for the 'to' side, toValue - attribute value which needs to
                 * be set, action - strategy(function) - used to customise what needs to be performed for different types of attributes i.e.: class, style, etc }
                 */
                _normalizeSingleAttrTo: function(attr) {
                    if (!attr.akn && !attr.html) {
                        throw ["Badly supplied attribute configuration for transformation:", JSON.stringify(this.transformation.akn)].join("");
                    }
                    var normAttr = {};
                    var aknNorm = this._configAttributeResolver(attr.akn);
                    var htmlNorm = this._configAttributeResolver(attr.html);
                    normAttr.from = aknNorm.name;
                    normAttr.fromValue = aknNorm.value;
                    normAttr.to = htmlNorm.name;
                    normAttr.toValue = htmlNorm.value;
                    normAttr.action = attributeTransformerFactory.getAttributeTransformerName(normAttr);
                    return normAttr;
                },
                /*
                 * Normalize attributes for 'to' side of transformation level @param given attributes to be normalized @return the normalized attributes for
                 * 'to' side
                 * 
                 */
                _normalizeAttrsTo: function(attrs) {

                    var normalizedAttrs = [];
                    if (!attrs) {
                        return normalizedAttrs;
                    }
                    if (!LODASH.isArray(attrs)) {
                        attrs = [attrs];
                    }
                    for (var ii = 0, length = attrs.length; ii < length; ii++) {
                        normalizedAttrs.push(this._normalizeSingleAttrTo(attrs[ii]));
                    }

                    return normalizedAttrs;
                },

                /*
                 * Normalize attributes for 'from' side using the normalized attributes from 'to' side @param normalizedAttrsTo - normalized attributes from
                 * 'to' side to be used to compute the 'from' side @return the normalized attributes for 'from' side
                 * 
                 */
                _normalizeAttrsFrom: function(normalizedAttrsTo) {
                    var normalizedAttrsFrom = [];
                    for (var ii = 0, length = normalizedAttrsTo.length; ii < length; ii++) {
                        var normalizedAttrFrom = {};
                        normalizedAttrFrom.to = normalizedAttrsTo[ii].from;
                        normalizedAttrFrom.toValue = normalizedAttrsTo[ii].fromValue;
                        normalizedAttrFrom.from = normalizedAttrsTo[ii].to;
                        normalizedAttrFrom.fromValue = normalizedAttrsTo[ii].toValue;
                        normalizedAttrFrom.action = attributeTransformerFactory.getAttributeTransformerName(normalizedAttrFrom);
                        normalizedAttrsFrom.push(normalizedAttrFrom);
                    }
                    return normalizedAttrsFrom;

                },
                /*
                 * Set the parent for given rawConfig/s @param rawConfig - given raw config
                 * 
                 */
                _setParentOnRawConfig: function(rawConfig, parent) {
                    if (rawConfig) {
                        if (LODASH.isArray(rawConfig)) {
                            for (var ii = 0, length = rawConfig.length; ii < length; ii++) {
                                rawConfig[ii].parent = parent;
                            }
                        } else {
                            rawConfig.parent = parent;
                        }
                    }

                }
            });

    return ConfigNormalizerStamp;

});
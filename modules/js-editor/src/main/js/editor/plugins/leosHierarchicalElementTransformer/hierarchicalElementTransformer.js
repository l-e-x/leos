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
define(function hierarchicalElementTransformer(require) {
    "use strict";
    var STAMPIT = require("stampit");
    var LOG = require("logger");

    var INLINE_FROM_MATCH = /^(text|span|strong|em|u|sup|sub|br|a|img)$/;

    /*
     * Create content elements with wrapping element(e.g.: alinea, subparagraph)  
     */
    function createContentWrapper(element, rootPath, wrapper) {
        var inlineGroup = [];
        var that = this;
        element.children.forEach(function(childElement) {
            var elementName = that._getElementName(childElement);
            var wrapperId = "data-akn-" + wrapper + "-id";
            // KLUG: temporarily fix for in-line elements in text node
            if (elementName === "p") {
                that.mapToChildProducts(childElement, {
                    toPath: rootPath,
                    toChild: wrapper,
                    attrs: [{
                        from: wrapperId,
                        to: "id",
                        action: "passAttributeTransformer"
                    }]
                });
                createContent.call(that, childElement, [rootPath, wrapper].join("/"), childElement.children);
            } else if (INLINE_FROM_MATCH.test(elementName)) {
                inlineGroup.push(childElement);
                var nextElementName = childElement.next ? that._getElementName(childElement.next) : null;
                if (!nextElementName || !INLINE_FROM_MATCH.test(nextElementName)) {
                    that.mapToChildProducts(childElement.parent, {
                        toPath: rootPath,
                        toChild: wrapper,
                        attrs: [{
                            from: wrapperId,
                            to: "id",
                            action: "passAttributeTransformer"
                        }]
                    });
                    createContent.call(that, childElement.parent, [rootPath, wrapper].join("/"), inlineGroup);
                    inlineGroup = [];
                }
            } else {
                // for other elements eg.: table, etc
                that.mapToNestedChildProduct(childElement, {
                    toPath: rootPath
                });
            }
        });
    }

    /*
     * Create content elements without wrapping element(e.g.: alinea, subparagraph)  
     */
    function createContentDirectly(element, rootPath) {
        var inlineGroup = [];
        var that = this;
        element.children.forEach(function(childElement) {
            var childElementName = that._getElementName(childElement);
            if (INLINE_FROM_MATCH.test(childElementName)) {
                inlineGroup.push(childElement);
                var nextElementName = childElement.next ? that._getElementName(childElement.next) : null;
                if (!nextElementName || !INLINE_FROM_MATCH.test(nextElementName)) {
                    createContent.call(that, childElement.parent, rootPath, inlineGroup);
                    inlineGroup = [];
                }
            } else if (childElementName === "p") {
                createContent.call(that, childElement, rootPath, childElement.children);
            } else {
                that.mapToNestedChildProduct(childElement, {
                    toPath: rootPath
                });
            }
        });
    }

    function createContent(element, rootPath, contentChildren) {
        this.mapToChildProducts(element, {
            toPath: rootPath,
            toChild: "content",
            attrs: [{
                from: "data-akn-content-id",
                to: "id",
                action: "passAttributeTransformer"
            }]
        });
        var contentPath = rootPath + "/content";
        this.mapToChildProducts(element, {
            toPath: contentPath,
            toChild: "mp",
            attrs: [{
                from: "data-akn-mp-id",
                to: "id",
                action: "passAttributeTransformer"
            }]
        });
        createContentChildren.call(this, element, contentPath + "/mp", contentChildren);
    }

    function shouldContentBeWrapped(element) {
        var that = this;
        var numOfNonSpaceElements = 0;
        var childElement;
        var isBlockElementPresent = false;
        for (var ii = 0; ii < element.children.length; ii++) {
            childElement = element.children[ii];
            var childElementName = this._getElementName(childElement);
            if (childElementName === "text" && childElement.value.trim() === "") {
                continue;
            } else {
                numOfNonSpaceElements++;
            }
            if (!INLINE_FROM_MATCH.test(childElementName)) {
                isBlockElementPresent = true;
            }
            if (numOfNonSpaceElements > 1 && isBlockElementPresent) {
                return true;
            }
        }
        return false;
    };

    var getElementName = function getElementName(element) {
        var elementName = null;
        if (element instanceof CKEDITOR.htmlParser.element) {
            elementName = element.name;
        } else if (element instanceof CKEDITOR.htmlParser.text) {
            elementName = "text";

        }
        return elementName;
    };

    var getElementPath = function getElementPath(element, fragment) {
        var hierarchicalPartsOfPath = [];
        var currentElement = element;
        do {
            hierarchicalPartsOfPath.unshift(getElementName(element).toLowerCase());
            if (currentElement === fragment) {
                break;
            } else {
                currentElement = currentElement.parent;
            }
            // check if the path is calculated for current element's parent if yes use it
            if (currentElement.hierarchicalPartsOfPath) {
                hierarchicalPartsOfPath = currentElement.hierarchicalPartsOfPath.concat(hierarchicalPartsOfPath);
                break;
            }
        } while (currentElement !== fragment);
        element.hierarchicalPartsOfPath = hierarchicalPartsOfPath;
        return hierarchicalPartsOfPath.join("/");
    }

    function createContentChildren(element, rootPath, subparagraphsChildren) {
        var that = this;
        subparagraphsChildren.forEach(function(childElement) {
            var childElementName = that._getElementName(childElement);
            if (childElementName === "text") {
                that.mapToChildProducts(element, {
                    toPath: rootPath,
                    toChild: "text",
                    toChildTextValue: childElement.value
                });
            } else {
                that.mapToNestedChildProduct(childElement, {
                    toPath: rootPath
                });
            }

        });
    }

    function containsPath(element, pathToBeMatched) {
        var matched = false;
        element.forEach(function(el) {
            var path = getElementPath(el, element);
            if (pathToBeMatched === path) {
                matched = true;
                return false;
            }
        });
        return matched;
    }

    var anchor = function(content) {
        return "^" + content + "$";
    };

    var hierarchicalElementTransformerStamp = STAMPIT().enclose(
            //executed on the instance creation
            function init() {
                var rootElementsForFrom = this.rootElementsForFrom;
                var contentWrapperForFrom = this.contentWrapperForFrom;
                var rootElementsForTo = this.rootElementsForTo;
                // Regular expression section
                var PSR = "\/";
                var rootElementsForFromRegExpString = rootElementsForFrom.join(PSR);
                var rootElementsForFromRegExp = new RegExp(anchor(rootElementsForFromRegExpString));
                var rootElementsWithNumForFromRegExp = new RegExp(anchor(rootElementsForFrom.concat(["num"]).join(PSR)));
                var rootElementsWithNumAndTextForFromRegExp = new RegExp(anchor(rootElementsForFrom.concat(["num", "text"]).join(PSR)));
                var rootElementsWithContentForFromRegExp = new RegExp(anchor(rootElementsForFrom.concat(["content"]).join(PSR)));
                var rootElementsWithContentAndMpForFromRegExp = new RegExp(anchor(rootElementsForFrom.concat(["content", "mp"]).join(PSR)));
                // path = paragraph/subparagraph
                var rootElementsWithContentWrapperForFromRegExp = new RegExp(anchor(rootElementsForFrom.concat([contentWrapperForFrom]).join(PSR)));
                //path = paragraph/subparagraph/content
                var rootElementsWithContentWrapperAndContentForFromRegExp = new RegExp(anchor([rootElementsForFromRegExpString, "\/(", contentWrapperForFrom,
                        "\/)?content"].join("")));
                //path = paragraph/subparagraph/content/mp
                var rootElementsWithContentWrapperAndContentAndMpForFromRegExp = new RegExp(anchor([rootElementsForFromRegExpString, "\/(", contentWrapperForFrom,
                        "\/)?content\/mp"].join("")));
                //path = paragraph/subparagraph/content/mp/text
                var rootElementsWithContentWrapperAndTextForFromRegExp = new RegExp(anchor([rootElementsForFromRegExpString, "\/(", contentWrapperForFrom,
                        "\/)?content\/mp\/text"].join("")));
                //path = paragraph/subparagraph/content/mp/? (anything except text)
                var rootElementsWithContentWrapperAndNestedForFromRegExp = new RegExp(anchor([rootElementsForFromRegExpString, "\/(", contentWrapperForFrom,
                        "\/)?content\/mp\/((?!text).)+"].join("")));
                //path = paragraph/? (anything after paragraph e.g. list except text|num|subparagraph|content)
                var rootElementsWithNestedElementForFromRegExp = new RegExp(anchor([rootElementsForFromRegExpString, "\/((?!text|num|", contentWrapperForFrom,
                        "|content).)+"].join("")));

                var rootElementsWithContentWrapperForToRegExp = new RegExp(anchor(rootElementsForTo.join(PSR)));
                // <= end of regular expression section
                //path section
                var rootsElementsPathForTo = rootElementsForTo.join("/");
                var rootsElementsWithPPathForTo = [rootsElementsPathForTo, "p"].join("/");
                var rootsElementsPathForFrom = rootElementsForFrom.join("/");
                var rootsElementsWithNumPathForFrom = [rootsElementsPathForFrom, "num"].join("/");
                var rootsElementsWithContentPathForFrom = [rootsElementsPathForFrom, "content"].join("/");
                var rootsElementsWithContentAndMpPathForFrom = [rootsElementsWithContentPathForFrom, "mp"].join("/");
                var rootsElementsWithNumAndTextPathForFrom = [rootsElementsWithNumPathForFrom, "text"].join("/");
                // <=end of path section 

                //content wrapper id (for e.g. data-akn-subparagraph-id)
                var contentWrapperId = "data-akn-" + contentWrapperForFrom + "-id";
                
                var transformationConfig = {
                    akn: this.firstLevelConfig.akn,
                    html: this.firstLevelConfig.html,
                    attr: this.firstLevelConfig.attr,
                    transformer: {
                        to: {
                            action: function(element) {
                                var path = element.transformationContext.elementPath;
                                this._.isContentWrapperPresent = this._.isContentWrapperPresent === undefined ? false : this._.isContentWrapperPresent;

                                if (rootElementsForFromRegExp.test(path)) {
                                    //For rootElement with 2 elements e.g. rootElementsForFrom : [ "list", "point" ]
                                    if (rootElementsForTo.length === 2) {
                                        this.mapToChildProducts(element, {
                                            toPath: rootElementsForTo[0],
                                            toChild: rootElementsForTo[1],
                                            attrs: [{
                                                from: "id",
                                                to: "id",
                                                action: "passAttributeTransformer"
                                            }]
                                        });
                                        this._.isContentWrapperPresent = false;
                                    } else if (rootElementsForTo.length > 2) {
                                        LOG.error("Invalid rootElement configuration found: '", rootElementsForTo);
                                    }
                                } else if(rootElementsWithNumForFromRegExp.test(path)) {
                                    this.mapToProducts(element, {
                                        toPath: rootsElementsPathForTo,
                                        attrs: [{
                                            from: "id",
                                            to: "data-akn-num-id",
                                            action: "passAttributeTransformer"
                                        }]
                                    });
                                } else if (rootElementsWithNumAndTextForFromRegExp.test(path)) {
                                    this.mapToProducts(element, {
                                        toPath: rootsElementsPathForTo,
                                        toAttribute: "data-akn-num"
                                    });
                                } else if(rootElementsWithContentForFromRegExp.test(path)) {
                                    this.mapToProducts(element, {
                                        toPath: rootsElementsPathForTo,
                                        attrs: [{
                                            from: "id",
                                            to: "data-akn-content-id",
                                            action: "passAttributeTransformer"
                                        }]
                                    });
                                } else if(rootElementsWithContentAndMpForFromRegExp.test(path)) {
                                    this.mapToProducts(element, {
                                        toPath: rootsElementsPathForTo,
                                        attrs: [{
                                            from: "id",
                                            to: "data-akn-mp-id",
                                            action: "passAttributeTransformer"
                                        }]
                                    });
                                }
                                else if (rootElementsWithContentWrapperForFromRegExp.test(path)) {
                                    this.mapToChildProducts(element, {
                                        toPath: rootsElementsPathForTo,
                                        toChild: "p",
                                        attrs: [{
                                            from: "id",
                                            to: contentWrapperId,
                                            action: "passAttributeTransformer"
                                        }]
                                    });
                                    this._.isContentWrapperPresent = true;
                                } else if(rootElementsWithContentWrapperAndContentForFromRegExp.test(path)) {
                                    this.mapToProducts(element, {
                                        toPath: rootsElementsWithPPathForTo,
                                        attrs: [{
                                            from: "id",
                                            to: "data-akn-content-id",
                                            action: "passAttributeTransformer"
                                        }]
                                    });
                                } else if(rootElementsWithContentWrapperAndContentAndMpForFromRegExp.test(path)) {
                                    this.mapToProducts(element, {
                                        toPath: rootsElementsWithPPathForTo,
                                        attrs: [{
                                            from: "id",
                                            to: "data-akn-mp-id",
                                            action: "passAttributeTransformer"
                                        }]
                                    });
                                    
                                } else if (rootElementsWithContentWrapperAndTextForFromRegExp.test(path)) {
                                    var toPath = this._.isContentWrapperPresent ? rootsElementsWithPPathForTo : rootsElementsPathForTo;
                                    this.mapToChildProducts(element, {
                                        toPath: toPath,
                                        toChild: "text",
                                        toChildTextValue: element.value
                                    });
                                } else if (rootElementsWithContentWrapperAndNestedForFromRegExp.test(path)) {
                                    var toPath = this._.isContentWrapperPresent ? rootsElementsWithPPathForTo : rootsElementsPathForTo;
                                    this.mapToNestedChildProduct(element, {
                                        toPath: toPath
                                    });
                                } else if (rootElementsWithNestedElementForFromRegExp.test(path)) {
                                    this.mapToNestedChildProduct(element, {
                                        toPath: rootsElementsPathForTo
                                    });
                                }
                            },
                            supports: function supports(element) {
                                return containsPath(element, rootsElementsPathForFrom);
                            }
                        },

                        from: {
                            action: function action(element) {
                                var path = element.transformationContext.elementPath;
                                if (rootElementsWithContentWrapperForToRegExp.test(path)) {
                                    this.mapToProducts(element, [{
                                        toPath: rootsElementsPathForFrom,
                                        attrs: [{
                                            from: "id",
                                            to: "id",
                                            action: "passAttributeTransformer"
                                        }]
                                    }, {
                                        toPath: rootsElementsWithNumPathForFrom,
                                        attrs: [{
                                            from: "data-akn-num-id",
                                            to: "id",
                                            action: "passAttributeTransformer"
                                        }]
                                    }, {
                                        toPath: rootsElementsWithNumAndTextPathForFrom,
                                        fromAttribute: "data-akn-num"
                                    }]);
                                    var isContentWrapperPresent = shouldContentBeWrapped.call(this, element);
                                    if (isContentWrapperPresent) {
                                        createContentWrapper.call(this, element, rootsElementsPathForFrom, contentWrapperForFrom);
                                    } else {
                                        createContentDirectly.call(this, element, rootsElementsPathForFrom)
                                    }
                                }
                            },
                            supports: function supports(element) {
                                return containsPath(element, rootsElementsPathForTo);
                            }
                        }

                    }
                };

                this.getTransformationConfig = function() {
                    return transformationConfig;
                };
            });

    return hierarchicalElementTransformerStamp;
});
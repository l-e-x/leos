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
define(function brTransformationConfig(require) {
    "use strict";

    var PARAGRAPH_NAME = "paragraph";
    var PARAGRAPH_TO_MATCH = /^paragraph$/;
    var PARAGRAPH_NUM_TEXT_TO_MATCH = /^paragraph\/num\/text$/;
    var PARAGRAPH_SUBPARAGRAPH_TO_MATCH = /^paragraph\/subparagraph$/;
    var PARAGRAPH_SUBPARAGRAPH_TEXT_TO_MATCH = /^paragraph\/(subparagraph\/)?content\/mp\/text$/;
    var PARAGRAPH_SUBPARAGRAPH_NESTED_ELEMENT_TO_MATCH = /^paragraph\/(subparagraph\/)?content\/mp\/((?!text).)+$/;
    var PARAGRAPH_NESTED_ELEMENT_TO_MATCH = /^paragraph\/((?!text|num|subparagraph|content).)+$/;
    var PARAGRAPH_FROM_MATCH = /^li$/;
    var INLINE_FROM_MATCH = /^text|span|strong|em|u|sup|sub|a$/;

    var transformationConfig = {
        akn : PARAGRAPH_NAME,
        html : 'li',
        attr : [ {
            akn : "leos:editable",
            html : "contenteditable"
        }, {
            akn : "id",
            html : "id"
        }, {
            html : "data-akn-name=aknNumberedParagraph"
        } ],
        transformer : {
            to : function(element) {
                var path = element.transformationContext.elementPath;
                this._.subparagraphNum = this._.subparagraphNum || 0;
                if (PARAGRAPH_TO_MATCH.test(path)) {
                    this.mapToProducts(element, {
                        toPath : "li"
                    });
                } else if (PARAGRAPH_NUM_TEXT_TO_MATCH.test(path)) {
                    this.mapToProducts(element, {
                        toPath : "li",
                        toAttribute : "data-akn-num"
                    });
                } else if (PARAGRAPH_SUBPARAGRAPH_TO_MATCH.test(path)) {
                    if (this._.subparagraphNum > 0) {
                        this.mapToChildProducts(element, {
                            toPath : "li",
                            toChild : "br",
                        });
                    }
                    this._.subparagraphNum++;
                } else if (PARAGRAPH_SUBPARAGRAPH_TEXT_TO_MATCH.test(path)) {
                    this.mapToChildProducts(element, {
                        toPath : "li",
                        toChild : "text",
                        toChildTextValue : element.value
                    });
                } else if (PARAGRAPH_SUBPARAGRAPH_NESTED_ELEMENT_TO_MATCH.test(path) || PARAGRAPH_NESTED_ELEMENT_TO_MATCH.test(path)) {
                    this.mapToNestedChildProduct(element, {
                        toPath : "li"
                    });
                } 
            },
            from : function(element) {
                var path = element.transformationContext.elementPath;
                if (PARAGRAPH_FROM_MATCH.test(path)) {
                    this.mapToProducts(element, {
                        toPath : "paragraph/num",
                    });

                    this.mapToProducts(element, {
                        toPath : "paragraph/num/text",
                        fromAttribute : "data-akn-num"
                    });

                    var isSubparagraphPresent = isSubparagraphPresentForFrom.call(this, element);
                    var that = this;
                    if (isSubparagraphPresent) {
                        createSubparagraphsForFrom.call(this, element);
                    } else {
                        createParagraphForFrom.call(this, element);
                    }
                }
            }
        }

    };

    function isSubparagraphPresentForFrom(element) {
        var that = this;
        var numOfParagraphBreaks = 0;
        var childElement, childElementName;
        var inlineElementPresentAfterParagraphBreak = false;
        for (var ii = 0; ii < element.children.length; ii++) {
            childElement = element.children[ii];
            childElementName = childElement.transformationContext.elementName;
            if (this._isWhitespaceElement(childElement)) {
                continue;
            }
            if (childElementName === "br"||!INLINE_FROM_MATCH.test(childElementName)) {
                numOfParagraphBreaks++;
            }

            if (numOfParagraphBreaks > 0) {
                    return true;
            }
        }
        return false;
    }

    
    function createSubparagraphsForFrom(element) {
        var inlineGroup = [];
        var that = this;
        element.children.forEach(function(childElement) {
            var elementName = childElement.transformationContext.elementName;
           if (INLINE_FROM_MATCH.test(elementName)) {
                inlineGroup.push(childElement);
                var nextElementName = childElement.next ? that._getElementName(childElement.next) : null;
                if (!nextElementName || nextElementName==="br" || !INLINE_FROM_MATCH.test(nextElementName)) {
                    that.mapToChildProducts(childElement.parent, {
                        toPath : "paragraph",
                        toChild : "subparagraph"
                    });
                    createContent.call(that, childElement.parent, "paragraph/subparagraph", inlineGroup);
                    inlineGroup = [];
                }
            } else {
                // for other elements eg.: table, etc
                that.mapToNestedChildProduct(childElement, {
                    toPath : "paragraph"
                });
            }
        });
    }
    
    
    function createParagraphForFrom(element) {
        var inlineGroup = [];
        var that = this;
        element.children.forEach(function(childElement) {
            var elementName = childElement.transformationContext.elementName;
            if (INLINE_FROM_MATCH.test(elementName)) {
                inlineGroup.push(childElement);
                var nextElementName = childElement.next ? that._getElementName(childElement.next) : null;
                if (!nextElementName || nextElementName==="br" || !INLINE_FROM_MATCH.test(nextElementName)) {
                    createContent.call(that, childElement.parent, "paragraph", inlineGroup);
                    inlineGroup = [];
                }
            }  else {
                that.mapToNestedChildProduct(childElement, {
                    toPath : "paragraph"
                });
            }
        });
    }
    
    function createContent(element, rootPath, contentChildren) {
        this.mapToChildProducts(element, {
            toPath : rootPath,
            toChild : "content"
        });
        var contentPath = rootPath + "/content";
        this.mapToChildProducts(element, {
            toPath : contentPath,
            toChild : "mp"
        });
        createContentChildren.call(this, element, contentPath + "/mp", contentChildren);
    }

    function createContentChildren(element, rootPath, subparagraphsChildren) {
        var that = this;
        subparagraphsChildren.forEach(function(childElement) {
            var childElementName = that._getElementName(childElement);
            if (childElementName === "text") {
                that.mapToChildProducts(element, {
                    toPath : rootPath,
                    toChild : "text",
                    toChildTextValue : childElement.value
                });
            } else {
                that.mapToNestedChildProduct(childElement, {
                    toPath : rootPath
                });
            }

        });
    }
    
    // return plugin module
    var pluginModule = {
        transformationConfig : transformationConfig
    };

    return pluginModule;
});
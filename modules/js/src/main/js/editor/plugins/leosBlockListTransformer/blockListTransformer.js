/*
 * Copyright 2017 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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
define(function blockListTransformer(require) {
    "use strict";
    var STAMPIT = require("stampit");
    var LOG = require("logger");
    
    var DATA_AKN_NUM = "data-akn-num";
    var DATA_AKN_NUM_ID = "data-akn-num-id";
    var DATA_AKN_MP_ID = "data-akn-mp-id";

    // Checks if an element is a text element or an inline element
    function _isTextOrInlineElement(element) {
        var elementName = _getElementName(element);
        return ((elementName === 'text') || CKEDITOR.dtd.$inline.hasOwnProperty(elementName));
    }

    // Parse all the children of a cell (td,th or caption) and map it directly to the Akn cell if these are NOT inline elements
    // All other inline elements should be mapped into Akn p elements
    function _createNestedChildrenFromHtmlToAkn(element, rootPath) {
        var that = this;
        var children = element.children;

        // If they are text or inline elements, they should be included in an Akn p element
        // This p element should be added only once
        if (children.some(_isTextOrInlineElement, that)) {
            that.mapToChildProducts(element, {
                toPath: rootPath,
                toChild: 'mp',
                attrs: [{
                    from: DATA_AKN_MP_ID,
                    to: "GUID",
                    action: "passAttributeTransformer"
                }]
            });
        }

        children.forEach(function(childElement) {
            var childElementName = _getElementName(childElement);

            // If there are text elements, they should be included in an Akn p element
            if (childElementName === 'text') {
                that.mapToChildProducts(element, {
                    toPath: rootPath + '/mp',
                    toChild: 'text',
                    toChildTextValue: childElement.value
                });
                // If they are inline elements, they should be included in an Akn p element
            } else if (CKEDITOR.dtd.$inline.hasOwnProperty(childElementName)) {
                that.mapToNestedChildProduct(childElement, {
                    toPath: rootPath + '/mp'
                });
                // Other elements are put in the akn rootPath
            } else {
                that.mapToNestedChildProduct(childElement, {
                    toPath: rootPath
                });
            }
        });
    }

    function _getElementName(element) {
        var elementName = null;
        if (element instanceof CKEDITOR.htmlParser.element) {
            elementName = element.name;
        } else if (element instanceof CKEDITOR.htmlParser.text) {
            elementName = "text";

        }
        return elementName;
    };

    var anchor = function(content) {
        return "^" + content + "$";
    };

    var blockListTransformerStamp = STAMPIT().enclose(
            // executed on the instance creation
            function init() {
                var rootElementsForAkn = this.rootElementsForAkn;
                var rootElementsForHtml = this.rootElementsForHtml;
                
                // Regular expression section
                var PSR = "\/";
                //Html side
                var rootElementsForHtmlRegExp = new RegExp(anchor(rootElementsForHtml.join(PSR)));
                //Akn side
                var rootElementsForAknRegExpString = rootElementsForAkn.join(PSR);
                var rootElementsForAknRegExp = new RegExp(anchor(rootElementsForAknRegExpString));
                var rootElementsWithNumForAknRegExp = new RegExp(anchor(rootElementsForAkn.concat(["num"]).join(PSR)));
                var rootElementsWithNumAndTextForAknRegExp = new RegExp(anchor(rootElementsForAkn.concat(["num", "text"]).join(PSR)));
                var rootElementsWithMpForAknRegExp = new RegExp(anchor(rootElementsForAkn.concat(["mp"]).join(PSR)));
                var rootElementsWithMpAndTextForAknRegExp = new RegExp(anchor(rootElementsForAkn.concat(["mp/text"]).join(PSR)));
                //path = blockList/item/? (anything except text|num) - for e.g. nested list
                var rootElementsWithNestedElementForAknRegExp = new RegExp(anchor([rootElementsForAknRegExpString, "\/((?!text|num).)+"].join("")));
                // <= end of regular expression section
                
                // path section
                var rootElementsPathForHtml = rootElementsForHtml.join("/");
                var rootElementsPathForAkn = rootElementsForAkn.join("/");
                var rootElementsWithNumPathForAkn = [rootElementsPathForAkn, "num"].join("/");
                var rootElementsWithNumAndTextPathForAkn = [rootElementsWithNumPathForAkn, "text"].join("/");
                // <=end of path section

                var transformationConfig = {
                    akn: this.blockListTransformationConfig.akn,
                    html: this.blockListTransformationConfig.html,
                    attr: this.blockListTransformationConfig.attr,
                    transformer: {
                        to: {
                            action: function action(element) {
                                var path = element.transformationContext.elementPath;
                                if (rootElementsForAknRegExp.test(path)) {
                                    this.mapToProducts(element, [{
                                        toPath: rootElementsPathForHtml,
                                        attrs: [{
                                            from: "GUID",
                                            to: "id",
                                            action: "passAttributeTransformer"
                                        }]
                                    }]);
                                } else if(rootElementsWithNumForAknRegExp.test(path)) {
                                    this.mapToProducts(element, {
                                        toPath: rootElementsPathForHtml,
                                        attrs: [{
                                            from: "GUID",
                                            to: DATA_AKN_NUM_ID,
                                            action: "passAttributeTransformer"
                                        }]
                                    });
                                } else if (rootElementsWithNumAndTextForAknRegExp.test(path)) {
                                    this.mapToProducts(element, {
                                        toPath: rootElementsPathForHtml,
                                        toAttribute: DATA_AKN_NUM
                                    });
                                } else if(rootElementsWithMpForAknRegExp.test(path)) {
                                    this.mapToProducts(element, {
                                        toPath: rootElementsPathForHtml,
                                        attrs: [{
                                            from: "GUID",
                                            to: DATA_AKN_MP_ID,
                                            action: "passAttributeTransformer"
                                        }]
                                    });
                                } else if (rootElementsWithMpAndTextForAknRegExp.test(path)) {
                                    this.mapToChildProducts(element, {
                                        toPath: rootElementsPathForHtml,
                                        toChild: "text",
                                        toChildTextValue: element.value
                                    });
                                } else if (rootElementsWithNestedElementForAknRegExp.test(path)) {
                                    this.mapToNestedChildProduct(element, {
                                        toPath: rootElementsPathForHtml
                                    });
                                }
                            }
                        },
                        from: {
                            action: function action(element) {
                                var path = element.transformationContext.elementPath;
                                if (rootElementsForHtmlRegExp.test(path)) {
                                    this.mapToProducts(element, [{
                                        toPath: rootElementsPathForAkn,
                                        attrs: [{
                                            from: "id",
                                            to: "GUID",
                                            action: "passAttributeTransformer"
                                        }]
                                    }, {
                                        toPath: rootElementsWithNumPathForAkn,
                                        attrs: [{
                                            from: DATA_AKN_NUM_ID,
                                            to: "GUID",
                                            action: "passAttributeTransformer"
                                        }]
                                    }, {
                                        toPath: rootElementsWithNumAndTextPathForAkn,
                                        fromAttribute: DATA_AKN_NUM
                                    }]);
                                    _createNestedChildrenFromHtmlToAkn.call(this, element, rootElementsPathForAkn);
                                }
                            }
                        }
                    }
                };

                this.getTransformationConfig = function() {
                    return transformationConfig;
                };
            });

    return blockListTransformerStamp;
});
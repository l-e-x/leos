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
define(function attributeTransformerModule(require) {
    "use strict";
    var LODASH = require("lodash");

    var attributeTransformers = {
        /* Decorates element with class attr. */
        "addClassAttributeTransformer": {
            isSupported: function(normAttr) {
                if (!!normAttr.to) {
                    // valid for {html:"class=leos"} or {akn:"class=leos", html:"class=leos"}
                    if (normAttr.to === "class" && (normAttr.from === "class" || !normAttr.from)) {
                        return true;
                    }
                }
                return false;
            },
            perform: function() {
                var classValue = this.toElement.attributes[this.attrConfig.to];
                if (!classValue) {
                    classValue = "";
                } 
                // handle the class name attribute at run-time. for eg: class=leos-highlight* (where * is calculated at run-time)
                if (this.attrConfig.toValue) {
                    var toValueArray = this.attrConfig.toValue.split(" ");
                    var currentClassValueArray = [];
                    if (this.fromElement.attributes && this.fromElement.attributes[this.attrConfig.to]) {
                        currentClassValueArray = this.fromElement.attributes[this.attrConfig.to].split(" ")
                    }
                    for (var j = 0; j < toValueArray.length; j++) {
                        var toValueClass = toValueArray[j];
                        var toValueRegex = new RegExp(toValueClass);
                        var wasMatched = false;
                        for (var i = 0; i < currentClassValueArray.length; i++) {
                            var currentClassValue = currentClassValueArray[i];
                            if (toValueClass.indexOf("*") != -1 && toValueRegex.test(currentClassValue)) {
                                classValue = classValue + " " + currentClassValue;
                                wasMatched = true;
                            }
                        }
                        if (!wasMatched) {
                            classValue = classValue + " " +  toValueClass;
                        }
                    }
                }
                this.toElement.attributes[this.attrConfig.to] = classValue.trim();
            }
        },

        /*
         * Pass attribute value for element. It could be to the same attribute e.g: attr1=value => attr1=value or to different one: attr1=value => attr2 = value
         */
        "passAttributeTransformer": {
            isSupported: function(normAttr) {
                return (normAttr.to && normAttr.from && !attributeTransformers['handleNonEditable'].isSupported(normAttr)
                        && !attributeTransformers['addClassAttributeTransformer'].isSupported(normAttr));
            },
            perform: function() {
                if (this.fromElement.attributes[this.attrConfig.from]) {
                    this.toElement.attributes[this.attrConfig.to] = this.fromElement.attributes[this.attrConfig.from];
                } else {
                    if (this.attrConfig.toValue) {
                        this.toElement.attributes[this.attrConfig.to] = this.attrConfig.toValue;
                    }
                }
            }
        },

        /* Handle the Non-Editable attribute to remove if contenteditable=true */
        "handleNonEditable": {
            isSupported: function(normAttr) {
                return (normAttr.to && (normAttr.from === "contenteditable" || normAttr.from === "editable"));
            },
            perform: function() {
                attributeTransformers['passAttributeTransformer'].perform.call(this);
                if (this.toElement.attributes[this.attrConfig.to] === "true") {
                    delete this.toElement.attributes[this.attrConfig.to];
                }
            }
        },

        /* Decorates element with attr, except class and style. */
        "addAttributeTransformer": {
            isSupported: function(normAttr) {
                if (attributeTransformers['addClassAttributeTransformer'].isSupported(normAttr)) {
                    return false;
                }
                if (normAttr.to) {
                    if (!normAttr.from) {
                        return true;
                    }
                }
                return false;
            },
            perform: function() {
                this.toElement.attributes[this.attrConfig.to] = this.attrConfig.toValue;
            }
        }
    };

    var attributeTransformerFactory = {

        getAttributeTransformerName: function getAttributeTransformerName(normAttr) {
            var transformerName = [];

            LODASH.forEach(attributeTransformers, function(attributeTransformer, attributeTransformerName) {
                if (attributeTransformer.isSupported(normAttr)) {
                    transformerName.push(attributeTransformerName);
                }
            });

            if (transformerName.length > 1) {
                var errorMessage = ["Not able to find unique attr strategy for:", JSON.stringify(normAttr)].join("");
                throw new Error(errorMessage);
            }

            return transformerName[0];
        },

        getAttributeTransformer: function getAttributeTransformerName(transformerName) {
            return attributeTransformers[transformerName];
        }
    };

    return attributeTransformerFactory;
});
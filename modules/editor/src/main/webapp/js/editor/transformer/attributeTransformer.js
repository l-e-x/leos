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
define(function attributeTransformerModule(require) {
    "use strict";
    var LODASH = require("lodash");

    var attributeTransformers = {
        /* Decorates element with class attr. */
        "addClassAttributeTransformer": {
            isSupported: function(normAttr) {
                if (!!normAttr.to) {
                    if (normAttr.to === "class") {
                        return true;
                    }
                }
                return false;
            },
            perform: function() {
                var classValue = this.toElement.attributes[this.attrConfig.to];
                if (!classValue) {
                    classValue = "";
                } else if (classValue !== "") {
                    classValue += " ";
                }
                classValue = classValue + this.attrConfig.toValue;
                this.toElement.attributes['class'] = classValue;
            }
        },

        /*
         * Pass attribute value for element. It could be to the same attribute e.g: attr1=value => attr1=value or to different one: attr1=value => attr2 = value
         */
        "passAttributeTransformer": {
            isSupported: function(normAttr) {
                if (normAttr.to && normAttr.from && !attributeTransformers['handleNonEditable'].isSupported(normAttr)
                        && !attributeTransformers['addClassAttributeTransformer'].isSupported(normAttr)) {
                    return true;
                }
                return false;
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
                if (normAttr.to && (normAttr.from === "contenteditable" || normAttr.from === "editable")) {
                    return true;
                }
                return false;
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
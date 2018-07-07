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
define(function transformerModule(require) {
    "use strict";
    var STAMPIT = require("stampit");
    var LOG = require("logger");
    var LODASH = require("lodash");
    var fragmentTransformerStamp = require("transformer/fragmentTransformer");

    /*
     * This stamp is responsible for transformation of the bulk of the fragments which constitutes the fragment passed to this instance. Particular fragments are
     * transformed by separate tragmentTransformer instances.
     * The result of the transformation is written to the fragment parameter rather than returning brand new product, because fragment needs to be linked to the CKEditor container.
     * 
     */
    var transformerStamp = STAMPIT().methods({
        /*
         * @param fragment - required parameter which specifies fragment to be transformed
         * 
         * @param transformationConfigResolver - required parameter which specifies how transformation needs to be performed e.g: [ { from: "mp", fromPath: "mp", to:
         * "p", toPath: "p" },{ from: "text", fromPath: "mp/text", to: "text", toPath: "p/text" } ]
         * 
         * @param direction - required parameter which takes either of two values: 'to' and 'from' @return the result is written to the the params.fragment
         * itself i.e.: any generated product replace the element which needs to be transformed according to the transformation config
         * 
         */
        transform: function transform(params) {
            if(this._isCKEditorWidget(params.fragment)) {
                return;
            }
            this._initPrivate();
            this._validateRequired("fragment", params);
            this._validateRequired("direction", params);
            this._.direction = params.direction;
            this._validateRequired("transformationConfigResolver", params);
            this._.transformationConfigResolver = params.transformationConfigResolver;
            var bindedTransformElement = LODASH.bind(this._transformElement, this);
            params.fragment.forEach(bindedTransformElement);

        },
        _isCKEditorWidget : function _isCKEditorWidget(fragment) {
            if(!fragment||!fragment.children) {
                return false;
            }
            var rootElement = fragment.children[0];
            return (rootElement.hasClass && rootElement.hasClass("cke_widget_wrapper"));
        },
        _getFragmentTransformer: function _getFragmentTransformer() {
            var fragmentTransformer = fragmentTransformerStamp();
            return fragmentTransformer;
        },

        /*
         * For given element the transformation config is retrieved and the transformation is performed accordingly. Otherwise element is skipped, it is passed
         * as it is. @param element - element to be transformed
         * 
         * 
         */
        _transformElement: function _transformElement(element) {
            var product = null;
            product = this._getFragmentTransformer().getTransformedElement({
                fragment: element,
                direction:this._.direction,
                transformationConfigResolver: this._.transformationConfigResolver
            });
            if (product) {
                element.replaceWith(product);
                // return false so that forEach iterator won't iterate over its children
                return false;
            }
            return true;
        },
        _validateRequired: function _validateRequired(paramName, params) {
            if (!params || !params[paramName]) {
                var errorMessage = ["Param with name: '", paramName, "' is required, please provide it."].join("");
                throw new Error(errorMessage);
            }

        }

        ,
        _initPrivate: function _initPrivate() {
            if (this._) {
                var errorMessage = "Instance already initialized. Probably method called more than once.";
                LOG.error(errorMessage);
                throw errorMessage;
            } else {
                this._ = {};
            }
        }

    });

    return transformerStamp;
});
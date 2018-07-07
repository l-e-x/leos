; // jshint ignore:line
define(function ElementFactoryModule(require) {
    "use strict";
    var STAMPIT = require("stampit");
    var LODASH = require("lodash");
    var CKEDITOR = require("ckEditor");

    /*
     * Factory helper to ease the effort to create CKEditor elements from simplified class-literal alike form e.g.: p: { attributes: {}, children: [{ text: {
     * value: "fdasfsda } }] }
     */
    var ElementFactory = STAMPIT().enclose(function initElementHelperCreator() {
        this._ = {};
        this._.rawElement = this.element;

    }).methods({

        getElement: function() {
            this._getElementHelper(this._.rawElement);
            return this._.normalizedElement;

        },

        _getElementHelper: function(rawElement, normalizedParentElement) {
            var binded_forEachElementDesc = LODASH.bind(this._forEachElementDesc, this, normalizedParentElement);
            LODASH.forEach(rawElement, binded_forEachElementDesc);

        },
        _getElementForName: function(type) {
            var element;
            if (type === "text") {
                element = new CKEDITOR.htmlParser.text();
            } else {
                element = new CKEDITOR.htmlParser.element(type);
            }
            return element;
        },

        _getElementName: function(element) {
            var elementName = null;
            if (element instanceof CKEDITOR.htmlParser.element) {
                elementName = element.name;
            } else if (element instanceof CKEDITOR.htmlParser.text) {
                elementName = "text";

            }
            return elementName;
        },
        _forEachElementDesc: function(normalizedParentElement, elementRaw, elementName) {
            var normalizedElement = this._getElementForName(elementName);
            if (elementRaw.attributes) {
                normalizedElement.attributes = elementRaw.attributes;
            }
            this._setContentOfNormalizedElement(elementRaw, normalizedElement);
            this.setParentToChildrenRelation(normalizedParentElement, normalizedElement);
            if (!this._.normalizedElement) {
                this._.normalizedElement = normalizedElement;
            }
            var children = elementRaw.children;
            if (children) {
                for (var ii = 0; ii < children.length; ii++) {
                    this._getElementHelper(children[ii], normalizedElement);
                }
            }
        },
        _setContentOfNormalizedElement: function(rawElement, normalizedElement) {
            if (this._getElementName(normalizedElement) === "text") {
                normalizedElement.value = rawElement.value;

            }
        },
        setParentToChildrenRelation: function(normalizedParentElement, normalizedElement) {
            if (normalizedParentElement) {
                normalizedParentElement.children = normalizedParentElement.children || [];
                normalizedParentElement.children.push(normalizedElement);
                normalizedElement.parent = normalizedParentElement;
            }

        }

    });

    return ElementFactory;
});
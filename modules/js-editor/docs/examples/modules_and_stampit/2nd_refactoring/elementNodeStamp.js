; // jshint ignore:line
define(function elementNodeStampModule(require) {
    "use strict";

    var STAMPIT = require("stampit");

    var elementNodeStamp = STAMPIT().enclose(function init() {
        this.TYPES = this.TYPES || {};
        this.TYPES.ELEMENT = "Element";
    }).methods({

        _handleElementType: function _handleElementType(config) {
            return new CKEDITOR.htmlParser.element(config.name || "", config.attributes);
        }
    });

    return elementNodeStamp;
});
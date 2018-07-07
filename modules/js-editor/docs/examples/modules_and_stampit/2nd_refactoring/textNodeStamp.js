; // jshint ignore:line
define(function textNodeStampModule(require) {
    "use strict";

    var STAMPIT = require("stampit");

    var textNodeStamp = STAMPIT().enclose(function init() {
        this.TYPES = this.TYPES || {};
        this.TYPES.TEXT = "Text";
    }).methods({

        _handleTextType: function _handleTextType(config) {
            return new CKEDITOR.htmlParser.text(config.value);
        }
    });

    return textNodeStamp;
});
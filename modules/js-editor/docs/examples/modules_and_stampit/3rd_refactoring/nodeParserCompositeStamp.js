; // jshint ignore:line
define(function nodeParserCompositeStampModule(require) {
    "use strict";

    var STAMPIT = require("stampit");

    var coreNodeStamp = require("editorTestUtils/coreNodeStamp");
    var elementNodeStamp = require("editorTestUtils/elementNodeStamp");
    var textNodeStamp = require("editorTestUtils/textNodeStamp");

    var nodeParserCompositeStamp = STAMPIT().compose(
        coreNodeStamp,
        elementNodeStamp,
        textNodeStamp);

    return nodeParserCompositeStamp;
});
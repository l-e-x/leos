; // jshint ignore:line
define(function fragmentParserStampModule(require) {
    "use strict";

    var STAMPIT = require("stampit");
    // FIXME the global CKEDITOR is being used because the object returned by ckeditor module will hang the loading
    //var CKEDITOR = require("ckeditor");

    /*
    html = '<mp><strong id="bId" class="bClass" style="bStyle">bold text</strong>normal text</mp>';
    */

    var fragmentParserStamp = STAMPIT().methods({

        parse: function parse(html) {
            return CKEDITOR.htmlParser.fragment.fromHtml(html);
        }
    });

    return fragmentParserStamp;
});
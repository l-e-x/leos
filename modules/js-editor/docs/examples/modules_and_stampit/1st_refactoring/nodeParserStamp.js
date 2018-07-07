; // jshint ignore:line
define(function nodeParserStampModule(require) {
    "use strict";

    var LOG = require("logger");
    var STAMPIT = require("stampit");
    // FIXME the global CKEDITOR is being used because the object returned by ckeditor module will hang the loading
    //var CKEDITOR = require("ckeditor");

    /*
    config = {
        type: "Element",
        name: "mp",
        children: [{
            type: "Element",
            name: "strong",
            attributes: {
                id: "bId",
                "class": "bClass",
                "style": "bStyle"
            },
            children: [{
                type: "Text",
                value: "bold text"
            }]
        }, {
            type: "Text",
            value: "normal text"
        }]
    };
    */

    var nodeParserStamp = STAMPIT().state({
        TYPES: {
            ELEMENT: "Element",
            TEXT: "Text"
        }
    }).methods({

        parse: function parse(config) {
            return this._handleNode(config || {});
        },

        _handleNode: function _handleNode(config, parent) {
            var nodeType = config.type || "Unknown";
            var handleName = "_handle" + nodeType + "Type";
            var handleFunc = (typeof this[handleName] === "function") ? this[handleName] : this._handleUnknownType;

            var node = handleFunc.bind(this)(config, parent);
            this._setRelationship(parent, node);

            if (config.children) {
                config.children.forEach(function _handleChildNode(childConfig) {
                    this._handleNode(childConfig, node)
                }, this);
            }

            return node;
        },

        _handleElementType: function _handleElementType(config, parent) {
            return new CKEDITOR.htmlParser.element(config.name || "", config.attributes);
        },

        _handleTextType: function _handleTextType(config, parent) {
            return new CKEDITOR.htmlParser.text(config.value);
        },

        _handleUnknownType: function _handleUnknownType(config, parent) {
            LOG.warn("Unable to handle unknown type:", config.type);
        },

        _setRelationship: function _setRelationship(parent, child) {
            if (parent && child) {
                parent.add(child);
            }
        }
    });

    return nodeParserStamp;
});
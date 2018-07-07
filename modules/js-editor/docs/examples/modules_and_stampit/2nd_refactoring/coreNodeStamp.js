; // jshint ignore:line
define(function coreNodeStampModule(require) {
    "use strict";

    var LOG = require("logger");
    var STAMPIT = require("stampit");

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

    var coreNodeStamp = STAMPIT().state({
        TYPES: {}
    }).methods({

        parse: function parse(config) {
            return this._handleNode(config || {});
        },

        _handleNode: function _handleNode(config, parent) {
            var node = this._getHandler(config.type)(config);
            this._setRelationship(parent, node);

            if (config.children) {
                config.children.forEach(function _handleChildNode(childConfig) {
                    this._handleNode(childConfig, node)
                }, this);
            }

            return node;
        },

        _handleUnknownType: function _handleUnknownType(config) {
            LOG.warn("Unable to handle unknown type:", config.type);
        },

        _setRelationship: function _setRelationship(parent, child) {
            if (parent && child) {
                parent.add(child);
            }
        },

        _getHandler: function _getHandler(type) {
            var nodeType = type || "Unknown";
            var handleName = "_handle" + nodeType + "Type";
            var handleFunc = (typeof this[handleName] === "function") ? this[handleName] : this._handleUnknownType;
            return handleFunc.bind(this);
        }
    });

    return coreNodeStamp;
});
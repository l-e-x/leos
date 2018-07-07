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
define(function leosWidgetPluginModule(require) {
    "use strict";

    // load module dependencies
    var LOG = require("logger");
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "leosWidget";

    var pluginDefinition = {
        requires: "widget",

        init: function init(editor) {
            var widgetsObserver = new MutationObserver(function observeChanges(mutations) {
                mutations.forEach(function processChange(mutation) {
                    if (mutation.type === "childList") {
                        processNodeRemoval(mutation.removedNodes, editor);
                    }
                });
            });
            var reportOptions = {
                childList: true,    // observe additions and removals of the target's children
                subtree: true,      // observe additions and removals of the target's descendants
                attributes: false,
                characterData: false
            };
            editor.on("contentDom", function registerWidgetsObserver(event) {
                var editorBody = event.editor.editable().$;
                LOG.debug("Registering widgets observer...");
                widgetsObserver.observe(editorBody, reportOptions);
            });
            editor.on("contentDomUnload", function unregisterWidgetsObserver(event) {
                LOG.debug("Unregistering widgets observer...");
                widgetsObserver.disconnect();
            });
            editor.widgets.on("instanceCreated", function instanceCreated(event) {
                var widget = event.data;
                LOG.debug("Widget instance created! [id=%d, name=%s]", widget.id, widget.name);
                widget.on("key", onWidgetKey);
            });
            editor.widgets.on("instanceDestroyed", function instanceDestroyed(event) {
                var widget = event.data;
                LOG.debug("Widget instance destroyed! [id=%d, name=%s]", widget.id, widget.name);
            });
        }
    };

    function processNodeRemoval(removedNodes, editor) {
        var widgetIds = [];
        var findOnlyOne = true;

        findWidgets(removedNodes, widgetIds, findOnlyOne);

        if (widgetIds.length > 0) {
            LOG.debug("Possibly removed widget IDs:", widgetIds);
            // force CKEditor to destroy the removed widgets, i.e.
            // the widget instances that are not present in the DOM
            editor.widgets.checkWidgets();
        }
    }

    function findWidgets(nodesList, widgetIds, findOnlyOne) {
        // convert "live" node list to "static" node array,
        // for easier processing and improved performance
        var nodesArray = [].slice.call(nodesList);

        nodesArray.some(function inspectNode(node) {
            if (node.nodeType === Node.ELEMENT_NODE) {
                if (("data-cke-widget-wrapper" in node.attributes) &&
                    ("data-cke-widget-id" in node.attributes)) {
                    // the current node represents a widget in the DOM,
                    // there is no need to find widgets nested inside it
                    widgetIds.push(node.attributes.getNamedItem("data-cke-widget-id").value);
                } else if (node.hasChildNodes()) {
                    // look inside the current node to find widgets
                    findWidgets(node.childNodes, widgetIds, findOnlyOne);
                }
            }
            // stop processing the remaining nodes if
            // only one widget was requested and found
            return (findOnlyOne && (widgetIds.length > 0));
        });
    }

    var widgetDeletionKeyCodes = [
        8,                  // DELETE
        46,                 // BACKSPACE
        CKEDITOR.CTRL + 8,  // CTRL+DELETE
        CKEDITOR.CTRL + 46, // CTRL+BACKSPACE
        CKEDITOR.CTRL + 86, // CTRL+V (paste)
        CKEDITOR.CTRL + 88  // CTRL+X (cut)
    ];

    function onWidgetKey(event) {
        var keyCode = event.data.keyCode;
        // widget is deletable by default, but can be overridden by 'deletable' data parameter
        // widget is deletable if data.deletable is not specified (undefined or null) or true
        var deletableWidget = (this.data.deletable == undefined) ? true : !!this.data.deletable;
        if ((!deletableWidget) && (widgetDeletionKeyCodes.indexOf(keyCode) >= 0)) {
            LOG.debug("Cancelling deletion key widget event! [key=%d, id=%d, name=%s]", keyCode, this.id, this.name);
            event.cancel();
        }
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // create plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});
/*
 * Copyright 2019 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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
define(function aknLevelPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var leosHierarchicalElementTransformerStamp = require("plugins/leosHierarchicalElementTransformer/hierarchicalElementTransformer");
    var pluginName = "aknLevel";
    var aknLevelHeadingWidget = require("plugins/aknLevelWidget/aknLevelHeadingWidget");
    var leosKeyHandler = require("plugins/leosKeyHandler/leosKeyHandler");

    var ENTER_KEY = 13;
    var SHIFT_ENTER = CKEDITOR.SHIFT + ENTER_KEY;

    var pluginDefinition = {
        icons: pluginName.toLowerCase(),
        init : function init(editor) {
            editor.on("toHtml", removeInitialSnapshot, null, null, 100);
            editor.on("toHtml", _wrapContentWithSubparagraph, null, null, 5);
            editor.on("toDataFormat", _unWrapContentFromSubparagraph, null, null, 15);
            
            leosKeyHandler.on({
                editor : editor,
                eventType : 'key',
                key : ENTER_KEY,
                action : _onEnterKey
            });
            
            leosKeyHandler.on({
                editor : editor,
                eventType : 'key',
                key : SHIFT_ENTER,
                action : _onShiftEnterKey
            });
        }
    };
    
    function _onEnterKey(context) {
        var selection = context.event.editor.getSelection();
        var startElement = leosKeyHandler.getSelectedElement(selection);
        if((getSelectedRange(context) && !getSelectedRange(context).collapsed) || startElement.getName() === 'h2') {
            context.event.cancel();
        }
    }

    function _onShiftEnterKey(context) {
        var selection = context.event.editor.getSelection();
        var startElement = leosKeyHandler.getSelectedElement(selection);
        if((getSelectedRange(context) && !getSelectedRange(context).collapsed) || startElement.getName() === 'h2') {
            context.event.cancel();
        }
    }

    /*
     * Removes the initial snapshot which don't have 'level' as top level element
     */
    function removeInitialSnapshot(event) {
        if (event.editor.undoManager.snapshots.length > 0) {
            if (event.editor.undoManager.snapshots[0].contents.indexOf("level") < 0) {
                event.editor.undoManager.snapshots.shift();
            }
        }
    }
    
    function _wrapContentWithSubparagraph(event) {
        var level = (event.data.dataValue instanceof CKEDITOR.htmlParser.element) ? event.data.dataValue.findOne("level") : null;
        if (level) {
            var content = level.findOne("content");
            if (content) {
                level.findOne("content").wrapWith(new CKEDITOR.htmlParser.element("subparagraph", {}));
            }
        }
    }
    
    function _unWrapContentFromSubparagraph(event) {
        if (!event.data.dataValue.includes("</list>") && (event.data.dataValue.match(new RegExp("<subparagraph", "g")) || []).length === 1) {
            event.data.dataValue = event.data.dataValue.replace(/<subparagraph.*><content/, "<content").replace("<\/subparagraph>", "");
        }
    }
    
    var getSelectedRange = function getSelectedRange(context) {
        var selection = context.event.editor.getSelection(), ranges = selection && selection.getRanges();
        var firstRange;
        if (ranges && ranges.length > 0) {
            firstRange = ranges[0];
        }
        return firstRange;
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    var leosHierarchicalElementTransformer = leosHierarchicalElementTransformerStamp({
        firstLevelConfig: {
            akn: 'level',
            html: 'ol[data-akn-name=aknLevel]',
            attr: [{
                html: "data-akn-name=aknLevel"
            }]
        },
        rootElementsForFrom: ["level"],
        contentWrapperForFrom: "subparagraph",
        rootElementsForTo: ["ol","li"]
    });

    var transformationConfig = leosHierarchicalElementTransformer.getTransformationConfig();

    // return plugin module
    var pluginModule = {
        name : pluginName,
        transformationConfig : transformationConfig
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    return pluginModule;
});
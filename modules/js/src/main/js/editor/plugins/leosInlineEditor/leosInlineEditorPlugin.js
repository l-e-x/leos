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
define(function leosInlineEditorPluginModule(require) {
    "use strict";
    /*
     * This plugin should be the first one to be loaded.
     */
    
    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var pluginName = "leosInlineEditor";
    
    var pluginDefinition = {

        init : function init(editor) {
            
            var contentHeight = _getContentHeight(editor.element);
            editor.on("change", function(event) {
                var newContentHeight = _getContentHeight(event.editor.element);
                if(contentHeight != newContentHeight) {
                    event.editor.fire("contentChange");
                    contentHeight = newContentHeight;
                }
            });
           
            function _getContentHeight(element) {
            	if (element) {
            		var editorElem = element.$;
            		return editorElem.getBoundingClientRect().height;
            	} else {
            		return 0;
            	} 
            }

            editor.on('blur', function (evt) {
                if (evt.editor.LEOS.implicitSaveEnabled) {
                    if (evt.editor.checkDirty()) {
                        editor.fire("save", {
                            data: editor.getData()
                        });
                    }
                    editor.fire("close");
                } else {
                    evt.editor.element.removeClass('leos-editor-focus');
                    evt.editor.element.addClass('leos-editor-blur');
                }
            });

            editor.on('focus', function(evt) {
                evt.editor.element.removeClass('leos-editor-blur');
                evt.editor.element.addClass('leos-editor-focus');
            });
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name : pluginName
    };

    return pluginModule;
});
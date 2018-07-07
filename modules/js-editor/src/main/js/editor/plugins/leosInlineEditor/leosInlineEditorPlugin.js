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
define(function leosInlineEditorPluginModule(require) {
    "use strict";
    /*
     * This plugin should be the first one to be loaded.
     */
    
    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var cssPath = "css/leosInlineEditor.css";
    var pluginName = "leosInlineEditor";
    
    var pluginDefinition = {

        init : function init(editor) {
            /*
             * By default ckeditor does not provide way to load css styles in inline mode.
             * This function override and provide way to load css with jquery.
             */
            editor.addContentsCss = function(href) {
               if (!$("link[href='"+href+"']").length) {
                    var cssLink = $("<link rel='stylesheet' type='text/css' href='" + href + "'>");
                    $("head").append(cssLink);
               }
            };
            
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));
            var contentHeight = _getContentHeight(editor.element);
            editor.on("change", function(event) {
                var newContentHeight = _getContentHeight(event.editor.element);
                if(contentHeight != newContentHeight) {
                    event.editor.fire("contentChange");
                    contentHeight = newContentHeight;
                }
            });
           
            function _getContentHeight(element) {
                var editorElem = element.$;
                return editorElem.getBoundingClientRect().height;
            }
            
            editor.on('blur', function(evt) {
                if (editor.element) {
                    var el = document.getElementById(editor.element.getId());
                    if (el) {
                        $(el).addClass('leos-editor-blur');
                    }
                }
            });

            editor.on('focus', function(evt) {
                if (editor.element) {
                    var el = document.getElementById(editor.element.getId());
                    if (el) {
                        $(el).removeClass('leos-editor-blur');
                    }
                }
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
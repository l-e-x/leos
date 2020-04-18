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
define(function leosBase64ImagePluginModule(require) {
    
    'use strict';
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var pluginName = "leosBase64Image";
    var dialogDefinition = require("./leosBase64ImageDialog");
    
    var pluginDefinition = {
        lang 	: 	["af","ar","bg","bn","bs","ca","cs","cy","da","de","el","en","en-au","en-ca","en-gb","eo","es","et","eu","fa","fi","fo","fr","fr-ca","gl","gu","he","hi","hr","hu","id","is","it","ja","ka","km","ko","ku","lt","lv","mk","mn","ms","nb","nl","no","pl","pt","pt-br","ro","ru","si","sk","sl","sq","sr","sr-latn","sv","th","tr","ug","uk","vi","zh","zh-cn"],
        requires: 	"dialog",
        icons	:	"base64image",
        hidpi	:	true,
        init: function(editor) {
            editor.lang.base64image = editor.lang.leosBase64Image;
            editor.ui.addButton("base64image", {
                label: editor.lang.common.image,
                command: dialogDefinition.dialogName,
                toolbar: "insert"
            });
            // adds dialog
            pluginTools.addDialog(dialogDefinition.dialogName, dialogDefinition.initializeDialog);
    
            var allowed = 'img[alt,!src]{border-style,border-width,float,height,margin,margin-bottom,margin-left,margin-right,margin-top,width}',
                required = 'img[alt,src]';
    
            editor.addCommand( dialogDefinition.dialogName, new CKEDITOR.dialogCommand( dialogDefinition.dialogName, {
                allowedContent: allowed,
                requiredContent: required,
                contentTransformations: [
                    [ 'img{width}: sizeToStyle', 'img[width]: sizeToAttribute' ],
                    [ 'img{float}: alignmentToStyle', 'img[align]: alignmentToAttribute' ]
                ]
            } ) );
            editor.on("doubleclick", function(evt){
                if(evt.data.element && !evt.data.element.isReadOnly() && evt.data.element.getName() === "img") {
                    evt.data.dialog = dialogDefinition.dialogName;
                    editor.getSelection().selectElement(evt.data.element);
                }
            });
            if(editor.addMenuItem) {
                editor.addMenuGroup("base64imageGroup");
                editor.addMenuItem("base64imageItem", {
                    label: editor.lang.common.image,
                    icon: this.path+"icons/base64image.png",
                    command: dialogDefinition.dialogName,
                    group: "base64imageGroup"
                });
            }
            if(editor.contextMenu) {
                editor.contextMenu.addListener(function(element, selection) {
                    if(element && element.getName() === "img") {
                        editor.getSelection().selectElement(element);
                        return { base64imageItem: CKEDITOR.TRISTATE_ON };
                    }
                    return null;
                });
            }
        
        	
        	
        }
    };
    
    pluginTools.addPlugin(pluginName, pluginDefinition);
    
    // return plugin module
    var pluginModule = {
        name: pluginName
    };
    
    return pluginModule;
});
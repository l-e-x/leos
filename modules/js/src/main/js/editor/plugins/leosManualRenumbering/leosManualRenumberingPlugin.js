/*
 * Copyright 2018 European Commission
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
define(function leosManualRenumberingPlugin(require) {
    "use strict";

    // load module dependencies
    const CKEDITOR = require("promise!ckEditor");
    const UTILS = require("core/leosUtils");
    const pluginTools = require("plugins/pluginTools");
    const dialogDefinition = require("./leosManualRenumberingDialog");

    const DATA_AKN_NUM = "data-akn-num";
    const NUM_ORIGIN = "data-num-origin";
    const DATA_ORIGIN = "data-origin";
    const EC_ORIGIN = "ec";
    const CN_ORIGIN = "cn";
    const DASH = "-";
    const BULLET = "•";

    const pluginName = "leosManualRenumbering";

    const pluginDefinition = {
        lang: 'en',
        requires : "dialog",
        init: function init(editor) {
            let that = this;
            editor.on("receiveData", function (event){
                if(_isManualRenumberingEnabled(editor)){
                    _initializeManualRenumberingMenuItem(editor, that.path);
                }
            });
        }
    };

    function _initializeManualRenumberingMenuItem(editor, path){
        if(!editor.getCommand("editNumber")){
            pluginTools.addDialog(dialogDefinition.dialogName, dialogDefinition.initializeDialog);
            editor.addCommand("editNumber", new CKEDITOR.dialogCommand(dialogDefinition.dialogName));
            if (editor.contextMenu) {
                editor.addMenuGroup('editNumberGroup');
                editor.addMenuItem('editNumber', {
                    label: editor.lang.leosManualRenumbering.editNumber,
                    icon: path + "icons/editnumber.png",
                    command: 'editNumber',
                    group: 'editNumberGroup'
                });
                editor.contextMenu.addListener(function(element) {
                    if(_isNumberEditable(editor, dialogDefinition.getNumberedElement(element))){
                        return { editNumber: CKEDITOR.TRISTATE_OFF };
                    }
                });
            }
        }
    }

    function _isManualRenumberingEnabled(editor){
        return editor.editable && editor.editable().getChildren && editor.editable().getChildren().count() > 0
            && "ec" === UTILS.getElementOrigin(editor.editable().getChildren().getItem(0));
    }

    function _isNumberEditable(editor, element){
        let isElementNumberEditable =  element && element.hasAttribute(DATA_AKN_NUM)
            && element.getAttribute(DATA_AKN_NUM) !== DASH && element.getAttribute(DATA_AKN_NUM) !== BULLET
            && (!element.hasAttribute(NUM_ORIGIN) || element.getAttribute(NUM_ORIGIN) !== EC_ORIGIN)
            && element.getParent()
            && element.getParent().hasAttribute(DATA_ORIGIN) && element.getParent().getAttribute(DATA_ORIGIN) === EC_ORIGIN;
        return element.getParent() ? isElementNumberEditable || _isNumberEditable(editor, element.getParent()) : isElementNumberEditable;
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    return {
        name : pluginName
    };
});

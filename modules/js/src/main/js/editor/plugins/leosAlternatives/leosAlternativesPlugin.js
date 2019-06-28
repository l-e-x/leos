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
define(function leosAlternativesPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var log = require("logger");
    var $ = require("jquery");
    var CKEDITOR = require("promise!ckEditor");
    var pluginName = "leosAlternatives";
    var optionLists = "";
    var dialogDefinition = require("./leosAlternativesDialog");
    var dialogCommand;
    
    var pluginDefinition = {
        lang: 'en',
        init : function init(editor) {
            log.debug("Initializing Alternatives plugin...");

            optionLists = editor.LEOS.alternatives;
            _displayLabelsAltButtons();

            editor.once("receiveData",_populateAlternativesToolbar);
            
            pluginTools.addDialog(dialogDefinition.dialogName, dialogDefinition.initializeDialog);
            dialogCommand = editor.addCommand(dialogDefinition.dialogName, new CKEDITOR.dialogCommand(dialogDefinition.dialogName));
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    //TODO Temporary solution to display labels in place of the icons in the "alternatives" buttons
    function _displayLabelsAltButtons() {
        var styleCss = " *[class*=\"cke_button__" + pluginName.toLowerCase() + "\"][class*=\"icon\"]" +
                    "    {" +
                    "        display: none !important; /*without icon*/" +
                    "    }" +
                    " *[class*=\"cke_button__" + pluginName.toLowerCase() + "\"][class*=\"label\"]" +
                    "    {" +
                    "        display : inline !important; /*show the text label*/" +
                    "    }";
        CKEDITOR.addCss(styleCss);
    }

    function _getCurrentAltConfigFromAttributes(editor) {
        var currentAltConfig = {};
        var element = editor.element.$.firstChild;
        if (element.attributes["leos:optionlist"]) {
            currentAltConfig.optionListName = element.attributes["leos:optionlist"].value;
            currentAltConfig.selectOptionIndex = element.attributes["leos:selectedoption"].value;
            currentAltConfig.rootEltDeletable = element.attributes["leos:deletable"].value;
            currentAltConfig.rootEltEditable = element.attributes["leos:editable"].value;
            currentAltConfig.rootEltId = element.attributes["id"].value;
        }
        if (!currentAltConfig.optionListName) {
            throw new Error("Could not get the alternatives configuration list name");
        }
        return currentAltConfig;
    }

    function _getAlternativesConfiguration(optionListName) {
        var optionLists = JSON.parse(_getOptionLists());
        if (!optionLists) {
            throw new Error("Could not get the alternatives configuration list");
        }
        var optionList = optionLists.find(listOfOptions => listOfOptions.name == optionListName);
        if (!optionList) {
            throw new Error("Could not get the alternatives configuration list");
        }
        return optionList;
    }
    
    function _isPredefinedAlternative(editor, optionList) {
        var element = editor.element.$.firstChild;
        var presentDefaultValue = optionList.list.find(option => option.content == element.innerText);
        return presentDefaultValue != undefined;
    }
    
    function _populateAlternativesToolbar(event) {
        var editor = event.editor;
        var currentConfig = _getCurrentAltConfigFromAttributes(editor);
        var optionList = _getAlternativesConfiguration(currentConfig.optionListName);
        var cmd;
        
        optionList.list.forEach(function(option) {
            editor.ui.addButton(pluginName + option.index, {
                label: "Alternative " + option.index,
                command: pluginName + option.index,
                toolbar: "alternatives"
            });
            if (!editor.getCommand(pluginName + option.index)) {
                var altCommand = editor.addCommand(pluginName + option.index, {
                    // when click over one of the Alternative tabs
                    exec: function(editor) {
                        var isPredefinedAlternative = _isPredefinedAlternative(editor, optionList);
                        if(isPredefinedAlternative){
                            _updateEditor(this, editor, optionList, option.index);
                        } else {
                            dialogCommand.exec();
                            
                            cmd = this;
                            editor.on("confirmNewAlternative", function(event){
                                _updateEditor(cmd, editor, optionList, option.index);
                            });
                        }
                    }
                });
            }
        });
        
        editor.on('key', function(event) {
            _unselectButton(event.editor);
        });
        
        if (optionList.list.length > 0) {
            editor.fire("refreshToolbar");
            var isPredefinedAlternative = _isPredefinedAlternative(editor, optionList);
            var cmd = editor.getCommand(pluginName + currentConfig.selectOptionIndex);
            if (cmd && isPredefinedAlternative) {
                cmd.setState(CKEDITOR.TRISTATE_ON);
            }
        }
    }
    
    function _unselectButton(editor) {
        var currentConfig = _getCurrentAltConfigFromAttributes(editor);
        var optionList = _getAlternativesConfiguration(currentConfig.optionListName);
        
        optionList.list.forEach(function(option) {
            var currentCmd = editor.getCommand(pluginName + option.index);
            if (currentCmd) {
                currentCmd.setState(CKEDITOR.TRISTATE_OFF);
            }
        });
    }

    function _updateEditor(cmd, editor, optionList, index) {
        if (optionList) {
            _updateContent(cmd, editor, optionList, index);
        }
    }

    function _updateContent(cmd, editor, optionList, index) {
        var currentConfig = _getCurrentAltConfigFromAttributes(editor);
        var option = optionList.list.find(listOfOption => listOfOption.index == index);
        var options = {
            callback: function() {
                _updateRootEltAttributes(editor, currentConfig, index);
                _updateButtonState(cmd, editor, optionList);
            }
        };
        var actualTag = $(editor.getData());
        $(actualTag).find("p").html(option.content.replace(/\n|\r/g));
        editor.setData($(actualTag)[0].outerHTML, options);
    }

    function _updateRootEltAttributes(editor, currentConfig, index) {
        var rootElt = editor.element.getChild(0);
        if (!rootElt.hasAttribute("id")) {
            rootElt.setAttribute("id", currentConfig.rootEltId);
        }
        if (!rootElt.hasAttribute("leos:editable")) {
            rootElt.setAttribute("leos:editable", currentConfig.rootEltEditable);
        }
        if (!rootElt.hasAttribute("leos:deletable")) {
            rootElt.setAttribute("leos:deletable", currentConfig.rootEltDeletable);
        }
        rootElt.setAttribute("leos:selectedoption", index);
        rootElt.setAttribute("leos:optionlist", currentConfig.optionListName);
    }

    function _updateButtonState(cmd, editor, optionList) {
        optionList.list.forEach(function(option) {
            var currentCmd = editor.getCommand(pluginName + option.index);
            if (currentCmd) {
                currentCmd.setState(CKEDITOR.TRISTATE_OFF);
            }
        });
        cmd.setState(CKEDITOR.TRISTATE_ON);
    }

    function _getOptionLists() {
        return optionLists;
    }
    
    // return plugin module
    var pluginModule = {
        name : pluginName
    };

    return pluginModule;
});
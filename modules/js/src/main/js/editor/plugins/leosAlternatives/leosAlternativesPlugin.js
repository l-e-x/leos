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
define(function leosAlternativesPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var log = require("logger");
    var $ = require("jquery");
    var CKEDITOR = require("promise!ckEditor");

    var pluginName = "leosAlternatives";

    //TODO Done for testing purpose
    var optionLists = "[" +
    "    {"+
    "        \"name\": \"listOption1\"," +
    "        \"list\": [{" +
    "            \"index\": \"1\"," +
    "            \"content\": \"<clause GUID=\\\"clause_1\\\"><content GUID=\\\"clause_1__content\\\"><p GUID=\\\"clause_1__content__p\\\">This Regulation shall be binding in its entirety and directly applicable in all Member States.</p></content></clause>\"" +
    "        },{" +
    "            \"index\": \"2\"," +
    "            \"content\": \"<clause GUID=\\\"clause_1\\\"><content GUID=\\\"clause_1__content\\\"><p GUID=\\\"clause_1__content__p\\\">This Regulation shall be binding in its entirety and directly applicable in the Member States in accordance with the Treaties.</p></content></clause>\"" +
    "        }]" +
    "    }," +
    "    {" +
    "        \"name\": \"listOption2\"," +
    "        \"list\": [{" +
    "            \"index\": \"1\"," +
    "            \"content\": \"<clause GUID=\\\"clause_1\\\"><content GUID=\\\"clause_1__content\\\"><p GUID=\\\"clause_1__content__p\\\">This Regulation shall be tested.</p></content></clause>\"" +
    "        },{" +
    "            \"index\": \"2\"," +
    "            \"content\": \"<clause GUID=\\\"clause_1\\\"><content GUID=\\\"clause_1__content\\\"><p GUID=\\\"clause_1__content__p\\\">This Regulation shall be binding in its entirety and directly applicable in the tests.</p></content></clause>\"" +
    "        },{" +
    "            \"index\": \"3\"," +
    "            \"content\": \"<clause GUID=\\\"clause_1\\\"><content GUID=\\\"clause_1__content\\\"><p GUID=\\\"clause_1__content__p\\\">This Regulation shall be binding in test.</p></content></clause>\"" +
    "        },{" +
    "            \"index\": \"4\"," +
    "            \"content\": \"<clause GUID=\\\"clause_1\\\"><content GUID=\\\"clause_1__content\\\"><p GUID=\\\"clause_1__content__p\\\">This Regulation shall be binding in its test.</p></content></clause>\"" +
    "        }]" +
    "    }" +
    "]";

    var pluginDefinition = {
        init : function init(editor) {
            log.debug("Initializing Alternatives plugin...");
            _displayLabelsAltButtons();

            editor.once("receiveData",_populateAlternativesToolbar);
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

    function _populateAlternativesToolbar(event) {
        var editor = event.editor;
        var currentConfig = _getCurrentAltConfigFromAttributes(editor);

        var optionList = _getAlternativesConfiguration(currentConfig.optionListName);
        optionList.list.forEach(function(option) {
            editor.ui.addButton(pluginName + option.index, {
                label: "Alternative " + option.index,
                command: pluginName + option.index,
                toolbar: "alternatives"
            });
            if (!editor.getCommand(pluginName + option.index)) {
                var altCommand = editor.addCommand(pluginName + option.index, {
                    exec: function(editor) {
                        _updateEditor(this, editor, optionList, option.index);
                    }
                });
                altCommand.readOnly = true;
            }
        });
        if (optionList.list.length > 0) {
            editor.fire("refreshToolbar");
            if (!editor.readOnly) {
                editor.setReadOnly();
            }
            var cmd = editor.getCommand(pluginName + currentConfig.selectOptionIndex);
            if (cmd) {
                cmd.setState(CKEDITOR.TRISTATE_ON);
            }
            _initReadOnlyMode(editor);
        }
    }

    function _initReadOnlyMode(editor) {
        editor.getCommand("leosInlineCancelDialog").readOnly = true;    //To get Close dialog activated on readOnly mode
        editor.getCommand("inlinecancel").readOnly = true;    //To get Close button activated on readOnly mode
        editor.getCommand("inlinesave").readOnly = true;      //To get Save button activated on readOnly mode
        editor.on("readOnly", _setReadOnly, null, null, 0);   //To keep readOnly mode while going to another browser tab
    }

    function _setReadOnly(event) {
        if (!event.editor.readOnly) {
            event.editor.setReadOnly();
        }
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
        editor.setData(option.content.replace(/\n|\r/g, ""), options);
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
        log.debug("Get List Of Options..");
        return optionLists;
    }

    // return plugin module
    var pluginModule = {
        name : pluginName,
    };

    return pluginModule;
});

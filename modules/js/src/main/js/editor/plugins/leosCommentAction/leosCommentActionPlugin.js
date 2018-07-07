/*
 * Copyright 2017 European Commission
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
define(function leosCommentActionPluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var log = require("logger");
    var $ = require("jquery");

    var pluginName = "leosCommentAction";

    var pluginDefinition = {
        init: function (editor) {
            editor.on("instanceReady", function _registerActionHandlers(event) {
                $("akomantoso").on("click.balloonAction", ".balloon-delete", _delete.bind(undefined, editor));
            });

            editor.on("destroy", function _unregisterActionHandlers(event) {
                $("akomantoso").off(".balloonAction");
            });
        }
    };

    function _delete(editor, event) {
        log.debug("Delete clicked on balloon...");
        var $xml = _getXmlObjectFromEditor(editor);
        var popupId = _getPopupIds(event.currentTarget);
        _removeNode($xml, popupId); //remove popup from XML
        _setXmlInEditor(editor, $xml);

        editor.fire("contentChange");
        editor.focus();
    }

    /*Helper functions*/
    function _getXmlObjectFromEditor(editor) {
        var xmlContent = editor.getData();
        xmlContent = xmlContent.replace('>', ' xmlns:leos="urn:eu:europa:ec:leos">');//inject namespace in root
        var xmlDoc = $.parseXML(xmlContent);
        return $(xmlDoc);
    }

    function _setXmlInEditor(editor, $updatedXMLObject) {
        var xmlString = (new XMLSerializer()).serializeToString($updatedXMLObject[0]);
        xmlString = xmlString.replace(' xmlns:leos="urn:eu:europa:ec:leos"', ' ');
        editor.setData(xmlString);
    }

    function _removeNode($xml, id) {
        var $xmlElement = $xml.find("#" + id);
        $xmlElement.remove();
        return $xmlElement;
    }

    function _replaceNodeInXML($xml, $newContent, idToReplace) {
        $newContent.attr("id", idToReplace); //to keep the same id
        $xml.find("#" + idToReplace).replaceWith($newContent);
        return $xml;
    }

    function _getPopupIds(balloonButton) {
        var $balloon = $(balloonButton).closest(".leos-balloon");
        var $popupElement = $balloon.data("bs.popover").$element;
        return $popupElement.attr('id');
    }

    function _getPopupParentId($xml, popupId){
        var $element = $xml.find("#" + popupId).parent();//aknp(p) or element
        return $element.attr('id');
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});
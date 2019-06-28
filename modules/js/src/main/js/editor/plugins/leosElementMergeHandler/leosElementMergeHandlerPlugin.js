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
define(function leosElementMergeHandlerPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var $ = require("jquery");

    var pluginName = "leosElementMergeHandler";
    var TRISTATE_DISABLED = CKEDITOR.TRISTATE_DISABLED, TRISTATE_OFF = CKEDITOR.TRISTATE_OFF;
    var MERGE_CMD_NAME = "elementmerge";
    var iconMerge = 'icons/leoselementmergehandler.png';

	var SOFT_ACTIONS_LIST = ['del', 'move_to'];

    var pluginDefinition = {

    	icons: pluginName.toLowerCase(),

        init: function init(editor) {
        	
            editor.ui.addButton('leosElementMerge', {
                label: 'Merge',
                command: MERGE_CMD_NAME,
                toolbar: 'splitmerge',
                icon: this.path + iconMerge
            });
            
            var mergeCommand = editor.addCommand(MERGE_CMD_NAME, {
                exec: function(editor) {
                	if (this.state != TRISTATE_DISABLED) {
                		editor.fire("merge", {
                			data: editor.getData()
                		});
                	}
                }
            });

            editor.on('focus', function(event) {
            	mergeCommand.setState(_isMergeAllowed(editor) ? TRISTATE_OFF : TRISTATE_DISABLED);
            }, null, null, 100);

            editor.on('dataReady', function(event) {
            	mergeCommand.setState(_isMergeAllowed(editor) ? TRISTATE_OFF : TRISTATE_DISABLED);
            }, null, null, 100);

            editor.on('change', function(event) {
            	mergeCommand.setState(_isMergeAllowed(editor) ? TRISTATE_OFF : TRISTATE_DISABLED);
            });
        }
    };

    function _isMergeAllowed(editor) {
    	var $editedElement = $('div#docContainer akomantoso div[data-wrapped-id]');
    	if (!($editedElement.length) || ($editedElement.length && ($editedElement.has('table').length
    			|| _isSoftDeletedOrMovedTo($editedElement.find('ol > li'), 'data-akn-attr-softaction')))) {
    		return false;
    	}
    	var $mergeOnElement = editor.LEOS != null ? $editedElement.prevAll(editor.LEOS.elementType + ', list').first() : $();
    	if (!($mergeOnElement.length) || ($mergeOnElement.length && (!($mergeOnElement.children('content').length)
    			|| $mergeOnElement.children('content:has(>table)').length
    			|| _isSoftDeletedOrMovedTo($mergeOnElement, 'leos:softaction')))) {
    		return false;
    	}
        return true;
    }

    function _isSoftDeletedOrMovedTo($element, attributeName) {
    	return SOFT_ACTIONS_LIST.includes($element.attr(attributeName));
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});
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
/**
 * @fileOverview Handles the tables. It uses the transformer 'leosTableTransformer' more information could be found here 
 * https://webgate.ec.europa.eu/CITnet/confluence/display/LEOS/Annex+Mappings  
 */
; // jshint ignore:line
define(function leosTablePluginModule(require) {
    'use strict';

    // load module dependencies
    var pluginTools = require('plugins/pluginTools');
    var leosTableTransformerStamp = require('plugins/leosTableTransformer/leosTableTransformer');
    var leosKeyHandler = require("plugins/leosKeyHandler/leosKeyHandler");
    var leosCommandStateHandler = require("plugins/leosCommandStateHandler/leosCommandStateHandler");
    var $ = require('jquery');

    var pluginName = 'leosTable';
    var ENTER_KEY = 13;
    var SHIFT_ENTER = CKEDITOR.SHIFT + ENTER_KEY;
    var HTML_CAPTION = "caption";
    var HTML_TABLE = "table";
    var changeStateElements = {
        articleHeading : {
            elementName: 'h2',
            selector: '[data-akn-name=aknHeading]'
        },
        caption: {
            elementName: 'caption',
            selector: null
        }
    };

    var pluginDefinition = {
        init : function init(editor) {
            //To hide table 'Alignment', 'Width', 'Height', 'Border Size', 'Cell Spacing' , 'Cell Padding' and 'Summary' combo/edit boxes in the dialog box
            editor.on('dialogShow', function(event) {
                var dialog = event.data;
                if (dialog.getName() === 'table' || dialog.getName() === 'tableProperties') {
                    var items = ['cmbAlign', 'txtWidth', 'txtHeight', 'txtBorder', 'txtCellSpace', 'txtCellPad', 'txtSummary'];
                    items.forEach( function(item) {
                        dialog.getContentElement('info', item).getElement().hide();
                    });
                    dialog.resize(310,150);
                }
            });

            //To remove the cell insert, delete and properties menu items from the table tools menu
            editor.on('instanceReady', function(ck) { 
                ck.editor.removeMenuItem('tablecell_insertBefore'); 
                ck.editor.removeMenuItem('tablecell_insertAfter'); 
                ck.editor.removeMenuItem('tablecell_delete'); 
                ck.editor.removeMenuItem('tablecell_properties'); 
                ck.editor.getCommand('tableDelete').exec = _tableDelete.bind(undefined, ck.editor);
            });
            editor.on('selectionChange', _onSelectionChange);

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
    
    function _tableDelete(editor) { // This is a copy of ckeditor plugins/table/plugin.js 'tableDelete' exec command function and modified
		var path = editor.elementPath(), // to avoid remove 'li' parent table element (check parent condition, 'li' element was added)
		table = path.contains( 'table', 1 );
		
		if ( !table )
			return;
		
		// If the table's parent has only one child remove it as well (unless it's a table cell, li element or the editable element)
		//(https://dev.ckeditor.com/ticket/5416, https://dev.ckeditor.com/ticket/6289, https://dev.ckeditor.com/ticket/12110)
		var parent = table.getParent(),
			editable = editor.editable();
		
		if ( parent.getChildCount() == 1 && !parent.is( 'td', 'th' , 'li' ) && !parent.equals( editable ) )
			table = parent;
		
		var range = editor.createRange();
		range.moveToPosition( table, CKEDITOR.POSITION_BEFORE_START );
		table.remove();
		range.select();
	}
    
    function _onSelectionChange(event) {
        leosCommandStateHandler.changeCommandState(event, 'table', changeStateElements);
    }
    
    function _onEnterKey(context) {
        var selection = context.event.editor.getSelection();
        if (!selection) return;
        if (selection.getType() !== CKEDITOR.SELECTION_NONE) {
            var startElement = leosKeyHandler.getSelectedElement(selection);
            var currentElement = startElement.$;
            var elementName = currentElement.nodeName.toLowerCase();
            if (elementName === HTML_CAPTION) {
                context.event.cancel();
            }
        }
    }
    
    function _onShiftEnterKey(context) {
        var selection = context.event.editor.getSelection();
        if (!selection) return;
        if (selection.getType() !== CKEDITOR.SELECTION_NONE) {
            var startElement = leosKeyHandler.getSelectedElement(selection);
            var currentElement = startElement.$;
            if ($(currentElement).parents(HTML_TABLE).length) {
                context.event.cancel();
            }
        }
    }
    
    pluginTools.addPlugin(pluginName, pluginDefinition);

    var leosTableTransformer = leosTableTransformerStamp({
        tableTransformationConfig: {
            akn : 'table',
            html : 'table',
            attr : [ {
                akn : 'xml:id',
                html : 'id',
            }, {
                akn : "leos:origin",
                html : "data-origin"
            }, {
                akn : 'border',
                html : 'border',
            }, {
                akn : 'cellpadding',
                html : 'cellpadding'
            }, {
                akn : 'cellspacing',
                html : 'cellspacing'
            }, {
                akn : 'style',
                html : 'style'
            }, {
                akn : 'summary',
                html : 'summary'
            }, {
                html : 'data-akn-name=leosTable'
            }],
            sub : {
                akn : 'tr',
                html : 'tr',
                attr : [{
                    akn : 'xml:id',
                    html : 'id',
                },{
                    akn : "leos:origin",
                    html : "data-origin"
                }],
                sub: {
                    akn : {
                        head: 'th',
                        body: 'td',
                    },
                    html : {
                        head: 'th',
                        body: 'td',
                    },
                    attr : [{
                        akn : 'rowspan',
                        html : 'rowspan',
                    }, {
                        akn : 'colspan',
                        html : 'colspan',
                    }, {
                        akn : 'style',
                        html : 'style',
                    },{
                        akn : 'xml:id',
                        html : 'id',
                    },{
                        akn : "leos:origin",
                        html : "data-origin"
                    }]
                }
            }
        }
    });
    
    var transformationConfig = leosTableTransformer.getTransformationConfig();

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    // return plugin module
    var pluginModule = {
        name : pluginName,
        transformationConfig : transformationConfig
    };

    return pluginModule;
});
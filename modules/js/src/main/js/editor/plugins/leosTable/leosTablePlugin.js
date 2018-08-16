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
                akn : 'GUID',
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
                    akn : 'GUID',
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
                        akn : 'GUID',
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
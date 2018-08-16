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
define(function leosPastePluginModule(require) {
    'use strict';

    // load module dependencies
    var pluginTools = require('plugins/pluginTools');
    var pluginName = 'leosPaste';

    var pluginDefinition = {
        init: function init(editor) {

            editor.on('paste', function (evt) {
                // Parse the HTML string to pseudo-DOM structure.
                var fragment = CKEDITOR.htmlParser.fragment.fromHtml(evt.data.dataValue);
                fragment.forEach( function( node ) {//saving editor to reuse later
                    node.editor = editor;
                });

                //Process the structure
                _processPaste(editor, fragment, evt.data.type);

                //Write back the object structure in dataValue
                var writer = new CKEDITOR.htmlParser.basicWriter();
                fragment.writeHtml(writer);
                evt.data.dataValue = writer.getHtml();
            }, 7);
        }
    };

    var numberingRegex = new RegExp(/^\s*[([{]?.{1,3}(\..{1,2})*[.)\]}]?\s*$/);
    var htmlFilter = new CKEDITOR.htmlParser.filter({
        text: function (content, node) {
            if (node.parent) {
                return content.replace(/&nbsp;/g, ' ');
            }
        },
        elements: {
            $: function (element) {
                //remove all styles and classes
                delete element.attributes.style;
                delete element.attributes.class;

                if (numberingRegex.test(element.getHtml()) && element.parent) {
                    return false;
                }

                //remove element if element is block element and not allowed in editor
                if (element.parent
                    && (CKEDITOR.dtd.$block[element.name] // p etc any block element
                    || !allowedInEditor(element) )) { //TODO find solution for table in article
                    element.replaceWithChildren();
                }

                function allowedInEditor(element){
                    return element.editor.config.pasteFilter.replace(/\[\*\]/g,'').split('; ').includes(element.name)
                }
            },
            a: function (element) {
                if (element.parent) {// if not already removed
                    //Convert to Authnote??
                    var textContent = convertElementToText(element).trim();
                    if (!textContent) {//if empty, remove
                        return false;
                    }
                    element.replaceWith(new CKEDITOR.htmlParser.text(textContent));
                }
            },
            span: function (element) {
                //Span needs to be kept as text only
                if (element.parent) {// if not already removed
                    var textContent = convertElementToText(element);//TODO find solution for bold/italic
                    if (!textContent.trim()) {//if empty, remove
                        return false;
                    }
                    element.replaceWith(new CKEDITOR.htmlParser.text(textContent));
                }
            },
            p:function (element) {
                //P will stay as it is but all its children will be accumulated into a single text node
                if (element.parent) {// if not already removed
                    element.filterChildren(htmlFilter);

                    var text = '';
                    for (var i = 0; i < element.children.length; i++) {
                        text += convertElementToText(element.children[i]);
                        element.children[i].remove();
                        i--;
                    }
                    element.add(new CKEDITOR.htmlParser.text(text));
                }
            },
            br: function (element) {
                if (element.parent) {
                    return false;
                }
            }
        }
    });

    function convertElementToText(node){
        if(node.type == CKEDITOR.NODE_TEXT){
            return node.value;
        }
        var text = '';
        for ( var i = 0, len = node.children.length; i < len; i++ ) {
            if(node.children[i].type == CKEDITOR.NODE_ELEMENT) {
                text += convertElementToText(node.children[i]);
            }
            else if(node.children[i].type == CKEDITOR.NODE_TEXT) {
                text+=node.children[i].value;
            }
        }
        return text;
    }

    function _processPaste(editor, fragment, type) {
        if (type === 'html') {
            fragment.filter(htmlFilter);//clean using filter for html and text
            _convertToAknXmlFragment(editor, fragment);
        }
        else if (type === 'text') {
            //it will come here as text for PDF
            //TODO
        }
    }

    function _convertToAknXmlFragment(editor, fragment) {
        var configElement = editor.config.defaultPasteElement;// this should come from profile
        if (configElement) {
            configElement
                .split('/')
                .reverse()
                .filter(tag =>(tag !== 'text'))
                .forEach(
                    function (currentValue, index, array) {
                        for (var i = 0; i < fragment.children.length; i++) {
                            fragment.children[i].wrapWith(new CKEDITOR.htmlParser.element(currentValue, {}));
                        }
                    }
                );
        }//else let it be text
    }

    function _getElementWithCursor(editor) {
        var element = editor.getSelection().getStartElement();
        //Check to avoid nested inline elements
        while (!element.isBlockBoundary()) {
            element = element.getParent();
        }
        return element;
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName,
    };

    return pluginModule;
});

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
 * @fileOverview This plugin was copied from the official plugin CKEditor_4.7.1 "justifu" and developed to add only center, justify styles.
 * To avoid the list element from center or justify styles, that's why this copy of the default list plugin.
 * Here are the main customizations:
 *     - Change the pluginName from "jsutify" to aknHtmlJustifyPlugin
 *     - Removed left and right align button and command
 *     - Restructered the original plugin, command definnition is passed as variable instead of function object.
 *     - To call exec and refresh not using prototype, instead exec and refresh added directly in command definition.
 */
; // jshint ignore:line
define(function aknHtmlJustifyPluginModule(require) {

    "use strict";
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var $ = require('jquery');
    var pluginName = "aknHtmlJustify";
    var listElement = 'li';
    var olElement = 'ol';
    var ulElement = 'ul';

    var pluginDefinition = {

        // jscs:disable maximumLineLength
        lang: 'af,ar,az,bg,bn,bs,ca,cs,cy,da,de,de-ch,el,en,en-au,en-ca,en-gb,eo,es,es-mx,et,eu,fa,fi,fo,fr,fr-ca,gl,gu,he,hi,hr,hu,id,is,it,ja,ka,km,ko,ku,lt,lv,mk,mn,ms,nb,nl,no,oc,pl,pt,pt-br,ro,ru,si,sk,sl,sq,sr,sr-latn,sv,th,tr,tt,ug,uk,vi,zh,zh-cn', // %REMOVE_LINE_CORE%
        // jscs:enable maximumLineLength
        icons: 'justifyblock,justifycenter', // %REMOVE_LINE_CORE%
        hidpi: true, // %REMOVE_LINE_CORE%

        init: function(editor) {
            editor.lang.justify = editor.lang.aknHtmlJustify;
            if (editor.blockless)
                return;

            var justifyCommand = {
                editor: editor,
                value: '',
                context: 'p',
                exec: function(editor) {
                    //this.value = value;
                    this.context = 'p';
                    var classes = editor.config.justifyClasses,
                        blockTag = editor.config.enterMode == CKEDITOR.ENTER_P ? 'p' : 'div';

                    if (classes) {
                        switch (this.value) {
                            case 'center':
                                this.cssClassName = classes[1];
                                break;

                            case 'justify':
                                this.cssClassName = classes[3];
                                break;
                        }

                        this.cssClassRegex = new RegExp('(?:^|\\s+)(?:' + classes.join('|') + ')(?=$|\\s)');
                        this.requiredContent = blockTag + '(' + this.cssClassName + ')';
                    } else {
                        this.requiredContent = blockTag + '{text-align}';
                    }

                    this.allowedContent = {
                        'caption div h1 h2 h3 h4 h5 h6 p pre td th li': {
                            // Do not add elements, but only text-align style if element is validated by other rule.
                            propertiesOnly: true,
                            styles: this.cssClassName ? null : 'text-align',
                            classes: this.cssClassName || null
                        }
                    };

                    // In enter mode BR we need to allow here for div, because when non other
                    // feature allows div justify is the only plugin that uses it.
                    if (editor.config.enterMode == CKEDITOR.ENTER_BR)
                        this.allowedContent.div = true;

                    var selection = editor.getSelection(),
                        enterMode = editor.config.enterMode;

                    if (!selection)
                        return;

                    var bookmarks = selection.createBookmarks(),
                        ranges = selection.getRanges();

                    var cssClassName = this.cssClassName,
                        iterator, block;

                    var useComputedState = editor.config.useComputedState;
                    useComputedState = useComputedState === undefined || useComputedState;

                    for (var i = ranges.length - 1; i >= 0; i--) {
                        iterator = ranges[i].createIterator();
                        iterator.enlargeBr = enterMode != CKEDITOR.ENTER_BR;

                        while ((block = iterator.getNextParagraph(enterMode == CKEDITOR.ENTER_P ? 'p' : 'div'))) {
                            if (block.isReadOnly())
                                continue;

                            block.removeAttribute('align');
                            block.removeStyle('text-align');

                            // Remove any of the alignment classes from the className.
                            var className = cssClassName && (block.$.className = CKEDITOR.tools.ltrim(block.$.className.replace(this.cssClassRegex, '')));

                            var apply = (this.state == CKEDITOR.TRISTATE_OFF) && (!useComputedState || (getAlignment(block, true) != this.value));

                            if (cssClassName) {
                                // Append the desired class name.
                                if (apply)
                                    block.addClass(cssClassName);
                                else if (!className)
                                    block.removeAttribute('class');
                            } else if (apply) {
                                block.setStyle('text-align', this.value);
                            }
                        }

                    }

                    editor.focus();
                    editor.forceNextSelectionCheck();
                    selection.selectBookmarks(bookmarks);
                },

                refresh: function(editor, path) {
                    var firstBlock = path.block || path.blockLimit;
                    var selectedHtml = editor.getSelectedHtml(true);
                   
                    // LEOS-2861 Create div to avoid problems with special selectors http://api.jquery.com/category/selectors/ 
                    selectedHtml = $("<div>").html(selectedHtml);
                    var countList = selectedHtml.children().find(listElement).length;
                    var countOlList = selectedHtml.children().find(olElement).length;
                    var countUlList = selectedHtml.children().find(ulElement).length;
                    var countChildren = selectedHtml.find('*').length - 1; //to remove div
                    var countDiff = countChildren-(countList+countOlList+countUlList);
                    
                    //count difference of list and all html is calculated
                    //style remains enabled if in selection there are elements other than list 
                    if (path.contains(listElement) && (countDiff <= 0)) {
                        this.setState(CKEDITOR.TRISTATE_DISABLED);
                    } else {
                        this.setState(firstBlock.getName() != 'body' && getAlignment(firstBlock, this.editor.config.useComputedState) == this.value ? CKEDITOR.TRISTATE_ON : CKEDITOR.TRISTATE_OFF);
                    }

                }
            }

            var center = $.extend({}, justifyCommand);
            var justify = $.extend({}, justifyCommand);
            justify.value = 'justify';
            center.value = 'center';

            editor.addCommand('justifycenter', center);
            editor.addCommand('justifyblock', justify);

            if (editor.ui.addButton) {

                editor.ui.addButton('JustifyCenter', {
                    label: editor.lang.aknHtmlJustify.center,
                    command: 'justifycenter',
                    toolbar: 'align,30'
                });

                editor.ui.addButton('JustifyBlock', {
                    label: editor.lang.aknHtmlJustify.block,
                    command: 'justifyblock',
                    toolbar: 'align,50'
                });
            }

            editor.on('dirChanged', onDirChanged);

            function getAlignment(element, useComputedState) {
                useComputedState = useComputedState === undefined || useComputedState;

                var align;
                if (useComputedState)
                    align = element.getComputedStyle('text-align');
                else {
                    while (!element.hasAttribute || !(element.hasAttribute('align') || element.getStyle('text-align'))) {
                        var parent = element.getParent();
                        if (!parent)
                            break;
                        element = parent;
                    }
                    align = element.getStyle('text-align') || element.getAttribute('align') || '';
                }

                // Sometimes computed values doesn't tell.
                align && (align = align.replace(/(?:-(?:moz|webkit)-)?(?:start|auto)/i, ''));

                !align && useComputedState && (align = element.getComputedStyle('direction') == 'rtl' ? 'right' : 'left');

                return align;
            }


            function onDirChanged(e) {
                var editor = e.editor;

                var range = editor.createRange();
                range.setStartBefore(e.data.node);
                range.setEndAfter(e.data.node);

                var walker = new CKEDITOR.dom.walker(range),
                    node;

                while ((node = walker.next())) {
                    if (node.type == CKEDITOR.NODE_ELEMENT) {
                        // A child with the defined dir is to be ignored.
                        if (!node.equals(e.data.node) && node.getDirection()) {
                            range.setStartAfter(node);
                            walker = new CKEDITOR.dom.walker(range);
                            continue;
                        }

                        // Switch the alignment.
                        var classes = editor.config.justifyClasses;

                        // Always switch CSS margins.
                        var style = 'text-align';
                        var align = node.getStyle(style);


                    }
                }
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
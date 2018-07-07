/*
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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
 * @fileOverview Handles the indentation of lists. This is a customized plugin similar to ckeditor indentList plugin. The customization is done to disable the
 * 'outdent' toolbar button for the first level list items so that it will not be possible to convert it into '<p>'.
 */

; // jshint ignore:line
define(function indentListPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var LOG = require("logger");
    var pluginTools = require("plugins/pluginTools");

    var pluginName = "indentlist";

    var list = CKEDITOR.plugins.list;
    var isNotWhitespaces = CKEDITOR.dom.walker.whitespaces(true), isNotBookmark = CKEDITOR.dom.walker.bookmark(false, true),
    TRISTATE_DISABLED = CKEDITOR.TRISTATE_DISABLED, TRISTATE_OFF = CKEDITOR.TRISTATE_OFF;

    var pluginDefinition = {
        init: function init(editor) {
            var indent = CKEDITOR.plugins.indent;
            // Register commands.
            indent.registerCommands(editor, {
                aknindentlist: new commandDefinition(editor, 'aknindentlist', true),
                aknoutdentlist: new commandDefinition(editor, 'aknoutdentlist')
            });

            function commandDefinition(editor) {
                indent.specificDefinition.apply(this, arguments);

                // Require ul OR ol list.
                this.requiredContent = ['ul', 'ol'];

                // Indent and outdent lists with TAB/SHIFT+TAB key. Indenting can
                // be done for any list item that isn't the first child of the parent.
                editor.on('key', function(evt) {
                    if (editor.mode != 'wysiwyg')
                        return;

                    if (evt.data.keyCode == this.indentKey) {
                        var list = this.getContext(editor.elementPath());

                        if (list) {
                            // Don't indent if in first list item of the parent.
                            if (this.isIndent && firstItemInPath(this.context, editor.elementPath(), list))
                                return;

                            // Exec related global indentation command. Global
                            // commands take care of bookmarks and selection,
                            // so it's much easier to use them instead of
                            // content-specific commands.
                            editor.execCommand(this.relatedGlobal);

                            // Cancel the key event so editor doesn't lose focus.
                            evt.cancel();
                        }
                    }
                }, this);

                // There are two different jobs for this plugin:
                // Indent job (priority=10), before indentblock. This job is before indentblock because, if this plugin is
                // loaded it has higher priority over indentblock. It means that, if possible, nesting is performed, and then block manipulation, if necessary.
                // Outdent job (priority=30), after outdentblock. This job got to be after outdentblock because in some cases
                // (margin, config#indentClass on list) outdent must be done on block-level.
                this.jobs[this.isIndent ? 10 : 30] = {
                    refresh: this.isIndent ? function(editor, path) {
                        var list = this.getContext(path), inFirstListItem = firstItemInPath(this.context, path, list);
                        if (!list || !this.isIndent || inFirstListItem) {
                            return TRISTATE_DISABLED;
                        }
                        return TRISTATE_OFF;
                    } 
                    : function(editor, path) {
                        var list = this.getContext(path);
                        
                        // custom code to disable the outdent toolbar button for first level list items.
                        var isFirstLevel = isFirstLevelList(editor, list);
                        if (!list || this.isIndent || isFirstLevel) {
                            return TRISTATE_DISABLED;
                        }
                        return TRISTATE_OFF;
                    },
                    exec: CKEDITOR.tools.bind(aknindentList, this)
                };
            }

            CKEDITOR.tools.extend(commandDefinition.prototype, indent.specificDefinition.prototype, {
                // Elements that, if in an elementpath, will be handled by this command. They restrict the scope of the plugin.
                context: {ol: 1, ul: 1}
            });
        },
    };

    function aknindentList(editor) {
        var that = this, database = this.database, context = this.context;

        function indent(listNode) {
            // Our starting and ending points of the range might be inside some blocks under a list item...
            // So before playing with the iterator, we need to expand the block to include the list items.
            var startContainer = range.startContainer, endContainer = range.endContainer;
            while (startContainer && !startContainer.getParent().equals(listNode))
                startContainer = startContainer.getParent();
            while (endContainer && !endContainer.getParent().equals(listNode))
                endContainer = endContainer.getParent();

            if (!startContainer || !endContainer)
                return false;

            // Now we can iterate over the individual items on the same tree depth.
            var block = startContainer, itemsToMove = [], stopFlag = false;

            while (!stopFlag) {
                if (block.equals(endContainer)) {
                    stopFlag = true;
                }
                itemsToMove.push(block);
                block = block.getNext();
            }
            if (itemsToMove.length < 1) {
                return false;
            }
            // Do indent or outdent operations on the array model of the list, not the list's DOM tree itself. The array model demands that it knows as much as
            // possible about the surrounding lists, we need to feed it the further ancestor node that is still a list.
            var listParents = listNode.getParents(true);
            for (var i = 0; i < listParents.length; i++) {
                if (listParents[i].getName && context[listParents[i].getName()]) {
                    listNode = listParents[i];
                    break;
                }
            }

            var indentOffset = that.isIndent ? 1 : -1, startItem = itemsToMove[0], lastItem = itemsToMove[itemsToMove.length - 1],

            // Convert the list DOM tree into a one dimensional array.
            listArray = CKEDITOR.plugins.list.listToArray(listNode, database),

            // Apply indenting or outdenting on the array.
            baseIndent = listArray[lastItem.getCustomData('listarray_index')].indent;

            for (i = startItem.getCustomData('listarray_index'); i <= lastItem.getCustomData('listarray_index'); i++) {
                listArray[i].indent += indentOffset;
                // Make sure the newly created sublist get a brand-new element of the same type. (#5372)
                if (indentOffset > 0) {
                    var listRoot = listArray[i].parent;
                    listArray[i].parent = new CKEDITOR.dom.element(listRoot.getName(), listRoot.getDocument());
                }
            }

            for (i = lastItem.getCustomData('listarray_index') + 1; i < listArray.length && listArray[i].indent > baseIndent; i++)
                listArray[i].indent += indentOffset;

            // Convert the array back to a DOM forest (yes we might have a few subtrees now). And replace the old list with the new forest.
            var newList = CKEDITOR.plugins.list.arrayToList(listArray, database, null, editor.config.enterMode, listNode.getDirection());

            // Avoid nested <li> after outdent even they're visually same, recording them for later refactoring.(#3982)
            if (!that.isIndent) {
                var parentLiElement;
                if ((parentLiElement = listNode.getParent()) && parentLiElement.is('li')) {
                    var children = newList.listNode.getChildren(), pendingLis = [], count = children.count(), child;

                    for (i = count - 1; i >= 0; i--) {
                        if ((child = children.getItem(i)) && child.is && child.is('li'))
                            pendingLis.push(child);
                    }
                }
            }

            if (newList)
                newList.listNode.replace(listNode);

            // Move the nested <li> to be appeared after the parent.
            if (pendingLis && pendingLis.length) {
                for (i = 0; i < pendingLis.length; i++) {
                    var li = pendingLis[i], followingList = li;

                    // Nest preceding <ul>/<ol> inside current <li> if any.
                    while ((followingList = followingList.getNext()) && followingList.is && followingList.getName() in context) {
                        // IE requires a filler NBSP for nested list inside empty list item, otherwise the list item will be inaccessiable. (#4476)
                        if (CKEDITOR.env.needsNbspFiller && !li.getFirst(neitherWhitespacesNorBookmark))
                            li.append(range.document.createText('\u00a0'));

                        li.append(followingList);
                    }

                    li.insertAfter(parentLiElement);
                }
            }

            if (newList)
                editor.fire('contentDomInvalidated');

            return true;
        }

        var selection = editor.getSelection(), ranges = selection && selection.getRanges(), iterator = ranges.createIterator(), range;

        while ((range = iterator.getNextRange())) {
            var rangeRoot = range.getCommonAncestor(), nearestListBlock = rangeRoot;

            while (nearestListBlock && !(nearestListBlock.type == CKEDITOR.NODE_ELEMENT && context[nearestListBlock.getName()]))
                nearestListBlock = nearestListBlock.getParent();

            // Avoid having selection boundaries out of the list.
            // <ul><li>[...</li></ul><p>...]</p> => <ul><li>[...]</li></ul><p>...</p>
            if (!nearestListBlock) {
                if ((nearestListBlock = range.startPath().contains(context)))
                    range.setEndAt(nearestListBlock, CKEDITOR.POSITION_BEFORE_END);
            }

            // Avoid having selection enclose the entire list. (#6138)  [<ul><li>...</li></ul>] =><ul><li>[...]</li></ul>
            if (!nearestListBlock) {
                var selectedNode = range.getEnclosedNode();
                if (selectedNode && selectedNode.type == CKEDITOR.NODE_ELEMENT && selectedNode.getName() in context) {
                    range.setStartAt(selectedNode, CKEDITOR.POSITION_AFTER_START);
                    range.setEndAt(selectedNode, CKEDITOR.POSITION_BEFORE_END);
                    nearestListBlock = selectedNode;
                }
            }

            // Avoid selection anchors under list root. <ul>[<li>...</li>]</ul> => <ul><li>[...]</li></ul>
            if (nearestListBlock && range.startContainer.type == CKEDITOR.NODE_ELEMENT && range.startContainer.getName() in context) {
                var walker = new CKEDITOR.dom.walker(range);
                walker.evaluator = listItem;
                range.startContainer = walker.next();
            }

            if (nearestListBlock && range.endContainer.type == CKEDITOR.NODE_ELEMENT && range.endContainer.getName() in context) {
                walker = new CKEDITOR.dom.walker(range);
                walker.evaluator = listItem;
                range.endContainer = walker.previous();
            }

            if (nearestListBlock)
                return indent(nearestListBlock);
        }
        return 0;
    }

    // Determines whether a node is a list <li> element.
    function listItem(node) {
        return node.type == CKEDITOR.NODE_ELEMENT && node.is('li');
    }

    function neitherWhitespacesNorBookmark(node) {
        return isNotWhitespaces(node) && isNotBookmark(node);
    }

    /**
     * Checks whether the first child of the list is in the path. The list can be extracted from the path or given explicitly e.g. for better performance if
     * cached.
     */
    function firstItemInPath(query, path, list) {
        var firstListItemInPath = path.contains(listItem);
        if (!list)
            list = path.contains(query);

        return list && firstListItemInPath && firstListItemInPath.equals(list.getFirst(listItem));
    };

    /**
     * Outdent:  Returns true if the current list item is at the first level.
     */
    var isFirstLevelList = function isFirstLevelList(editor, list) {
        var firstRange = getSelectedRange(editor);
        return isFirstLevelLiSelected(firstRange);
    }

    var getSelectedRange = function getSelectedRange(editor) {
        var selection = editor.getSelection(), ranges = selection && selection.getRanges();
        var firstRange;
        if (ranges && ranges.length > 0) {
            firstRange = ranges[0];
        }
        return firstRange;
    }

    var getEnclosedLiElement = function getEnclosedLiElement(element) {
        return element.getAscendant('li', true);
    };

    var isFirstLevelLiSelected = function isFirstLevelLiSelected(range) {
        if (range && !range.collapsed) {
            return findWithinRange(range, function(node) {
                //Check if the node is the first level list item.
                return node && node.getAscendant && !node.getAscendant('li');
            });
        } else {
            var liElement = getEnclosedLiElement(range.endContainer);
            return liElement && !liElement.getAscendant('li')
        }
    };

    var findWithinRange = function findWithinRange(range, isFirstLevelLi) {
        var walker = new CKEDITOR.dom.walker(range);
        var node = range.getTouchedStartNode();
        while (node) {
            node = getEnclosedLiElement(node);
            if (isFirstLevelLi(node)) {
                //If the node is the first level li item, return
                return true;
            }
            node = walker.next();
        }
        return false;
    }

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName,
    };

    return pluginModule;
});
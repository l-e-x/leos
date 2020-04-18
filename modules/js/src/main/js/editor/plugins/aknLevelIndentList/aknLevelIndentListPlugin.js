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
define(function aknLevelIndentListPluginModule(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var LOG = require("logger");
    var pluginTools = require("plugins/pluginTools");
    var leosPluginUtils = require("plugins/leosPluginUtils");
    var ORDER_LIST_ELEMENT = "ol";
    var HTML_POINT = "li";

    var pluginName = "aknLevelIndentList";

    var isNotWhitespaces = CKEDITOR.dom.walker.whitespaces( true ),
        isNotBookmark = CKEDITOR.dom.walker.bookmark( false, true ),
        TRISTATE_DISABLED = CKEDITOR.TRISTATE_DISABLED,
        TRISTATE_OFF = CKEDITOR.TRISTATE_OFF;

    var pluginDefinition = {
        requires: 'indent',
        init: function init(editor) {
            var globalHelpers = CKEDITOR.plugins.indent;

            // Register commands.
            globalHelpers.registerCommands( editor, {
                aknindentsubpararaph: new commandDefinition( editor, 'aknindentsubparagraph', true ),
                aknoutdentsubpararaph: new commandDefinition( editor, 'aknoutdentsubparagraph' )
            } );

            function commandDefinition(editor) {
                globalHelpers.specificDefinition.apply( this, arguments );

                // Require ul OR ol list.
                this.requiredContent = ['ul', 'ol', 'p'];

                // Indent and outdent lists with TAB/SHIFT+TAB key. Indenting can
                // be done for any list item that isn't the first child of the parent.
                editor.on('key', function(evt) {
                    var path = editor.elementPath();

                    if (editor.mode != 'wysiwyg')
                        return;

                    if (evt.data.keyCode == this.indentKey) {
                        // Prevent of getting context of empty path (#424)(https://dev.ckeditor.com/ticket/17028).
                        if (!path) {
                            return;
                        }

                        var list = this.getContext(path);

                        if (list) {
                            // Don't indent if in first list item of the parent.
                            // Outdent, however, can always be done to collapse
                            // the list into a paragraph (div).
                            if (this.isIndent && firstItemInPath(this.context, path, list))
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
                //
                // * Indent job (priority=10), before indentblock.
                //
                //   This job is before indentblock because, if this plugin is
                //   loaded it has higher priority over indentblock. It means that,
                //   if possible, nesting is performed, and then block manipulation,
                //   if necessary.
                //
                // * Outdent job (priority=30), after outdentblock.
                //
                //   This job got to be after outdentblock because in some cases
                //   (margin, config#indentClass on list) outdent must be done on
                //   block-level.

                this.jobs[this.isIndent ? 10 : 30] = {
                    refresh: this.isIndent ?
                        function(editor, path) {
                            var list = this.getContext(path);
                            var range = getSelectedRange(editor);
                            if(isFirstItemInLevel(path, list) || _shouldDisableTriState(range)){
                                return TRISTATE_DISABLED;
                            }else if (list && list.getName() === 'ol' && leosPluginUtils.isLevelList(list) && !isFirstItemInLevel(path, list)
                                && !_isLevelListDepthMoreThanThreshold(getEnclosedLevelElement(range.startContainer), getEnclosedLevelElement(range.endContainer), leosPluginUtils.MAX_LEVEL_LIST_DEPTH)) {
                                return TRISTATE_OFF;
                            } else if (!list
                                || firstItemInPath( this.context, path, list )
                                || _isListDepthMoreThanThreshold(getEnclosedLiElement(range.startContainer), getEnclosedLiElement(range.endContainer), leosPluginUtils.MAX_LIST_LEVEL) ) {
                                return TRISTATE_DISABLED;
                            } else {
                                return TRISTATE_OFF;
                            }
                        } : function(editor, path) {
                            var list = this.getContext(path);
                            // custom code to disable the outdent toolbar button for first level list items.
                            if (!list || isFirstLevelList(editor, list)) {
                                return TRISTATE_DISABLED;
                            } else {
                                return TRISTATE_OFF;
                            }
                        },

                    exec: CKEDITOR.tools.bind(aknindentList, this)
                };
            }

            CKEDITOR.tools.extend(commandDefinition.prototype, globalHelpers.specificDefinition.prototype, {
                // Elements that, if in an elementpath, will be handled by this
                // command. They restrict the scope of the plugin.
                context: {ol: 1, ul: 1}
            });
        }
    };
    
    function aknindentList(editor) {
        var that = this, database = this.database, context = this.context, range;
        editor.fire("beforeAknIndentList");

        function indent(listNode) {
            // Our starting and ending points of the range might be inside some blocks under a list item...
            // So before playing with the iterator, we need to expand the block to include the list items.

            var isListElement = leosPluginUtils.isLevelList(listNode);
            var isRangeStartInTextElement = isListElement && range.startContainer.type !== CKEDITOR.NODE_ELEMENT;
            var startContainer = isRangeStartInTextElement ? range.startContainer.getParent() : range.startContainer,
                endContainer = isRangeStartInTextElement ? range.endContainer.getParent() : range.endContainer;
            while (!isListElement && startContainer && !startContainer.getParent().equals(listNode))
                startContainer = startContainer.getParent();
            while (!isListElement && endContainer && !endContainer.getParent().equals(listNode))
                endContainer = endContainer.getParent();

            if (!startContainer || !endContainer
                || (that.isIndent && _isListDepthMoreThanThreshold(startContainer, endContainer, leosPluginUtils.MAX_LIST_LEVEL)) && !_isOnlyLevelElementSelected(startContainer, endContainer)){
                return false;
            }

            // Now we can iterate over the individual items on the same tree depth.
            var block = startContainer, itemsToMove = [], stopFlag = false;

            while (!stopFlag) {
                if(block){
                    if ( block.equals( endContainer ) )
                        stopFlag = true;

                    itemsToMove.push(block);
                    block = block.getNext();

                }else{
                    stopFlag = true;
                }
            }

            if ( itemsToMove.length < 1 )
                return false;

            // Do indent or outdent operations on the array model of the list, not the
            // list's DOM tree itself. The array model demands that it knows as much as
            // possible about the surrounding lists, we need to feed it the further
            // ancestor node that is still a list.
            var listParents = listNode.getParents(true);
            for (var i = 0; i < listParents.length; i++) {
                if (listParents[i].getName && context[listParents[i].getName()]) {
                    listNode = listParents[i];
                    break;
                }
            }

            var indentOffset = that.isIndent ? 1 : -1,

                startItem = itemsToMove[0], lastItem = itemsToMove[itemsToMove.length - 1],

                // Convert the list DOM tree into a one dimensional array.
                listArray = CKEDITOR.plugins.leosLevelList.listToArray(listNode, database),

                // Apply indenting or outdenting on the array.
                baseIndent = listArray[lastItem.getCustomData('listarray_index')].indent;

            for (i = startItem.getCustomData('listarray_index'); i <= lastItem.getCustomData('listarray_index'); i++) {
                listArray[i].indent += indentOffset;
                // Make sure the newly created sublist get a brand-new element of the same type. (http://dev.ckeditor.com/ticket/5372)
                if (indentOffset > 0) {
                    var isLevelElement = leosPluginUtils.isLevelList(listArray[i].element.getAscendant('ol',true));
                    var listRoot = isLevelElement ? listArray[i].grandparent : listArray[i].parent;

                    // Find previous list item which has the same indention offset as the new indention offset
                    // of current item to copy its root tag (so the proper list-style-type is used) (#842).
                    for ( var j = i - 1; j >= 0; j-- ) {
                        if ( listArray[ j ].indent === indentOffset ) {
                            listRoot = isLevelElement ? listArray[ j ].grandparent : listArray[ j ].parent;
                            break;
                        }
                    }
                    if(_isLevelElementContainsOrderedList(listArray[i].element)){
                        listArray[i+1].indent = listArray[i].indent;
                        listArray[i+1].indentThisElement = true;
                    }
                    if (isLevelElement) {
                        var newElement = new CKEDITOR.dom.element(listArray[i].parent.getName(), listArray[i].parent.getDocument());
                        newElement.setAttributes(listArray[i].element.getAttributes());
                        newElement.setAttribute('data-akn-mp-tag-name', 'p');
                        listArray[i].element = newElement;
                        listArray[i].indentThisElement = true;
                    }
                    listArray[i].parent = new CKEDITOR.dom.element(listRoot.getName(), listRoot.getDocument());
                }
            }

            for (i = lastItem.getCustomData('listarray_index') + 1; i < listArray.length && listArray[i].indent > baseIndent; i++)
               listArray[i].indent += indentOffset;

            // Convert the array back to a DOM forest (yes we might have a few subtrees now).
            // And replace the old list with the new forest.
            var newList = CKEDITOR.plugins.leosLevelList.arrayToList(listArray, database, null, editor.config.enterMode, listNode.getDirection());

            // Avoid nested <li> after outdent even they're visually same,
            // recording them for later refactoring.(http://dev.ckeditor.com/ticket/3982)
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

            if (leosPluginUtils.isLevelList(listNode)) {
                newList.listNode.replace(listNode.getChildren().toArray().find(element => element.getName() === 'li'));
            } else {
                newList.listNode.replace(listNode);
            }


            // Move the nested <li> to be appeared after the parent.
            if (pendingLis && pendingLis.length) {
                for (i = 0; i < pendingLis.length; i++) {
                    var li = pendingLis[i], followingList = li;

                    // Nest preceding <ul>/<ol> inside current <li> if any.
                    while ((followingList = followingList.getNext()) && followingList.is && followingList.getName() in context) {
                        // IE requires a filler NBSP for nested list inside empty list item,
                        // otherwise the list item will be inaccessiable. (http://dev.ckeditor.com/ticket/4476)
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

        var selection = editor.getSelection(),
            ranges = selection && selection.getRanges(),
            iterator = ranges.createIterator();

        while ((range = iterator.getNextRange())) {
            var nearestListBlock = range.getCommonAncestor();

            while ( nearestListBlock && !( nearestListBlock.type == CKEDITOR.NODE_ELEMENT && context[ nearestListBlock.getName() ] ) ) {
                // Avoid having plugin propagate to parent of editor in inline mode by canceling the indentation. (http://dev.ckeditor.com/ticket/12796)
                if ( editor.editable().equals( nearestListBlock ) ) {
                    nearestListBlock = false;
                    break;
                }
                nearestListBlock = nearestListBlock.getParent();
            }

            // Avoid having selection boundaries out of the list.
            // <ul><li>[...</li></ul><p>...]</p> => <ul><li>[...]</li></ul><p>...</p>
            if (!nearestListBlock) {
                if ((nearestListBlock = range.startPath().contains(context)))
                    range.setEndAt(nearestListBlock, CKEDITOR.POSITION_BEFORE_END);
            }

            // Avoid having selection enclose the entire list. (http://dev.ckeditor.com/ticket/6138)
            // [<ul><li>...</li></ul>] =><ul><li>[...]</li></ul>
            if (!nearestListBlock) {
                var selectedNode = range.getEnclosedNode();
                if (selectedNode && selectedNode.type == CKEDITOR.NODE_ELEMENT && selectedNode.getName() in context) {
                    range.setStartAt(selectedNode, CKEDITOR.POSITION_AFTER_START);
                    range.setEndAt(selectedNode, CKEDITOR.POSITION_BEFORE_END);
                    nearestListBlock = selectedNode;
                }
            }

            // Avoid selection anchors under list root.
            // <ul>[<li>...</li>]</ul> => <ul><li>[...]</li></ul>
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

    function _shouldDisableTriState(range) {
        var startContainer = range.startContainer;
        var endContainer = range.endContainer;
        var startAtHeading = range.startContainer.getAscendant('h2', true);
        var endAtHeading = range.endContainer.getAscendant('h2', true);

        if (leosPluginUtils.isSelectionInFirstLevelList(startContainer) || leosPluginUtils.isSelectionInFirstLevelList(endContainer)) {
            return (startContainer && endContainer && _isCrossListSelectionIncludesFirstLevelList(startContainer, endContainer)) || startAtHeading || endAtHeading;
        } else {
            return false;
        }

    }

    function _isCrossListSelectionIncludesFirstLevelList(startItem, endItem) {
        return !startItem.getAscendant('ol').equals(endItem.getAscendant('ol'))
    }

    function _isLevelListDepthMoreThanThreshold(startSelection, endSelection, maxLevel) {
        var isStartSelectionDeeperThanThreshold = isSingleLevelElementDeeperThanThreshold(startSelection, maxLevel);
        var isEndSelectionDeeperThanThreshold = isSingleLevelElementDeeperThanThreshold(endSelection, maxLevel);
        return isStartSelectionDeeperThanThreshold || isEndSelectionDeeperThanThreshold;
    }

    function isSingleLevelElementDeeperThanThreshold(selected, maxLevel){
        if(!selected){
            return false;
        }
        if (maxLevel > 0) {
            return _isLevelDownsideDepthMoreThanThreshold(selected, maxLevel);
        } else {
            return true;
        }
    }

    /**
     * Returns true if the depth of the whole tree (ol structure) is more than the threshold maxLevel.
     * The total tree depth is given by the level upside + level downside of the selected element.
     */
    function _isListDepthMoreThanThreshold(startSelection, endSelection, maxLevel) {
        var isStartSelectionDeeperThanThreshold = isSingleElementDeeperThanThreshold(startSelection, maxLevel);
        var isEndSelectionDeeperThanThreshold = isSingleElementDeeperThanThreshold(endSelection, maxLevel);
        LOG.debug("Depth calculation. isStartSelectionDeeperThanThreshold:" + isStartSelectionDeeperThanThreshold + ", isEndSelectionDeeperThanThreshold:" + isEndSelectionDeeperThanThreshold);
        return isStartSelectionDeeperThanThreshold || isEndSelectionDeeperThanThreshold;
    }

    function _isOnlyLevelElementSelected(startSelection, endSelection) {
        return ((endSelection.getName() === 'p' && endSelection.getAscendant('ol',true) && leosPluginUtils.isLevelList(endSelection.getAscendant('ol',true))) &&
            (startSelection.getName() === 'p' && startSelection.getAscendant('ol',true) && leosPluginUtils.isLevelList(startSelection.getAscendant('ol',true))));
    }

    function isSingleElementDeeperThanThreshold(selected, maxLevel){
        if(!selected){
            return false;
        }
        selected = selected.getAscendant(HTML_POINT, true);//get first li it founds in hierarchy considering even itself
        var actualLevel = leosPluginUtils.calculateListLevel(selected);
        var stopLevelDownside = maxLevel - actualLevel;
        //LOG.debug("Depth maxLevel:" + maxLevel + ", actualLevel:" + actualLevel + ", stopLevelDownside: " + stopLevelDownside);
        if (stopLevelDownside > 0) {
            // if maxlevel is 4, and we selected an element in the second level, checks if any of the child has depth more than 2.
            return _isDownsideDepthMoreThanThreshold(selected, stopLevelDownside);
        } else {
            return true;
        }
    }

    /**
     * Returns true if the depth of any of child of the selected element reach stopLevel.
     * Html Structure:
     * <article>
     *     <ol>
     *        <li> ... </li>
     *        <li>
     *            <p/>                     (1, 2, 3 items)  (cursor here, actual depth level 1)
     *            <ol>
     *               <li> ... </li>
     *               <li>
     *                    <p/>             (a, b, c items)  (cursor here, actual depth level 2)
     *                    <ol>
     *                       <li> ... </li>
     *                       <li>
     *                           <p/>      (i, ii items)    (cursor here, actual depth level 3)
     *                           <ol>
     *                               <li/> (- items)        (cursor here, actual depth level 4)
     *                               <li/>
     *                          </ol>
     *                       </li>
     *                    </ol>
     *               </li>
     *            <ol>
     *        </li>
     *     </ol>
     * <article>
     */
    function _isDownsideDepthMoreThanThreshold(element, stopLevel) {
        var level = 0;
        var childList = element.getChildren();
        for (var child_idx = 0; child_idx < childList.count(); child_idx++) {
            var child = childList.getItem(child_idx);
            var child_name = leosPluginUtils.getElementName(child);
            // only if we find an order_list_element (ol) it means we found another depth level
            if (child_name === ORDER_LIST_ELEMENT) {
                level++;
                //LOG.debug(child_idx+"-th child found: " + child_name + ", calculated level: " + level + ", stopLevel: " + stopLevel);
                if (level >= stopLevel) {
                    return true;
                } else {
                    // go deeper in the list, check the children (li elements)
                    for (var li_item_idx = 0; li_item_idx < child.getChildren().count(); li_item_idx++) {
                        var listItem = child.getChildren().getItem(li_item_idx);
                        if (_isDownsideDepthMoreThanThreshold(listItem, (stopLevel - level))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    function _isLevelDownsideDepthMoreThanThreshold(element, stopLevel) {
        var level = 0;
        var childList =  _isLevelElementContainsOrderedList(element) ? new Array(element.getNext()) : [];
        for (var i = 0; i < childList.length; i++) {
            var child = childList[i];
            var child_name = leosPluginUtils.getElementName(child);
            // only if we find an order_list_element (ol) it means we found another depth level
            if (child_name === ORDER_LIST_ELEMENT) {
                level++;
                if (level >= stopLevel) {
                    return true;
                } else {
                    // go deeper in the list, check the children (li elements)
                    for (var li_item_idx = 0; li_item_idx < child.getChildren().count(); li_item_idx++) {
                        var listItem = child.getChildren().getItem(li_item_idx);
                        if (_isDownsideDepthMoreThanThreshold(listItem, (stopLevel - level))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    function _isLevelElementContainsOrderedList(element){
        return element && leosPluginUtils.isLevelList(element.getAscendant(ORDER_LIST_ELEMENT), true) && element.getNext()
            && element.getNext().type === CKEDITOR.NODE_ELEMENT && element.getNext().getName() === ORDER_LIST_ELEMENT;
    }

    // Determines whether a node is a list <li> element.
    function listItem(node) {
        return node.type == CKEDITOR.NODE_ELEMENT && (node.is('li') || node.is('p'));
    }

    function neitherWhitespacesNorBookmark(node) {
        return isNotWhitespaces(node) && isNotBookmark(node);
    }

    /**
     * Checks whether the first child of the list is in the path.ORDER_LIST_ELEMENT
     * The list can be extracted from the path or given explicitly
     * e.g. for better performance if cached.
     *
     * @since 4.4.6
     * @param {Object} query See the {@link CKEDITOR.dom.elementPath#contains} method arguments.
     * @param {CKEDITOR.dom.elementPath} path
     * @param {CKEDITOR.dom.element} [list]
     * @returns {Boolean}
     * @member CKEDITOR.plugins.indentList
     */
    function firstItemInPath(query, path, list) {
        var firstListItemInPath = path.contains(listItem);
        if (!list)
            list = path.contains(query);

        return list && firstListItemInPath && firstListItemInPath.equals(list.getFirst(listItem));
    }
    
    /**
     * returns true when:
     *
     * 1) Child of <li> is a Text node:
     * <li>
     *      Text
     * </li>
     *
     * 2) Child is a single <p> inside <li>
     * <li>
     *     <p>Text</p>
     * </li>
     **/
    function isFirstItemInLevel(path, list) {
        var firstListItemInPath = path.contains(listItem);
        // case 1)
        if (firstListItemInPath
            && firstListItemInPath.type === CKEDITOR.NODE_TEXT
            && firstListItemInPath.getParent().type === CKEDITOR.NODE_ELEMENT
            && firstListItemInPath.getParent().getName() === 'li') {
            return true;
        }
        // case 2)
        if (firstListItemInPath && firstListItemInPath.type === CKEDITOR.NODE_ELEMENT) {
            if (firstListItemInPath.getName() === 'p') {
                var listElement = list.getFirst(listItem);
                var firstElement = listElement.getFirst(listItem);
                if (firstListItemInPath.equals(firstElement)) {
                    return true;
                }
            } else if (firstListItemInPath.getName() === 'li' && leosPluginUtils.isLevelList(firstListItemInPath.getAscendant('ol'))) {
                return firstListItemInPath.find('p') && firstListItemInPath.find('p').$.length <= 1;
            }
        }
        return false;
    }

    /**
     * Outdent:  Returns true if the current list item is at the first level.
     */
    var isFirstLevelList = function isFirstLevelList(editor, list) {
        var firstRange = getSelectedRange(editor);
        return isFirstLevelLiSelected(firstRange);
    };

    var getSelectedRange = function getSelectedRange(editor) {
        var selection = editor.getSelection(), ranges = selection && selection.getRanges();
        var firstRange;
        if (ranges && ranges.length > 0) {
            firstRange = ranges[0];
        }
        return firstRange;
    };

    var getEnclosedLiElement = function getEnclosedLiElement(element) {
        return element.getAscendant('li', true);
    };

    var getEnclosedLevelElement = function getEnclosedLevelElement(element){
        return element.getAscendant('p', true);
    }

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
    };
    
    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});

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
; // jshint ignore:line
define(function leosCrossReferenceDialog(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var pluginName = "leosCrossReference";
    var jsTreeCssPath = pluginTools.getResourceUrl(pluginName, "css/jstree/themes/default/style.css");
    var cssPath = pluginTools.getResourceUrl(pluginName, "css/leosCrossReference.css");

    var dialogDefinition = {
        dialogName: "leosCrossReferenceDialog"
    };

    dialogDefinition.initializeDialog = function initializeDialog(editor) {
        var htmlTocTemplate = '<div id="treeContainer"></div>';
        var htmlContentTemplate = '<div id="content"><div id="contentContainer"  class="selected-content"></div>'
            + '<div id="referenceElement" class="selected-content"><label id="lblRefElement"><strong>Referenced element: </strong></lable><label id="pathLabel"> </label></div>'
            + '<div id="refAs"><label id="lblRefAs"><strong>Show reference as: </strong></label><input placeholder="Label your reference..." id="refrenceTextLabel" type="text"  size="40" ><div id="errorLabel" > </div></div></div>';

        function handleOkDialogButton(dialog) {
            var okButton = dialog._.buttons['ok'];
            var internalOk = okButton.click;
            okButton.click = function(event) {
                if (nodeContentHandler.checkIfValid()) {
                    internalOk.call(okButton, event);
                }
            }
        }

        var dialogDefinition = {
            title: "Cross-reference",
            minWidth: 800,
            minHeight: 600,
            resizable: CKEDITOR.DIALOG_RESIZE_NONE,
            contents: [{
                id: "info",
                elements: [{
                    id: "crossRef",
                    type: 'hbox',
                    className: 'crDialogbox',
                    widths: ['30%', '60%'],
                    height: 600,
                    /* Set the dialog state upon data from widget, this is only used for editing already existed widgets. */
                    setup: function setup(widget) {
                        nodeContentHandler.setUpExisting(widget.element.getHtml(), $(widget.element.$).attr('href'));
                    },
                    /* This function set up final state on the widget. */
                    commit: function commit(widget) {
                        if (nodeContentHandler.isExistingValuesChanged()) {
                            // fire data event so editor is set to dirty
                            widget.fire("data");
                        }
                        var $widget = $(widget.element.$);
                        // Sets the user friendly label for referenced element
                        $widget.html(nodeContentHandler.getRefrenceText());
                        $widget.attr("href", nodeContentHandler.getNodeId());
                    },

                    children: [{
                        type: 'html',
                        title: 'Table of Content',
                        html: htmlTocTemplate,
                        className: 'crTableOfContent'
                    }, {
                        type: 'html',
                        title: 'Selected Content',
                        className: 'crContent',
                        html: htmlContentTemplate
                    }]
                }]
            }],
            onLoad: function(event) {
                handleOkDialogButton(event.sender);
                // load plugin css to dialog
                appendPluginCss(cssPath);
                nodeContentHandler.init(editor);
                tableOfContentHandler.init(editor, nodeContentHandler);

            },
            onShow: function() {
                nodeContentHandler.reset(editor);
                tableOfContentHandler.reset();
            }

        };
        return dialogDefinition;
    };

    function appendPluginCss(css) {
        $('<link>').appendTo('head').attr({
            type : 'text/css',
            rel : 'stylesheet',
            href : css
        });
    }

    /*
     * This handler is used to manage the content display for the selected node in the table of content and the selection of element in the content pane itself.
     * The final outcome is the path for the node selected in the table of content plus path of the selected element in the content pane.
     */
    var nodeContentHandler = {
        init: function init(editor) {
            this.SELECTABLE_ELEMENTS = "heading,paragraph,subparagraph,point,recital,citation";
            // this is right-upper div conainer, which holds the data for selected node
            this.$contentContainer = $('#contentContainer');
            this.$pathLabel = $("#pathLabel");
            this.$refrenceTextLabel = $("#refrenceTextLabel");
            this.$errorLabel = $("#errorLabel");
            this.fullPath = "";
            this.treePath = "";
            this.treeNodeId = "";
            this.nodeId = "";
            this.refrenceText = "";
            var that = this;
            editor.on("receiveElement", function(event) {
                that.setContent(event.data.elementFragment, event.editor);
                if (that.existingNodeId) {
                    var $existingSelected = that.$contentContainer.find("#" + that.existingNodeId);
                    if ($existingSelected.length == 1 && that.treeNodeId !== that.existingNodeId) {
                        that.selectElementInContent($existingSelected);
                    }
                }
                that.handleOnClick();
            });
        },
        handleOnClick: function handleOnClick() {
            var that = this;
            // adds hidden border
            this.$contentContainer.find(that.SELECTABLE_ELEMENTS).addClass("selectable-element");
            this.$contentContainer.find(this.SELECTABLE_ELEMENTS).click(function(event) {
                var $this = $(this);
                if($this.hasClass("selectable-element-selected")) {
                    that.deSelectElementInContent();
                } else {
                    that.selectElementInContent($this);
                }
                event.stopPropagation();
            });

        },
        selectElementInContent: function selectElementInContent($element) {
            this.setNodeId($element.attr("id"));
            // deselect element if any
            this.$contentContainer.find(this.SELECTABLE_ELEMENTS).removeClass("selectable-element-selected");
            // add highlight for selected element
            $element.addClass("selectable-element-selected");
            // calculate full path: slected node in tree plus elemnet in content
            this.fullPath = this.treePath + "/" + $element.prop("tagName");
            this.setPathLabel(this.fullPath);
            $element.get(0).scrollIntoView(true);
        },
        deSelectElementInContent : function deSelectElementInContent() {
            this.setNodeId(this.treeNodeId);
            // deselect element if any
            this.$contentContainer.find(this.SELECTABLE_ELEMENTS).removeClass("selectable-element-selected");
            // calculate full path: slected node in tree plus elemnet in content
            this.fullPath = this.treePath ;
            this.setPathLabel(this.fullPath);
        },
        /* Display the path for the selected node. */
        setPathLabel: function setPathLabel(path) {
            this.$pathLabel.html(path);
        },
        /* Display content for the selected node in the table of content. */
        setContent: function setContent(htmlText, editor) {
            if(editor) {
                this.normalizeStyles(editor);
            }
            this.$contentContainer.html(htmlText);
        },
        /*Removes reset class from Ckeditor*/
        normalizeStyles: function normalizeStyles(editor) {
            var dialogElement = document.getElementsByClassName("cke_editor_" + editor.name + "_dialog");
            //Fix for firefox (argument is expected in the item())
            var $item = $(dialogElement.item(0));
            $item.removeClass("cke_reset_all");
        },
        /* Set the path for currently selected node in the table of content. */
        setTreePath: function setTreePath(path) {
            this.fullPath = this.treePath = path;
            this.setPathLabel(this.treePath);
        },
        // Retrieve the refrenced node label(this is set by the user)
        getRefrenceText: function getRefrenceText() {
            this.refrenceText = CKEDITOR.tools.htmlEncode(this.$refrenceTextLabel.val());
            return this.refrenceText;
        },
        setRefrenceTextLabel: function setRefrenceTextLabel(label) {
            this.$refrenceTextLabel.val(CKEDITOR.tools.htmlDecode(label));
        },
        getNodeId: function getNodeId() {
            return this.nodeId;
        },
        setNodeId: function setNodeId(nodeId) {
            this.nodeId = nodeId;
        },
        setTreeNodeId: function setTreeNodeId(treeNodeId) {
            this.nodeId = this.treeNodeId = treeNodeId;
        },
        reset: function reset(editor) {
            this.refrenceText = "";
            this.fullPath = "";
            this.nodeId = this.treeNodeId = "";
            this.setTreePath("");
            this.setPathLabel("");
            this.setContent("", editor);
            this.setRefrenceTextLabel("");
            this.clearErrors();
            this.setExistingValues(undefined, undefined);
        },
        checkIfValid: function checkIfValid() {
            if (this.getRefrenceText().trim().length === 0 || this.treePath.trim().length === 0) {
                this.showErrors();
                return false;
            }
            this.clearErrors();
            return true;
        },
        showErrors: function showErrors() {
            this.$errorLabel.addClass("error-label");
            this.$errorLabel.html("Referenced element is required and reference label should not be empty.");
        },
        clearErrors: function clearErrors() {
            this.$errorLabel.removeClass("error-label");
            this.$errorLabel.html("");
        },
        /* Used in case of the editing existing cross-ref */
        setExistingValues: function setExistingValues(label, nodeId) {
            this.existingUeserMessage = label;
            this.existingNodeId = nodeId;
        },
        isExistingValuesChanged: function isExistingValuesChanged() {
            return (this.existingUeserMessage !== this.refrenceText || this.existingNodeId !== this.nodeId);
        },

        setUpExisting: function setUpExisting(refText, nodeId) {
            this.setExistingValues(refText, nodeId);
            this.setRefrenceTextLabel(refText);
            tableOfContentHandler.loadTableOfContent(nodeId);
        }
    };

    /* This handler is used to manage the tree for table of content. */
    var tableOfContentHandler = {
        init: function init(editor, nodeContentHandler) {
            this.editor = editor;
            this.nodeContentHandler = nodeContentHandler;
            this.treePath = "";
            this.$treeContainer = $('#treeContainer');
            // this.loadTableOfContent();
            this.handleTableOfContentLoaded();
        },
        loadTableOfContent: function loadTableOfContent(selectedNodeId) {
            selectedNodeId = !selectedNodeId || selectedNodeId.trim().length == 0 ? undefined : selectedNodeId;
            this.editor.fire("requestToc", {
                selectedNodeId: selectedNodeId
            });
        },
        handleTableOfContentLoaded: function handleTableOfContentLoaded() {
            var that = this;
            this.editor.on("receiveToc", function(event) {
                var tocItems = event.data.tocItems;
                var elementAncestorsIds = event.data.elementAncestorsIds;
                var theMostNestedTreeItem = that.matchSelectedTreeItem({
                    tocItems: tocItems,
                    ancestorsIds: elementAncestorsIds,
                    nodeId: nodeContentHandler.existingNodeId
                });
                that.createTreeInstance(tocItems, theMostNestedTreeItem);
            });
        },
        handleNodeSelection: function handleNodeSelection() {
            var that = this;
            this.$treeContainer.on("changed.jstree", function(e, data) {
                // only one node can be selected at given time
                var treeInstance = data.instance;
                var selectedNodeId = data.selected[0];
                that.nodeContentHandler.setTreeNodeId(selectedNodeId);
                var treePath = treeInstance.get_path(selectedNodeId, "/");
                that.nodeContentHandler.setTreePath(treePath);
                var tocItem = treeInstance.get_node(selectedNodeId).original;
                if (tocItem.type === "ARTICLE" || tocItem.type === "CITATIONS" || tocItem.type === "RECITALS") {
                    that.editor.fire("requestElement", {
                        elementId: tocItem.id,
                        elementType: tocItem.type.toLowerCase()
                    });
                } else {
                    that.nodeContentHandler.setContent(treeInstance.get_node(data.node).text, that.editor);
                }
            });

        },
        reset: function reset() {
            try {
                // deselect_all throw exception, it could be some bug in jstree
                this.$treeContainer.jstree("deselect_all");
            } catch (e) {
                // ignoring cause this is bug
            }
            this.$treeContainer.jstree("close_all");
        },
        matchSelectedTreeItem: function matchSelectedTreeItem(treeContext) {
            var selectedIds = [];
            if (treeContext.ancestorsIds) {
                selectedIds = selectedIds.concat(treeContext.ancestorsIds);
            }

            if (treeContext.nodeId) {
                selectedIds.push(treeContext.nodeId);
            }

            return this._matchSelectedTreeItem(treeContext.tocItems, selectedIds);
        },
        _matchSelectedTreeItem: function _matchSelectedTreeItem(tocItems, selectedIds) {
            var currentItem;
            if (!selectedIds || selectedIds.length == 0) {
                return;
            }
            var currentId = selectedIds[0];
            var that = this;
            tocItems.forEach(function(item) {
                if (item.id === currentId) {
                    currentItem = item;
                    if (item.children) {
                        var childCurrentItem = that._matchSelectedTreeItem(item.children, selectedIds.slice(1));
                        if (childCurrentItem) {
                            currentItem = childCurrentItem;
                        }
                    }
                }
            });
            return currentItem;
        },
        /*On every cross refrence widget setup(creation or edition of the existing one) the tree is re-created with the reload of tocItems form serverside to have the latest data*/
        createTreeInstance: function createTreeInstance(tocItems, selectedItem) {
            this.$treeContainer.jstree("destroy");
            this.$treeContainer.jstree({
                'core': {
                    "themes": {
                        "url": jsTreeCssPath,
                        "icons" : false
                    },
                    "data": tocItems,
                    "multiple": false
                }
            });
            var that = this;
            this.$treeContainer.on("loaded.jstree", function(arg) {
                that.$treeContainer.jstree("select_node", selectedItem);
            });
            this.handleNodeSelection();
        }
    };

    return dialogDefinition;
});

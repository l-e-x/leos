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
define(function leosCrossReferenceDialog(require) {
    "use strict";

    // load module dependencies
    var CKEDITOR = require("promise!ckEditor");
    var pluginTools = require("plugins/pluginTools");
    var pluginName = "leosCrossReference";
    var jsTreePath = pluginTools.toUrl("jsTree");
    var $ = require('jquery');
    var cssPath = pluginTools.getResourceUrl(pluginName, "css/leosCrossReference.css");

    var leosCrossReferenceRuleResolver = require("./leosCrossReferenceRuleResolver");

    var dialogDefinition = {
        dialogName: "leosCrossReferenceDialog"
    };

    dialogDefinition.initializeDialog = function initializeDialog(editor) {
        var docType = editor.LEOS.type;
        var htmlTocTemplate = '<div id="treeContainer"></div>';
        var htmlContentTemplate = '<div id="content"><div id="contentContainer"  class="selected-content '
                + docType
                + '"></div>'
                + '<div id="referenceElement" class="selected-content"><label id="lblRefElement"><strong>Selected element(s): </strong></lable><label id="pathLabel"> </label></div>'
                + '<div id="refAs"><label id="lblRefAs"><strong>Reference shown as: </strong></label><input id="refrenceTextLabel" type="text" readonly="true"><div id="brokenRefLabel"></div><div id="errorLabel"></div></div></div>';

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
            title: "Internal reference",
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
                        var brokenRef =  widget.element.getAttribute('leos:broken');
                        var existingSelectedMrefId = widget.element.getId();
                        nodeContentHandler.setSelectedMrefId(existingSelectedMrefId)
                        if(brokenRef) {
                            nodeContentHandler.showBrokenRefLabel(widget.element.getOuterHtml());
                            nodeContentHandler.showErrors("brokenRef");
                        } else {
                            nodeContentHandler.clearBrokenRefLabel();
                            var $existingSelectedItems = $(widget.element.$).children('ref');
                            for (var index = 0; index < $existingSelectedItems.length; index++) {
                                var refId = $existingSelectedItems[index].getAttribute('href');
                                nodeContentHandler.addSelectedElementId(refId);
                            }
                        }
                        nodeContentHandler.setUpExisting(widget.element.getHtml());
                    },
                    /* This function set up final state on the widget. */
                    commit: function commit(widget) {
                        if (nodeContentHandler.isExistingValuesChanged()) {
                            // fire data event so editor is set to dirty
                            widget.fire("data");
                        }
                        var $widget = $(widget.element.$);
                        $widget.removeAttr('leos:broken');
                        // Sets the user friendly label for referenced element
                        $widget.html(nodeContentHandler.getRefrenceText());
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
            type: 'text/css',
            rel: 'stylesheet',
            href: css
        });
    }

    /*
     * This handler is used to manage the content display for the selected node in the table of content and the selection of element in the content pane itself.
     * The final outcome is the path for the node selected in the table of content plus path of the selected element in the content pane.
     */
    var nodeContentHandler = {

        init: function init(editor) {
            this.editor = editor;
            this.SELECTABLE_ELEMENTS = "heading,paragraph,subparagraph,point,alinea,recital";
            this.PATH_SEPARATOR = "/";
            // this is right-upper div container, which holds the data for selected node
            this.$contentContainer = $('#contentContainer');
            this.$pathLabel = $("#pathLabel");
            this.$refrenceTextLabel = $("#refrenceTextLabel");
            this.$errorLabel = $("#errorLabel");
            this.$brokenRefLabel = $("#brokenRefLabel");
            this.fullPath = "";
            this.treePath = "";
            this.treeNodeIds = new Set();
            this.elementIds = new Set();
            this.refrenceText = "";
            this.articleHeadingAknName = 'data-akn-name';
            this.articleHeadingId = 'data-akn-heading-id';
            
            var that = this;
            editor.on("receiveElement", function(event) {
                that.setContent(event.data.elementFragment, event.editor);
                that.showExistingReferences();
                that.handleOnClick();
            });

            editor.on("receiveRefLabel", function(event) {
                that.setRefrenceTextLabel(event.data);
            });
        },
        showExistingReferences: function showExistingReferences() {
            var that = this;
            this.existingRefIds.forEach(function(nodeId) {
                if (!that.getTreeNodeIds().has(nodeId)) {
                    var $existingSelected = that.$contentContainer.find("#" + nodeId);
                    $existingSelected && $existingSelected.length > 0 ? that.selectElementInContent($existingSelected) : 
                            that.showWarnings("refChanged");
                }
            });  
        }, 
        handleOnClick: function handleOnClick() {
            var that = this;
            // adds hidden border
            this.$contentContainer.find(that.SELECTABLE_ELEMENTS).addClass("selectable-element");
            this.$contentContainer.find(this.SELECTABLE_ELEMENTS).click(function(event) {
                that.clearErrorLabel();
                var $this = $(this);
                event.ctrlKey ? that.handleMultiSelection($this) : that.handleSingleSelection($this);

                if (that.getSelectedElementIds().size > 0) {
                    that.requestRefLabel(that.getSelectedElementIds());
                } else if (that.getTreeNodeIds().size > 0) {
                    that.requestRefLabel(that.getTreeNodeIds());
                }
                event.stopPropagation();
            });
        },
        selectElementInContent: function selectElementInContent($element) {
            if(this.existingRefIds.size > 0 && !this.existingRefIds.has($element.attr("id"))) {
               this.showWarnings("refChanged");
            }
            // add highlight for selected element
            $element.addClass("selectable-element-selected");
            this.addSelectedElementId($element.attr("id"));

            this.setFullPathToPathLabel($element);
            $element.get(0).scrollIntoView(true);
        },
        deSelectElementInContent: function deSelectElementInContent($element) {
            // deselect element if any
            $element.removeClass("selectable-element-selected");
            this.removeSelectedElementId($element.attr("id"));

            this.fullPath = this.treePath;
            var $remainingSelectedElements = this.$contentContainer.find(".selectable-element-selected");
            if ($remainingSelectedElements.length > 0) {
                var that = this;
                $remainingSelectedElements.each(function(index) {
                    that.setFullPathToPathLabel($(this));
                });
            } else { // None is selected clear the node id list
                this.getSelectedElementIds().clear();
                this.setPathLabel(this.fullPath);
            }
        },
        handleSingleSelection: function handleSingleSelection($element) {
            if ($element.hasClass("selectable-element-selected")) {
                this.$contentContainer.find(this.SELECTABLE_ELEMENTS).removeClass("selectable-element-selected");
                this.deSelectElementInContent($element);
            } else {
                // deselect already selected element if any
                this.$contentContainer.find(this.SELECTABLE_ELEMENTS).removeClass("selectable-element-selected");
                this.getSelectedElementIds().clear();
                this.fullPath = this.treePath;
                this.selectElementInContent($element);
            }
        },
        handleMultiSelection: function handleMultiSelection($element) {
            if ($element.hasClass("selectable-element-selected")) {
                this.deSelectElementInContent($element);
            } else {
                var $selectedElements = this.$contentContainer.find('.selectable-element-selected');
                var result = leosCrossReferenceRuleResolver.isSelectionAllowed($element, $selectedElements, false);
                result ? this.selectElementInContent($element) : 
                                             this.showWarnings("selectionNotAllowed");
            }
        },
        // Display the path label for the selected node(s)
        setFullPathToPathLabel: function setFullPathToPathLabel($element) {
            this.fullPath = (this.fullPath === this.treePath) ? "" : this.fullPath;
            this.fullPath += this.calculatePathForElement($element);
            this.fullPath.lastIndexOf(" - ") != -1 ? this.setPathLabel(this.fullPath.substring(0, this.fullPath.lastIndexOf(" - "))) : this
                    .setPathLabel(this.fullPath);
        },
        calculatePathForElement: function calculatePathForElement($element) {
            var that = this;
            // calculate path: selected node in tree plus element in content
            var $num = $element.children("num");
            var parents = $element.parentsUntil("#" + Array.from(this.getTreeNodeIds())[0]);
            var parentPath = "";

            for (var index = parents.length - 1; index >= 0; index--) {
                var $parentNum = $(parents[index]).children("num");
                if ($parentNum != null && $parentNum.length > 0) {
                    parentPath += parents[index].localName + " " + $parentNum.text().replace(".", "") + that.PATH_SEPARATOR;
                }
            }
            return this.treePath + this.PATH_SEPARATOR + parentPath + $element.prop("localName") + " "
                    + (($num.length > 0) ? $num.text().replace(".", "") : ($element.index() + 1)) + " - ";
        },
        /* Display the path for the selected node. */
        setPathLabel: function setPathLabel(path) {
            this.$pathLabel.html(path);
        },
        /* Display content for the selected node in the table of content. */
        setContent: function setContent(htmlText, editor) {
            if (editor) {
                this.normalizeStyles(editor);
            }
            this.$contentContainer.html(htmlText);
        },
        /* Removes reset class from Ckeditor */
        normalizeStyles: function normalizeStyles(editor) {
            var dialogElement = document.getElementsByClassName("cke_editor_" + editor.name + "_dialog");
            // Fix for firefox (argument is expected in the item())
            var $item = $(dialogElement.item(0));
            $item.removeClass("cke_reset_all");
        },
        /* Set the path for currently selected node in the table of content. */
        setTreePath: function setTreePath(path) {
            this.fullPath = this.treePath = path;
            this.setPathLabel(this.treePath);
        },
        getSelectedElementIds: function getSelectedElementIds() {
            return this.elementIds;
        },
        addSelectedElementId: function addSelectedElementId(elementId) {
            this.getSelectedElementIds().add(elementId);
        },
        removeSelectedElementId: function removeSelectedElementId(elementId) {
            this.getSelectedElementIds().delete(elementId);
        },
        getSelectedMrefId: function getSelectedMrefId() {
            return this.selectedMrefId;
        },
        setSelectedMrefId: function setSelectedMrefId(elementId) {
            this.selectedMrefId = elementId;
        },
        setTreeNodeIds: function setTreeNodeIds(treeNodes) {
            var that = this;
            this.getTreeNodeIds().clear();
            this.getSelectedElementIds().clear(); // clear selected content on tree node selection change
            treeNodes.forEach(function(node) {
                that.getTreeNodeIds().add(node.original.id);
            });
        },
        getTreeNodeIds: function getTreeNodeIds() {
            return this.treeNodeIds;
        },
        reset: function reset(editor) {
            this.refrenceText = "";
            this.fullPath = "";
            this.getSelectedElementIds().clear();
            this.getTreeNodeIds().clear();
            this.setTreePath("");
            this.setPathLabel("");
            this.setContent("", editor);
            this.setRefrenceTextLabel("");
            this.clearErrorLabel();
            this.setExistingValues(undefined);
        },
        checkIfValid: function checkIfValid() {
            if(this.treePath.trim().length === 0) {
                this.showErrors("refElementRequired");
                return false;
            } else if (this.getRefrenceText().trim().length === 0) {
                this.showErrors("supportedElementsMessage");
                return false;
            }
            this.clearErrorLabel();
            return true;
        },
        showBrokenRefLabel: function showBrokenRefLabel(brokenRefLabel) {
            var brokenRefMsg = this.editor.lang.leosCrossReference.existingRef + brokenRefLabel; 
            this.$brokenRefLabel.html(brokenRefMsg);
        },
        showErrors: function showErrors(errMsg) {
            this.clearErrorLabel();
            this.$errorLabel.addClass("error-label");
            this.$errorLabel.html(this.editor.lang.leosCrossReference[errMsg]);
        },
        showWarnings: function showWarnings(warningMsg) {
            this.clearErrorLabel();
            this.$errorLabel.addClass("warning-label");
            this.$errorLabel.html(this.editor.lang.leosCrossReference[warningMsg]);
        },
        clearErrorLabel: function clearErrorLabel() {
            this.$errorLabel.removeClass();
            this.$errorLabel.html("");
        },
        clearBrokenRefLabel: function clearBrokenRefLabel() {
            this.$brokenRefLabel.html("");
        },
        /* Used in case of the editing existing cross-ref */
        setExistingValues: function setExistingValues(label) {
            this.existingUserMessage = label;
            this.existingRefIds = new Set(this.getSelectedElementIds());
        },
        isExistingValuesChanged: function isExistingValuesChanged() {
            return (this.existingUserMessage !== this.getRefrenceText() || !this.isEqualSets(this.existingRefIds, this.getSelectedElementIds()));
        },
        // Retrieve the referenced node label(this is set by the user)
        getRefrenceText: function getRefrenceText() {
            return this.refrenceText;
        },
        setRefrenceTextLabel: function setRefrenceTextLabel(label) {
            this.$refrenceTextLabel.val(this.parseRefLabel(label));
        },
        parseRefLabel: function parseRefLabel(label) {
            var el = $('<div>');
            el.html(label);
            if(el.text().search("null") === -1) {
                this.refrenceText = label;
                return el.text();
            } else {
                this.refrenceText = "";
                return "";
            }
        },
        setUpExisting: function setUpExisting(label) {
            this.setExistingValues(label);
            this.setRefrenceTextLabel(label);
            tableOfContentHandler.loadTableOfContent(this.getSelectedElementIds());
        },
        requestRefLabel: function requestRefLabel(nodeList) {
            var currentEditPosition = this.findCurrentEditedElement();
            nodeList = Array.from(nodeList).reduce(function(accumulator, currentValue) {
                accumulator.push("," + currentValue);
                return accumulator;
            }, []);
            var data = currentEditPosition ? {
                references: nodeList,
                currentEditPosition: currentEditPosition
            } : {
                references: nodeList
            };
            this.editor.fire("requestRefLabel", data);
        },
        findCurrentEditedElement: function findCurrentEditedElement() {
            var editor = this.editor;
            var startElement = editor.getSelection().getStartElement();
            if (!startElement) return;
            if (startElement.getId() === null) {
                // if id is null, it means that it is not a valid XML selected element
                // Mref exists, it returns the mref id
                return this.getSelectedMrefId();
            }
            if (startElement.hasAttribute(this.articleHeadingAknName) &&
                startElement.getAttribute(this.articleHeadingAknName) === 'aknHeading') {
                return startElement.getAttribute(this.articleHeadingId);
            } else {
                //FIXME Need to check on data-akn-name attribute to fetch the Id
                var blockBoundryElement = startElement.getAscendant({p: 1,li: 1}, true);
                return blockBoundryElement ? blockBoundryElement.getAttribute('id') : null;
            }
        },       
        isEqualSets: function isEqualSets(set1, set2) {
            if (!set1 || !set2) {
                return false;
            }
            if (set1.size != set2.size) {
                return false;
            }

            set1.forEach(function(value) {
                if (!set2.has(value)) {
                    return false;
                }
            });
            return true;
        }
    };

    /* This handler is used to manage the tree for table of content. */
    var tableOfContentHandler = {
        init: function init(editor, nodeContentHandler) {
            this.editor = editor;
            this.nodeContentHandler = nodeContentHandler;
            this.treePath = "";
            this.$treeContainer = $('#treeContainer');
            this.NbrOfSelectedNodes = 0;
            this.handleTableOfContentLoaded();
        },
        loadTableOfContent: function loadTableOfContent(selectedNodeIds) {
            this.editor.fire("requestToc", {
                selectedNodeIds: Array.from(selectedNodeIds)
            });
        },
        contentRequired: function contentRequired() {
            return (this.NbrOfSelectedNodes <= 1);
        },
        handleTableOfContentLoaded: function handleTableOfContentLoaded() {
            var that = this;
            this.editor.on("receiveToc", function(event) {
                var tocItems = event.data.tocItems;
                var elementAncestorsIds = event.data.elementAncestorsIds;
                var selectedTreeItems = that.matchSelectedTreeItem({
                    tocItems: tocItems,
                    ancestorsIds: elementAncestorsIds,
                    nodeIds: nodeContentHandler.existingRefIds
                });
                that.NbrOfSelectedNodes = selectedTreeItems ? selectedTreeItems.length : 0;
                that.createTreeInstance(tocItems, selectedTreeItems);
            });
        },

        matchSelectedTreeItem: function matchSelectedTreeItem(treeContext) {
            var selectedIds = [];
            if (treeContext.ancestorsIds) {
                selectedIds = selectedIds.concat(treeContext.ancestorsIds);
            }

            if (treeContext.nodeIds) {
                selectedIds = selectedIds.concat(Array.from(treeContext.nodeIds));
            }
            return this._matchSelectedTreeItem(treeContext.tocItems, selectedIds);
        },

        _matchSelectedTreeItem: function _matchSelectedTreeItem(tocItems, selectedIds) {
            var currentItem = [];
            if (!selectedIds || selectedIds.length == 0) {
                return;
            }
            var that = this;
            tocItems.forEach(function(item) {
                if (selectedIds.indexOf(item.id) !== -1) {
                    if (item.children.length > 0) {
                        var childCurrentItem = that._matchSelectedTreeItem(item.children, selectedIds.slice(1));
                        if (childCurrentItem && childCurrentItem.length > 0) {
                            currentItem = currentItem.concat(childCurrentItem);
                        } else {
                            currentItem.push(item);
                        }
                    } else {
                        currentItem.push(item);
                    }
                }
            });
            return currentItem;
        },
        handleNodeSelection: function handleNodeSelection() {
            var that = this;
            this.$treeContainer.on("changed.jstree", function(event, data) {
                if (data && data.node) {
                    that.nodeContentHandler.clearErrorLabel();
                    var treeInstance = data.instance;
                    var selectedNodes = treeInstance.get_selected(true);
                    var firstSelectedNode = selectedNodes[0];
                    var result = leosCrossReferenceRuleResolver.isSelectionAllowed(data.node, firstSelectedNode, true);
                    
                    if (result) {
                        that.nodeContentHandler.setTreeNodeIds(selectedNodes);
                        that.nodeContentHandler.setTreePath(that.calculateTreePath(selectedNodes));
                        
                        if(data.event && data.event.type === "click") {
                            var treeNodeIds = that.nodeContentHandler.getTreeNodeIds();
                            that.nodeContentHandler.requestRefLabel(treeNodeIds);
                            that.NbrOfSelectedNodes = treeNodeIds.size;
                        }

                        that.populateContent(data.node, selectedNodes.length);
                    } else {
                        treeInstance.deselect_node(data.node, true); // If not sibling de-select currently selected element
                        that.nodeContentHandler.showWarnings("selectionNotAllowed");
                    }

                    if (data.selected.length == 0) {
                        that.nodeContentHandler.reset(that.editor);
                    }
                }
            });
        },
        calculateTreePath: function calculateTreePath(selectedNodes) {
            var treePath = "";
            selectedNodes.forEach(function(node, index, nodes) {
                var typeName = (node.original.number != null) ? node.original.type + " " + node.original.number : node.original.type;
                treePath += typeName + (index < (nodes.length - 1) ? " - " : "");
            });
            return treePath;
        },
        populateContent: function populateContent(selectedNode, nbrOfSelectedNodes) {
            var selectedNodeTypeName = selectedNode.original.type;
            if (this.contentRequired() && (nbrOfSelectedNodes === 1) && (selectedNodeTypeName === "ARTICLE" || selectedNodeTypeName === "CITATIONS" || selectedNodeTypeName === "RECITALS")) {
                this.editor.fire("requestElement", {
                    elementId: selectedNode.original.id,
                    elementType: selectedNodeTypeName.toLowerCase()
                });
            } else {
                this.nodeContentHandler.setContent('<div class="no-preview-available">' + this.editor.lang.leosCrossReference.contentPaneMessage + '<div>');
            }
        },
        reset: function reset() {
            try {
                // deselect_all throw exception, it could be some bug in jstree
                this.$treeContainer.jstree("deselect_all", true);
            } catch (e) {
                // ignoring cause this is bug
            }
            this.$treeContainer.jstree("close_all");
        },

        /*
         * On every cross refrence widget setup(creation or edition of the existing one) the tree is re-created with the reload of tocItems form serverside to
         * have the latest data
         */
        createTreeInstance: function createTreeInstance(tocItems, selectedItems) {
            this.$treeContainer.jstree("destroy");
            var themesDir = jsTreePath.replace(/[^\/]+$/, '') + "themes";
            this.$treeContainer.jstree({
                'core': {
                    "themes": {
                        "url": true,
                        "dir": themesDir,
                        "icons": false
                    },
                    "data": tocItems,
                    "multiple": true
                }
            });
            var that = this;
            this.$treeContainer.on("loaded.jstree", function(arg) {
                that.$treeContainer.jstree("open_all");
                that.$treeContainer.jstree("select_node", selectedItems);
            });
            this.handleNodeSelection();
        }
    };

    return dialogDefinition;
});

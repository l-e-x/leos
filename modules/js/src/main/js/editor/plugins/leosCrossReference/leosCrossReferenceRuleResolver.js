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
define(function leosCrossReferenceRuleResolverModule(require) {
    "use strict";
    
    var $ = require('jquery');
    var ARTICLE = "article";

    function _isSelectionAllowed(currentSelectedElement, prevSelectedElements, isTreeNode) {
        var result = false;
        var selectionRules = new Set();
        selectionRules.add(_checkNotEmpty);
        var siblingRule = (!isTreeNode) ? _isValidContentSelection : _isValidTreeNodeSelection;
        selectionRules.add(siblingRule);
        for (let rule of selectionRules) {
          result = rule.call(this, currentSelectedElement, prevSelectedElements);
          if(!result) break;
        }
        return result;
    }
    
    function _checkNotEmpty(currentSelectedElement, prevSelectedElements) {
      if((!currentSelectedElement && !prevSelectedElements) || 
                    (currentSelectedElement.length == 0 && prevSelectedElements.length == 0)) {
          return false;
      }
      return true;
    }
    
    function _isValidContentSelection(currentSelectedElement, prevSelectedElements) {
        if (!prevSelectedElements || prevSelectedElements.length == 0) {
            return true;
        }
        return currentSelectedElement[0].parentElement.isSameNode(prevSelectedElements[0].parentElement);
    }

    function _isValidTreeNodeSelection(currentSelectedTreeNode, prevSelectedTreeNode) {
        if (!prevSelectedTreeNode || prevSelectedTreeNode.length == 0) {
            return false;
        }
        return _isAllowed(currentSelectedTreeNode, prevSelectedTreeNode);
    }

    function _isAllowed(currentSelectedTreeNode, prevSelectedTreeNode) {
        var parentCurrentElement = document.getElementById(currentSelectedTreeNode.parent),
            parentPrevElement = document.getElementById(prevSelectedTreeNode.parent);

        var currentAknTag = (currentSelectedTreeNode.original && currentSelectedTreeNode.original.tocItem) ?
            currentSelectedTreeNode.original.tocItem.aknTag : null;
        var prevAknTag = (currentSelectedTreeNode.original && prevSelectedTreeNode.original.tocItem) ?
            prevSelectedTreeNode.original.tocItem.aknTag : null;
    
        var result = false;
        if (!currentAknTag || !prevAknTag) {
            result = false;
        } else if (currentAknTag === ARTICLE) {
            result = (currentAknTag === prevAknTag);
        } else if (parentCurrentElement != null) {
            result = parentCurrentElement.isSameNode(parentPrevElement) && (currentAknTag === prevAknTag);
        }
        return result;
    }
    
    return {
        isSelectionAllowed: _isSelectionAllowed,
    }
});
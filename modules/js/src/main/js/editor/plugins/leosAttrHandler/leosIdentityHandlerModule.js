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
define(function leosIdentityHandler(require) {
    "use strict";

    // load module dependencies
    const CKEDITOR = require("promise!ckEditor");
    const REG_EXP_FOR_UNICODE_ZERO_WIDTH_SPACE_IN_HEX = /\u200B/g;
    const REG_EXP_FOR_AKN_IDS = new RegExp("data-akn-(\\w-?)+-id");
    const REG_EXP_FOR_AKN_ORIGIN = new RegExp("data-(\\w-?)*origin");
    const regExes = [REG_EXP_FOR_AKN_IDS, REG_EXP_FOR_AKN_ORIGIN];

    const topLevelAncestors = (function(){
        const ancestors = {};
        ancestors["aknParagraph"] = true;
        ancestors["aknNumberedParagraph"] = true;
        ancestors["recital"] = true;
        ancestors["citation"] = true;
        ancestors["aknOrderedList"] = true;
        ancestors["aknUnorderedList"] = true;
        return ancestors;
    })();

    function _handleIdentity(firstElement, firstElementDetails, secondElement, secondElementDetails){
        if(isElementOutDented(firstElementDetails, secondElementDetails)){
            removeIdentityFromNewElementAndItsSmallestSibling(secondElement, firstElementDetails);
        } else {
            const firstElementAncestor = firstElementDetails.ancestors[firstElementDetails.ancestors.length - 1];
            const secondElementAncestor = secondElementDetails.ancestors[secondElementDetails.ancestors.length - 1];
            removeIdentityFromTheSmallestElementOrTheSmallestAncestor(firstElement, firstElementAncestor, secondElement, secondElementAncestor);
        }
    }

    function isElementOutDented(oldElementDetails, newElementDetails){
        const elementCouldBeOutDented = oldElementDetails.elementWeight === 0;
        if(elementCouldBeOutDented){
            const oldElementGreatParent = oldElementDetails.greatAncestors[oldElementDetails.greatAncestors.length - 1];
            const newElementParent = newElementDetails.ancestors[newElementDetails.ancestors.length - 1];
            const isNewElementOutDentedInsideOldGreatParent = oldElementGreatParent.equals(newElementParent);
            const isNewElementOutDentedBeforeOldGreatParent = newElementParent.hasNext() && oldElementGreatParent.equals(newElementParent.getNext());
            const isNewElementOutDentedAfterOldGreatParent = newElementParent.hasPrevious() && oldElementGreatParent.equals(newElementParent.getPrevious());
            return isNewElementOutDentedInsideOldGreatParent || isNewElementOutDentedBeforeOldGreatParent || isNewElementOutDentedAfterOldGreatParent;
        } else {
            return false;
        }
    }

    function removeIdentityFromNewElementAndItsSmallestSibling(newElement, oldElementDetails){
        removeIdentity(newElement);
        const oldElementsParentIsAListThatCanBeSplit = oldElementDetails.ancestors.length > 1;
        const parentIsSplitInTwo = oldElementDetails.hasPrevious && oldElementDetails.hasNext;
        if(oldElementsParentIsAListThatCanBeSplit && parentIsSplitInTwo){
            const parentFirstPart = oldElementDetails.ancestors[1];
            const parentSecondPart = newElement.getNext().findOne(getMultipleAttributesQuerySelector(parentFirstPart));
            removeIdentityFromTSmallestAncestorAndItsChildren(parentFirstPart, newElement.getPrevious(), parentSecondPart, newElement.getNext());
        }
    }

    function removeIdentityFromTSmallestAncestorAndItsChildren(firstElement, firstElementAncestor, secondElement, secondElementAncestor){
        const objectToRemoveIdentityFrom = getObjectToRemoveIdentityFrom(firstElement, firstElementAncestor, secondElement, secondElementAncestor);
        objectToRemoveIdentityFrom.removeIdFrom.find("*").toArray()
            .filter(function(element){return element instanceof CKEDITOR.dom.element;})
                .forEach(function(element){removeIdentity(element);});
        removeIdentity(objectToRemoveIdentityFrom.removeIdFrom);
    }

    function removeIdentityFromTheSmallestElementOrTheSmallestAncestor(firstElement, firstElementAncestor, secondElement, secondElementAncestor){
        const objectToRemoveIdentityFrom = getObjectToRemoveIdentityFrom(firstElement, firstElementAncestor, secondElement, secondElementAncestor);
        let element = objectToRemoveIdentityFrom.removeAttributesFrom;
        do {
            removeAttributes(element);
            if(element && element.getParent() && !objectToRemoveIdentityFrom.removeIdFrom.equals(element)){
                element = element.getParent();
            } else {
                break;
            }
        } while(element && !element.hasAttribute("id"));
        removeIdentity(objectToRemoveIdentityFrom.removeIdFrom);
    }

    function getObjectToRemoveIdentityFrom(firstElement, firstElementAncestor, secondElement, secondElementAncestor){
        if (!secondElement.equals(firstElement)) {
            // Enter or Shift+Enter were pressed somewhere inside the element or at it's end
            return getObjectFromSplitElementCase(firstElement, firstElementAncestor, secondElement, secondElementAncestor);
        } else {
            // Enter or Shift+Enter were pressed at the beginning of the element
           return getObjectFromNewElementCase(secondElement, firstElementAncestor, secondElementAncestor);
        }
    }

    function getObjectFromSplitElementCase(firstElement, firstElementAncestor, secondElement, secondElementAncestor){
        if (firstElementAncestor.equals(secondElementAncestor)) {
            return getObjectFromCommonAncestorSplitElementCase(firstElement, secondElement, secondElementAncestor);
        } else {
            return getObjectFromDifferentAncestorsSplitElementCase(firstElement, firstElementAncestor, secondElement, secondElementAncestor);
        }
    }

    function getObjectFromCommonAncestorSplitElementCase(firstElement, secondElement, commonAncestor){
        let query = getMultipleAttributesQuerySelector(secondElement);
        let [firstElementAncestor, secondElementAncestor] = [firstElement, secondElement];
        // Compute closest un-common ancestors
        while (firstElementAncestor.getParent() && !commonAncestor.equals(firstElementAncestor.getParent()) && firstElementAncestor.getParent().find(query).count() === 1){
            firstElementAncestor = firstElementAncestor.getParent();
            secondElementAncestor = secondElementAncestor.getParent();
        }
        return getObjectToRemoveAttributes(firstElement, firstElementAncestor, secondElement, secondElementAncestor);
    }

    function getObjectFromDifferentAncestorsSplitElementCase(firstElement, firstElementAncestor, secondElement, secondElementAncestor){
        return getObjectToRemoveAttributes(firstElement, firstElementAncestor, secondElement, secondElementAncestor);
    }

    function getObjectToRemoveAttributes(firstElement, firstElementAncestor, secondElement, secondElementAncestor){
        if (getWeight(firstElementAncestor) < getWeight(secondElementAncestor)) {
            copyIdentity(firstElementAncestor, secondElementAncestor);
            moveIdentity(firstElement, firstElementAncestor, secondElement, secondElementAncestor);
            return {
                removeAttributesFrom: firstElement,
                removeIdFrom: firstElementAncestor
            };
        } else {
            return {
                removeAttributesFrom: secondElement,
                removeIdFrom: secondElementAncestor
            };
        }
    }

    function getObjectFromNewElementCase (newElement, firstElementAncestor, secondElementAncestor){
        if (firstElementAncestor.equals(secondElementAncestor)) {
            return getObjectFromCommonAncestorNewElementCase(newElement, secondElementAncestor);
        } else {
            return getObjectFromDifferentAncestorsNewElementCase(newElement, firstElementAncestor, secondElementAncestor);
        }
    }

    function getObjectFromDifferentAncestorsNewElementCase(newElement, firstElementAncestor, secondElementAncestor){
        return getObjectToRemoveAttributes(firstElementAncestor.findOne(getMultipleAttributesQuerySelector(newElement)),
            firstElementAncestor, newElement, secondElementAncestor);
    }

    function getObjectFromCommonAncestorNewElementCase(newElement, commonAncestor){
        //Id is already moved by the editor so we don't need to
        if(newElement.getPrevious()) {
            return {
                removeAttributesFrom: newElement.getPrevious(),
                removeIdFrom: newElement.getPrevious()
            };
        } else {
            let query = getMultipleAttributesQuerySelector(newElement);
            let elements = commonAncestor.find(query);
            let removeAttributesFrom = newElement.equals(elements.getItem(1)) ? elements.getItem(0) : elements.getItem(1);
            let [removeIdFrom, moveIdTo] = [removeAttributesFrom, newElement];
            // Compute closest un-common ancestors
            while (removeIdFrom.getParent() && !commonAncestor.equals(removeIdFrom.getParent()) && removeIdFrom.getParent().find(query).count() === 1){
                removeIdFrom = removeIdFrom.getParent();
                moveIdTo = moveIdTo.getParent();
            }
            return getObjectToRemoveAttributes(removeAttributesFrom, removeIdFrom, newElement, moveIdTo);
        }
    }

    function getMultipleAttributesQuerySelector(element){
        let query = element.getName().toLowerCase();
        for (let i = 0; i < regExes.length; i++){
            for (let j = 0; j < element.$.attributes.length; j++) {
                if (element.$.attributes[j].name.match(regExes[i])) {
                    query = query + "[" + element.$.attributes[j].name + "='" + element.getAttribute(element.$.attributes[j].name) + "']";
                }
            }
        }
        return query;
    }

    function _getElementDetails(element){
        while (!element.isBlockBoundary()) {
            element = element.getParent();
        }
        let ancestors = getAncestors(element);
        return {
            elementWeight : getWeight(element),
            hasPrevious : element.hasPrevious(),
            hasNext : element.hasNext(),
            ancestors : ancestors,
            greatAncestors : getAncestors(ancestors[ancestors.length -1].getParent()),
        };
    }

    function getAncestors(element){
        let ancestors = [];
        ancestors.push(element);
        while(element instanceof CKEDITOR.dom.element &&  element.getParent()
            && (!element.getAttribute("data-akn-name") || !topLevelAncestors[element.getAttribute("data-akn-name")])){
            element = element.getParent();
            ancestors.push(element);
        }
        return ancestors;
    }

    function getWeight(element){
        if(element instanceof CKEDITOR.dom.element){
            return (element.getText().trim().replace(REG_EXP_FOR_UNICODE_ZERO_WIDTH_SPACE_IN_HEX, '').length);
        } else if (element){
            return element.textContent ? element.textContent.length : ( element.innerText ? element.innerText.length : 0 );
        } else {
            return 0;
        }
    }

    function copyAttributes(sourceElement, destinationElement) {
        if (sourceElement && sourceElement.$.attributes) {
            let attributes = [];
            for (let i = 0; i < regExes.length; i++){
                for (let j = 0; j < sourceElement.$.attributes.length; j++) {
                    if (sourceElement.$.attributes[j].name.match(regExes[i])) {
                        attributes[sourceElement.$.attributes[j].name] = sourceElement.getAttribute(sourceElement.$.attributes[j].name);
                    }
                }
            }
            destinationElement.setAttributes(attributes);
        }
    }

    function removeAttributes(element) {
        if (element && element.$.attributes) {
            let attributes = [];
            for (let i = 0; i < regExes.length; i++){
                for (let j = 0; j < element.$.attributes.length; j++) {
                    if (element.$.attributes[j].name.match(regExes[i])) {
                        attributes.push(element.$.attributes[j].name);
                    }
                }
            }
            element.removeAttributes(attributes);
        }
    }

    function copyId(sourceElement, destinationElement) {
        if (sourceElement && sourceElement.hasAttribute("id") && destinationElement) {
            destinationElement.setAttribute("id", sourceElement.getAttribute("id"));
        }
    }

    function removeId(element){
        if (element && element.hasAttribute("id")) {
            element.removeAttribute("id");
        }
    }

    function copyIdentity(sourceElement, destinationElement){
        copyId(sourceElement, destinationElement);
        copyAttributes(sourceElement, destinationElement);
    }

    function removeIdentity(element){
        removeId(element);
        removeAttributes(element);
    }

    function moveIdentity(sourceElement, sourceElementAncestor, destinationElement, destinationElementAncestor) {
        if(!sourceElement || !destinationElement){
            return;
        }
        while (!sourceElement.equals(sourceElementAncestor) && !destinationElement.equals(destinationElementAncestor)){
            copyIdentity(sourceElement, destinationElement);
            if(sourceElement.getParent() && destinationElement.getParent()){
                removeId(sourceElement);
                sourceElement = sourceElement.getParent();
                destinationElement = destinationElement.getParent();
            } else {
                break;
            }
        }
    }

    // return module definition
    return {
        handleIdentity: _handleIdentity,
        getElementDetails: _getElementDetails
    };
});
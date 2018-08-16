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
define(function importElementExtensionModule(require) {
    "use strict";

    var $ = require("jquery");
    var log = require("logger");
    var UTILS = require("core/leosUtils");
    
    var CHECKBOX_SELECTOR = ".leos-import-wrapper [data-element-type='import']";
    var ELEMENT_SELECTOR = ".leos-wrapped-content";
    
    function _initImportExtension(connector) {
        connector.requestSelectedElements = _requestSelectedElements;
        connector.selectAllElements = _selectAllElements;
        
        var rootElement = UTILS.getParentElement(connector);
        _registerImportHandler(connector, rootElement);
    }

    function _registerImportHandler(connector, rootElement) {
        $(rootElement).on("click.checkbox",
                CHECKBOX_SELECTOR, "checkbox",
                _handleAction.bind(undefined, connector));
        
        $(rootElement).on("click.element",
                ELEMENT_SELECTOR, "element",
                _handleAction.bind(undefined, connector));
    }
    
    function _handleAction(connector, event) {
        var count = 0;
        var checkBoxes = $(CHECKBOX_SELECTOR);
        checkBoxes.each(function(idx, checkBox) {
           if(event.data == "element" && 
               checkBox.value == event.currentTarget.firstElementChild.id) {
               checkBox.checked = checkBox.checked ? false : true;
            }
            
            if(checkBox.checked) {
                ++count;
            }
        });
        var data = {
            count: count
        }
        connector.handleSelectionChange(data);
    }
    
    function _selectAllElements(value, elementName) {
        var connector = this, count = 0;
        var checkBoxes = $(CHECKBOX_SELECTOR);
        checkBoxes.each(function(idx, checkBox) {
            if(checkBox.getAttribute("data-wrapped-type") === elementName) {
                checkBox.checked = value;
            }
            if(checkBox.checked) {
                ++count;
            }
        });
        var data = {
            count: count
        }
        connector.handleSelectionChange(data);
    }

    function _requestSelectedElements() {
        var connector = this;
        var selectedElements = [];
        var checkBoxes = $(CHECKBOX_SELECTOR);
        checkBoxes.each(function(idx, checkBox) { 
            if(checkBox.checked) {
                selectedElements.push(checkBox.value);
            }
        });
        var data = {
            elementIds: selectedElements
        }
        connector.receiveSelectedElements(data);
    }
    
    function _getElementContent($element) {
        return $element.children(".leos-wrapped-content");
    }
    
    return {
        init : _initImportExtension
    };
});

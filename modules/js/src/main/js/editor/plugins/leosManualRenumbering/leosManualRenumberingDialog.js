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
define(function leosManualRenumberingDialog(require) {
    "use strict";

    const CKEDITOR = require("promise!ckEditor");
    const dialogName = "leosManualRenumberingDialog";

    const DATA_AKN_NUM = "data-akn-num";
    const NUM_ORIGIN = "data-num-origin";
    const DATA_ORIGIN = "data-origin";
    const EC_ORIGIN = "ec";
    const CN_ORIGIN = "cn";
    const DASH = "-";
    const BULLET = "•";

    function _getNumberedElement(element){
        let numberedElement = element;
        while (numberedElement && numberedElement.getParent && numberedElement.getParent()
        && (!numberedElement.isBlockBoundary()
            || !numberedElement.hasAttribute(DATA_AKN_NUM)
            || numberedElement.getAttribute(DATA_AKN_NUM) === DASH
            || numberedElement.getAttribute(DATA_AKN_NUM) === BULLET
            || (numberedElement.hasAttribute(NUM_ORIGIN) && numberedElement.getAttribute(NUM_ORIGIN) === EC_ORIGIN)
            || ((!numberedElement.hasAttribute(NUM_ORIGIN) || numberedElement.getAttribute(NUM_ORIGIN) === CN_ORIGIN)
                && (!numberedElement.getParent().hasAttribute(DATA_ORIGIN) || numberedElement.getParent().getAttribute(DATA_ORIGIN) !== EC_ORIGIN)))) {
            numberedElement = numberedElement.getParent();
        }
        return numberedElement;
    }

    function _getValueAndFormat(element){
        let number  = element.getAttribute(DATA_AKN_NUM);
        let value = number;
        let format = "#";
        if(number.endsWith(".")){
            format = "#.";
            value = number.substr(0,number.length - 1);
        } else if(number.startsWith("(") && number.endsWith(")")){
            format = "(#)";
            value = number.substr(1,number.length - 2);
        }
        return {
            value: value,
            format: format
        }
    }

    const dialogDefinition = {
        dialogName: dialogName,
        getNumberedElement: _getNumberedElement,
    };

    dialogDefinition.initializeDialog = function initializeDialog(editor) {
        return {
            title : editor.lang.leosManualRenumbering.editNumber,
            resizable: CKEDITOR.DIALOG_RESIZE_BOTH,
            minWidth : 200,
            minHeight : 100,
            contents: [{
                id: 'editNumber',
                label: editor.lang.leosManualRenumbering.editNumberLabel,
                elements: [{
                    type: 'text',
                    id: 'number',
                    label: editor.lang.leosManualRenumbering.numberLabel,
                    validate: CKEDITOR.dialog.validate.regex(/^[0-9A-Za-z.\-]+$/, editor.lang.leosManualRenumbering.numberValidationMessage),
                    setup : function setup(text) {
                        this.setValue(text);
                    }
                }]
            }],

            onShow: function() {
                this.element = _getNumberedElement(editor.getSelection().getStartElement());
                let valueAndFormat = _getValueAndFormat(this.element);
                this.format = valueAndFormat.format;
                this.setupContent(valueAndFormat.value);
            },

            onOk: function() {
                let dialog = this;
                this.element.setAttribute(DATA_AKN_NUM, this.format.replace("#", dialog.getValueOf( 'editNumber', 'number' )));
                this.element.setAttribute(NUM_ORIGIN, CN_ORIGIN);
            }
        };
    };

    return dialogDefinition;
});
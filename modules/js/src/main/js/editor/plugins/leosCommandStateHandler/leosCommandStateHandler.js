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
define(function leosCommandStateHandler(require) {
    "use strict";

    //Function takes event and command name from respective plugin command. changeStateElements provides elements
    //for which command is to be disabled.
    var changeCommandState = function changeCommandState(event, commandName, changeStateElements) {
        var command = event.editor.getCommand(commandName);
        var selection = event.editor.getSelection();
        if (!selection) return;
        shouldDisable(selection, changeStateElements) ? command.disable() : command.enable();
    };

    function shouldDisable(selection, changeStateElements) {
        var startElement = selection.getStartElement();
        if (startElement.getAttribute('contenteditable') === 'false') {
            return true;
        } else {
            if (changeStateElements) {
                var elements = Object.values(changeStateElements);
                for (var i = 0; i < elements.length; i++) {
                    if (startElement.getAscendant(elements[i].elementName, true)) {
                        if (elements[i].selector) {
                            if ($(startElement.$).closest(elements[i].selector).length) {
                                return true;
                            }
                        }else {
                            return true
                        }
                    }
                }
            }
        }
    }

    return {
        changeCommandState: changeCommandState
    }
});
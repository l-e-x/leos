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
define(function suggestionExtensionModule(require) {
    "use strict";

    // load module dependencies
    var log = require("logger");
    var $ = require("jquery");
    var CKEDITOR = require("promise!ckEditor");
    var _ = require("lodash");
    
    var leosCore = require("js/leosCore");
    var postal = require("postal");
    var suggestionEditor = require("./suggestionEditor");
    var suggestionWrapperTemplate = require("text!./template/leosSuggestionWrapperTemplate.html");
    
    // utilities
    var UTILS = leosCore.utils;
    var CONFIG = leosCore.config;

    // handle extension initialization
    function _initExtension(connector) {
        log.debug("Initializing Suggestions extension...");
        
        suggestionEditor.setup(connector);
        _setupSuggestionButtonListener(connector);
    }

    function _setupSuggestionButtonListener(connector) {
        var rootElement = UTILS.getParentElement(connector);
        $(rootElement).on("click.suggestion", '.suggestion-btn', _createSuggestion.bind(undefined, connector));
    }
    
    function _createSuggestion(connector, event) {
       var $origElement = $(event.currentTarget).closest('.commentable-section');
       _insertEditorWrapper($origElement);
       var data = {
          elementId: $origElement.attr('id'),
       }
       connector.createSuggestion(data);
    }
    
    function _insertEditorWrapper($origElement) {
        var template = _.template(suggestionWrapperTemplate)({
               id: $origElement.attr('id'),
        });
        $origElement.after(template);
    }
    
    function _tearDownSuggestionListener(connector) {
        var rootElement = UTILS.getParentElement(connector);
        $(rootElement).off("click", 'suggestion');
    }
    
    return {
        init: _initExtension
    };
});
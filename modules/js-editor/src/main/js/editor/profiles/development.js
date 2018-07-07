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
;   // jshint ignore:line
define(function developmentProfileModule(require) {
    "use strict";

    // require profile dependencies, if needed
    // e.g. ckEditor, plugins or utilities

    var profileName = "Development";

    // create profile configuration
    var profileConfig = {
        // user interface language localization
        language: "en",
        // custom configuration to load (none if empty)
        customConfig: "",
        // comma-separated list of plugins to be loaded
        plugins: "toolbar,wysiwygarea,elementspath," +
        "clipboard,undo,enterkey,entities," +
        "button,dialog,dialogui,sourcearea",
        // comma-separated list of plugins that must not be loaded
        removePlugins: "",
        // comma-separated list of additional plugins to be loaded
        extraPlugins: "basicstyles,link",
        // convert all entities into Unicode numerical decimal format
        entities_processNumerical: "force",
        // toolbar groups arrangement, optimized for a single toolbar row
        toolbarGroups: [
            {name: "document", groups: ["document", "doctools"]},
            {name: "clipboard", groups: ["clipboard", "undo"]},
            {name: "editing", groups: ["find", "selection", "spellchecker"]},
            {name: "forms"},
            {name: "basicstyles", groups: ["basicstyles", "cleanup"]},
            {name: "paragraph", groups: ["list", "indent", "blocks", "align", "bidi"]},
            {name: "links"},
            {name: "insert"},
            {name: "styles"},
            {name: "colors"},
            {name: "tools"},
            {name: "others"},
            {name: "mode"},
            {name: "about"}
        ],
        // comma-separated list of toolbar button names that must not be rendered
        removeButtons: "Underline,Strike",
        // semicolon-separated list of dialog elements that must not be rendered
        // element is a string concatenation of dialog name + colon + tab name
        removeDialogTabs: "link:advanced"
    };

    // create profile definition
    var profileDefinition = {
        name: profileName,
        config: profileConfig
    };

    return profileDefinition;
});
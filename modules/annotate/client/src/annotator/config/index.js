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
'use strict';

var settingsFrom = require('./settings');

/**
 * Reads the Hypothesis configuration from the environment.
 *
 * @param {Window} window_ - The Window object to read config from.
 */
function configFrom(window_) {
  var settings = settingsFrom(window_);
  return {
    annotations: settings.annotations,
    // URL where client assets are served from. Used when injecting the client
    // into child iframes.
    assetRoot: settings.hostPageSetting('assetRoot', {allowInBrowserExt: true}),
    branding: settings.hostPageSetting('branding'),
    // URL of the client's boot script. Used when injecting the client into
    // child iframes.
    clientUrl: settings.clientUrl,
    enableExperimentalNewNoteButton: settings.hostPageSetting('enableExperimentalNewNoteButton'),
    theme: settings.hostPageSetting('theme'),
    usernameUrl: settings.hostPageSetting('usernameUrl'),
    onLayoutChange: settings.hostPageSetting('onLayoutChange'),
    openSidebar: settings.hostPageSetting('openSidebar', {allowInBrowserExt: true}),
    query: settings.query,
    services: settings.hostPageSetting('services'),
    showHighlights: settings.showHighlights,
    sidebarAppUrl: settings.sidebarAppUrl,
    // Subframe identifier given when a frame is being embedded into
    // by a top level client
    subFrameIdentifier: settings.hostPageSetting('subFrameIdentifier', {allowInBrowserExt: true}),

    annotationContainer: settings.hostPageSetting('annotationContainer'), //LEOS Change
    leosDocumentRootNode: settings.hostPageSetting('leosDocumentRootNode'), //LEOS Change
    ignoredTags: settings.hostPageSetting('ignoredTags', {defaultValue: []}), //LEOS Change
    allowedSelectorTags: settings.hostPageSetting('allowedSelectorTags', {defaultValue: '*'}), //LEOS Change
    editableSelector: settings.hostPageSetting('editableSelector', {defaultValue: ''}), // LEOS Change
    notAllowedSuggestSelector: settings.hostPageSetting('notAllowedSuggestSelector', {defaultValue: '*'}), // LEOS Change
    displayMetadataCondition: settings.hostPageSetting('displayMetadataCondition', {defaultValue: {}}), // LEOS Change
    readOnly: settings.hostPageSetting('readOnly', {defaultValue: 'false'}) // LEOS Change
  };
}

module.exports = configFrom;

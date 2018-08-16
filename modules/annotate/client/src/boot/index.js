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
'use strict';

// This is the main entry point for the Hypothesis client in the host page
// and the sidebar application.
//
// The same boot script is used for both entry points so that the browser
// already has it cached when it encounters the reference in the sidebar
// application.

// Variables replaced by the build script

/* global __MANIFEST__ */

var boot = require('./boot');
var settings = require('../shared/settings').jsonConfigsFrom(document);

boot(document, {
  assetRoot: settings.assetRoot || '__ASSET_ROOT__',
  manifest: __MANIFEST__,
  sidebarAppUrl: settings.sidebarAppUrl || '__SIDEBAR_APP_URL__',
});

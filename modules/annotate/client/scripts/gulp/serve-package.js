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

var { readFileSync } = require('fs');

var express = require('express');
var { log } = require('gulp-util');

var { version } = require('../../package.json');

/**
 * An express server which serves the contents of the package.
 *
 * The server mirrors the URL structure of cdn.hypothes.is, an S3-backed domain
 * which serves the client's assets in production.
 *
 * When developing the client, the Hypothesis service should be configured to
 * use the URL of this service as the client URL, so that the boot script is
 * returned by the service's '/embed.js' route and included in the '/app.html'
 * app.
 */
function servePackage(port, hostname) {
  var app = express();

  // Enable CORS for assets so that cross-origin font loading works.
  app.use(function (req, res, next) {
    res.append('Access-Control-Allow-Origin', '*');
    res.append('Access-Control-Allow-Methods', 'GET');
    next();
  });

  var serveBootScript = function (req, res) {
    var entryPath = require.resolve('../..');
    var entryScript = readFileSync(entryPath).toString('utf-8');
    res.send(entryScript);
  };

  // Set up URLs which serve the boot script and package content, mirroring
  // cdn.hypothes.is' structure.
  // LEOS changes for serving
  app.get(`/annotate/client`, serveBootScript);
  app.use(`/annotate/client`, express.static('./build'));
  //-----------------------------------------------------------------------------

  app.listen(port, function () {
    // LEOS Change
    log(`Client package served at http://${hostname}:${port}/annotate/client`);
  });
}

module.exports = servePackage;

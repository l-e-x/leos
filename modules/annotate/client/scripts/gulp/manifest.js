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

var path = require('path');
var crypto = require('crypto');

var through = require('through2');
var VinylFile = require('vinyl');
var buildConfig = require('../config');//LEOS Change
var cfg = buildConfig.getConfig(); //LEOS Change

/**
 * Gulp plugin that generates a cache-busting manifest file.
 *
 * Returns a function that creates a stream which takes
 * a stream of Vinyl files as inputs and outputs a JSON
 * manifest mapping input paths (eg. "scripts/foo.js")
 * to URLs with cache-busting query parameters (eg. "scripts/foo.js?af95bd").
 */
module.exports = function (opts) {
  var manifest = {};

  return through.obj(function (file, enc, callback) {
    var hash = crypto.createHash('sha1');
    hash.update(file.contents);

    var hashSuffix = hash.digest('hex').slice(0, 6);
    var relPath = path.relative(cfg.dest.base + '/', file.path).replace('\\', '/');//LEOS Changes
    manifest[relPath] = relPath + '?' + hashSuffix;

    callback();
  }, function (callback) {
    var manifestFile = new VinylFile({
      path: opts.name,
      contents: new Buffer(JSON.stringify(manifest, null, 2), 'utf-8'),
    });
    this.push(manifestFile);
    callback();
  });
};

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
#!/usr/bin/env node

/**
 * Replaces the "[Unreleased]" header for changes in the next release with the
 * current package version from package.json
 */

'use strict';

const fs = require('fs');

const pkg = require('../package.json');

const dateStr = new Date().toISOString().slice(0,10);
const versionLine = `## [${pkg.version}] - ${dateStr}`;

const changelogPath = require.resolve('../CHANGELOG.md');
const changelog = fs.readFileSync(changelogPath).toString();
const updatedChangelog = changelog.split('\n')
  .map(ln => ln.match(/\[Unreleased\]/) ? versionLine : ln)
  .join('\n');

if (updatedChangelog === changelog) {
  console.error('Failed to find "Unreleased" section in changelog');
  process.exit(1);
}

fs.writeFileSync(changelogPath, updatedChangelog);


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

const Chance = require('chance');
const chance = new Chance();

function group() {
  const id = chance.hash({length: 15});
  const name = chance.string();
  const group = {
    id: id,
    name: name,
    links: {
      html: `http://localhost:5000/groups/${id}/${name}`,
    },
    type: 'private',
  };
  return group;
}

function organization(options={}) {
  const org = {
    id: chance.hash({length : 15}),
    name: chance.string(),
    logo: chance.url(),
  };
  return Object.assign(org, options);
}

function defaultOrganization() {
  return {
    id: '__default__',
    name: 'Hypothesis',
    logo: 'http://example.com/hylogo',
  };
}

function expandedGroup(options={}) {
  const expanded = group();
  expanded.organization = organization();

  return Object.assign(expanded, options);
}

module.exports = {
  group,
  expandedGroup,
  organization,
  defaultOrganization,
};

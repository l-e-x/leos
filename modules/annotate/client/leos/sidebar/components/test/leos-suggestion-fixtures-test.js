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

/** Return an comment domain model object for a new comment
 */
function newSuggestion() {
  return {
    id: undefined,
    $highlight: undefined,
    target: ['foo', 'bar'],
    references: [],
    text: 'Annotation text',
    tags: ['suggestion'],
  };
}

/** Return an comment domain model object for a new comment
 */
function newComment() {
  return {
    id: undefined,
    $highlight: undefined,
    target: ['foo', 'bar'],
    references: [],
    text: 'Annotation text',
    tags: ['comment'],
  };
}

/** Return an highlight domain model object for a new highlight
 */
function newHighlight() {
  return {
    id: undefined,
    $highlight: true,
    target: [{source: 'http://example.org'}],
    tags: ['highlight'],
  };
}

/**
 * Return a fake comment with the basic properties filled in.
 */
function defaultComment() {
  return {
    id: 'deadbeef',
    tags: ['comment'],
    permissions: {
        read:['group:__world__'],
        write:['group:__world__'],
    },
    document: {
      title: 'A special document',
    },
    target: [{source: 'source', 'selector': []}],
    uri: 'http://example.com',
    user: 'acct:bill@localhost',
    updated: '2015-05-10T20:18:56.613388+00:00',
  };
}

/**
 * Return a fake suggestion with the basic properties filled in.
 */
function defaultSuggestion() {
  return {
    id: 'deadbeef',
    tags: ['suggestion'],
    permissions: {
        read:['group:__world__'],
        write:['group:__world__'],
    },
    document: {
      title: 'A special document',
    },
    target: [{source: 'source', 'selector': []}],
    uri: 'http://example.com',
    user: 'acct:bill@localhost',
    updated: '2015-05-10T20:18:56.613388+00:00',
  };
}

/**
 * Return a fake suggestion with the basic properties filled in.
 */
function defaultHighlight() {
  return {
    id: 'deadbeef',
    tags: ['highlight'],
    permissions: {
        read:['group:__world__'],
        write:['group:__world__'],
    },
    document: {
      title: 'A special document',
    },
    target: [{source: 'source', 'selector': []}],
    uri: 'http://example.com',
    user: 'acct:bill@localhost',
    updated: '2015-05-10T20:18:56.613388+00:00',
  };
}

module.exports = {
  defaultComment: defaultComment,
  defaultSuggestion: defaultSuggestion,
  defaultHighlight: defaultHighlight,
  newComment: newComment,
  newSuggestion: newSuggestion,
  newHighlight: newHighlight,
};
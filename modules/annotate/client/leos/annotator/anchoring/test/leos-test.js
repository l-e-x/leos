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

var leos;

var toResult = require('../../../../src/shared/test/promise-util').toResult;
var unroll = require('../../../../src/shared/test/util').unroll;
var fixture = require('./leos-anchoring-fixture.html');

/** Return all text node children of `container`. */
function textNodes(container) {
  var nodes = [];
  var walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT);
  while (walker.nextNode()) {
    nodes.push(walker.currentNode);
  }
  return nodes;
}

/**
 * Return the single Node which matches an XPath `query`
 *
 * @param {Node} context
 * @param {string} query
 */
function findNode(context, query) {
  if (query.slice(0,1) === '/') {
    query = query.slice(1);
  }
  var result = document.evaluate(query, context, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null);
  return result.singleNodeValue;
}

/**
 * Resolve a serialized description of a range into a Range object.
 *
 * @param {Element} root
 * @param {Object} descriptor
 */
function toRange(root, descriptor) {
  var startNode;
  var endNode;

  if (typeof descriptor.startContainer === 'string') {
    startNode = findNode(root, descriptor.startContainer);
  } else {
    startNode = textNodes(root)[descriptor.startContainer];
  }

  if (typeof descriptor.endContainer === 'string') {
    endNode = findNode(root, descriptor.endContainer);
  } else {
    endNode = textNodes(root)[descriptor.endContainer];
  }

  var range = document.createRange();
  range.setStart(startNode, descriptor.startOffset);
  range.setEnd(endNode, descriptor.endOffset);
  return range;
}

function findByType(selectors, type) {
  return selectors.find(function (s) { return s.type === type; });
}

/**
 * Return a copy of a list of selectors sorted by type.
 */
function sortByType(selectors) {
  return selectors.slice().sort(function (a, b) {
    return a.type.localeCompare(b.type);
  });
}

/**
 * Test cases for mapping ranges to selectors.
 *
 * Originally taken from https://github.com/openannotation/annotator/blob/v1.2.x/test/spec/range_spec.coffee
 */
var rangeSpecs = [
  // Format:
  //   [startContainer, startOffset, endContainer, endOffset, quote, description]
  // Where the *Container nodes are expressed as either an XPath relative to
  // the container or the index into the list of text nodes within the container
  // node

  // Test cases from Annotator v1.2.x's range_spec.coffee
  [ 12,           3,   12,           33,  'ULATION OF THE EUROPEAN PARLIA',                                                                                                                                                                                         'Partial node contents.' ],
  [ 12,           0,   12,           56,  'REGULATION OF THE EUROPEAN PARLIAMENT AND OF THE COUNCIL',                                                                                                                                                               'Full node contents, textNode refs.' ],
  [ './/akomantoso[1]/bill[1]//aknbody[1]//article[1]/heading[1]', 0,   './/akomantoso[1]/bill[1]//aknbody[1]//article[1]/heading[1]', 1,   'Scope',                                                                                                                'Full node contents, elementNode refs.' ],
  [ 14,           48,  18,           22,  'UROPEAN UNION,   Having regard to the T',                                                                                                                                                                                'Spanning 2 nodes.' ],
  [ './/akomantoso[1]/bill[1]//preamble[1]/formula[1]/aknp[1]', 0,   18,           22,  'THE EUROPEAN PARLIAMENT AND THE COUNCIL OF THE EUROPEAN UNION,   Having regard to the T',                                                                                  'Spanning 2 nodes, elementNode start ref.' ],
  [ 14,           48,  './/akomantoso[1]/bill[1]//preamble[1]//citations[1]/citation[1]/aknp[1]',     1,   'UROPEAN UNION,   Having regard to the Treaty on the Functioning of the European Union, and in particular Article [...] thereof,',                       'Spanning 2 nodes, elementNode end ref.' ],
];

/**
 * Test cases for which describing the range is known to fail for certain
 * selectors.
 */
var expectedFailures = [
  // [description, expectedFailureTypes]

  // Currently empty.
];

describe('HTML anchoring', function () {
  var container;

  beforeEach(function () {
    var sidebarUrl = document.createElement('link');
    sidebarUrl.rel = 'sidebar';
    sidebarUrl.href = 'test/sidebar';
    sidebarUrl.type = 'application/annotator+html';

    var clientUrl = document.createElement('link');
    clientUrl.rel = 'hypothesis-client';
    clientUrl.href = 'test/boot.js';
    clientUrl.type = 'application/annotator+javascript';

    container = document.createElement('section');
    container.innerHTML = fixture;
    document.head.appendChild(sidebarUrl);
    document.head.appendChild(clientUrl);
    document.body.appendChild(container);
    leos = require('../leos');
  });

  afterEach(function () {
    container.remove();
  });

  var testCases = rangeSpecs.map(function (data) {
    return {
      range: {
        startContainer: data[0],
        startOffset: data[1],
        endContainer: data[2],
        endOffset: data[3],
      },
      quote: data[4],
      description: data[5],
    };
  });

  unroll('describes and anchors "#description"', function (testCase) {
    // Resolve the range descriptor to a DOM Range, verify that the expected
    // text was selected.
    var range = toRange(container, testCase.range);
    assert.equal(range.toString(), testCase.quote);

    // Capture a set of selectors describing the range and perform basic sanity
    // checks on them.
    var selectors = leos.describe(container, range);

    var rangeSel = findByType(selectors, 'RangeSelector');
    var positionSel = findByType(selectors, 'TextPositionSelector');
    var quoteSel = findByType(selectors, 'TextQuoteSelector');

    var failInfo = expectedFailures.find(function (f) {
      return f[0] === testCase.description;
    });
    var failTypes = {};
    if (failInfo) {
      failTypes = failInfo[1];
    }

    var assertRange = failTypes.range ? assert.notOk : assert.ok;
    var assertQuote = failTypes.quote ? assert.notOk : assert.ok;
    var assertPosition = failTypes.position ? assert.notOk : assert.ok;

    assertRange(rangeSel, 'range selector');
    assertPosition(positionSel, 'position selector');
    assertQuote(quoteSel, 'quote selector');

    // Map each selector back to a Range and check that it refers to the same
    // text. We test each selector in turn to make sure they are all valid.
    var anchored = selectors.map(function (sel) {
      if (sel.type != 'FragmentSelector' && sel.type != 'LeosSelector') {
        return leos.anchor(container, [sel]).then(function (anchoredRange) {
          assert.equal(range.toString(), anchoredRange.toString());
        });
      }
    });
    return Promise.all(anchored);
  }, testCases);
});

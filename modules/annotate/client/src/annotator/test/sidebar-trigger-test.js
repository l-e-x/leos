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

var sidebarTrigger = require('../sidebar-trigger');

describe('sidebarTrigger', function () {
  var triggerEl1;
  var triggerEl2;

  beforeEach(function () {
    triggerEl1 = document.createElement('button');
    triggerEl1.setAttribute('data-hypothesis-trigger');
    document.body.appendChild(triggerEl1);

    triggerEl2 = document.createElement('button');
    triggerEl2.setAttribute('data-hypothesis-trigger');
    document.body.appendChild(triggerEl2);
  });

  it('calls the show callback which a trigger button is clicked', function () {
    var fakeShowFn = sinon.stub();
    sidebarTrigger(document, fakeShowFn);

    triggerEl1.dispatchEvent(new Event('click'));
    triggerEl2.dispatchEvent(new Event('click'));

    assert.calledTwice(fakeShowFn);
  });
});

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

var angular = require('angular');
var unroll = require('../../../shared/test/util').unroll;

describe('BrandingDirective', function () {

  var $compile;
  var $rootScope;
  var customSettings;

  // Settings are set and frozen when the app initializes.
  // This function allows us a way to quickly setup our environment
  // with desired settings. Note, needs to be called for angular
  // to be initialized for the test
  var setSettingsAndBootApp = function(){

    angular.module('app', [])
      .directive('hBranding', require('../h-branding'));

    angular.mock.module('app', {
      settings: customSettings || {},
    });

    angular.mock.inject(function (_$compile_, _$rootScope_) {
      $compile = _$compile_;
      $rootScope = _$rootScope_;
    });
  };

  // convenience method to only worry about what the test
  // should be doing, applying settings variants.
  var applyBrandingSettings = function(brandingSettings){

    customSettings = brandingSettings ? {
      branding: brandingSettings,
    } : null;

    setSettingsAndBootApp();
  };

  // creates a new element with the attribute string provided.
  // Allowing us to do many attribute configurations before
  // compilation happens.
  var makeElementWithAttrs = function(attrString){
    var $element = angular.element('<span ' + attrString + ' ></span>');
    $compile($element)($rootScope.$new());
    $rootScope.$digest();
    return $element;
  };

  afterEach(function(){
    customSettings = {};
  });

  it('branding should not happen on elements with unknown branding attributes', function () {

    // note, we need to set some form of branding settings or this
    // test will pass simply because branding isn't required
    applyBrandingSettings({
      appBackgroundColor: 'blue',
    });

    var el = makeElementWithAttrs('h-branding="randomBackgroundColor"');
    assert.equal(el[0].style.backgroundColor, '');
  });

  unroll('applies branding to elements', function (testCase) {
    applyBrandingSettings(testCase.settings);

    var el = makeElementWithAttrs(testCase.attrs);

    if(Array.isArray(testCase.styleChanged)){
        // we expect that if styleChanged is an array
        // that expectedPropValue will be an equal length array
      testCase.styleChanged.forEach(function(styleChanged, index){
        assert.equal(el[0].style[styleChanged], testCase.expectedPropValue[index]);
      });
    }else{
      assert.equal(el[0].style[testCase.styleChanged], testCase.expectedPropValue);
    }

  }, require('./h-branding-fixtures'));

});

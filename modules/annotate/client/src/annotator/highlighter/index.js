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

const domWrapHighlighter = require('./dom-wrap-highlighter');
const overlayHighlighter = require('./overlay-highlighter');
const features = require('../features');

// we need a facade for the highlighter interface
// that will let us lazy check the overlay_highlighter feature
// flag and later determine which interface should be used.
const highlighterFacade = {};
let overlayFlagEnabled;

Object.keys(domWrapHighlighter).forEach((methodName)=>{
  highlighterFacade[methodName] = (...args)=>{
    // lazy check the value but we will
    // use that first value as the rule throughout
    // the in memory session
    if(overlayFlagEnabled === undefined){
      overlayFlagEnabled = features.flagEnabled('overlay_highlighter');
    }

    const method = overlayFlagEnabled ? overlayHighlighter[methodName] : domWrapHighlighter[methodName];
    return method.apply(null, args);
  };
});

module.exports = highlighterFacade;

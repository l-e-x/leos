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
// Test data for the firehose of branding combinations
module.exports = [

  // ALL SUPPORTED PROPERTIES
  {
    settings: {appBackgroundColor: 'blue'},
    attrs: 'h-branding="appBackgroundColor"',
    styleChanged: 'backgroundColor',
    expectedPropValue: 'blue',
  },
  {
    settings: {accentColor: 'red'},
    attrs: 'h-branding="accentColor"',
    styleChanged: 'color',
    expectedPropValue: 'red',
  },
  {
    settings: {ctaBackgroundColor: 'red'},
    attrs: 'h-branding="ctaBackgroundColor"',
    styleChanged: 'backgroundColor',
    expectedPropValue: 'red',
  },
  {
    settings: {ctaTextColor: 'yellow'},
    attrs: 'h-branding="ctaTextColor"',
    styleChanged: 'color',
    expectedPropValue: 'yellow',
  },
  {
    settings: {selectionFontFamily: 'georgia, arial'},
    attrs: 'h-branding="selectionFontFamily"',
    styleChanged: 'fontFamily',
    expectedPropValue: 'georgia, arial',
  },
  {
    settings: {annotationFontFamily: 'georgia, arial'},
    attrs: 'h-branding="annotationFontFamily"',
    styleChanged: 'fontFamily',
    expectedPropValue: 'georgia, arial',
  },

  // EMPTY VALUE
  {
    settings: {appBackgroundColor: ''},
    attrs: 'h-branding="appBackgroundColor"',
    styleChanged: 'backgroundColor',
    expectedPropValue: '',
  },

  // MULTIPLES
  {
    settings: {appBackgroundColor: 'blue', annotationFontFamily: 'arial'},
    attrs: 'h-branding="appBackgroundColor, annotationFontFamily"',
    styleChanged: ['backgroundColor', 'fontFamily'],
    expectedPropValue: ['blue', 'arial'],
  },
  {
    settings: {appBackgroundColor: 'orange', annotationFontFamily: 'helvetica'},
    attrs: 'h-branding="appBackgroundColor,annotationFontFamily"',
    styleChanged: ['backgroundColor', 'fontFamily'],
    expectedPropValue: ['orange', 'helvetica'],
  },
  {
    settings: {appBackgroundColor: 'blue', annotationFontFamily: 'arial'},
    attrs: 'h-branding="appBackgroundColor"',
    styleChanged: ['backgroundColor', 'fontFamily'],
    expectedPropValue: ['blue', ''],
  },
  {
    settings: {appBackgroundColor: 'blue'},
    attrs: 'h-branding="appBackgroundColor, annotationFontFamily"',
    styleChanged: ['backgroundColor', 'fontFamily'],
    expectedPropValue: ['blue', ''],
  },
];

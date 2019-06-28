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

var loaded = false;

module.exports = function(trackingId){

  // small measure to make we do not accidentally
  // load the analytics scripts more than once
  if(loaded){
    return;
  }

  loaded = true;

  /* eslint-disable */

  // Google Analytics snippet to load the analytics script
  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

  ga('create', trackingId, 'auto');

  // overrides helper that requires http or https protocols.
  // obvious issue when it comes to extensions with protocols
  // like "chrome-extension://" but isn't a huge need for us
  // anywhere else as well.
  // https://developers.google.com/analytics/devguides/collection/analyticsjs/tasks#disabling
  ga('set', 'checkProtocolTask', null);
  
  // anonymize collected IP addresses for GDPR
  // https://developers.google.com/analytics/devguides/collection/analyticsjs/ip-anonymization
  ga('set', 'anonymizeIp', true);

  /* eslint-enable */
};

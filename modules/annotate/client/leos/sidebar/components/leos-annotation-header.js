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

var annotationHeader = require('../../../src/sidebar/components/annotation-header');

// @ngInject
function LeosAnnotationHeaderController($injector, features, groups, settings, serviceUrl) {
  $injector.invoke(annotationHeader.controller, this, {features: features, groups: groups, settings: settings, serviceUrl: serviceUrl});
};

function LeosAnnotationHeaderBindings() {
  let bindings = annotationHeader.bindings;
  bindings.authorize = '&';
  bindings.edit = '&';
  bindings.delete = '&';
  bindings.reply = '&';
  bindings.isDeleted = '<';
  bindings.isSelected = '<';
  bindings.isSaving = '<';
  bindings.id = '<';
  return bindings;
}

/**
 * Header component for an annotation card.
 *
 * Header which displays the username, last update timestamp and other key
 * metadata about an annotation.
 */
module.exports = {
  controller: LeosAnnotationHeaderController,
  controllerAs: 'vm',
  bindings: LeosAnnotationHeaderBindings(),
  template: require('../templates/leos-annotation-header.html'),
};

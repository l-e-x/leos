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
// @ngInject
function AnnotateSentapiAppController(
  $document, $location, $rootScope, $route, $scope,
  $window, settings, api
) {
  let self = this;
  this.isError = false;
  this.uris = [];
  let hostBridge = $document[0].hostBridge;

  if (hostBridge.callback && typeof(hostBridge.callback)) {
    this.callback = hostBridge.callback;
  }
  else {
    this.isError = true;
    this.message = "Missing callback in configuration";
    console.log(this.message);
    self.callback({result: "FAIL", message: this.message});
  }

  if (settings.message && settings.message.title) {
    $window.document.title = settings.message.title;
  }
  else {
    this.isError = true;
    this.message = "Missing title in configuration";
    console.log(this.message);
    self.callback({result: "FAIL", message: this.message});
  }

  if (settings.message && settings.message.confirmMessage) {
    this.message = settings.message.confirmMessage;
  }
  else {
    this.isError = true;
    this.message = "Missing message in configuration";
    console.log(this.message);
    self.callback({result: "FAIL", message: this.message});
  }

  if (settings.message && settings.message.failureMessage) {
    this.failureMessage = settings.message.failureMessage;
  }
  else {
    this.isError = true;
    this.failureMessage = "Missing failure message in configuration";
    console.log(this.message);
    self.callback({result: "FAIL", message: this.message});
  }

  this.buttons = {};
  if (settings.message.buttons && settings.message.buttons.yes) {
    this.buttons.yes = settings.message.buttons.yes;
  }
  else {
    this.isError = true;
    this.message = "Missing button's text in configuration";
    console.log(this.message);
    self.callback({result: "FAIL", message: this.message});
  }
  if (!(settings && settings.parameters && settings.parameters.group)) {
    this.isError = true;
    this.message = "Group parameter is mandatory";
    console.log(this.message);
    self.callback({result: "FAIL", message: this.message});
  }
  if (!(settings && settings.parameters && settings.parameters.uris)) {
    this.isError = true;
    this.message = "URIs parameter is mandatory";
    console.log(this.message);
    self.callback({result: "FAIL", message: this.message});
  }
  if (!(settings && settings.metadata)) {
    this.isError = true;
    this.message = "Metadata parameter is mandatory";
    console.log(this.message);
    self.callback({result: "FAIL", message: this.message});
  }

  this.sending = false;
  this.sent = false;
  this.uris = settings.parameters.uris;

  function requestCallback(response) {
    $scope.$apply(function() {
      self.sending = false;
      self.sent = true;
    });
    self.callback(response);
    if (response.result == "SUCCESS") {
      $window.close();
    }
    else {
      $scope.$apply(function() {
        self.isError = true;
        self.message = self.failureMessage;
      });
    }
  }

  this.sendRequest = function() {
    self.sending = true;
    api.sendRequest(self.uris, requestCallback);
  }
}

module.exports = {
  controller: AnnotateSentapiAppController,
  controllerAs: 'vm',
  template: require('../templates/annotate-sentapi-app.html'),
};

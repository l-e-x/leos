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
var hypothesis_adder = require('../../src/annotator/adder');
var template = require('./leos-adder.html');

var SUGGESTION_BTN_SELECTOR = '.js-suggestion-btn'
var COMMENT_BTN_SELECTOR = '.js-annotate-btn'
var HIGHLIGHT_BTN_SELECTOR = '.js-highlight-btn';

hypothesis_adder.Adder.prototype.handleSuggestCommand = function(event) {
  event.preventDefault();
  event.stopPropagation();

  this.options.onSuggest();

  this.hide();
}

hypothesis_adder.Adder.prototype.extend = function(container, hostBridge, options) {
  this.hostBridge = hostBridge;
  this.options = options;
  self = this;

  if (container.shadowRoot != null) {
    container.shadowRoot.querySelector("hypothesis-adder-actions").innerHTML = template;
    this.element = container.shadowRoot.querySelector('.js-adder');
  }
  else {
    container.querySelector("hypothesis-adder-actions").innerHTML = template;
    this.element = container.querySelector('.js-adder');
  }

  var suggestBtn = this.element.querySelector(SUGGESTION_BTN_SELECTOR);
  var commentBtn = this.element.querySelector(COMMENT_BTN_SELECTOR);
  var highlightBtn = this.element.querySelector(HIGHLIGHT_BTN_SELECTOR);
  if (highlightBtn) {
    highlightBtn.onclick = function(event) {
      event.preventDefault();
      event.stopPropagation();

      options.onHighlight();
    
      self.hide();
    };
  }

  if (commentBtn) {
    commentBtn.onclick = function(event) {
      event.preventDefault();
      event.stopPropagation();

      options.onComment();
    
      self.hide();
    };
  }

  if (suggestBtn) {
    if ((self.hostBridge["requestUserPermissions"]) && (typeof self.hostBridge["requestUserPermissions"] == 'function')) {
      // Add handler on host bridge to let leos application responds
      self.hostBridge["responseUserPermissions"] = function(data) {
        console.log("Received message from host for request UserPermissions");
        if (data.indexOf('CAN_SUGGEST') !== -1) {
          suggestBtn.style.display = "flex";
        }
      };
      self.hostBridge["requestUserPermissions"]();
    }
  
    suggestBtn.style.display = "none";
    suggestBtn.onclick = this.handleSuggestCommand.bind(this);
  }
}

hypothesis_adder.Adder.prototype.disableSuggestionButton = function() {
  var suggestBtn = this.element.querySelector(SUGGESTION_BTN_SELECTOR);
  if (suggestBtn) {
    suggestBtn.onclick = null;
    suggestBtn.classList.add("annotator-disabled");
  }
}

hypothesis_adder.Adder.prototype.enableSuggestionButton = function() {
  var suggestBtn = this.element.querySelector(SUGGESTION_BTN_SELECTOR);
  if (suggestBtn) {
    suggestBtn.onclick = this.handleSuggestCommand.bind(this);
    suggestBtn.classList.remove("annotator-disabled");
  }
}

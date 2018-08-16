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

class FakeWindow {
  constructor() {
    this.callbacks = [];

    this.screen = {
      width: 1024,
      height: 768,
    };


    this.location = 'https://client.hypothes.is/app.html';
    this.open = sinon.spy(href => {
      var win = new FakeWindow;
      win.location = href;
      return win;
    });

    this.setTimeout = window.setTimeout.bind(window);
    this.clearTimeout = window.clearTimeout.bind(window);
  }

  get location() {
    return this.url;
  }

  set location(href) {
    this.url = new URL(href);
  }

  addEventListener(event, callback) {
    this.callbacks.push({event, callback});
  }

  removeEventListener(event, callback) {
    this.callbacks = this.callbacks.filter((cb) =>
      !(cb.event === event && cb.callback === callback)
    );
  }

  trigger(event) {
    this.callbacks.forEach((cb) => {
      if (cb.event === event.type) {
        cb.callback(event);
      }
    });
  }

  sendMessage(data) {
    var evt = new MessageEvent('message', { data });
    this.trigger(evt);
  }
}

module.exports = FakeWindow;

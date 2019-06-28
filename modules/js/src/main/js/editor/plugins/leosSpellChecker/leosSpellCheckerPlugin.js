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
; // jshint ignore:line
define(function leosSpellCheckerPluginModule(require) {
    'use strict';

    // load module dependencies
    var CKEDITOR = require('promise!ckEditor');
    var pluginTools = require('plugins/pluginTools');

    var pluginName = 'leosSpellChecker';

    var pluginDefinition = {
        init: function init(editor) {
        	if (editor.LEOS.isSpellCheckerEnabled) {
                editor.config.scayt_maxSuggestions = 3;
                editor.config.scayt_sLang = 'en_GB';
                editor.config.scayt_disableOptionsStorage = 'lang';

                if (editor.LEOS.spellCheckerServiceUrl) {
                    var serviceUrl = new URL(editor.LEOS.spellCheckerServiceUrl);
                    editor.config.scayt_serviceHost = serviceUrl.hostname;
                    editor.config.scayt_servicePath = serviceUrl.pathname;
                    editor.config.scayt_servicePort = serviceUrl.port;
                    editor.config.scayt_serviceProtocol = serviceUrl.protocol.substring(0, serviceUrl.protocol.length - 1);
                }

                if (editor.LEOS.spellCheckerSourceUrl) {
                    editor.config.scayt_srcUrl = editor.LEOS.spellCheckerSourceUrl;
                }

                if (editor.LEOS.spellCheckerServiceUrl && editor.LEOS.spellCheckerSourceUrl) {
                    editor.config.grayt_autoStartup = true;
                    editor.config.scayt_autoStartup = true;
                    editor.on('dialogShow', function(event) {
                        var dialog = event.data;
                        if (dialog.getName() === 'scaytDialog') {
                            dialog.hidePage('about');
                        }
                	});
                    editor.on('instanceReady', function(event) {
                        event.editor.removeMenuItem('scaytLangs');
                        event.editor.removeMenuItem('scaytDict');
                        event.editor.removeMenuItem('scaytAbout');  
                    });
                }
            }
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    // return plugin module
    var pluginModule = {
        name: pluginName
    };

    return pluginModule;
});
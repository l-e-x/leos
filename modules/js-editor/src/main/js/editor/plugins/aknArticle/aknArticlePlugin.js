/*
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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
define(function aknArticlePluginModule(require) {
    "use strict";

    // load module dependencies
    var pluginTools = require("plugins/pluginTools");
    var aknArticleNumberWidget = require("./aknArticleNumberWidget");
    var aknArticleHeadingWidget = require("./aknArticleHeadingWidget");

    var pluginName = "aknArticle";
    var cssPath = "css/aknArticle.css";

    var pluginDefinition = {
        requires : "widget,leosWidget",

        init : function init(editor) {

            editor.on("toHtml", removeInitialSnapshot, null, null, 100);

            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, cssPath));

            editor.widgets.add(aknArticleNumberWidget.name, aknArticleNumberWidget.definition);
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, aknArticleNumberWidget.css));

            editor.widgets.add(aknArticleHeadingWidget.name, aknArticleHeadingWidget.definition);
            editor.addContentsCss(pluginTools.getResourceUrl(pluginName, aknArticleHeadingWidget.css));
        }
    };

    pluginTools.addPlugin(pluginName, pluginDefinition);

    
    /*
     * Removes the initial snapshot which don't have 'article' as top level element 
     */
    function removeInitialSnapshot(event) {
        if (event.editor.undoManager.snapshots.length > 0) {
            if (event.editor.undoManager.snapshots[0].contents.indexOf("article")<0) {
                event.editor.undoManager.snapshots.shift();
            }
        }
    }
    
    var transformationConfig = {
        akn : 'article',
        html : 'article',
        attr : [ {
            akn : "id",
            html : "id"
        }, {
            akn : "leos:editable",
            html : "data-akn-attr-editable"
        }, {
            akn : "leos:deletable",
            html : "data-akn-attr-deletable"
        }, {
            html : "data-akn-name=article"
        } ],
        sub : [ {
            akn : "num",
            html : "article/h1",
            attr : [ {
                html : "class=akn-article-num"
            }, {
                akn : "leos:editable",
                html : "contenteditable=false"
            }, {
                akn : "id",
                html : "data-akn-num-id"
            } ],
            sub : {
                akn : "text",
                html : "article/h1/text"
            }
        }, {
            akn : "heading",
            html : "article/h2",
            attr : [ {
                html : "class=akn-article-heading"
            }, {
                akn : "leos:editable",
                html : "contenteditable"
            }, {
                akn : "id",
                html : "data-akn-heading-id"
            } ],
            sub : {
                akn : "text",
                html : "article/h2/text"
            }
        }, {
            akn : "article",
            html : "article/ol",
            attr : [ {
                akn : "id",
                html : "id"
            } ]
        } ]
    };

    // return plugin module
    var pluginModule = {
        name : pluginName
    };

    pluginTools.addTransformationConfigForPlugin(transformationConfig, pluginName);

    return pluginModule;
});
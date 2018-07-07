; // jshint ignore:line
define(function examplePluginModule(require) {
    "use strict";

    var CKEDITOR = require("ckEditor");

    var pluginName = "example";
    var pluginPath = "TODO";        // TODO somehow get the path to the plugin at runtime

    var pluginDefinition = {

        icons: pluginName.toLowerCase(),

        init: function init(editor) {
            // create example command
            editor.addCommand("exampleCmd", {
                exec: function execExampleCmd(editor) {
                    var now = new Date();
                    editor.insertHtml("The current date and time is: <em>" + now.toString() + "</em>");
                }
            });

            // create example toolbar button
            editor.ui.addButton("Example", {
                label: "Execute Example",
                command: "exampleCmd",
                toolbar: "insert"
            });
        }
    };

    CKEDITOR.plugins.addExternal(pluginName, pluginPath);
    CKEDITOR.plugins.add(pluginName, pluginDefinition);

    var transformationConfig = {
        akn: "b",
        html: "strong",
        attr: [{
            akn: "id",
            html: "id"
        }],
        sub: {
            akn: "text",
            html: "strong/text"
        },
        direct: true
    };

    // TODO register transformation config with transformer

    // return plugin module
    var pluginModule = {
        name: pluginName,
        config: transformationConfig    // TODO not sure if it's useful to expose transformation config
    };
    
    return pluginModule;
});
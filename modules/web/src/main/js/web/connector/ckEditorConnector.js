/*
 * Copyright 2015 European Commission
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
/* This connector is connected to CKEditorComponent class in server side.*/
window.eu_europa_ec_leos_web_ui_component_CKEditorComponent = function() {

    var abstractJSComponent=this;
    var profileId  = abstractJSComponent.getState().profileId;
    var editorName = abstractJSComponent.getState().editorName;
    var ckEditor = abstractJSComponent.getElement();    /*ckEditor is the DOM element reference*/
    ckEditor.id=editorName;

    LEOS.createEditor(profileId, editorName,  ckConnector_setServerSidelisteners);

    /* default call to prpagateAll state changes to the server side cause onStateChange to fire at client side*/
    this.onStateChange = function() {
        var newContent= abstractJSComponent.getState().content;
        console.log("stateChange Called");
        if (typeof CKEDITOR !== 'undefined') {
            var editor = CKEDITOR.instances[editorName];
            ckConnector_pushContentInEditor(editor, newContent);
        }
    }/*end onStateChange*/

    
    /* this function can be invoked from JS or vaadin for getting things done on CKEditor. 
     * its purpose is to do CKeditor level manipulation and then handlover to vaadin by calling onAction method */
    this.ckConnector_doAction= function (action, editorName) {
        var editor = CKEDITOR.instances[editorName];
        if (editor) {
            switch (action) {
             case "save": {
                 abstractJSComponent.onAction("onSave", editor.getData());
                 editor.dataChangedPushedToServer = false;
             }
             break;
             case "close": {
                 editor.destroy();
                 abstractJSComponent.onAction("onClose");
             }
             break;
             case "instanceReady": {
                 editor.dataChangedPushedToServer = false;
                 ckConnector_pushContentInEditor(editor, abstractJSComponent.getState().content );
                 abstractJSComponent.onAction("onInstanceReady");
             }
             break;
             case "change":{
                 if (!editor.dataChangedPushedToServer) {
                     abstractJSComponent.onAction("onChange");
                     editor.dataChangedPushedToServer = true;
                 }
             }
             break;
          }/* end switch*/
       }/*if editor*/
    }
    
    /*callback method, once ckeditor instance is ready*/
    function ckConnector_setServerSidelisteners(editor) {
        if (editor) {
            editor.on('instanceReady', function(event) {
                event.listenerData.ckConnector_doAction("instanceReady", editor.name);

                /*for propagating the change events to server*/
                editor.on('change', function () {
                    event.listenerData.ckConnector_doAction("change", editor.name);
                },null, event.listenerData);

            }, null, abstractJSComponent);
        }
    }

    function ckConnector_pushContentInEditor(editor,newContent) {
        if (editor && editor.dataChangedPushedToServer == false) {//do not refresh if content is new in editor
            editor.dataChangedPushedToServer = true;//stop change to be reported to server
            var options = {
                callback: function(editorName) { 
                    this.dataChangedPushedToServer = false;//restore
                    }
            };
            editor.setData(newContent, options);//this triggers change
        }
    }
    
    
}





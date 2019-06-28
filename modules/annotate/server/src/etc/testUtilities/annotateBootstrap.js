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
//config for local development
var base_config = {
    leosDocumentRootNode: "akomantoso",
    annotationContainer: "#docContainer",
    server: "http://localhost:9099/annotate",
    clientId : "AnnotateIssuedClientId",
    secret : "AnnotateIssuedSecret",
    authority : "LEOS",
    userLogin : "demo" //use any user login present in UD-repo
};

function startup_leos() {
    var dummyConnector ={};
    _addHostConfig(document);
    _configureHostBridge(dummyConnector);
    _addScript(document, `${base_config.server+"/client/boot.js"}`);
}

function _configureHostBridge(connector) {
    connector.hostBridge = connector.hostBridge || {};

    var annotationContainerElt = document.querySelector(base_config.annotationContainer);
    if (annotationContainerElt) {
        window.onresize = function(event) {
            var event = new Event("annotationSidebarResize");
            annotationContainerElt.dispatchEvent(event);
        };

        var dummySecurityResponse = function (connector) {
            console.log("dummy responseSecurityToken sent");
            connector.hostBridge.responseSecurityToken(_getToken());
        };
        var dummyPermissionsResponse = function (connector) {
            console.log("dummy responseUserPermissions sent");
            connector.hostBridge.responseUserPermissions(["CAN_READ", "CAN_UPDATE", "CAN_DELETE", "CAN_COMMENT", "CAN_MERGE_SUGGESTION", "CAN_SHARE", "CAN_PRINT_LW"]);
        };
        var dummyMergeResponse = function (connector) {
            console.log("dummy responseMergeSuggestion sent");
            connector.hostBridge.responseMergeSuggestion({result: "SUCCESS",message: "Suggestion successfully merged."});
        };
        var dummyDocumentMetadata = function (connector) {
            console.log("dummy responseDocumentMetadata sent");
            connector.hostBridge.responseDocumentMetadata('{}');
        };
        var dummySearchMetadata = function (connector) {
            console.log("dummy responseSearchMetadata sent");
            connector.hostBridge.responseSearchMetadata('[]');
        };

        connector.hostBridge.requestSecurityToken = function () {
            setTimeout(dummySecurityResponse,100, connector);
        };
        connector.hostBridge.requestUserPermissions = function () {
            setTimeout(dummyPermissionsResponse,100, connector);
        };
        connector.hostBridge.requestMergeSuggestion = function (selector) {
            setTimeout(dummyMergeResponse,100, connector, selector);
        };
        connector.hostBridge.requestDocumentMetadata = function (selector) {
            setTimeout(dummyDocumentMetadata,100, connector, selector);
        };
        connector.hostBridge.requestSearchMetadata = function (selector) {
            setTimeout(dummySearchMetadata,100, connector, selector);
        };

        annotationContainerElt.hostBridge = connector.hostBridge;
    }
}

function _getToken() {
    // Defining our token parts
    var header = {
        "alg": "HS256",
        "typ": "JWT"
    };

    var dateNow = Math.floor(Date.now() / 1000);

    var data = {
        "iss": `${base_config.clientId}`,
        "sub":`acct:${base_config.userLogin}@${base_config.authority}`,
        "aud": "intragate.development.ec.europa.eu",
        "iat": dateNow ,
        "nbf": dateNow -50,
        "exp":dateNow + 600
    };


    function base64url(source) {
        // Encode in classical base64
        encodedSource = CryptoJS.enc.Base64.stringify(source);
        // Remove padding equal characters
        encodedSource = encodedSource.replace(/=+$/, '');
        // Replace characters according to base64url specifications
        encodedSource = encodedSource.replace(/\+/g, '-');
        encodedSource = encodedSource.replace(/\//g, '_');
        return encodedSource;
    }

    var encodedHeader = base64url(CryptoJS.enc.Utf8.parse(JSON.stringify(header)));
    var encodedData = base64url(CryptoJS.enc.Utf8.parse(JSON.stringify(data)));
    var signature = encodedHeader + "." + encodedData;
    signature = CryptoJS.HmacSHA256(signature, base_config.secret);
    signature = base64url(signature);

    return `${encodedHeader}.${encodedData}.${signature}` ;
}

function _addScript(doc, url) {
    var script = doc.createElement('script');
    script.addEventListener('error', _onErrorLoad, false);
    script.type = 'text/javascript';
    script.src = url;
    script.async = true;
    doc.head.appendChild(script);

    function _onErrorLoad(event) {
        console.log('Error occurred while loading script ', event);
    }
}

function _addHostConfig(doc) {
    var script = doc.createElement('script');
    script.type = 'application/json';
    script.className = 'js-hypothesis-config';
    var webSocketUrl = base_config.server.replace('https','wss').replace('http','ws')+"/ws";

    script.innerHTML = `{
            "leosDocumentRootNode": "${base_config.leosDocumentRootNode}",
            "annotationContainer": "${base_config.annotationContainer}",
            "ignoredTags": ["div"],
            "allowedSelectorTags": "a.ref2link-generated",
            "editableAttribute": "leos:editable",
            "oauthClientId": "${base_config.clientId}",
            "assetRoot": "${base_config.server}/client",
            "sidebarAppUrl": "${base_config.server}/app.html",
            "services": [{
                "authority": "${base_config.authority}",
                "apiUrl": "${base_config.server}/api/",
                "websocketUrl":"${webSocketUrl}"                                
                }]
            }`;
    doc.body.appendChild(script);
}

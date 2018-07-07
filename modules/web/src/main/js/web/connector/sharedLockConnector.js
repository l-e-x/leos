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
var documentLocks;
/* This connector is connected to SharedLockComponent class in server side.*/
/* All state changes to the serverside cause onStateChange to fire at client side*/
window.eu_europa_ec_leos_web_ui_component_SharedLockComponent = function() {
    /* Store changes from the server-side to global for use later*/
    this.onStateChange = function() {
        documentLocks= this.getState().locks;
     };
};

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
/* This connector is connected to SideCommentsComponent class in server side.*/
window.eu_europa_ec_leos_web_ui_component_CommentsComponent = function() {
    'use strict'

    //** All state changes to the server side cause onStateChange to fire at client side *//
    this.onStateChange = function() {
        console.debug("On state change called...");
        if (this.getState().ready) {
            console.debug("On state change called, initializing side comments with..", this.getState().existingComments)
            LEOS.setUpSideComments(this.getState().existingComments, this.getState().currentUser);
        }
    };/* end on state change */

};/* end connector */


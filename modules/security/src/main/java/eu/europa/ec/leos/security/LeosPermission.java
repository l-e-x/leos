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
package eu.europa.ec.leos.security;

public enum LeosPermission {
    CAN_CREATE,
    CAN_READ,
    CAN_UPDATE,
    CAN_DELETE,
    CAN_COMMENT,
    CAN_SUGGEST,
    CAN_MERGE_SUGGESTION,
    CAN_SHARE,
    CAN_PRINT_LW,
    CAN_PRINT_DW,
    CAN_CREATE_MILESTONE,
    CAN_RESTORE_PREVIOUS_VERSION,
    CAN_ADD_REMOVE_COLLABORATOR,
    CAN_DOWNLOAD_PROPOSAL,
    CAN_UPLOAD,
    CAN_SEE_SOURCE
}

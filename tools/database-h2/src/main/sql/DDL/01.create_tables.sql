--
-- Copyright 2016 European Commission
--
-- Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
-- You may not use this work except in compliance with the Licence.
-- You may obtain a copy of the Licence at:
--
--     https://joinup.ec.europa.eu/software/page/eupl
--
-- Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the Licence for the specific language governing permissions and limitations under the Licence.
--

CREATE TABLE LEOS_USER (
    USR_ID NUMBER(38) NOT NULL,
    USR_LOGIN VARCHAR2(50 CHAR) NOT NULL,
    USR_NAME VARCHAR2(200 CHAR) NOT NULL,
    USR_CREATED_BY NUMBER(38) NOT NULL,
    USR_CREATED_ON DATE NOT NULL,
    USR_UPDATED_BY NUMBER(38),
    USR_UPDATED_ON DATE,
    USR_STATE CHAR(1) NOT NULL
);
--
-- Copyright 2019 European Commission
--
-- Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
-- You may not use this work except in compliance with the Licence.
-- You may obtain a copy of the Licence at:
--
--     https://joinup.ec.europa.eu/software/page/eupl
--
-- Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the Licence for the specific language governing permissions and limitations under the Licence.
--

------------------------------------------------------------------------
-- Data to be inserted initially into H2 database
------------------------------------------------------------------------
DELETE FROM METADATA;
DELETE FROM GROUPS;
DELETE FROM USERS;
DELETE FROM USERS_GROUPS;
DELETE FROM TAGS;
DELETE FROM DOCUMENTS;
DELETE FROM ANNOTATIONS;
DELETE FROM AUTHCLIENTS;
DELETE FROM TOKENS;

-- default group
INSERT INTO GROUPS(NAME, DESCRIPTION, DISPLAYNAME, ISPUBLIC)
  VALUES ('__world__', 'Everybody', 'Collaborators', 1);

-- client that we are connected to - HERE YOU CAN FILL IN THE CLIENT_ID AND SECRET OF YOUR LOCAL CLIENT
INSERT INTO AUTHCLIENTS(DESCRIPTION, CLIENT_ID, SECRET, AUTHORITIES)
  VALUES ('leos client', 'AnnotateIssuedClientId', 'AnnotateIssuedSecret', NULL);
Commit;
--
-- Copyright 2018 European Commission
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

-- Dummy data for default group
DELETE FROM GROUPS;
DELETE FROM USERS;
DELETE FROM USERS_GROUPS;
DELETE FROM TAGS;
DELETE FROM DOCUMENTS;
DELETE FROM ANNOTATIONS;
DELETE FROM AUTHCLIENTS;
DELETE FROM TOKENS;

ALTER SEQUENCE GROUPS_SEQ RESTART WITH 1;
ALTER SEQUENCE USERS_SEQ RESTART WITH 1;
ALTER SEQUENCE USERS_GROUPS_SEQ RESTART WITH 1;
ALTER SEQUENCE TAGS_SEQ RESTART WITH 1;
ALTER SEQUENCE DOCUMENTS_SEQ RESTART WITH 1;
ALTER SEQUENCE AUTHCLIENTS_SEQ RESTART WITH 1;
ALTER SEQUENCE TOKENS_SEQ RESTART WITH 1;

Insert into GROUPS(NAME, DESCRIPTION, DISPLAYNAME, ISPUBLIC)
 values ('__world__', 'Everybody', 'Public', 1);
 
Commit;

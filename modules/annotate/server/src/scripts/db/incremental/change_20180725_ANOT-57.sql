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

------------------------------------
-- Changes to initial Oracle 
-- database creation scripts
--
-- adds new column in table TOKENS
--
-- change initiated by ANOT-57
------------------------------------

-- since the new column is not nullable, we clean the table before - contains only temporary data anyway
DELETE FROM "TOKENS";

-- add new column
ALTER TABLE "TOKENS" ADD ("AUTHORITY" VARCHAR2(50) NOT NULL);

COMMENT ON COLUMN "TOKENS"."AUTHORITY" IS 'Authority for which the token is issued';

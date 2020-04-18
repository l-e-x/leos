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
-- adds a new column on METADATA table for the "version" property
-- creates an index on the new column
-- 
-- change initiated by ANOT-89
------------------------------------
ALTER TABLE "METADATA" ADD "VERSION" VARCHAR2(50);
COMMENT ON COLUMN "METADATA"."VERSION" IS 'Version of the annotated document';

CREATE INDEX "METADATA_IX_VERSION" ON "METADATA" (VERSION ASC);

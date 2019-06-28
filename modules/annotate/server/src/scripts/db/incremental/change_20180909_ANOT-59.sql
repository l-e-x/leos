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
-- removes two columns (being foreign key constraints) from ANNOTATIONS table
--
-- change initiated by ANOT-59
------------------------------------

-- remove the foreign key constraints
ALTER TABLE "ANNOTATIONS" DROP CONSTRAINT "ANNOTATIONS_FK_DOCUMENTS";
ALTER TABLE "ANNOTATIONS" DROP CONSTRAINT "ANNOTATIONS_FK_GROUPS";

-- remove the column indexes
DROP INDEX "ANNOTATIONS_IX_DOCUMENTS";
DROP INDEX "ANNOTATIONS_IX_GROUPS";

-- remove the columns
ALTER TABLE "ANNOTATIONS" DROP COLUMN "DOCUMENT_ID";
ALTER TABLE "ANNOTATIONS" DROP COLUMN "GROUP_ID";

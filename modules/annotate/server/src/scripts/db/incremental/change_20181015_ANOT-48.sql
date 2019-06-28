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
-- adds a new column on ANNOTATIONS table for status transitions (soft delete),
--   and two auditing columns for status change
-- creates a combined index on columns mostly used for search operations
-- note: we don't update the STATUS_UPDATED field using a trigger as it isn't be supported by H2 and implementations would diverge
--
-- also adds two auditing columns for response status change on METADATA table
-- 
-- change initiated by ANOT-48
------------------------------------
ALTER TABLE "ANNOTATIONS" ADD ("STATUS" SMALLINT DEFAULT 0 NOT NULL);
COMMENT ON COLUMN "ANNOTATIONS"."STATUS" IS 'Annotation status, e.g. normal/deleted/accepted/rejected';

ALTER TABLE ANNOTATIONS ADD "STATUS_UPDATED" DATE;
COMMENT ON COLUMN "ANNOTATIONS"."STATUS_UPDATED" IS 'Timestamp of status change';

ALTER TABLE ANNOTATIONS ADD "STATUS_UPDATED_BY" NUMBER;
COMMENT ON COLUMN "ANNOTATIONS"."STATUS_UPDATED_BY" IS 'User id of user that changed status';

CREATE INDEX "ANNOTATIONS_IX_STATUS_ROOT" ON "ANNOTATIONS" (STATUS ASC, ROOT ASC);

ALTER TABLE METADATA ADD "RESPONSE_STATUS_UPDATED" DATE;
COMMENT ON COLUMN "METADATA"."RESPONSE_STATUS_UPDATED" IS 'Timestamp of response status change';

ALTER TABLE METADATA ADD "RESPONSE_STATUS_UPDATED_BY" NUMBER;
COMMENT ON COLUMN "METADATA"."RESPONSE_STATUS_UPDATED_BY" IS 'User id of user that changed response status';


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
-- adds new table METADATA
--
-- change initiated by ANOT-52
------------------------------------

------------------------------------
-- METADATA
-- requires sequence, table, trigger
------------------------------------
CREATE SEQUENCE "METADATA_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE;

CREATE TABLE "METADATA" (
  "ID" NUMBER NOT NULL ENABLE, 
  "DOCUMENT_ID" NUMBER NOT NULL ENABLE, 
  "GROUP_ID" NUMBER NOT NULL ENABLE, 
  "SYSTEM_ID" VARCHAR2(50 BYTE) NOT NULL ENABLE, 
  "KEYVALUES" CLOB, 
  "RESPONSE_STATUS" NUMBER(*,0), 
  CONSTRAINT "METADATA_PK" PRIMARY KEY ("ID") USING INDEX ENABLE, 
  CONSTRAINT "METADATA_UK_DOC_GROUP_SYSID" UNIQUE ("DOCUMENT_ID", "GROUP_ID", "SYSTEM_ID") USING INDEX ENABLE, 
  CONSTRAINT "METADATA_FK_GROUPS" FOREIGN KEY ("GROUP_ID") REFERENCES "GROUPS" ("GROUP_ID") ON DELETE CASCADE ENABLE, 
  CONSTRAINT "METADATA_FK_DOCUMENTS" FOREIGN KEY ("DOCUMENT_ID") REFERENCES "DOCUMENTS" ("DOCUMENT_ID") ON DELETE CASCADE ENABLE
)
LOB ("KEYVALUES") STORE AS BASICFILE;

COMMENT ON COLUMN "METADATA"."ID" IS 'internal ID';
COMMENT ON COLUMN "METADATA"."DOCUMENT_ID" IS 'ID of the document to which the metadata belongs to';
COMMENT ON COLUMN "METADATA"."GROUP_ID" IS 'ID of the group to which the metadata belongs to';
COMMENT ON COLUMN "METADATA"."SYSTEM_ID" IS 'ID of the related system/authority';
COMMENT ON COLUMN "METADATA"."KEYVALUES" IS 'Key-value pairs of metadata';
COMMENT ON COLUMN "METADATA"."RESPONSE_STATUS" IS 'Enum denoting status of ISC responses';

CREATE INDEX "METADATA_IX_RESPONSE_STATUS" ON "METADATA" ("RESPONSE_STATUS");
CREATE INDEX "METADATA_IX_SYSTEM_ID" ON "METADATA" ("SYSTEM_ID");

CREATE OR REPLACE TRIGGER "METADATA_TRG" 
BEFORE INSERT ON METADATA 
FOR EACH ROW 
BEGIN
  <<COLUMN_SEQUENCES>>
  BEGIN
    IF INSERTING AND :NEW.ID IS NULL THEN
      SELECT METADATA_SEQ.NEXTVAL INTO :NEW.ID FROM SYS.DUAL;
    END IF;
  END COLUMN_SEQUENCES;
END;
/
ALTER TRIGGER "METADATA_TRG" ENABLE;


------------------------------------
-- ANNOTATIONS
-- add column, foreign key and index
------------------------------------

ALTER TABLE "ANNOTATIONS" ADD "METADATA_ID" NUMBER;
COMMENT ON COLUMN "ANNOTATIONS"."METADATA_ID" IS 'ID of the related metadata set';

------------------------------------
-- NOTE: after this, entries in the METADATA table will be created 
--       for each combination of DOCUMENTS x GROUPS (x SYSTEM_ID) of the ANNOTATIONS table
--       afterwards, a foreign key from ANNOTATIONS to METADATA will be established
------------------------------------

BEGIN
  -- first insert METADATA entries for each combination of DOCUMENT_ID x GROUP_ID
  FOR annotCombinationRec IN
    (SELECT DISTINCT DOCUMENT_ID, GROUP_ID FROM ANNOTATIONS)
  LOOP
    INSERT INTO METADATA (DOCUMENT_ID, GROUP_ID, SYSTEM_ID)
    VALUES (annotCombinationRec.DOCUMENT_ID, annotCombinationRec.GROUP_ID, 'LEOS');
  END LOOP;
  
  -- then update the ANNOTATIONS entries with the IDs of the created METADATA entries
  FOR metaRec IN
    (SELECT * FROM METADATA)
  LOOP
    UPDATE ANNOTATIONS SET METADATA_ID = metaRec.ID
    WHERE ANNOTATIONS.DOCUMENT_ID=metaRec.DOCUMENT_ID AND ANNOTATIONS.GROUP_ID=metaRec.GROUP_ID;
  END LOOP;
END;
/

------------------------------------
-- ANNOTATIONS
-- add foreign key and index
------------------------------------

-- now make the new column 'not nullable' and then define the foreign key
ALTER TABLE "ANNOTATIONS" MODIFY ("METADATA_ID" NOT NULL);
ALTER TABLE "ANNOTATIONS" ADD CONSTRAINT "ANNOTATIONS_FK_METADATA" FOREIGN KEY ("METADATA_ID") REFERENCES "METADATA" ("ID") ON DELETE CASCADE ENABLE;

CREATE INDEX "ANNOTATIONS_IX_METADATA" ON "ANNOTATIONS" ("METADATA_ID");

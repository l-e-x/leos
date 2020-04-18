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
-- Changes to Oracle database 
--
-- adds a new table AUTHCLIENTS
--
-- change initiated by ANOT-51
------------------------------------
CREATE SEQUENCE "AUTHCLIENTS_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE;

CREATE TABLE "AUTHCLIENTS" (
  "DESCRIPTION" VARCHAR2(50 BYTE) NOT NULL ENABLE, 
  "ID" NUMBER NOT NULL ENABLE, 
  "SECRET" VARCHAR2(80 BYTE) NOT NULL ENABLE, 
  "CLIENT_ID" VARCHAR2(80 BYTE) NOT NULL ENABLE, 
  "AUTHORITIES" VARCHAR2(80 BYTE), 
  CONSTRAINT "AUTHCLIENTS_PK" PRIMARY KEY ("ID") USING INDEX ENABLE, 
  CONSTRAINT "AUTHCLIENTS_UK_SECRET" UNIQUE ("SECRET") USING INDEX ENABLE, 
  CONSTRAINT "AUTHCLIENTS_UK_CLIENT_ID" UNIQUE ("CLIENT_ID") USING INDEX ENABLE
);

COMMENT ON COLUMN "AUTHCLIENTS"."DESCRIPTION" IS 'Description for identitying the client';
COMMENT ON COLUMN "AUTHCLIENTS"."ID" IS 'internal ID';
COMMENT ON COLUMN "AUTHCLIENTS"."SECRET" IS 'Client''s secret key, used for decrypting tokens';
COMMENT ON COLUMN "AUTHCLIENTS"."CLIENT_ID" IS 'Client''s issuer ID, used to identify its tokens';
COMMENT ON COLUMN "AUTHCLIENTS"."AUTHORITIES" IS 'Authorities for which this client is allowed to authorize users; separated by semi-colons';

CREATE OR REPLACE TRIGGER "AUTHCLIENTS_TRG" 
  BEFORE INSERT ON AUTHCLIENTS 
  FOR EACH ROW 
  BEGIN
    <<COLUMN_SEQUENCES>>
    BEGIN
      IF INSERTING AND :NEW.ID IS NULL THEN
        SELECT AUTHCLIENTS_SEQ.NEXTVAL INTO :NEW.ID FROM SYS.DUAL;
      END IF;
    END COLUMN_SEQUENCES;
  END;
/
ALTER TRIGGER "AUTHCLIENTS_TRG" ENABLE;
/
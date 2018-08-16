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

------------------------------------
-- Changes to initial Oracle 
-- database creation scripts
--
-- adds new table TOKENS
-- removes three columns from table USERS
--
-- change initiated by ANOT-50
------------------------------------

------------------------------------
-- TOKENS
-- requires sequence, table, trigger
------------------------------------

CREATE SEQUENCE "TOKENS_SEQ"  MINVALUE 1 MAXVALUE 9999999999999999999999999999 INCREMENT BY 1 START WITH 1 CACHE 20 NOORDER  NOCYCLE;

CREATE TABLE "TOKENS" (
  "ID" NUMBER NOT NULL ENABLE, 
  "USER_ID" NUMBER NOT NULL ENABLE, 
  "ACCESS_TOKEN" VARCHAR2(50 BYTE) NOT NULL ENABLE, 
  "ACCESS_TOKEN_EXPIRES" DATE NOT NULL ENABLE, 
  "REFRESH_TOKEN" VARCHAR2(50 BYTE) NOT NULL ENABLE, 
  "REFRESH_TOKEN_EXPIRES" DATE NOT NULL ENABLE, 
  CONSTRAINT "TOKENS_PK" PRIMARY KEY ("ID") USING INDEX ENABLE, 
  CONSTRAINT "TOKENS_UK_REFRESH_TOKEN" UNIQUE ("REFRESH_TOKEN") USING INDEX ENABLE, 
  CONSTRAINT "TOKENS_UK_ACCESS_TOKEN" UNIQUE ("ACCESS_TOKEN") USING INDEX ENABLE, 
  CONSTRAINT "TOKENS_FK_USERS" FOREIGN KEY ("USER_ID") REFERENCES "USERS" ("USER_ID") ON DELETE CASCADE ENABLE
);

COMMENT ON COLUMN "TOKENS"."ID" IS 'Internal ID';
COMMENT ON COLUMN "TOKENS"."USER_ID" IS 'ID of the user owning the tokens';
COMMENT ON COLUMN "TOKENS"."ACCESS_TOKEN" IS 'Access token granted to the user';
COMMENT ON COLUMN "TOKENS"."ACCESS_TOKEN_EXPIRES" IS 'Expiration timestamp of access token';
COMMENT ON COLUMN "TOKENS"."REFRESH_TOKEN" IS 'Refresh token granted to the user';
COMMENT ON COLUMN "TOKENS"."REFRESH_TOKEN_EXPIRES" IS 'Expiration timestamp of the refresh token';

CREATE OR REPLACE TRIGGER "TOKENS_TRG" 
  BEFORE INSERT ON TOKENS 
  FOR EACH ROW 
  BEGIN
    <<COLUMN_SEQUENCES>>
    BEGIN
      IF INSERTING AND :NEW.ID IS NULL THEN
        SELECT TOKENS_SEQ.NEXTVAL INTO :NEW.ID FROM SYS.DUAL;
      END IF;
    END COLUMN_SEQUENCES;
  END;
/
ALTER TRIGGER "TOKENS_TRG" ENABLE;


------------------------------------
-- USERS
-- remove columns
------------------------------------
ALTER TABLE "USERS" DROP COLUMN "ACCESS_TOKEN";
ALTER TABLE "USERS" DROP COLUMN "ACCESS_TOKEN_CREATED";
ALTER TABLE "USERS" DROP COLUMN "REFRESH_TOKEN";

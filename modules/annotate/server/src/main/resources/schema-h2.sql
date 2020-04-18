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
-- H2 database startup
------------------------------------

------------------------------------
-- table USERS
------------------------------------
CREATE SEQUENCE IF NOT EXISTS "USERS_SEQ" MINVALUE 1 INCREMENT BY 1 START WITH 1;

CREATE TABLE IF NOT EXISTS USERS (
  USER_ID                          NUMBER default USERS_SEQ.nextval PRIMARY KEY,
  LOGIN                            VARCHAR2(30 CHAR),
  SIDEBAR_TUTORIAL_DISMISSED       NUMBER(1,0) DEFAULT 0 NOT NULL,
  CONSTRAINT "USERS_PK" PRIMARY KEY ("USER_ID"),
  CONSTRAINT "USERS_UK_LOGIN" UNIQUE ("LOGIN")
);

COMMENT ON COLUMN "USERS"."USER_ID" IS 'Internal user ID';
COMMENT ON COLUMN "USERS"."LOGIN" IS 'User login name';
COMMENT ON COLUMN "USERS"."SIDEBAR_TUTORIAL_DISMISSED" IS 'Flag indicating whether the small tutorial in the sidebar was closed already';

-- note: for Oracle, we have to create a trigger to update the USER_ID using sequence; this is not needed for H2

  
------------------------------------
-- table GROUPS
------------------------------------
CREATE SEQUENCE IF NOT EXISTS "GROUPS_SEQ" MINVALUE 1 INCREMENT BY 1 START WITH 1;

CREATE TABLE IF NOT EXISTS GROUPS (
  GROUP_ID                         NUMBER default GROUPS_SEQ.nextval PRIMARY KEY,
  NAME                             VARCHAR2(25 CHAR) NOT NULL,
  DESCRIPTION                      VARCHAR2(50 CHAR) NOT NULL,
  DISPLAYNAME                      VARCHAR2(30 CHAR) NOT NULL,
  ISPUBLIC                         NUMBER(1,0) DEFAULT 1 NOT NULL,
  CONSTRAINT "GROUPS_PK" PRIMARY KEY ("GROUP_ID"), 
  CONSTRAINT "GROUPS_UK_NAMES" UNIQUE ("NAME", "DISPLAYNAME")
);

COMMENT ON COLUMN "GROUPS"."GROUP_ID" IS 'Group ID';
COMMENT ON COLUMN "GROUPS"."NAME" IS 'Internal group ID';
COMMENT ON COLUMN "GROUPS"."DESCRIPTION" IS 'Description of the group''s purpose';
COMMENT ON COLUMN "GROUPS"."DISPLAYNAME" IS 'Nice name shown to the user';
COMMENT ON COLUMN "GROUPS"."ISPUBLIC" IS 'Flag indicating whether group is public';

-- note: for Oracle, we have to create a trigger to update the GROUP_ID using sequence; this is not needed for H2


------------------------------------
-- table USERS_GROUPS
------------------------------------
CREATE SEQUENCE IF NOT EXISTS "USERS_GROUPS_SEQ" MINVALUE 1 INCREMENT BY 1 START WITH 1;

CREATE TABLE IF NOT EXISTS USERS_GROUPS (
  ID                               NUMBER default USERS_GROUPS_SEQ.nextval PRIMARY KEY,
  USER_ID                          NUMBER NOT NULL,
  GROUP_ID                         NUMBER NOT NULL,
  CONSTRAINT "USERS_GROUPS_PK" PRIMARY KEY ("ID"), 
  CONSTRAINT "USERS_GROUPS_UK_USER_GROUP" UNIQUE ("USER_ID", "GROUP_ID"),
  CONSTRAINT "USERS_GROUPS_FK_GROUPS" FOREIGN KEY ("GROUP_ID") REFERENCES "GROUPS" ("GROUP_ID") ON DELETE CASCADE, 
  CONSTRAINT "USERS_GROUPS_FK_USERS" FOREIGN KEY ("USER_ID") REFERENCES "USERS" ("USER_ID") ON DELETE CASCADE
);

COMMENT ON COLUMN "USERS_GROUPS"."ID" IS 'internal ID';
COMMENT ON COLUMN "USERS_GROUPS"."USER_ID" IS 'ID of the user belonging to a group';
COMMENT ON COLUMN "USERS_GROUPS"."GROUP_ID" IS 'Group ID of the belonging group';

CREATE INDEX IF NOT EXISTS "USERS_GROUPS_IX_GROUPS" ON "USERS_GROUPS" ("GROUP_ID");

-- note: for Oracle, we have to create a trigger to update the ID using sequence; this is not needed for H2


------------------------------------
-- table DOCUMENTS
------------------------------------
CREATE SEQUENCE IF NOT EXISTS "DOCUMENTS_SEQ" MINVALUE 1 INCREMENT BY 1 START WITH 1;

CREATE TABLE IF NOT EXISTS DOCUMENTS (
  DOCUMENT_ID                      NUMBER default DOCUMENTS_SEQ.nextval PRIMARY KEY,
  TITLE                            VARCHAR2(100 CHAR),
  URI                              VARCHAR2(500 CHAR) NOT NULL,
  CONSTRAINT "DOCUMENTS_PK" PRIMARY KEY ("DOCUMENT_ID"),
  CONSTRAINT "DOCUMENTS_UK_URI" UNIQUE ("URI")
);

COMMENT ON COLUMN "DOCUMENTS"."DOCUMENT_ID" IS 'Internal document ID';
COMMENT ON COLUMN "DOCUMENTS"."TITLE" IS 'Document''s title';
COMMENT ON COLUMN "DOCUMENTS"."URI" IS 'URI of the annotated document';

-- note: for Oracle, we have to create a trigger to update the DOCUMENT_ID using sequence; this is not needed for H2


------------------------------------
-- table METADATA
------------------------------------
CREATE SEQUENCE IF NOT EXISTS "METADATA_SEQ" MINVALUE 1 INCREMENT BY 1 START WITH 1;

CREATE TABLE IF NOT EXISTS METADATA (
  ID                               NUMBER DEFAULT METADATA_SEQ.nextval PRIMARY KEY, 
  DOCUMENT_ID                      NUMBER NOT NULL, 
  GROUP_ID                         NUMBER NOT NULL, 
  SYSTEM_ID                        VARCHAR2(50 CHAR) NOT NULL,
  VERSION                          VARCHAR2(50 CHAR),
  KEYVALUES                        CLOB, 
  RESPONSE_STATUS                  SMALLINT,
  RESPONSE_STATUS_UPDATED          TIMESTAMP,
  RESPONSE_STATUS_UPDATED_BY       NUMBER,
  CONSTRAINT "METADATA_PK" PRIMARY KEY ("ID"), 
  CONSTRAINT "METADATA_FK_GROUPS" FOREIGN KEY ("GROUP_ID") REFERENCES "GROUPS" ("GROUP_ID") ON DELETE CASCADE, 
  CONSTRAINT "METADATA_FK_DOCUMENTS" FOREIGN KEY ("DOCUMENT_ID") REFERENCES "DOCUMENTS" ("DOCUMENT_ID") ON DELETE CASCADE
);

COMMENT ON COLUMN "METADATA"."ID" IS 'internal ID';
COMMENT ON COLUMN "METADATA"."DOCUMENT_ID" IS 'ID of the document to which the metadata belongs to';
COMMENT ON COLUMN "METADATA"."GROUP_ID" IS 'ID of the group to which the metadata belongs to';
COMMENT ON COLUMN "METADATA"."SYSTEM_ID" IS 'ID of the related system/authority';
COMMENT ON COLUMN "METADATA"."VERSION" IS 'Version of the annotated document';
COMMENT ON COLUMN "METADATA"."KEYVALUES" IS 'Key-value pairs of metadata';
COMMENT ON COLUMN "METADATA"."RESPONSE_STATUS" IS 'Enum denoting status of ISC responses';
COMMENT ON COLUMN "METADATA"."RESPONSE_STATUS_UPDATED" IS 'Timestamp of response status change';
COMMENT ON COLUMN "METADATA"."RESPONSE_STATUS_UPDATED_BY" IS 'User id of user that changed response status';

CREATE INDEX IF NOT EXISTS "METADATA_IX_RESPONSE_STATUS" ON "METADATA" ("RESPONSE_STATUS");
CREATE INDEX IF NOT EXISTS "METADATA_IX_SYSTEM_ID" ON "METADATA" ("SYSTEM_ID");
CREATE INDEX IF NOT EXISTS "METADATA_IX_VERSION" ON "METADATA" ("VERSION");
  
-- note: for Oracle, we have to create a trigger to update the ID using sequence; this is not needed for H2


------------------------------------
-- table ANNOTATIONS
------------------------------------
CREATE TABLE IF NOT EXISTS ANNOTATIONS (
  ANNOTATION_ID                    VARCHAR2(22 CHAR) PRIMARY KEY,
  LINKED_ANNOT_ID                  VARCHAR2(22 CHAR),
  TEXT                             CLOB,
  CREATED                          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  UPDATED                          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  USER_ID                          NUMBER NOT NULL,
  SHARED                           NUMBER(1,0) DEFAULT 1 NOT NULL, 
  TARGET_SELECTORS                 CLOB NOT NULL, 
  METADATA_ID                      NUMBER NOT NULL,
  ROOT                             VARCHAR2(88 CHAR) AS (CASE REFERENCES WHEN NULL THEN NULL ELSE SUBSTR("REFERENCES",1,22) END), 
  REFERENCES                       CLOB,
  STATUS                           SMALLINT DEFAULT 0 NOT NULL,
  STATUS_UPDATED                   TIMESTAMP,
  STATUS_UPDATED_BY                NUMBER,
  SENT_DELETED                     NUMBER(1,0) DEFAULT 0 NOT NULL,
  RESP_VERSION_SENT_DELETED        NUMBER DEFAULT 0 NOT NULL,
  CONSTRAINT "ANNOTATIONS_PK" PRIMARY KEY ("ANNOTATION_ID"),
  CONSTRAINT "ANNOTATIONS_FK_USERS" FOREIGN KEY ("USER_ID") REFERENCES "USERS" ("USER_ID") ON DELETE CASCADE, 
  CONSTRAINT "ANNOTATIONS_FK_METADATA" FOREIGN KEY ("METADATA_ID") REFERENCES "METADATA" ("ID") ON DELETE CASCADE,
  CONSTRAINT "ANNOTATIONS_FK_ROOT" FOREIGN KEY ("ROOT") REFERENCES "ANNOTATIONS" ("ANNOTATION_ID") ON DELETE CASCADE
);

COMMENT ON COLUMN "ANNOTATIONS"."ANNOTATION_ID" IS 'UUID';
COMMENT ON COLUMN "ANNOTATIONS"."LINKED_ANNOT_ID" IS 'ID of linked annotation';
COMMENT ON COLUMN "ANNOTATIONS"."TEXT" IS 'annotated text';
COMMENT ON COLUMN "ANNOTATIONS"."CREATED" IS 'date of creation of the annotation';
COMMENT ON COLUMN "ANNOTATIONS"."UPDATED" IS 'date of last update of the annotation';
COMMENT ON COLUMN "ANNOTATIONS"."USER_ID" IS 'user''s ID, see USERS table';
COMMENT ON COLUMN "ANNOTATIONS"."SHARED" IS 'flag indicating whether annotation is private or group-public';
COMMENT ON COLUMN "ANNOTATIONS"."TARGET_SELECTORS" IS 'serialized selectors; JSON';
COMMENT ON COLUMN "ANNOTATIONS"."METADATA_ID" IS 'ID of the related metadata set';
COMMENT ON COLUMN "ANNOTATIONS"."REFERENCES" IS 'List of parent annotations';
COMMENT ON COLUMN "ANNOTATIONS"."ROOT" IS 'ID of thread root (for replies)';
COMMENT ON COLUMN "ANNOTATIONS"."STATUS" IS 'Annotation status, e.g. normal/deleted/accepted/rejected';
COMMENT ON COLUMN "ANNOTATIONS"."STATUS_UPDATED" IS 'Timestamp of status change';
COMMENT ON COLUMN "ANNOTATIONS"."STATUS_UPDATED_BY" IS 'User id of user that changed status';
COMMENT ON COLUMN "ANNOTATIONS"."SENT_DELETED" IS 'Flag for pre-deleting annotation';
COMMENT ON COLUMN "ANNOTATIONS"."RESP_VERSION_SENT_DELETED" IS 'The ISC response version during which the annotation was sent-deleted';

CREATE INDEX IF NOT EXISTS "ANNOTATIONS_IX_USERS" ON "ANNOTATIONS" ("USER_ID");
CREATE INDEX IF NOT EXISTS "ANNOTATIONS_IX_METADATA" ON "ANNOTATIONS" ("METADATA_ID");
CREATE INDEX IF NOT EXISTS "ANNOTATIONS_IX_STATUS_ROOT" ON "ANNOTATIONS" ("STATUS" ASC, "ROOT" ASC);

 
------------------------------------
-- table TAGS
------------------------------------
CREATE SEQUENCE IF NOT EXISTS "TAGS_SEQ" MINVALUE 1 INCREMENT BY 1 START WITH 1;

CREATE TABLE IF NOT EXISTS TAGS (
  TAG_ID                           NUMBER default TAGS_SEQ.nextval PRIMARY KEY,
  ANNOTATION_ID                    VARCHAR2(22 CHAR) NOT NULL,
  NAME                             VARCHAR2(50 CHAR) NOT NULL,
  CONSTRAINT "TAGS_PK" PRIMARY KEY ("TAG_ID"), 
  CONSTRAINT "TAGS_UK_NAME_ANNOT" UNIQUE ("NAME", "ANNOTATION_ID"), 
  CONSTRAINT "TAGS_FK_ANNOTATION" FOREIGN KEY ("ANNOTATION_ID") REFERENCES "ANNOTATIONS" ("ANNOTATION_ID") ON DELETE CASCADE
);

COMMENT ON COLUMN "TAGS"."NAME" IS 'Tag';
COMMENT ON COLUMN "TAGS"."ANNOTATION_ID" IS 'Annotation to which the tag belongs to';
COMMENT ON COLUMN "TAGS"."TAG_ID" IS 'ID';

-- note: for Oracle, we have to create a trigger to update the TAG_ID using sequence; this is not needed for H2

CREATE ALIAS IF NOT EXISTS deAccent AS '
  String deAccent(String value) throws Exception{
      return value.toUpperCase();
  }
';


------------------------------------
-- table AUTHCLIENTS
------------------------------------
CREATE SEQUENCE IF NOT EXISTS "AUTHCLIENTS_SEQ" MINVALUE 1 INCREMENT BY 1 START WITH 1;

CREATE TABLE IF NOT EXISTS AUTHCLIENTS (
  DESCRIPTION                      VARCHAR2(50 CHAR) NOT NULL, 
  ID                               NUMBER DEFAULT AUTHCLIENTS_SEQ.nextval PRIMARY KEY, 
  SECRET                           VARCHAR2(80 CHAR) NOT NULL, 
  CLIENT_ID                        VARCHAR2(80 CHAR) NOT NULL, 
  AUTHORITIES                      VARCHAR2(80 CHAR), 
  CONSTRAINT "AUTHCLIENTS_PK" PRIMARY KEY ("ID"), 
  CONSTRAINT "AUTHCLIENTS_UK_SECRET" UNIQUE ("SECRET"), 
  CONSTRAINT "AUTHCLIENTS_UK_CLIENT_ID" UNIQUE ("CLIENT_ID")
);

COMMENT ON COLUMN "AUTHCLIENTS"."DESCRIPTION" IS 'Description for identitying the client';
COMMENT ON COLUMN "AUTHCLIENTS"."ID" IS 'internal ID';
COMMENT ON COLUMN "AUTHCLIENTS"."SECRET" IS 'Client''s secret key, used for decrypting tokens';
COMMENT ON COLUMN "AUTHCLIENTS"."CLIENT_ID" IS 'Client''s ID, used to identify its tokens in issuer field';
COMMENT ON COLUMN "AUTHCLIENTS"."AUTHORITIES" IS 'Authorities for which this client is allowed to authorize users; separated by semi-colons';

-- note: for Oracle, we have to create a trigger to update the ID using sequence; this is not needed for H2


------------------------------------
-- table TOKENS
------------------------------------
CREATE SEQUENCE IF NOT EXISTS "TOKENS_SEQ" MINVALUE 1 INCREMENT BY 1 START WITH 1;

CREATE TABLE IF NOT EXISTS TOKENS (
  ID                               NUMBER DEFAULT TOKENS_SEQ.nextval PRIMARY KEY, 
  USER_ID                          NUMBER NOT NULL, 
  ACCESS_TOKEN                     VARCHAR2(50 CHAR) NOT NULL, 
  ACCESS_TOKEN_EXPIRES             TIMESTAMP NOT NULL, 
  REFRESH_TOKEN                    VARCHAR2(50 CHAR) NOT NULL, 
  REFRESH_TOKEN_EXPIRES            TIMESTAMP NOT NULL, 
  AUTHORITY                        VARCHAR2(50 CHAR) NOT NULL,
  CONSTRAINT "TOKENS_PK" PRIMARY KEY ("ID"), 
  CONSTRAINT "TOKENS_UK_REFRESH_TOKEN" UNIQUE ("REFRESH_TOKEN"), 
  CONSTRAINT "TOKENS_UK_ACCESS_TOKEN" UNIQUE ("ACCESS_TOKEN"), 
  CONSTRAINT "TOKENS_FK_USERS" FOREIGN KEY ("USER_ID") REFERENCES "USERS" ("USER_ID") ON DELETE CASCADE
);

COMMENT ON COLUMN "TOKENS"."ID" IS 'Internal ID';
COMMENT ON COLUMN "TOKENS"."USER_ID" IS 'ID of the user owning the tokens';
COMMENT ON COLUMN "TOKENS"."ACCESS_TOKEN" IS 'Access token granted to the user';
COMMENT ON COLUMN "TOKENS"."ACCESS_TOKEN_EXPIRES" IS 'Expiration timestamp of access token';
COMMENT ON COLUMN "TOKENS"."REFRESH_TOKEN" IS 'Refresh token granted to the user';
COMMENT ON COLUMN "TOKENS"."REFRESH_TOKEN_EXPIRES" IS 'Expiration timestamp of the refresh token';
COMMENT ON COLUMN "TOKENS"."AUTHORITY" IS 'Authority for which the token is issued';

-- note: for Oracle, we have to create a trigger to update the ID using sequence; this is not needed for H2

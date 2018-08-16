====
    Copyright 2018 European Commission

    Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
    You may not use this work except in compliance with the Licence.
    You may obtain a copy of the Licence at:

        https://joinup.ec.europa.eu/software/page/eupl

    Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the Licence for the specific language governing permissions and limitations under the Licence.
====

ANNOTATE APPLICATION
--------------------------------------------------------------------------------
Annotate 
This application is divided in two parts 
1)  client
2)  server

CLIENT
--------------------------------------------------------------------------------
Client part is an angular application which runs in the scope of another webapp 
    to provide annotation functionality.


SERVER
--------------------------------------------------------------------------------
Server part is an API server which expose an API for client to consume and also 
    serves static(client) files.


DEVELOPMENT OF SERVER
--------------------------------------------------------------------------------
Prerequisite
1) Maven 3.3.9+
2) JDK 1.8_0_151+
3) Maven should have internet connectivity. You may have to add proxy in maven 
    settings.xml and {annotate}\client\.npmrc
4) TOMCAT 8.5.20 or weblogic 12.1.3
5) If Eclipse is used as development IDE, Oxygen release is recommended as it 
    supports the Tomcat server version mentioned.


COMPILING
--------------------------------------------------------------------------------
1) UNZIP the Zip in folder henceforth referred as {annotate}

2) Check or modify the database that should be used by the server application.
   To do so, set the according profile in file 
   {annotate}\server\src\main\resources\application.properties file:
   - if you want to use H2 in-memory database, set
        spring.profiles.active=h2
   - if you want to use Oracle database, set
        spring.profiles.active=oracle

3) To compile for environment {env} (possible values-dev/local), run the following 
   command in {annotate}
	$ mvn clean install -Denv={env}
   For example
	$ mvn clean install -Denv=local
	It injects appropriate values from property files.

4) Now, on successful finish, WAR file would be visible in {annotate}/server/target
    folder.

5) This WAR file can be deployed in TOMCAT, Weblogic or any other application 
    server.


DB SETUP
--------------------------------------------------------------------------------
A) For H2
    If you want to use H2 in-memory database, there is nothing more to do here.
    The H2 database is initialized automatically during deployment step using 
    the schema script file located at 
        {annotate}/server/src/main/resources/schema-h2.sql
    and filled with required data from the 
        {annotate}/server/src/main/resources/data-h2.sql

B) For Oracle
    If you want to use Oracle database, however, the database schema needs to be 
    created initially. Appropriate scripts for the creation of all required elements
    like tables, triggers, sequences, ... are found at 
        {annotate}/server/src/scripts/db/consolidated/schema-oracle.sql
    1) Log in to your database and run these scripts, making sure you have  
        appropriate permissions to create these objects (CREATE SEQUENCE,  
        CREATE TABLE and CREATE TRIGGER should be sufficient).
    2) Next, some initial configuration needs to be inserted into the database. 
        This data is contained in a script that can be found at
         {annotate}/server/src/scripts/db/consolidated/data-oracle.sql
    3) Log in to your database and run this script.


DEPLOYMENT
--------------------------------------------------------------------------------
A) For TOMCAT
    1) Create data source jdbc/leosDB for DB connections in context.xml.
    2) Deploy the WAR server-{env}.war in TOMCAT with context root at '/annotate' 
	    with port 9099
    3) Welcome page will be available at http://localhost:9099/annotate/app.html


B) For WEBLOGIC
    1) Create data source jdbc/leosDB for DB connections
    2) Deploy the WAR server-{env}.war in Weblogic at port 9099
    3) Welcome page will be available at http://localhost:9099/annotate/app.html


NOTES
--------------------------------------------------------------------------------
1) The server can work with Http/https both.
2) Configurable properties are present at following places
	{annotate}/server/src/main/filters/common.properties
	{annotate}/server/src/main/filters/{env}.properties
	{annotate}/client/scripts/config.js
	Above-mentioned port 9099 might have to be configured in the 'config.js' 
	 property file.
3) When using Oracle, you may need to install oracle driver in MAVEN as this is 
	not open source. 
4) When using Oracle, an entry for the default group needs to be created in the 
    GROUPS table. The "Name" column of this group needs to coincide to the corres-
    ponding property in common.properties file (defaultgroup.name).

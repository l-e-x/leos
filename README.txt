PROJECT: LEOS
Joinup Release: 2.0.0-SNAPSHOT
Date: 2018-08-16

INTRODUCTION
============

This is a joinup release of Project LEOS (pilot) which enables users to draft and edit legal texts in AkomaNtoso XML format.


IMPORTANT NOTES
===============

This release is intended to provide an experience with the software and is stripped of several important components to enable ease of use.
    * This software is adapted to run on a local server for demo purposes and without proper security mechanisms.
    * This software doesn't provide any persistence mechanism to save documents so it should not be used to do any actual work.
    * This software is still under active development so some features may be added, removed or changed over course of time.


DEPENDENCIES
============

To compile the supplied source files and run the generated WAR the following software should be configured:
    * Java SDK version 8.0
    * Maven version 3.3.9+
      (Maven runtime memory might need to be set: MAVEN_OPTS=-Xms256m -Xmx512m)
      (Maven settings, proxy and mirrors, might need to be adjusted to your environment and internet access requirements)
    * Supported browser is Google Chrome version 45+
      (Mozilla Firefox ESR version 38.3 and Microsoft Internet Explorer version 11 are known to work with minor issues)

DEMO
====

You can experience LEOS on your local machine by executing a few steps. You have two options available:
    * Option A: for Windows as operating system.
    * Option B: for other operating systems. This option might also be preferable if you experience connections reset failures when
      maven dependencies are being downloaded.

For both options if you are behind a proxy it is needed to add your proxy settings in file {LEOS}\modules\annotate\client\.npmrc
ie: proxy=http://user:password@proxy-server:port

OPTION A)
If your machine's operating system is Microsoft Windows, you can simply execute the provided script: run-all.bat.
This script will execute individual scripts that will compile AND run each of the required software components (user database,
document repository, annotation service and LEOS).

Open the browser and navigate to the LEOS web interface available at the following URL:

    http://localhost:8080/leos-pilot/ui

LEOS is pre-configured with these demo users:

    +-----------+-------+----------+--------+
    | NAME      | LOGIN | PASSWORD |ROLE    |
    +-----------+-------+----------+--------+
    | Demo User | demo  | demo     |Normal  |
    +-----------+-------+----------+--------+
    | John Doe  | john  | demo     |Normal  |
    +-----------+-------+----------+--------+
    | Jane Doe  | jane  | demo     |Support |
    +-----------+-------+----------+--------+
    | S Leo     | leos  | demo     |Support |
    +-----------+-------+----------+--------+

OPTION B) 
If your machine's operating system is not windows or want to run components one by one, you should follow below steps.

B1. UNZIP ARCHIVE
================

You must unzip the distribution archive.

    1) Unzip the distribution archive in the local file system
        a) A new directory should now be present: {LEOS}


B2. COMPILING SOURCES
=====================

You must compile the sources on the command line.

    1) To compile the repository:
        a) Traverse to folder {LEOS}\tools\cmis\chemistry-opencmis-server-inmemory
        b) Execute the following command:
            mvn clean install
            
    2) To compile the user database:
        a) Traverse to folder {LEOS}\tools\user-repo
        b) Execute the following command:
            mvn clean install
            
    3) To compile annotate:
        a) Traverse to folder {LEOS}\modules\annotate
        b) Execute the following command:
            mvn clean install
    
    4) To compile LEOS:
        a) Traverse to folder {LEOS}
        b) Execute the following command:
            mvn clean install

B3. RUNNING USER DATABASE
=========================

You must run the user database on the command line.

    1) Traverse to folder {LEOS}\tools\user-repo
    2) Execute the following command:
            mvn spring-boot:run -Drun.profiles=h2        


B4. RUNNING REPOSITORY
======================

You must run the repository on the command line.

    1) Traverse to folder {LEOS}\tools\cmis\chemistry-opencmis-server-inmemory
    2) Execute the following command:
            mvn jetty:run-war


B5. RUNNING ANNOTATE
====================

You must run annotate on the command line.

    1) Traverse to folder {LEOS}\modules\annotate\server
    2) Execute the following command:
            mvn spring-boot:run -Dspring.profiles.active=h2

Once you'll run LEOS, annotate sidebar will be available to annotate documents.
For more details about DB and running this module, check {LEOS}\modules\annotate\README.txt file.

B6. RUNNING LEOS
================

Note: database and repository must already be running.

You must run LEOS on the command line.

    1) Traverse to folder {LEOS}\modules\web
    2) Execute the following command:
            mvn jetty:run-war

Open the browser and navigate to the LEOS web interface available at the following URL:

    http://localhost:8080/leos-pilot/ui

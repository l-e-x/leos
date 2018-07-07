PROJECT: LEOS
Joinup Release: 3.1.0-SNAPSHOT
Date: 2016-09-07

INTRODUCTION
============

This is a joinup release of Project LEOS (prototype) which enables users to edit legal texts in AkomaNtoso XML format.


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
    * Maven version 3.0.5+
      (Maven runtime memory might need to be set: MAVEN_OPTS=-Xms256m -Xmx512m)
      (Maven settings, proxy and mirrors, might need to be adjusted to your environment and internet access requirements)
    * Supported browser is Google Chrome version 45+
      (Mozilla Firefox ESR version 38.3 and Microsoft Internet Explorer version 11 are known to work with minor issues)


DEMO
====

You can experience LEOS on your local machine by executing a few steps in order, following the instructions below.

First, you must compile the sources once. Second, you must run three software components at the same time.

If your machine's operating system is Microsoft Windows, you can simply execute the provided script: run-all.bat.
This script will execute individual scripts that will compile AND run each of the required software components.

Open the browser and navigate to the LEOS web interface available at the following URL:

    http://localhost:8080/leos-prototype/ui

LEOS is pre-configured with these demo users:

    +-----------+-------+----------+
    | NAME      | LOGIN | PASSWORD |
    +-----------+-------+----------+
    | Demo User | demo  | demo     |
    +-----------+-------+----------+
    | John Doe  | john  | demo     |
    +-----------+-------+----------+
    | Jane Doe  | jane  | demo     |
    +-----------+-------+----------+


UNZIP ARCHIVE
=============

You must unzip the distribution archive.

    1) Unzip the distribution archive in the local file system
        a) A new directory should now be present: {LEOS}


COMPILING SOURCES
=================

You must compile the sources on the command line.

    1) To compile the repository:
        a) Traverse to folder {LEOS}\tools\cmis\chemistry-opencmis-server-inmemory
        b) Execute the following command:
            mvn clean install

    2) To compile LEOS:
        a) Traverse to folder {LEOS}
        b) Execute the following command:
            mvn clean install


RUNNING DATABASE
================

You must run the database on the command line.

    1) Traverse to folder {LEOS}\tools\database
    2) Execute the following command:
            mvn inmemdb:run


RUNNING REPOSITORY
==================

You must run the repository on the command line.

    1) Traverse to folder {LEOS}\tools\cmis\chemistry-opencmis-server-inmemory
    2) Execute the following command:
            mvn jetty:run-war


RUNNING LEOS
============

Note: database and repository must already be running.

You must run LEOS on the command line.

    1) Traverse to folder {LEOS}\modules\web-os
    2) Execute the following command:
            mvn jetty:run-war


BUGS
====

There might be bugs or incomplete features present in this version as it is a prototype under active development.
    * Suggestions on document title cannot be accepted/deleted.
    * Suggestions/Comments on article heading are lost on save.

CHANGE LOG
==========

3.1.0 (Prototype)
-----------------
    * Comments Viewer
       - Feature to suggest a change in the text
    * Document Viewer
       - Accept or delete suggested changes
       - Comments deletion with button
    * Updated dependencies (frameworks and libraries)
    * Technical improvements and bug fixes

3.0.0 (Prototype)
-----------------
    * Updated HOWTO documentation
    * Document Viewer
        - Document changes pane
          - Version selector
          - Change markers
          - Change navigation
    * Editor
        - New inline edition
        - Improved user comments
    * Updated dependencies (frameworks and libraries)
    * Technical improvements, updated module structure and bug fixes

2.0.0 (Prototype)
-----------------
    * New HOWTO documentation
    * New user interface theme
        - Colors
        - Fonts
        - Icons
    * Repository Browser
        - Redesigned user interface
        - Document list filters
        - Deletion of documents
        - Management of contributors
    * LEOS Editor
        - User comments
        - Text highlights
        - Cross-references
        - Sub-paragraphs
    * Comments viewer
        - View, add, edit and delete comments
    * Collaborative work
        - Document author (Role)
        - Document contributor (Role)
        - Document stages (Workflow)
    * Updated dependencies (frameworks and libraries)
    * Fixes (incomplete) for Firefox (ESR 38.3) and Internet Explorer (11)

1.0.0 (Prototype)
-----------------
    * Initial joinup open source release.
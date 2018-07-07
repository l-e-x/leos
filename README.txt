PROJECT: LEOS
Joinup Release: 1.0.0-SNAPSHOT
Date: 2015-06-22

INTRODUCTION
============

This is a joinup release of Project LEOS(prototype) which enables users to edit legal text in AkomaNtoso XML format.

IMPORTANT NOTES
===============

This release is intended to provide an experience with the software and is stripped of several important components to enable ease of use.
* This software is adapted to run on a local server for demo purposes and without any security mechanism.
* This software doesn't provide any persistence mechanism to save work so it should not be used to do any actual work.
* This software is still under active development so some features may be added, removed or change over course of time.
 
DEPENDENCIES
============

To compile the supplied source files and run the generated WAR the following software should be configured:
* JDK version 7+
* Maven version 3.0.5+
* Supported browser is Google Chrome version 41+

COMPILING AND RUNNING
=====================

1) Unzip the project archive on the local file system
    a) A new directory should now be present: {LEOS}

2) To compile and run the CMIS server on the command line:
    a) Traverse to Folder {LEOS}\tools\cmis\chemistry-opencmis-server-inmemory
    b) Execute the following command:
            mvn clean install jetty:run-war

3) To compile and run the LEOS server on the command line:
    a) Traverse to folder {LEOS}
    b) Execute the following command:
            mvn clean install
    c) Traverse to folder {LEOS}\modules\web
    d) Execute the following command:
            mvn jetty:run-war

4) CMIS and LEOS servers should now be running
    a) Open the browser and navigate to the LEOS web interface available at the following URL:
            http://localhost:8080/leos-prototype

BUGS
====

There might be bugs or incomplete features present in this version as it is a prototype under active development.
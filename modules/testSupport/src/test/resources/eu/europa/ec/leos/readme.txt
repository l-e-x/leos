--To unit test the DB functionality using the in-memory database please follow the steps below--

1.Run the maven build with the new leos-web/pom.xml to add the dependencies for spring-test, hsqldb and junit test.

-- TESTS Using HSQLDB --
2. update test-jdbc.properties with the HSQLDB driver connection settings.
3. update leos-web/pom.xml with sql-maven-plugin settings (uncomment if commented)
4. run mvn test on command line
5. an in-memory database will be created and executed the test successfully and rollback afterwards

--TESTS using Oracle DB
2. update test-jdbc.properties with the ORACLE driver connection settings.
3. comment out the sql-maven-plugin settings in leos-web/pom.xml (comment if commented)
4. run mvn test on command line
5. Test should be run successfully.
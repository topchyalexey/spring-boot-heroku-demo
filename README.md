# QR Docs

[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)


[![Deploy to Heroku](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

## License

Code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0.txt).

## Run locally 

 'export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=128M -Dserver.port=8090 -Dspring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver"'
 'mvn clean install  spring-boot:run -DJDBC_DATABASE_URL=jdbc:hsqldb:mem:test;DB_CLOSE_DELAY=-1 '

# QR Docs

[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)


[![Deploy to Heroku](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)

## License

Code is under the [Apache Licence v2](https://www.apache.org/licenses/LICENSE-2.0.txt).

## Run locally 

 `export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=128M -Dserver.port=8090 -Dspring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver"`
 
 `mvn clean install  spring-boot:run -DJDBC_DATABASE_URL=jdbc:hsqldb:mem:test;DB_CLOSE_DELAY=-1`
 
### Switch java version


`sudo update-java-alternatives -s java-8-oracle`  
`sudo apt-get install oracle-java8-set-default`
`sudo update-alternatives --config java`

`nano /etc/environment`
```
	PATH="/usr/lib/jvm/java-8-oracle/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games"
	JAVA_HOME="/usr/lib/jvm/java-8-oracle"
```

`source /etc/environment`
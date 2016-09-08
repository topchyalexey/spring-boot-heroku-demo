package ru.loletop.qrdocs;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

//@Configuration
public class DatabaseConfig {
    //@Bean
    //@Primary
    //@ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return null; // new org.apache.tomcat.jdbc.pool.DataSource();
    }
} 
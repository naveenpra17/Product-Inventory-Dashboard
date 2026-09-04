package com.inventory.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Configuration
@Profile("prod")
public class RenderDatabaseConfig {

    @Bean
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            throw new IllegalStateException(
                    "DATABASE_URL is required when running with the 'prod' profile (e.g. on Render).");
        }

        log.info("Configuring PostgreSQL datasource from DATABASE_URL");
        return new HikariDataSource(buildConfig(databaseUrl));
    }

    private HikariConfig buildConfig(String databaseUrl) {
        try {
            URI uri = new URI(databaseUrl.replace("postgres://", "postgresql://"));

            String username = uri.getUserInfo() != null
                    ? URLDecoder.decode(uri.getUserInfo().split(":")[0], StandardCharsets.UTF_8)
                    : "";
            String password = uri.getUserInfo() != null && uri.getUserInfo().contains(":")
                    ? URLDecoder.decode(uri.getUserInfo().split(":", 2)[1], StandardCharsets.UTF_8)
                    : "";
            String jdbcUrl = String.format(
                    "jdbc:postgresql://%s:%d%s%s",
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery() != null ? "?" + uri.getQuery() : ""
            );

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(5);
            return config;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to parse DATABASE_URL for PostgreSQL", ex);
        }
    }
}

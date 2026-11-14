package com.bdquickmart.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Wires a single PostgreSQL connection URI ({@code SUPABASE_DATABASE_URL}, or
 * the generic alias {@code DATABASE_URL}) into Spring Boot's DataSource.
 *
 * <p>Spring Boot does not parse such a URI on its own: it needs
 * {@code spring.datasource.url}/{@code username}/{@code password} separately,
 * and the Supabase Session Pooler URI embeds a (possibly URL-encoded) password
 * in the user-info component. This post-processor parses the URI once, early in
 * startup, and injects the derived properties at the highest precedence so the
 * operator only has to supply one secret environment variable.
 *
 * <p>Precedence rules:
 * <ul>
 *   <li>If the resolved {@code spring.datasource.url} is an H2 in-memory URL
 *       (the test profile), nothing is changed — tests keep their isolated DB.</li>
 *   <li>If {@code SUPABASE_DATABASE_URL} is set, it is authoritative. A stale
 *       or accidental {@code DB_URL} is overridden, which also neutralises any
 *       leftover MySQL-era variables in a local {@code .env}.</li>
 *   <li>Otherwise, if only {@code DATABASE_URL} is set and it looks like a
 *       PostgreSQL URI, it is used.</li>
 *   <li>If neither is set, this post-processor does nothing and the existing
 *       {@code DB_URL}/{@code DB_USERNAME}/{@code DB_PASSWORD} configuration in
 *       {@code application.yml} applies unchanged (e.g. Render split secrets).</li>
 * </ul>
 *
 * <p>No credential is ever logged: the returned JDBC URL contains no user or
 * password, and the password is only ever placed into the in-memory property
 * source consumed by Hikari.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    /** Runs after {@code ConfigDataEnvironmentPostProcessor} (which loads application*.yml). */
    public static final int ORDER = Ordered.LOWEST_PRECEDENCE - 10;

    private static final String PROPERTY_SOURCE_NAME = "bdqmDatabaseUrl";

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
                                       SpringApplication application) {
        // Idempotent: never add the source twice.
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        // Never interfere with the in-memory test / integration database.
        String existingUrl = environment.getProperty("spring.datasource.url", "");
        if (existingUrl.startsWith("jdbc:h2:")) {
            return;
        }

        String supabase = environment.getProperty("SUPABASE_DATABASE_URL");
        String generic = environment.getProperty("DATABASE_URL");

        String uri = null;
        boolean authoritative = false;
        if (supabase != null && !supabase.isBlank()) {
            uri = supabase.trim();
            authoritative = true; // SUPABASE_DATABASE_URL always wins over split vars
        } else if (generic != null && SupabaseDatabaseUrl.looksLikePostgresUri(generic)) {
            uri = generic.trim();
        }

        if (uri == null) {
            return;
        }

        // A malformed SUPABASE_DATABASE_URL is a hard configuration error: fail
        // fast with a clear message rather than silently falling back to a
        // localhost default that would never work in production.
        final SupabaseDatabaseUrl.Parts parts;
        try {
            parts = SupabaseDatabaseUrl.parse(uri);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Invalid database connection URI: " + ex.getMessage(), ex);
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", parts.jdbcUrl());
        props.put("spring.datasource.username", parts.username());
        props.put("spring.datasource.password", parts.password());
        props.put("spring.datasource.driver-class-name", parts.driverClassName());
        // When a Supabase URI is authoritative we KNOW the dialect is
        // PostgreSQL; pin it so a stale DB_DIALECT cannot regress the app and
        // so Hibernate need not read DB metadata during startup.
        if (authoritative) {
            props.put("spring.jpa.properties.hibernate.dialect", SupabaseDatabaseUrl.POSTGRES_DIALECT);
        }

        environment.getPropertySources().addFirst(
                new MapPropertySource(PROPERTY_SOURCE_NAME, props));
    }
}

package com.bdquickmart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bdquickmart.config.DatabaseUrlEnvironmentPostProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * Unit tests for {@link DatabaseUrlEnvironmentPostProcessor}. No Spring
 * context, no database, no Docker and NO real credentials — all URIs use fake
 * placeholder values. Verifies that a single connection-URI env var is wired
 * into the DataSource correctly and that the processor never interferes with
 * the in-memory test database or an explicit split configuration.
 */
@DisplayName("Database URI EnvironmentPostProcessor")
class DatabaseUrlEnvironmentPostProcessorTest {

    private static final String PG_URI =
            "postgresql://postgres.fakeref:FakePass123@aws-0-ap-south-1.pooler.supabase.com:5432/postgres";

    private final DatabaseUrlEnvironmentPostProcessor processor =
            new DatabaseUrlEnvironmentPostProcessor();

    @Test
    @DisplayName("SUPABASE_DATABASE_URL drives url/username/password/driver/dialect")
    void supabaseUriIsAuthoritative() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("SUPABASE_DATABASE_URL", PG_URI);
        // A stale/accidental DB_URL must be overridden by the authoritative URI.
        env.setProperty("DB_URL", "jdbc:mysql://stale:3306/legacy");
        env.setProperty("DB_DIALECT", "org.hibernate.dialect.MySQLDialect");

        processor.postProcessEnvironment(env, null);

        assertEquals("jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require",
                env.getProperty("spring.datasource.url"));
        assertEquals("postgres.fakeref", env.getProperty("spring.datasource.username"));
        assertEquals("FakePass123", env.getProperty("spring.datasource.password"));
        assertEquals("org.postgresql.Driver", env.getProperty("spring.datasource.driver-class-name"));
        assertEquals("org.hibernate.dialect.PostgreSQLDialect",
                env.getProperty("spring.jpa.properties.hibernate.dialect"));
    }

    @Test
    @DisplayName("generic DATABASE_URL is used only when it is a PostgreSQL URI")
    void genericDatabaseUrlAlias() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("DATABASE_URL", PG_URI);
        processor.postProcessEnvironment(env, null);
        assertEquals("jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require",
                env.getProperty("spring.datasource.url"));

        MockEnvironment mysql = new MockEnvironment();
        mysql.setProperty("DATABASE_URL", "mysql://u:p@h/db");
        processor.postProcessEnvironment(mysql, null);
        assertNull(mysql.getProperty("spring.datasource.url"),
                "a non-Postgres DATABASE_URL must be ignored");
    }

    @Test
    @DisplayName("H2 test datasource is never touched")
    void h2TestDatasourceIsUntouched() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.url", "jdbc:h2:mem:bdqm;MODE=PostgreSQL");
        env.setProperty("SUPABASE_DATABASE_URL", PG_URI);
        processor.postProcessEnvironment(env, null);
        assertEquals("jdbc:h2:mem:bdqm;MODE=PostgreSQL", env.getProperty("spring.datasource.url"));
        assertNull(env.getProperty("spring.datasource.password"));
    }

    @Test
    @DisplayName("no connection URI leaves existing DB_* configuration intact")
    void noUriDoesNothing() {
        MockEnvironment env = new MockEnvironment();
        processor.postProcessEnvironment(env, null);
        assertNull(env.getProperty("spring.datasource.url"));
    }

    @Test
    @DisplayName("processing twice is idempotent")
    void idempotent() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("SUPABASE_DATABASE_URL", PG_URI);
        processor.postProcessEnvironment(env, null);
        processor.postProcessEnvironment(env, null);
        assertEquals("jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require",
                env.getProperty("spring.datasource.url"));
    }
}

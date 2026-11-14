package com.bdquickmart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bdquickmart.config.SupabaseDatabaseUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link SupabaseDatabaseUrl}. No Spring context, no
 * database, no Docker and NO real credentials — every value here is a fake
 * placeholder. Verifies the URI-parsing contract that lets the app be driven by
 * a single {@code SUPABASE_DATABASE_URL} secret.
 */
@DisplayName("Supabase / PostgreSQL connection URI parsing")
class SupabaseDatabaseUrlTest {

    private static final String BASE =
            "postgresql://postgres.abcdefgh:FAKEPASS@aws-0-ap-south-1.pooler.supabase.com:6543/postgres";

    @Test
    @DisplayName("looksLikePostgresUri recognises all supported forms")
    void recognisesSupportedSchemes() {
        assertTrue(SupabaseDatabaseUrl.looksLikePostgresUri("postgresql://u:p@h/db"));
        assertTrue(SupabaseDatabaseUrl.looksLikePostgresUri("postgres://u:p@h/db"));
        assertTrue(SupabaseDatabaseUrl.looksLikePostgresUri("jdbc:postgresql://u:p@h/db"));
        assertTrue(SupabaseDatabaseUrl.looksLikePostgresUri(" JDBC:POSTGRESQL://u:p@h/db "));
        assertFalse(SupabaseDatabaseUrl.looksLikePostgresUri("jdbc:mysql://h/db"));
        assertFalse(SupabaseDatabaseUrl.looksLikePostgresUri("mysql://h/db"));
        assertFalse(SupabaseDatabaseUrl.looksLikePostgresUri(""));
        assertFalse(SupabaseDatabaseUrl.looksLikePostgresUri(null));
    }

    @Test
    @DisplayName("splits URI into JDBC URL + separate credentials, with TLS enforced")
    void parsesStandardUri() {
        SupabaseDatabaseUrl.Parts p = SupabaseDatabaseUrl.parse(BASE);

        assertEquals("jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require",
                p.jdbcUrl());
        assertEquals("postgres.abcdefgh", p.username());
        assertEquals("FAKEPASS", p.password());
        assertEquals("org.postgresql.Driver", p.driverClassName());
        // Credentials must NOT be embedded in the URL (it is safe to log).
        assertFalse(p.jdbcUrl().contains("FAKEPASS"));
        assertFalse(p.jdbcUrl().contains("@"));
    }

    @Test
    @DisplayName("URL-encoded password (and UTF-8) is percent-decoded; '+' stays literal")
    void decodesUrlEncodedPassword() {
        // p@ss/w:ord  ->  p%40ss%2Fw%3Aord ; a literal '+' must not become a space
        String uri = "postgresql://appuser:p%40ss%2Fw%3Aord%2Bx@db.example.com:5432/mydb";
        SupabaseDatabaseUrl.Parts p = SupabaseDatabaseUrl.parse(uri);
        assertEquals("p@ss/w:ord+x", p.password());
        assertEquals("appuser", p.username());

        // Bangla UTF-8 percent-encoded password
        String bn = "postgresql://u:%E0%A6%AC%E0%A6%BE%E0%A6%82%E0%A6%B2%E0%A6%BE@h:5432/db";
        assertEquals("বাংলা", SupabaseDatabaseUrl.parse(bn).password());
    }

    @Test
    @DisplayName("existing sslmode and other query params are preserved, not duplicated")
    void preservesQueryParams() {
        String uri = BASE.replace(":6543/postgres", ":5432/postgres")
                + "?sslmode=verify-full&ApplicationName=bdqm&prepareThreshold=0";
        SupabaseDatabaseUrl.Parts p = SupabaseDatabaseUrl.parse(uri);
        assertTrue(p.jdbcUrl().contains("sslmode=verify-full"));
        assertFalse(p.jdbcUrl().contains("sslmode=require"), "must not downgrade/duplicate sslmode");
        assertTrue(p.jdbcUrl().contains("ApplicationName=bdqm"));
        assertTrue(p.jdbcUrl().contains("prepareThreshold=0"));
    }

    @Test
    @DisplayName("jdbc: prefix and default port are handled")
    void handlesJdbcPrefixAndDefaultPort() {
        SupabaseDatabaseUrl.Parts p = SupabaseDatabaseUrl.parse(
                "jdbc:postgresql://u:p@db.example.com/postgres");
        assertTrue(p.jdbcUrl().startsWith("jdbc:postgresql://db.example.com:5432/postgres"));
        assertEquals("u", p.username());
        assertEquals("p", p.password());
    }

    @Test
    @DisplayName("credentials supplied via query params are consumed, not leaked into URL")
    void readsCredentialsFromQueryParams() {
        SupabaseDatabaseUrl.Parts p = SupabaseDatabaseUrl.parse(
                "postgresql://db.example.com:5432/mydb?user=appuser&password=s%33cret");
        assertEquals("appuser", p.username());
        assertEquals("s3cret", p.password());
        assertFalse(p.jdbcUrl().contains("password"));
        assertFalse(p.jdbcUrl().contains("user="));
        assertTrue(p.jdbcUrl().contains("sslmode=require"));
    }

    @Test
    @DisplayName("blank, malformed and non-Postgres URIs fail fast")
    void rejectsInvalidUris() {
        assertThrows(IllegalArgumentException.class, () -> SupabaseDatabaseUrl.parse(""));
        assertThrows(IllegalArgumentException.class, () -> SupabaseDatabaseUrl.parse("   "));
        assertThrows(IllegalArgumentException.class, () -> SupabaseDatabaseUrl.parse("mysql://u:p@h/db"));
        assertThrows(IllegalArgumentException.class,
                () -> SupabaseDatabaseUrl.parse("postgresql://u:p@/db"));
        assertThrows(IllegalArgumentException.class,
                () -> SupabaseDatabaseUrl.parse("postgresql://u:p@host"));
        // No user-info at all -> no username -> refuse to silently use an
        // empty credential.
        assertThrows(IllegalArgumentException.class,
                () -> SupabaseDatabaseUrl.parse("postgresql://host:5432/db"));

        // A user-info with a username but no password is valid (password may be
        // supplied out-of-band); it must NOT be rejected.
        SupabaseDatabaseUrl.Parts noPw = SupabaseDatabaseUrl.parse(
                "postgresql://onlyuser@host:5432/db");
        assertEquals("onlyuser", noPw.username());
        assertEquals("", noPw.password());
    }
}

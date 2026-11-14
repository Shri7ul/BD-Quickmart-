package com.bdquickmart.config;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses a single PostgreSQL connection URI (the shape supplied by Supabase,
 * Render, Heroku, etc.) into the values Spring's {@code DataSource} needs: a
 * JDBC URL, a username and a password.
 *
 * <p>This exists so the application can be configured with ONE secret
 * environment variable ({@code SUPABASE_DATABASE_URL}, or its generic alias
 * {@code DATABASE_URL}) instead of asking a human to manually split the host,
 * port, database, username and password apart. The user-info component is
 * percent-decoded, so URL-encoded passwords (e.g. {@code %40} for {@code @},
 * {@code %2F} for {@code /}) are handled correctly.
 *
 * <p>Accepted forms:
 * <pre>
 *   postgresql://user:password@host:port/db?sslmode=require
 *   postgres://user:password@host:port/db
 *   jdbc:postgresql://user:password@host:port/db?sslmode=require
 * </pre>
 *
 * <p>Query parameters are preserved, {@code sslmode=require} is enforced when
 * absent (Supabase mandates TLS), and credentials are deliberately kept OUT of
 * the JDBC URL — they are returned separately so Hikari receives them as
 * {@code username}/{@code password} and the URL can be logged safely.
 */
public final class SupabaseDatabaseUrl {

    /** Driver class name applied when a connection URI drives the config. */
    public static final String DRIVER_CLASS_NAME = "org.postgresql.Driver";
    public static final String POSTGRES_DIALECT = "org.hibernate.dialect.PostgreSQLDialect";

    private static final String DEFAULT_PORT = "5432";
    private static final String SSL_PARAM = "sslmode";

    private SupabaseDatabaseUrl() {
    }

    /** Immutable result of parsing a connection URI. */
    public record Parts(String jdbcUrl, String username, String password, String driverClassName) {
    }

    /**
     * @return true when {@code raw} looks like a PostgreSQL connection URI.
     */
    public static boolean looksLikePostgresUri(String raw) {
        if (raw == null) {
            return false;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        return s.startsWith("postgresql://")
                || s.startsWith("postgres://")
                || s.startsWith("jdbc:postgresql://")
                || s.startsWith("jdbc:postgres://");
    }

    /**
     * Parse a PostgreSQL connection URI.
     *
     * @throws IllegalArgumentException when the URI is missing a host/database,
     *                                  has an unsupported scheme, or contains a
     *                                  malformed percent-escape.
     */
    public static Parts parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Database connection URI is blank");
        }
        String value = raw.trim();

        // Strip an optional jdbc: prefix so java.net.URI can parse the rest.
        String forUri = value;
        if (forUri.regionMatches(true, 0, "jdbc:", 0, 5)) {
            forUri = forUri.substring(5);
        }

        final URI uri;
        try {
            uri = URI.create(forUri);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Malformed database connection URI", ex);
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"postgresql".equals(scheme) && !"postgres".equals(scheme)) {
            throw new IllegalArgumentException(
                    "Unsupported database URI scheme '" + scheme + "' (expected postgresql/postgres)");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Database connection URI has no host");
        }
        int port = uri.getPort() > 0 ? uri.getPort() : Integer.parseInt(DEFAULT_PORT);
        String database = uri.getPath();
        if (database == null || database.isBlank() || "/".equals(database)) {
            throw new IllegalArgumentException("Database connection URI has no database name");
        }
        if (!database.startsWith("/")) {
            database = "/" + database;
        }

        // --- Credentials: prefer user-info, fall back to query parameters ----
        String user = null;
        String password = null;
        String rawUserInfo = uri.getRawUserInfo();
        if (rawUserInfo != null && !rawUserInfo.isBlank()) {
            int colon = rawUserInfo.indexOf(':');
            if (colon < 0) {
                user = percentDecode(rawUserInfo);
                password = "";
            } else {
                user = percentDecode(rawUserInfo.substring(0, colon));
                password = percentDecode(rawUserInfo.substring(colon + 1));
            }
        }

        // --- Query parameters: preserve, strip creds, enforce TLS -----------
        List<String> params = new ArrayList<>();
        boolean hasSslMode = false;
        String rawQuery = uri.getRawQuery();
        if (rawQuery != null && !rawQuery.isBlank()) {
            for (String pair : rawQuery.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }
                int eq = pair.indexOf('=');
                String key = eq < 0 ? pair : pair.substring(0, eq);
                String keyLower = key.toLowerCase(Locale.ROOT);
                if ("user".equals(keyLower) || "password".equals(keyLower)) {
                    // Credentials from query params are consumed below, not kept
                    // in the URL (they are returned as separate fields).
                    String v = eq < 0 ? "" : percentDecode(pair.substring(eq + 1));
                    if (user == null && "user".equals(keyLower)) {
                        user = v;
                    } else if (password == null && "password".equals(keyLower)) {
                        password = v;
                    }
                    continue;
                }
                if (SSL_PARAM.equals(keyLower)) {
                    hasSslMode = true;
                }
                params.add(pair);
            }
        }
        if (!hasSslMode) {
            params.add("sslmode=require");
        }

        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("Database connection URI has no username");
        }
        if (password == null) {
            password = "";
        }

        StringBuilder jdbc = new StringBuilder();
        jdbc.append("jdbc:postgresql://").append(host).append(':').append(port).append(database);
        if (!params.isEmpty()) {
            jdbc.append('?').append(String.join("&", params));
        }

        return new Parts(jdbc.toString(), user, password, DRIVER_CLASS_NAME);
    }

    /**
     * Percent-decodes {@code %XX} escapes as UTF-8 bytes while leaving every
     * other character (including a literal {@code +}) untouched. Unlike
     * {@link java.net.URLDecoder}, a {@code +} is NOT treated as a space — that
     * is the correct behaviour for a URI authority component.
     */
    private static String percentDecode(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());
        for (int i = 0; i < s.length(); ) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi >= 0 && lo >= 0) {
                    out.write((hi << 4) | lo);
                    i += 3;
                    continue;
                }
            }
            byte[] bytes = String.valueOf(c).getBytes(StandardCharsets.UTF_8);
            out.write(bytes, 0, bytes.length);
            i++;
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}

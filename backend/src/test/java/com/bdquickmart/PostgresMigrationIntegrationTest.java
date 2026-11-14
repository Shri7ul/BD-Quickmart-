package com.bdquickmart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bdquickmart.modules.auth.RefreshTokenRepository;
import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserRepository;
import com.bdquickmart.modules.user.UserRole;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * REAL PostgreSQL validation of the fresh Flyway schema. These tests apply the
 * actual {@code V1__init_auth_schema.sql} to a brand-new PostgreSQL container
 * and start the full Spring context against it, then exercise the behaviours
 * that H2 cannot faithfully reproduce: TIMESTAMPTZ/UTC, functional lower()
 * unique indexes, partial indexes, CHECK/FK constraints, and SELECT ... FOR
 * UPDATE serialisation used by refresh-token rotation.
 *
 * <p>Requires Docker. {@code @Testcontainers(disabledWithoutDocker = true)}
 * SKIPS the whole class when Docker is unavailable so {@code mvn verify} still
 * succeeds on a machine without Docker. Run this suite in CI to obtain genuine
 * PostgreSQL evidence. These are NOT H2 tests and must not be reported as
 * PostgreSQL tests passing when they were skipped.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Real PostgreSQL (Testcontainers) migration & behaviour")
class PostgresMigrationIntegrationTest {

    private static final String TEST_JWT_SECRET =
            "bdqm-test-only-jwt-secret-0123456789-abcdefghijklmnop";

    private static final AtomicLong SEQ = new AtomicLong();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        // Only LAZY suppliers are registered here. @DynamicPropertySource is
        // evaluated during Spring TestContext bootstrap, which may run BEFORE
        // the Testcontainers extension has started the static @Container. The
        // suppliers are resolved later, when the DataSource is created during
        // context refresh — by then the container is guaranteed to be running.
        // The REAL versioned migration is applied in migrateSchema() below.
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "false"); // applied in @BeforeAll
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.bootstrap.admin.enabled", () -> "false");
    }

    @BeforeAll
    static void migrateSchema() {
        // JUnit invokes BeforeAllCallback extensions (TestcontainersExtension
        // starts the @Container) before any user @BeforeAll method, and prepares
        // the test instance / Spring context afterwards. Applying the REAL
        // versioned migration here guarantees the schema exists before Hibernate
        // connects (ddl-auto=none), without ever touching the container from
        // @DynamicPropertySource. When Docker is absent the whole class is
        // skipped, so this method never runs against a missing container.
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    // ------------------------------------------------------------------
    // Helpers (raw JDBC bypasses application normalisation on purpose)
    // ------------------------------------------------------------------

    private static String hash() {
        return (UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "")).substring(0, 64);
    }

    private static String uniqueEmail(String local) {
        return local + SEQ.incrementAndGet() + "@example.com";
    }

    private static String uniquePhone() {
        return String.format("019%08d", SEQ.incrementAndGet());
    }

    private long insertUserRaw(String email, String phone, String role, String status) {
        String publicId = UUID.randomUUID().toString();
        jdbc.update("insert into users "
                        + "(public_id, email, phone, password_hash, full_name, role, status, created_at, updated_at) "
                        + "values (?,?,?,?,?,?,?, now(), now())",
                publicId, email, phone, "bcrypt-hash", "Raw User", role, status);
        Long id = jdbc.queryForObject("select id from users where public_id = ?",
                Long.class, publicId);
        return id;
    }

    private String insertRefreshTokenRaw(long userId) {
        String tokenHash = hash();
        jdbc.update("insert into refresh_tokens "
                        + "(token_hash, family_id, user_id, expires_at, created_at) "
                        + "values (?,?,?, now() + interval '7 days', now())",
                tokenHash, UUID.randomUUID().toString(), userId);
        return tokenHash;
    }

    // ------------------------------------------------------------------
    // Fresh-schema / Flyway validation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("V1 applied exactly once and the complete schema exists")
    void flywayAppliedV1AndSchemaIsComplete() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true",
                Integer.class);
        assertEquals(1, applied, "exactly one successful migration (V1)");

        assertTrue(tableExists("users"));
        assertTrue(tableExists("refresh_tokens"));
        for (String index : new String[]{"uk_users_email_lower", "uk_users_phone_lower",
                "idx_refresh_family_live", "idx_users_role_status", "uk_refresh_token_hash"}) {
            assertTrue(indexExists(index), "missing index: " + index);
        }
        // Functional indexes really are expression/partial indexes, not plain
        // column indexes.
        Integer expressionIndexes = jdbc.queryForObject(
                "select count(*) from pg_indexes where schemaname='public' "
                        + "and (indexdef like '%lower(%' or indexdef like '%WHERE%')",
                Integer.class);
        assertTrue(expressionIndexes >= 3,
                "expected lower() functional indexes and the partial index");
    }

    private boolean tableExists(String table) {
        Integer n = jdbc.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema='public' and table_name=?",
                Integer.class, table);
        return n != null && n == 1;
    }

    private boolean indexExists(String index) {
        Integer n = jdbc.queryForObject(
                "select count(*) from pg_indexes where schemaname='public' and indexname=?",
                Integer.class, index);
        return n != null && n == 1;
    }

    // ------------------------------------------------------------------
    // Case-insensitive uniqueness (explicit; NOT accent-insensitive)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("lower(email) functional unique index rejects a case-only duplicate")
    void caseInsensitiveEmailUniqueIndexIsEnforced() {
        String base = uniqueEmail("case");
        String upper = base.toUpperCase(Locale.ROOT); // differs only by case
        insertUserRaw(base, uniquePhone(), "CUSTOMER", "ACTIVE");
        // A plain UNIQUE(email) would NOT catch this; only lower(email) does.
        assertThrows(DataIntegrityViolationException.class,
                () -> insertUserRaw(upper, uniquePhone(), "CUSTOMER", "ACTIVE"));
    }

    @Test
    @DisplayName("lower(phone) functional unique index rejects a case-only duplicate")
    void caseInsensitivePhoneUniqueIndexIsEnforced() {
        String mixed = "Abc" + SEQ.incrementAndGet();
        String lower = mixed.toLowerCase(Locale.ROOT);
        insertUserRaw(uniqueEmail("phoneci"), mixed, "CUSTOMER", "ACTIVE");
        assertThrows(DataIntegrityViolationException.class,
                () -> insertUserRaw(uniqueEmail("phoneci"), lower, "CUSTOMER", "ACTIVE"));
    }

    @Test
    @DisplayName("Accent-insensitive folding is deliberately NOT applied")
    void noAccentInsensitiveFolding() {
        String plain = uniqueEmail("jose");
        String accented = uniqueEmail("josé");
        insertUserRaw(plain, uniquePhone(), "CUSTOMER", "ACTIVE");
        insertUserRaw(accented, uniquePhone(), "CUSTOMER", "ACTIVE"); // must succeed
        Integer count = jdbc.queryForObject(
                "select count(*) from users where email in (?,?)", Integer.class,
                plain, accented);
        assertEquals(2, count, "'e' and 'é' must remain distinct (case-only, not accent-insensitive)");
    }

    // ------------------------------------------------------------------
    // TIMESTAMPTZ / UTC
    // ------------------------------------------------------------------

    @Test
    @DisplayName("TIMESTAMPTZ round-trips an Instant in UTC")
    void timestamptzRoundTripsInUtc() {
        Instant instant = Instant.parse("2026-01-01T12:34:56Z");
        String publicId = UUID.randomUUID().toString();
        jdbc.update("insert into users "
                        + "(public_id, email, phone, password_hash, full_name, role, status, created_at, updated_at) "
                        + "values (?,?,?,?,?,?,?,?,?)",
                publicId, uniqueEmail("ts"), uniquePhone(), "hash", "TS", "CUSTOMER", "ACTIVE",
                Timestamp.from(instant), Timestamp.from(instant));

        Instant read = jdbc.queryForObject(
                "select created_at from users where public_id = ?",
                (rs, n) -> rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                publicId);
        assertEquals(instant, read);
    }

    // ------------------------------------------------------------------
    // Constraints: CHECK (six roles / four states) and FK
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CHECK constraints reject unknown roles and statuses")
    void roleAndStatusChecksAreEnforced() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertUserRaw(uniqueEmail("role"), uniquePhone(), "HACKER", "ACTIVE"));
        assertThrows(DataIntegrityViolationException.class,
                () -> insertUserRaw(uniqueEmail("status"), uniquePhone(), "CUSTOMER", "UNKNOWN"));
    }

    @Test
    @DisplayName("refresh_tokens.user_id FK is enforced")
    void refreshTokenForeignKeyIsEnforced() {
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbc.update("insert into refresh_tokens "
                                + "(token_hash, family_id, user_id, expires_at, created_at) "
                                + "values (?,?,?, now() + interval '7 days', now())",
                        hash(), UUID.randomUUID().toString(), 9_999_999L));
    }

    // ------------------------------------------------------------------
    // Entity/schema alignment (JPA works on the migrated schema)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("JPA entities round-trip on the Flyway-migrated schema")
    void jpaEntityRoundTripsOnPostgresSchema() {
        String publicId = UUID.randomUUID().toString();
        UserEntity u = new UserEntity(publicId, uniqueEmail("jpa"), uniquePhone(),
                "hash", "JPA User", UserRole.STORE_MANAGER, AccountStatus.ACTIVE);
        UserEntity saved = userRepository.saveAndFlush(u);
        assertNotNull(saved.getId(), "IDENTITY primary key was generated");
        assertNotNull(saved.getCreatedAt(), "JPA auditing populated created_at");

        UserEntity found = userRepository.findByPublicId(publicId).orElseThrow();
        assertEquals(UserRole.STORE_MANAGER, found.getRole());
        assertEquals(AccountStatus.ACTIVE, found.getStatus());
    }

    // ------------------------------------------------------------------
    // Refresh-token rotation: SELECT ... FOR UPDATE serialises concurrent
    // readers (atomicity / concurrency safety)
    // ------------------------------------------------------------------

    @Test
    @DisplayName("PESSIMISTIC_WRITE (FOR UPDATE) serialises concurrent rotation")
    void pessimisticWriteLockSerialisesConcurrentReaders() throws Exception {
        long userId = insertUserRaw(uniqueEmail("lock"), uniquePhone(), "CUSTOMER", "ACTIVE");
        String tokenHash = insertRefreshTokenRaw(userId);

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        try {
            Future<?> first = pool.submit(() -> tx.executeWithoutResult(status -> {
                refreshTokenRepository.findByHashForUpdate(tokenHash).orElseThrow();
                firstLocked.countDown();
                try {
                    releaseFirst.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));

            assertTrue(firstLocked.await(5, TimeUnit.SECONDS),
                    "first transaction should acquire the FOR UPDATE lock");

            Future<?> second = pool.submit(() -> tx.executeWithoutResult(status -> {
                secondStarted.countDown();
                refreshTokenRepository.findByHashForUpdate(tokenHash).orElseThrow();
            }));

            assertTrue(secondStarted.await(5, TimeUnit.SECONDS),
                    "second transaction should start");
            Thread.sleep(400);
            boolean blockedWhileFirstHeldLock = !second.isDone();

            releaseFirst.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);

            assertTrue(blockedWhileFirstHeldLock,
                    "the second FOR UPDATE reader must block until the first commits");
        } finally {
            releaseFirst.countDown();
            pool.shutdownNow();
        }
    }
}

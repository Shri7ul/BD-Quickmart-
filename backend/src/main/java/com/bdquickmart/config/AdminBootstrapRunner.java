package com.bdquickmart.config;

import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserRepository;
import com.bdquickmart.modules.user.UserRole;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Opt-in, one-time creation of the very first SUPER_ADMIN account.
 *
 * <p>Disabled by default and guarded by {@code APP_BOOTSTRAP_ADMIN_ENABLED}.
 * No default password exists: every field is required when enabled, the
 * password is hashed immediately and is never logged. The switch must be
 * turned off again after the first successful start (environment variables
 * are visible via {@code ps}).
 *
 * <p><b>Concurrency safety (cloud deploy):</b> the insert runs in its own
 * {@code REQUIRES_NEW} transaction and the database unique constraints
 * ({@code uk_users_email}, {@code uk_users_phone} and the functional
 * {@code lower(...)} indexes) act as the final arbiter. If two instances
 * bootstrap simultaneously, both can pass the {@code existsByRole} check, but
 * only one INSERT can win; the loser gets a {@link DataIntegrityViolationException}
 * which is swallowed here so no duplicate administrator is created and the
 * second instance still starts cleanly. This does not rely on the in-process
 * check alone.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final BootstrapProperties props;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    public AdminBootstrapRunner(BootstrapProperties props,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                PlatformTransactionManager transactionManager) {
        this.props = props;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionManager = transactionManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.enabled()) {
            return;
        }

        log.warn("Admin bootstrap is ENABLED. Disable APP_BOOTSTRAP_ADMIN_ENABLED "
                + "immediately after the first administrator has been created.");

        if (props.hasMissingRequiredFields()) {
            throw new IllegalStateException("Admin bootstrap is enabled but one of "
                    + "APP_BOOTSTRAP_ADMIN_EMAIL/PASSWORD/FULL_NAME/PHONE is blank");
        }

        // Normalise exactly like AuthService.register so the account is
        // reachable through the normal login path and the case-insensitive
        // lower(email) index is meaningful.
        final String email = props.email().trim().toLowerCase(Locale.ROOT);
        final String phone = props.phone().trim();

        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            Boolean created = tx.execute(status -> {
                // Re-check inside the transaction; the outer check is only a
                // fast path. Under concurrent startup both instances may see
                // "no SUPER_ADMIN" here — the DB constraints decide the winner.
                if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
                    log.info("A SUPER_ADMIN already exists; skipping admin bootstrap.");
                    return false;
                }
                if (userRepository.existsByEmail(email) || userRepository.existsByPhone(phone)) {
                    log.warn("Admin bootstrap email/phone is already in use by another "
                            + "account; skipping to avoid an unsafe overwrite. Verify "
                            + "the APP_BOOTSTRAP_ADMIN_* values.");
                    return false;
                }

                UserEntity admin = new UserEntity(
                        UUID.randomUUID().toString(),
                        email,
                        phone,
                        passwordEncoder.encode(props.password()),
                        props.fullName().trim(),
                        UserRole.SUPER_ADMIN,
                        AccountStatus.ACTIVE);
                userRepository.save(admin);
                return true;
            });

            if (Boolean.TRUE.equals(created)) {
                log.info("Bootstrap SUPER_ADMIN created for {}", email);
            }
        } catch (DataIntegrityViolationException ex) {
            // Another instance won the race (or the identity was claimed in
            // between). Exactly one administrator exists; do not fail startup.
            log.info("SUPER_ADMIN bootstrap raced with another instance; skipping. "
                    + "The unique constraints prevented a duplicate administrator.");
        }
    }
}

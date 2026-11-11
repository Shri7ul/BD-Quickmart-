package com.bdquickmart.config;

import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserRepository;
import com.bdquickmart.modules.user.UserRole;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Opt-in creation of the very first SUPER_ADMIN account.
 *
 * <p>Disabled by default and guarded by {@code APP_BOOTSTRAP_ADMIN_ENABLED}.
 * The password is hashed immediately and is never logged. Intended only for a
 * controlled, single-instance first run: environment variables are visible via
 * {@code ps}, so the switch must be turned off again after use. A companion
 * SQL template is provided for operators who prefer an out-of-band hash.
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final BootstrapProperties props;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(BootstrapProperties props,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder) {
        this.props = props;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        if (userRepository.existsByRole(UserRole.SUPER_ADMIN)) {
            log.info("A SUPER_ADMIN already exists; skipping admin bootstrap.");
            return;
        }
        if (userRepository.existsByEmail(props.email()) || userRepository.existsByPhone(props.phone())) {
            throw new IllegalStateException("Admin bootstrap email/phone is already in use");
        }

        UserEntity admin = new UserEntity(
                UUID.randomUUID().toString(),
                props.email(),
                props.phone(),
                passwordEncoder.encode(props.password()),
                props.fullName(),
                UserRole.SUPER_ADMIN,
                AccountStatus.ACTIVE);
        userRepository.save(admin);
        log.info("Bootstrap SUPER_ADMIN created for {}", props.email());
    }
}

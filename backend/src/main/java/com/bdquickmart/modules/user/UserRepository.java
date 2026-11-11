package com.bdquickmart.modules.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByPublicId(String publicId);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhone(String phone);

    /** Login accepts either an email address or a phone number. */
    Optional<UserEntity> findByEmailOrPhone(String email, String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByRole(UserRole role);

    List<UserEntity> findAllByRoleAndStatus(UserRole role, AccountStatus status);
}

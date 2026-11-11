package com.bdquickmart.modules.auth;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    /**
     * Looks up a token hash under a pessimistic write lock so concurrent
     * refresh attempts for the same token are serialized (rotation + replay
     * detection must be atomic).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshTokenEntity t where t.tokenHash = :hash")
    Optional<RefreshTokenEntity> findByHashForUpdate(@Param("hash") String hash);

    /** Live (not yet revoked) members of a token family. */
    @Query("select t from RefreshTokenEntity t "
            + "where t.familyId = :familyId and t.revokedAt is null")
    List<RefreshTokenEntity> findLiveByFamilyId(@Param("familyId") String familyId);

    /** Live refresh tokens belonging to a user, used by logout. */
    @Query("select t from RefreshTokenEntity t "
            + "where t.user.id = :userId and t.revokedAt is null")
    List<RefreshTokenEntity> findLiveByUserId(@Param("userId") Long userId);
}

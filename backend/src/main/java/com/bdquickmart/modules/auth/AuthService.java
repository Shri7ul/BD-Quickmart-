package com.bdquickmart.modules.auth;

import com.bdquickmart.common.exception.BadRequestException;
import com.bdquickmart.common.exception.DuplicateResourceException;
import com.bdquickmart.common.exception.ResourceNotFoundException;
import com.bdquickmart.config.JwtProperties;
import com.bdquickmart.modules.auth.dto.AuthResponse;
import com.bdquickmart.modules.auth.dto.LoginRequest;
import com.bdquickmart.modules.auth.dto.ProvisionRequest;
import com.bdquickmart.modules.auth.dto.RegisterRequest;
import com.bdquickmart.modules.auth.exception.AccountDisabledException;
import com.bdquickmart.modules.auth.exception.InvalidCredentialsException;
import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserMapper;
import com.bdquickmart.modules.user.UserRepository;
import com.bdquickmart.modules.user.UserRole;
import com.bdquickmart.modules.user.dto.UserResponse;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication and account-lifecycle business logic.
 *
 * <p>Refresh tokens are rotated atomically. Reusing a revoked token is treated
 * as replay: the whole token family is revoked and a generic 401 is returned.
 * The revocation is performed inside a transaction that returns normally, so
 * it is committed <em>before</em> the controller turns the empty result into
 * a 401 (throwing from within the transaction would roll the revocation back).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties,
                       Clock clock) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        UserRole role = parseRole(request.role(), false);
        String email = normalizeEmail(request.email());
        String phone = request.phone().trim();
        ensureUnique(email, phone);

        UserEntity user = new UserEntity(
                UUID.randomUUID().toString(),
                email,
                phone,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                role,
                role == UserRole.RIDER ? AccountStatus.PENDING : AccountStatus.ACTIVE);
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.identifier().trim();
        UserEntity user = userRepository
                .findByEmailOrPhone(normalizeEmail(identifier), identifier)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() == AccountStatus.DISABLED
                || user.getStatus() == AccountStatus.REJECTED) {
            throw new AccountDisabledException();
        }
        // A PENDING rider may obtain a token but cannot reach the rider API
        // until an administrator sets the account ACTIVE.
        return issueTokens(user, UUID.randomUUID().toString());
    }

    /**
     * Rotates a refresh token. Returns {@link Optional#empty()} for every
     * failure so all of them collapse to a single generic 401.
     */
    @Transactional
    public Optional<AuthResponse> refresh(String rawToken) {
        TokenClaims claims;
        try {
            claims = jwtService.parse(rawToken);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
        if (!claims.isRefresh()) {
            return Optional.empty();
        }

        String hash = jwtService.hashToken(rawToken);
        RefreshTokenEntity stored = refreshTokenRepository.findByHashForUpdate(hash)
                .orElse(null);
        if (stored == null) {
            return Optional.empty();
        }
        if (stored.getRevokedAt() != null) {
            // Replay detected: revoke the entire family, then fail generically.
            revokeFamily(stored.getFamilyId());
            return Optional.empty();
        }
        if (!stored.getExpiresAt().isAfter(clock.instant())) {
            return Optional.empty();
        }

        UserEntity user = stored.getUser();
        if (user == null || user.getDeletedAt() != null
                || user.getStatus() == AccountStatus.DISABLED
                || user.getStatus() == AccountStatus.REJECTED) {
            return Optional.empty();
        }

        String newAccess = jwtService.generateAccessToken(user.getPublicId(), user.getRole());
        String newRefresh = jwtService.generateRefreshToken(
                user.getPublicId(), user.getRole(), stored.getFamilyId());
        String newHash = jwtService.hashToken(newRefresh);
        Instant now = clock.instant();

        stored.setRevokedAt(now);
        stored.setReplacedByHash(newHash);
        refreshTokenRepository.save(new RefreshTokenEntity(
                newHash,
                stored.getFamilyId(),
                user,
                now.plusSeconds(jwtProperties.refreshExpiration()),
                now));

        return Optional.of(new AuthResponse(newAccess, newRefresh, "Bearer",
                jwtProperties.accessExpiration(), UserMapper.toResponse(user)));
    }

    /** Revokes every live refresh token belonging to the user (logout). */
    @Transactional
    public void logout(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        userRepository.findByPublicId(publicId).ifPresent(user -> {
            Instant now = clock.instant();
            for (RefreshTokenEntity token : refreshTokenRepository.findLiveByUserId(user.getId())) {
                token.setRevokedAt(now);
            }
        });
    }

    @Transactional(readOnly = true)
    public UserResponse me(String publicId) {
        return userRepository.findByPublicId(publicId)
                .map(UserMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", "publicId", publicId));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> pendingRiders() {
        return userRepository.findAllByRoleAndStatus(UserRole.RIDER, AccountStatus.PENDING)
                .stream().map(UserMapper::toResponse).toList();
    }

    @Transactional
    public UserResponse approveRider(String publicId) {
        return changeRiderStatus(publicId, AccountStatus.ACTIVE);
    }

    @Transactional
    public UserResponse rejectRider(String publicId) {
        return changeRiderStatus(publicId, AccountStatus.REJECTED);
    }

    @Transactional
    public UserResponse provision(ProvisionRequest request) {
        UserRole role = parseRole(request.role(), true);
        String email = normalizeEmail(request.email());
        String phone = request.phone().trim();
        ensureUnique(email, phone);

        UserEntity user = new UserEntity(
                UUID.randomUUID().toString(),
                email,
                phone,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                role,
                AccountStatus.ACTIVE);
        userRepository.save(user);
        return UserMapper.toResponse(user);
    }

    private AuthResponse issueTokens(UserEntity user, String familyId) {
        String access = jwtService.generateAccessToken(user.getPublicId(), user.getRole());
        String refresh = jwtService.generateRefreshToken(
                user.getPublicId(), user.getRole(), familyId);
        Instant now = clock.instant();
        refreshTokenRepository.save(new RefreshTokenEntity(
                jwtService.hashToken(refresh),
                familyId,
                user,
                now.plusSeconds(jwtProperties.refreshExpiration()),
                now));
        return new AuthResponse(access, refresh, "Bearer",
                jwtProperties.accessExpiration(), UserMapper.toResponse(user));
    }

    private void revokeFamily(String familyId) {
        Instant now = clock.instant();
        for (RefreshTokenEntity token : refreshTokenRepository.findLiveByFamilyId(familyId)) {
            token.setRevokedAt(now);
        }
    }

    private UserResponse changeRiderStatus(String publicId, AccountStatus status) {
        UserEntity user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider", "publicId", publicId));
        if (user.getRole() != UserRole.RIDER) {
            throw new BadRequestException("Account is not a rider");
        }
        user.setStatus(status);
        return UserMapper.toResponse(user);
    }

    private UserRole parseRole(String raw, boolean privilegedOnly) {
        UserRole role;
        try {
            role = UserRole.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new BadRequestException("Unknown role: " + raw);
        }
        if (privilegedOnly ? !role.isPrivileged() : !role.isPublicRegistration()) {
            throw new BadRequestException(privilegedOnly
                    ? "Only privileged roles may be provisioned here"
                    : "Public registration is only allowed for CUSTOMER and RIDER");
        }
        return role;
    }

    private void ensureUnique(String email, String phone) {
        if (userRepository.existsByEmail(email) || userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("Email or phone number is already registered");
        }
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}

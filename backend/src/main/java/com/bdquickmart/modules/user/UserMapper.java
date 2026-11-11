package com.bdquickmart.modules.user;

import com.bdquickmart.modules.user.dto.UserResponse;

/** Maps {@link UserEntity} to its safe, public representation. */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(UserEntity entity) {
        return new UserResponse(
                entity.getPublicId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getRole().name(),
                entity.getStatus().name());
    }
}

package com.bdquickmart.modules.rider;

import com.bdquickmart.common.exception.BusinessException;
import com.bdquickmart.common.exception.ResourceNotFoundException;
import com.bdquickmart.common.api.ApiCode;
import com.bdquickmart.modules.user.AccountStatus;
import com.bdquickmart.modules.user.UserEntity;
import com.bdquickmart.modules.user.UserRepository;
import com.bdquickmart.modules.user.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rider availability state, backed by the database (never local UI state). */
@Service
@Transactional(readOnly = true)
public class RiderService {

    private final RiderAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public RiderService(RiderAvailabilityRepository availabilityRepository,
                        UserRepository userRepository, Clock clock) {
        this.availabilityRepository = availabilityRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public void setAvailability(String riderPublicId, boolean online) {
        UserEntity rider = userRepository.findByPublicId(riderPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider", "publicId", riderPublicId));
        if (rider.getRole() != UserRole.RIDER || rider.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException(ApiCode.CONFLICT, "Only approved riders can go online");
        }
        RiderAvailabilityEntity state = availabilityRepository.findById(rider.getId())
                .orElseGet(() -> new RiderAvailabilityEntity(rider.getId(), false, clock.instant()));
        state.setOnline(online, clock.instant());
        availabilityRepository.save(state);
    }

    public boolean isOnline(Long riderId) {
        return availabilityRepository.findById(riderId).map(RiderAvailabilityEntity::isOnline).orElse(false);
    }

    public List<UserEntity> eligibleRiders() {
        return userRepository.findAllByRoleAndStatus(UserRole.RIDER, AccountStatus.ACTIVE);
    }
}

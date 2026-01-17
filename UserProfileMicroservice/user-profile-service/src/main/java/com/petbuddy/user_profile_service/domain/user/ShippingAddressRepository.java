package com.petbuddy.user_profile_service.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {
    List<ShippingAddress> findAllByUserId(UUID userId);
    Optional<ShippingAddress> findByIdAndUserId(Long addressId, UUID userId);
    void deleteByIdAndUserId(Long addressId, UUID userId);

    @Modifying
    @Query("UPDATE ShippingAddress sa SET sa.is_default = false WHERE sa.user.id = :userId")
    void clearAllDefaultsForUser(UUID userId);

    @Modifying
    @Query("UPDATE ShippingAddress sa SET sa.is_default = true WHERE sa.id = :addressId AND sa.user.id = :userId")
    void setDefaultForUser(Long addressId, UUID userId);

    @Modifying
    @Query("UPDATE ShippingAddress a SET a.isDefault = false WHERE a.user.id = :userId and a.isDefault = true")
    void clearDefaultAddresses(UUID userId);

}
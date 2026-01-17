package com.petbuddy.user_profile_service.domain.user;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Entity
@Table(name = "shipping_addresses", indexes = {
        @Index(name = "idx_shipping_address_user_id", columnList = "user_id"),
        @Index(name = "idx_shipping_address_zip_code", columnList = "zip_code"),
        @Index(name = "idx_shipping_address_city", columnList = "city"),
        @Index(name = "idx_address_user_id_default", columnList = "user_id, is_default")
},
uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "is_default"})
})
@Data
public class ShippingAddress {
    @Id
    @SequenceGenerator(
        name = "address_seq",
        sequenceName = "address_id_seq",
        allocationSize = 50
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "address_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String addressLine1;

    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(name = "zip_code", nullable = false)
    @Pattern(regexp = "^[0-9]{5}(-[0-9]{4})?$", message = "Invalid ZIP code")
    private String zipCode;

    @Column(nullable = false)
    private String country;

    @Column(name = "is_default")
    private boolean isDefault;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted")
    private boolean deleted = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
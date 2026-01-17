package com.petbuddy.user_profile_service.domain.user;

import com.petbuddy.user_profile_service.domain.pet.Pet;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_phone_search_hash", columnList = "phone_search_hash")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "auth_user_id", unique = true, nullable = false, updatable = false)
    private String authUserId;

    @Email
    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "age")
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "phone_hash_encrypted", nullable = true)
    private String phoneHashEncrypted;

    @Column(name = "phone_verification_token", nullable = true)
    private String phoneVerificationToken;

    @Column(name = "phone_search_hash", nullable = true)
    private String phoneSearchHash;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "volunteer")
    private Boolean volunteer;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted")
    private boolean deleted = false;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShippingAddress> shippingAddresses = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentMethodReference> paymentMethodReferences = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Pet> pets = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PreRemove
    private void anonymize() {
        this.email = "deleted-" + this.id + "@example.com";
        this.phoneHashEncrypted = null;
    }

    public void addPet(Pet pet) {
        pets.add(pet);
        pet.setUser(this);
    }

    public void addShippingAddress(ShippingAddress address) {
        shippingAddresses.add(address);
        address.setUser(this);
    }

    public void addPaymentMethod(PaymentMethodReference paymentMethod) {
        paymentMethodReferences.add(paymentMethod);
        paymentMethod.setUser(this);
    }

    public void removePet(Pet pet) {
        pets.remove(pet);
        pet.setUser(null);
    }

    public void removeShippingAddress(ShippingAddress address) {
        shippingAddresses.remove(address);
        address.setUser(null);
    }

    public void removePaymentMethod(PaymentMethodReference paymentMethod) {
        paymentMethodReferences.remove(paymentMethod);
        paymentMethod.setUser(null);
    }

}
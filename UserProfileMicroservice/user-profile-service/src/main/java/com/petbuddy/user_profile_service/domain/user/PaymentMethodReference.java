package com.petbuddy.user_profile_service.domain.user;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "payment_method_refs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "is_default"})
})
@Data
public class PaymentMethodReference {
    @Id
    @SequenceGenerator(
        name = "payment_ref_seq",
        sequenceName = "payment_ref_id_seq",
        allocationSize = 50
    )
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "payment_ref_seq"
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "payment_provider_token_encrypted", nullable = false)
    private String paymentProviderTokenEncrypted; // Encrypted

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type")
    private CardType cardType; // Enum: VISA, MASTERCARD, etc.

    @Column(name = "last_four_digits")
    private String lastFourDigits; // Masked in logs/APIs

    @Column(name = "is_default")
    private boolean isDefault;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted")
    private boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

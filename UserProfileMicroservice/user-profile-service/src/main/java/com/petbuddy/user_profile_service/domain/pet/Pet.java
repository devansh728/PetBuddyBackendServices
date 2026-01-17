package com.petbuddy.user_profile_service.domain.pet;

import com.petbuddy.user_profile_service.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pets", indexes = {
        @Index(name = "idx_pet_user_id", columnList = "user_id"),
        @Index(name = "idx_pet_species", columnList = "species"),
        @Index(name = "idx_pet_breed", columnList = "breed"),
        @Index(name = "idx_pet_name", columnList = "name"),
        @Index(name = "idx_pet_dob", columnList = "dob")
})
@NamedEntityGraph(name = "Pet.withDocuments", attributeNodes = { @NamedAttributeNode("medicalDocuments") })
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pet {
    @Id
    @SequenceGenerator(name = "pet_seq", sequenceName = "pet_id_seq", allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pet_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Species species;

    private String breed;

    @Column(name = "dob")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Column(name = "weight_kg")
    @Min(value = 0, message = "Weight must be positive")
    @Max(value = 200, message = "Weight must be realistic")
    private Double weightKg;

    @Column(columnDefinition = "JSONB")
    private String allergies;

    @OneToMany(mappedBy = "pet", cascade = { CascadeType.PERSIST, CascadeType.MERGE }, orphanRemoval = true)
    @BatchSize(size = 20)
    private List<MedicalDocument> medicalDocuments = new ArrayList<>();

    @Version
    @Column(name = "version")
    private Long version;

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
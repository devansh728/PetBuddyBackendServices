package com.petbuddy.user_profile_service.domain.pet;

import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetRepository extends JpaRepository<Pet, Long> {

    @QueryHints(@QueryHint(name = HibernateHints.HINT_READ_ONLY, value = "true"))
    @Override
    List<Pet> findAll();

    @QueryHints(@QueryHint(name = HibernateHints.HINT_READ_ONLY, value = "true"))
    @Query("SELECT p FROM Pet p WHERE p.user.id = :userId AND p.deleted = false")
    List<Pet> findAllByUserId(@Param("userId") UUID userId);

    @EntityGraph(value = "Pet.withDocuments")
    @Query("SELECT p FROM Pet p WHERE p.id = :petId AND p.user.id = :userId AND p.deleted = false")
    Optional<Pet> findByIdAndUserId(@Param("petId") Long petId, @Param("userId") UUID userId);

    @Query("UPDATE Pet p SET p.deleted = true WHERE p.id = :petId AND p.user.id = :userId")
    void deleteByIdAndUserId(@Param("petId") Long petId, @Param("userId") UUID userId);
}
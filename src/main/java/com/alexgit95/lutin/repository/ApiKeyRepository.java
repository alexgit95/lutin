package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByEmpreinte(String empreinte);

    boolean existsByNomCle(String nomCle);

    // Au plus 20 clés : le plafond évite toute pagination.
    List<ApiKey> findAllByOrderByCreeLeAscIdAsc();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update ApiKey k set k.derniereUtilisation = :le where k.id = :id")
    int marquerUtilisee(@Param("id") Long id, @Param("le") Instant le);
}

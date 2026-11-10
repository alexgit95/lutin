package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.AlerteRemboursement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlerteRemboursementRepository extends JpaRepository<AlerteRemboursement, Long> {

    List<AlerteRemboursement> findByListeIdOrderByCreeLeDescIdDesc(Long listeId, Pageable pageable);

    long countByListeId(Long listeId);

    long countByListeIdAndListeProprietaireLogin(Long listeId, String login);

    Optional<AlerteRemboursement> findByIdAndListeId(Long id, Long listeId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from AlerteRemboursement a where a.liste.id = :listeId")
    void supprimerDeLaListe(@Param("listeId") Long listeId);
}

package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.Personne;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PersonneRepository extends JpaRepository<Personne, Long> {

    List<Personne> findByListeIdOrderByNom(Long listeId);

    long countByListeId(Long listeId);

    void deleteByListeId(Long listeId);

    @Query("select coalesce(sum(p.budget), 0) from Personne p where p.liste.id = :listeId")
    BigDecimal sommeBudgets(@Param("listeId") Long listeId);
}

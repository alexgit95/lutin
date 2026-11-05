package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.Partage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartageRepository extends JpaRepository<Partage, Long> {

    @EntityGraph(attributePaths = {"liste", "liste.proprietaire"})
    List<Partage> findByLecteurLoginOrderByListeNom(String lecteurLogin);

    @EntityGraph(attributePaths = "lecteur")
    List<Partage> findByListeIdOrderByLecteurLogin(Long listeId);

    boolean existsByListeIdAndLecteurId(Long listeId, Long lecteurId);

    boolean existsByListeIdAndLecteurLogin(Long listeId, String lecteurLogin);

    void deleteByListeId(Long listeId);
}

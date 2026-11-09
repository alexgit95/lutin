package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.Cadeau;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CadeauRepository extends JpaRepository<Cadeau, Long> {

    // Même prédicat que sommeDebourse : l'argent a quitté le compte courant.
    String REMBOURSABLE = "c.pourTiers = false and c.prix is not null "
            + "and c.statut <> com.alexgit95.lutin.domain.Statut.A_ACHETER";

    @EntityGraph(attributePaths = "personne")
    Page<Cadeau> findByListeId(Long listeId, Pageable pageable);

    @EntityGraph(attributePaths = "personne")
    Page<Cadeau> findByListeIdAndTagsId(Long listeId, Long tagId, Pageable pageable);

    // Un masqué n'a jamais de tag pour un lecteur : il ne ressort pas d'un filtre.
    @EntityGraph(attributePaths = "personne")
    Page<Cadeau> findByListeIdAndTagsIdAndMasqueFalse(Long listeId, Long tagId, Pageable pageable);

    // Visibles d'abord, puis masqués : les clés de tri d'un masqué sont constantes, seul l'identifiant ordonne.
    @EntityGraph(attributePaths = "personne")
    @Query(value = "select c from Cadeau c where c.liste.id = :listeId order by c.masque, "
            + "case when c.masque = true then '' else c.personne.nom end, "
            + "case when c.masque = true then 0L else c.personne.id end, "
            + "case when c.masque = true then '' else c.nom end, c.id",
            countQuery = "select count(c) from Cadeau c where c.liste.id = :listeId")
    Page<Cadeau> findPourLecteur(@Param("listeId") Long listeId, Pageable pageable);

    long countByPersonneId(Long personneId);

    List<Cadeau> findByPersonneIdAndRembourseLeIsNotNull(Long personneId);

    @EntityGraph(attributePaths = "personne")
    @Query(value = "select c from Cadeau c where c.liste.id = :listeId and " + REMBOURSABLE
            + " order by case when c.rembourseLe is null then 0 else 1 end, c.personne.nom, c.personne.id, c.nom, c.id",
            countQuery = "select count(c) from Cadeau c where c.liste.id = :listeId and " + REMBOURSABLE)
    Page<Cadeau> findRemboursables(@Param("listeId") Long listeId, Pageable pageable);

    @Query("select coalesce(sum(c.prix), 0) from Cadeau c where c.liste.id = :listeId and " + REMBOURSABLE
            + " and c.rembourseLe is null")
    BigDecimal sommeARembourser(@Param("listeId") Long listeId);

    @Query("select coalesce(sum(c.prix), 0) from Cadeau c where c.liste.id = :listeId and " + REMBOURSABLE
            + " and c.rembourseLe is not null")
    BigDecimal sommeRembourse(@Param("listeId") Long listeId);

    /** Seuls les cadeaux encore remboursables et non remboursés sont touchés ; renvoie leur nombre. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Cadeau c set c.rembourseLe = :date where c.liste.id = :listeId and " + REMBOURSABLE
            + " and c.rembourseLe is null")
    int marquerRembourses(@Param("listeId") Long listeId, @Param("date") LocalDate date);

    void deleteByListeId(Long listeId);

    void deleteByPersonneId(Long personneId);

    // Cadeaux pour compte de tiers et sans prix exclus ; "À acheter" exclu du déboursé.
    @Query("select coalesce(sum(c.prix), 0) from Cadeau c where c.liste.id = :listeId "
            + "and c.pourTiers = false and c.prix is not null "
            + "and c.statut <> com.alexgit95.lutin.domain.Statut.A_ACHETER")
    BigDecimal sommeDebourse(@Param("listeId") Long listeId);

    @Query("select coalesce(sum(c.prix), 0) from Cadeau c where c.liste.id = :listeId "
            + "and c.pourTiers = false and c.prix is not null "
            + "and c.statut = com.alexgit95.lutin.domain.Statut.A_ACHETER")
    BigDecimal sommeResteAPayer(@Param("listeId") Long listeId);

    @Query("select c.statut, count(c) from Cadeau c where c.liste.id = :listeId "
            + "and (:inclureMasques = true or c.masque = false) group by c.statut")
    List<Object[]> compterParStatut(@Param("listeId") Long listeId, @Param("inclureMasques") boolean inclureMasques);

    // Mêmes règles que sommeDebourse et sommeResteAPayer, ventilées par personne : (personneId, dépensé, à payer).
    @Query("select c.personne.id, "
            + "coalesce(sum(case when c.statut <> com.alexgit95.lutin.domain.Statut.A_ACHETER then c.prix end), 0), "
            + "coalesce(sum(case when c.statut = com.alexgit95.lutin.domain.Statut.A_ACHETER then c.prix end), 0) "
            + "from Cadeau c where c.liste.id = :listeId and c.pourTiers = false and c.prix is not null "
            + "group by c.personne.id")
    List<Object[]> sommesParPersonne(@Param("listeId") Long listeId);
}

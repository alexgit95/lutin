package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.Avis;
import com.alexgit95.lutin.domain.TypeAvis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AvisRepository extends JpaRepository<Avis, Long> {

    Page<Avis> findByDestinataireLoginAndCreeLeAfterOrderByCreeLeDescIdDesc(String login, Instant limite,
                                                                            Pageable pageable);

    Page<Avis> findByDestinataireLoginAndWishlistIdAndCreeLeAfterOrderByCreeLeDescIdDesc(String login,
            Long wishlistId, Instant limite, Pageable pageable);

    long countByDestinataireLoginAndCreeLeAfter(String login, Instant limite);

    boolean existsByDestinataireIdAndSouhaitIdAndType(Long destinataireId, Long souhaitId, TypeAvis type);

    Optional<Avis> findByIdAndDestinataireLogin(Long id, String login);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Avis a where a.souhaitId = :souhaitId and a.type = :type")
    void supprimerDeLArticle(@Param("souhaitId") Long souhaitId, @Param("type") TypeAvis type);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Avis a where a.wishlistId = :wishlistId")
    void supprimerDeLaWishlist(@Param("wishlistId") Long wishlistId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Avis a set a.wishlistId = null where a.wishlistId = :wishlistId")
    void detacherDeLaWishlist(@Param("wishlistId") Long wishlistId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Avis a where a.destinataire.id = :lecteurId and a.wishlistId = :wishlistId")
    void supprimerDuLecteur(@Param("lecteurId") Long lecteurId, @Param("wishlistId") Long wishlistId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Avis a where a.creeLe < :limite")
    void supprimerAvant(@Param("limite") Instant limite);
}

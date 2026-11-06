package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.Souhait;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface SouhaitRepository extends JpaRepository<Souhait, Long> {

    // Ordre de création : il ne dépend jamais de l'état « pris ».
    Page<Souhait> findByWishlistIdOrderByIdAsc(Long wishlistId, Pageable pageable);

    Optional<Souhait> findByIdAndWishlistId(Long id, Long wishlistId);

    long countByWishlistIdAndPrisParNomIsNotNull(Long wishlistId);

    /** Prise atomique : renvoie 0 si l'article est déjà pris ou n'appartient pas à la wishlist. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Souhait s set s.prisParNom = :nom, s.prisLe = :le "
            + "where s.id = :id and s.wishlist.id = :wishlistId and s.prisParNom is null")
    int prendre(@Param("id") Long id, @Param("wishlistId") Long wishlistId,
                @Param("nom") String nom, @Param("le") Instant le);

    /** Libération atomique : renvoie 0 si l'article est déjà libre ou n'appartient pas à la wishlist. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Souhait s set s.prisParNom = null, s.prisLe = null "
            + "where s.id = :id and s.wishlist.id = :wishlistId and s.prisParNom is not null")
    int liberer(@Param("id") Long id, @Param("wishlistId") Long wishlistId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Souhait s where s.wishlist.id = :wishlistId")
    void supprimerDeLaWishlist(@Param("wishlistId") Long wishlistId);
}

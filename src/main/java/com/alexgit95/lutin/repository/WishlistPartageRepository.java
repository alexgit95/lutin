package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.WishlistPartage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistPartageRepository extends JpaRepository<WishlistPartage, Long> {

    @EntityGraph(attributePaths = {"wishlist", "wishlist.proprietaire"})
    List<WishlistPartage> findByLecteurLoginOrderByWishlistNom(String lecteurLogin);

    @EntityGraph(attributePaths = "lecteur")
    List<WishlistPartage> findByWishlistIdOrderByLecteurLogin(Long wishlistId);

    boolean existsByWishlistIdAndLecteurId(Long wishlistId, Long lecteurId);

    boolean existsByWishlistIdAndLecteurLogin(Long wishlistId, String lecteurLogin);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from WishlistPartage p where p.wishlist.id = :wishlistId")
    void supprimerDeLaWishlist(@Param("wishlistId") Long wishlistId);
}

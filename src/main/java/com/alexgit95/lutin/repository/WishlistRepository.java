package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.Wishlist;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    List<Wishlist> findByProprietaireLoginOrderByNom(String login);

    @EntityGraph(attributePaths = "proprietaire")
    Optional<Wishlist> findWithProprietaireById(Long id);

    @EntityGraph(attributePaths = "proprietaire")
    Optional<Wishlist> findWithProprietaireByJetonPublic(String jetonPublic);

    boolean existsByProprietaireIdAndNomCle(Long proprietaireId, String nomCle);

    boolean existsByProprietaireIdAndNomCleAndIdNot(Long proprietaireId, String nomCle, Long id);
}

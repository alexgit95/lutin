package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.ListeAchat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListeAchatRepository extends JpaRepository<ListeAchat, Long> {

    List<ListeAchat> findByProprietaireLoginOrderByNom(String login);

    @EntityGraph(attributePaths = "proprietaire")
    Optional<ListeAchat> findWithProprietaireById(Long id);
}

package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByLogin(String login);

    boolean existsByLogin(String login);

    List<Utilisateur> findAllByOrderByLoginAsc();
}

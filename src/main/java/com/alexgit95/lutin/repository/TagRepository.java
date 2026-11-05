package com.alexgit95.lutin.repository;

import com.alexgit95.lutin.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByListeIdOrderByNom(Long listeId);

    List<Tag> findByListeIdAndIdIn(Long listeId, Collection<Long> ids);

    boolean existsByListeIdAndNomIgnoreCase(Long listeId, String nom);

    void deleteByListeId(Long listeId);
}

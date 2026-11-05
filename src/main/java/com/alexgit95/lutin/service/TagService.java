package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.domain.Tag;
import com.alexgit95.lutin.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TagService {

    private final TagRepository tags;
    private final AccesListeService acces;

    public TagService(TagRepository tags, AccesListeService acces) {
        this.tags = tags;
        this.acces = acces;
    }

    public Tag creer(Long listeId, String login, String nom) {
        ListeAchat liste = acces.proprietaire(listeId, login);
        String propre = nom == null ? "" : nom.trim();
        if (propre.isEmpty() || propre.length() > 50) {
            throw new RequeteInvalideException("Le tag est obligatoire (50 caractères maximum)");
        }
        // Le dialecte SQLite n'applique pas la contrainte composite : unicité vérifiée ici.
        if (tags.existsByListeIdAndNomIgnoreCase(listeId, propre)) {
            throw new RequeteInvalideException("Ce tag existe déjà");
        }
        return tags.save(new Tag(propre, liste));
    }
}

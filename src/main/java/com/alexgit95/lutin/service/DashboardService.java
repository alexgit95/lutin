package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.Cadeau;
import com.alexgit95.lutin.domain.Personne;
import com.alexgit95.lutin.domain.Statut;
import com.alexgit95.lutin.domain.Tag;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import com.alexgit95.lutin.repository.TagRepository;
import com.alexgit95.lutin.vue.VueCadeau;
import com.alexgit95.lutin.vue.VueListe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Construit le dashboard d'une liste pour son propriétaire ou un lecteur. */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    public static final int TAILLE_PAGE = 50;

    private final AccesListeService acces;
    private final CadeauRepository cadeaux;
    private final PersonneRepository personnes;
    private final TagRepository tags;

    public DashboardService(AccesListeService acces, CadeauRepository cadeaux,
                            PersonneRepository personnes, TagRepository tags) {
        this.acces = acces;
        this.cadeaux = cadeaux;
        this.personnes = personnes;
        this.tags = tags;
    }

    public record Options(List<VueListe.VuePersonne> personnes, List<VueListe.VueTag> tags) {
    }

    /** Destinataires et tags proposables dans le formulaire de cadeau (propriétaire uniquement). */
    public Options options(Long listeId, String login) {
        acces.proprietaire(listeId, login);
        return new Options(
                personnes.findByListeIdOrderByNom(listeId).stream().map(this::vuePersonne).toList(),
                tags.findByListeIdOrderByNom(listeId).stream().map(this::vueTag).toList());
    }

    public VueListe construire(Long listeId, String login, Long tagId, int page) {
        AccesListeService.Acces a = acces.lecture(listeId, login);
        boolean lecteur = !a.proprietaire();

        PageRequest demande = PageRequest.of(Math.max(page, 0), TAILLE_PAGE,
                Sort.by("personne.nom", "nom"));
        Page<Cadeau> resultat = tagId == null
                ? cadeaux.findByListeId(listeId, demande)
                : cadeaux.findByListeIdAndTagsId(listeId, tagId, demande);
        List<VueCadeau> vues = resultat.getContent().stream().map(c -> VueCadeau.depuis(c, lecteur)).toList();

        // Les totaux incluent les cadeaux masqués ; la répartition par statut les exclut pour un lecteur.
        Map<Statut, Long> repartition = new EnumMap<>(Statut.class);
        for (Statut s : Statut.values()) {
            repartition.put(s, 0L);
        }
        for (Object[] ligne : cadeaux.compterParStatut(listeId, !lecteur)) {
            repartition.put((Statut) ligne[0], (Long) ligne[1]);
        }

        List<VueListe.VueTag> vuesTags = tags.findByListeIdOrderByNom(listeId).stream()
                .map(this::vueTag).toList();

        return new VueListe(listeId, a.liste().getNom(), a.liste().getProprietaire().getLogin(), lecteur,
                personnes.sommeBudgets(listeId), cadeaux.sommeDebourse(listeId), cadeaux.sommeResteAPayer(listeId),
                repartition, resultat.getTotalElements(), vuesTags, vues,
                tagId, resultat.getNumber(), Math.max(resultat.getTotalPages(), 1));
    }

    private VueListe.VuePersonne vuePersonne(Personne p) {
        return new VueListe.VuePersonne(p.getId(), p.getNom(), p.getBudget());
    }

    private VueListe.VueTag vueTag(Tag t) {
        return new VueListe.VueTag(t.getId(), t.getNom());
    }
}

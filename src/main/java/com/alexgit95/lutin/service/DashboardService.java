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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    public record LotCadeaux(Long listeId, Long tagActif, boolean lecteur,
                             List<VueCadeau> cadeaux, List<VueListe.VueGroupe> groupes,
                             int page, boolean suiteDisponible) {
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

        Page<Cadeau> resultat = chargerPage(listeId, tagId, page);
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
        List<VueListe.VueGroupe> groupes = grouper(vues);

        return new VueListe(listeId, a.liste().getNom(), a.liste().getProprietaire().getLogin(), lecteur,
                personnes.sommeBudgets(listeId), cadeaux.sommeDebourse(listeId), cadeaux.sommeResteAPayer(listeId),
            repartition, resultat.getTotalElements(), vuesTags, vues, groupes,
                tagId, resultat.getNumber(), Math.max(resultat.getTotalPages(), 1));
    }

    public LotCadeaux chargerLot(Long listeId, String login, Long tagId, int page) {
        AccesListeService.Acces a = acces.lecture(listeId, login);
        boolean lecteur = !a.proprietaire();
        Page<Cadeau> resultat = chargerPage(listeId, tagId, page);
        List<VueCadeau> vues = resultat.getContent().stream().map(c -> VueCadeau.depuis(c, lecteur)).toList();
        return new LotCadeaux(listeId, tagId, lecteur, vues, grouper(vues),
                resultat.getNumber(), resultat.hasNext());
    }

    private List<VueListe.VueGroupe> grouper(List<VueCadeau> cadeaux) {
        List<VueListe.VueGroupe> groupes = new ArrayList<>();
        List<VueCadeau> enCours = new ArrayList<>();
        Long personneId = null;
        String personne = null;
        boolean premier = true;

        for (VueCadeau cadeau : cadeaux) {
            if (!premier && !Objects.equals(personneId, cadeau.personneId())) {
                groupes.add(new VueListe.VueGroupe(personneId, personne, List.copyOf(enCours)));
                enCours = new ArrayList<>();
            }
            if (premier || !Objects.equals(personneId, cadeau.personneId())) {
                personneId = cadeau.personneId();
                personne = cadeau.personne();
            }
            enCours.add(cadeau);
            premier = false;
        }
        if (!premier) {
            groupes.add(new VueListe.VueGroupe(personneId, personne, List.copyOf(enCours)));
        }
        return List.copyOf(groupes);
    }

    private Page<Cadeau> chargerPage(Long listeId, Long tagId, int page) {
        PageRequest demande = PageRequest.of(Math.max(page, 0), TAILLE_PAGE,
                Sort.by("personne.nom", "personne.id", "nom", "id"));
        return tagId == null
                ? cadeaux.findByListeId(listeId, demande)
                : cadeaux.findByListeIdAndTagsId(listeId, tagId, demande);
    }

    private VueListe.VuePersonne vuePersonne(Personne p) {
        return new VueListe.VuePersonne(p.getId(), p.getNom(), p.getBudget());
    }

    private VueListe.VueTag vueTag(Tag t) {
        return new VueListe.VueTag(t.getId(), t.getNom());
    }
}

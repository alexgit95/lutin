package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.ListeAchat;
import com.alexgit95.lutin.domain.Personne;
import com.alexgit95.lutin.repository.CadeauRepository;
import com.alexgit95.lutin.repository.PersonneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional
public class PersonneService {

    private static final BigDecimal BUDGET_MAX = new BigDecimal("10000000000");

    private final PersonneRepository personnes;
    private final CadeauRepository cadeaux;
    private final AccesListeService acces;

    public PersonneService(PersonneRepository personnes, CadeauRepository cadeaux, AccesListeService acces) {
        this.personnes = personnes;
        this.cadeaux = cadeaux;
        this.acces = acces;
    }

    public Personne creer(Long listeId, String login, String nom, BigDecimal budget) {
        ListeAchat liste = acces.proprietaire(listeId, login);
        return personnes.save(new Personne(nomValide(nom), budgetValide(budget), liste));
    }

    @Transactional(readOnly = true)
    public Personne obtenir(Long listeId, Long personneId, String login) {
        acces.proprietaire(listeId, login);
        return dansLaListe(listeId, personneId);
    }

    public void modifier(Long listeId, Long personneId, String login, String nom, BigDecimal budget) {
        acces.proprietaire(listeId, login);
        Personne personne = dansLaListe(listeId, personneId);
        personne.setNom(nomValide(nom));
        personne.setBudget(budgetValide(budget));
    }

    /** Supprime la personne et tous les cadeaux qui lui sont destinés. */
    public void supprimer(Long listeId, Long personneId, String login) {
        acces.proprietaire(listeId, login);
        Personne personne = dansLaListe(listeId, personneId);
        cadeaux.deleteByPersonneId(personneId);
        personnes.delete(personne);
    }

    private Personne dansLaListe(Long listeId, Long personneId) {
        Personne personne = personnes.findById(personneId)
                .orElseThrow(() -> new ElementIntrouvableException("Personne introuvable"));
        if (!personne.getListe().getId().equals(listeId)) {
            throw new ElementIntrouvableException("Personne introuvable");
        }
        return personne;
    }

    private String nomValide(String nom) {
        String propre = nom == null ? "" : nom.trim();
        if (propre.isEmpty() || propre.length() > 100) {
            throw new RequeteInvalideException("Le nom est obligatoire (100 caractères maximum)");
        }
        return propre;
    }

    private BigDecimal budgetValide(BigDecimal budget) {
        if (budget == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal arrondi = budget.setScale(2, RoundingMode.HALF_UP);
        if (arrondi.signum() < 0 || arrondi.compareTo(BUDGET_MAX) >= 0) {
            throw new RequeteInvalideException("Le budget doit être positif");
        }
        return arrondi;
    }
}

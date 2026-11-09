if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js'));
}

function fusionnerGroupesCadeaux() {
    const liste = document.querySelector('#cadeaux');
    if (!liste) return;

    const groupes = Array.from(liste.querySelectorAll(':scope > .groupe-cadeaux'));
    let precedent = null;
    for (const groupe of groupes) {
        if (precedent && precedent.dataset.groupePersonne === groupe.dataset.groupePersonne) {
            precedent.querySelector('tbody').append(...groupe.querySelector('tbody').children);
            groupe.remove();
        } else {
            precedent = groupe;
        }
    }
}

function rendreChargementReessayable(event) {
    const element = event.detail.elt;
    if (!(element instanceof HTMLElement) || !element.classList.contains('chargement-lot')) return;

    element.textContent = 'Chargement impossible. Réessayer';
    element.setAttribute('hx-trigger', 'click');
    htmx.process(element);
}

document.body.addEventListener('htmx:afterSettle', fusionnerGroupesCadeaux);
document.body.addEventListener('htmx:responseError', rendreChargementReessayable);
document.body.addEventListener('htmx:sendError', rendreChargementReessayable);

function afficherErreurHtmx(message) {
    let bandeau = document.querySelector('#bandeau-erreur-htmx');
    if (!bandeau) {
        bandeau = document.createElement('div');
        bandeau.id = 'bandeau-erreur-htmx';
        bandeau.className = 'bandeau-erreur';
        bandeau.setAttribute('role', 'alert');
        const texte = document.createElement('span');
        texte.className = 'bandeau-erreur-texte';
        const fermer = document.createElement('button');
        fermer.type = 'button';
        fermer.textContent = 'Fermer';
        fermer.addEventListener('click', () => bandeau.remove());
        bandeau.append(texte, fermer);
        document.body.prepend(bandeau);
    }
    bandeau.querySelector('.bandeau-erreur-texte').textContent = message;
}

function messageStatutHtmx(statut) {
    if (statut === 401) return 'Session expirée (401), rechargez la page.';
    if (statut === 403) return 'Action refusée (403).';
    if (statut >= 500) return 'Erreur du serveur (' + statut + ').';
    return 'Erreur (' + statut + ').';
}

document.body.addEventListener('htmx:responseError', (event) => afficherErreurHtmx(messageStatutHtmx(event.detail.xhr.status)));
// Une redirection Cloudflare refusée par le navigateur apparaît comme une requête interrompue, sans statut.
document.body.addEventListener('htmx:sendError', () => afficherErreurHtmx('Connexion impossible ou session expirée, rechargez la page.'));

let changementStatut = null;

function mouvementReduit() {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

function lireCompteurs() {
    const compteurs = {};
    document.querySelectorAll('#workflow li[data-statut]').forEach((etape) => {
        compteurs[etape.dataset.statut] = Number(etape.querySelector('.workflow-compteur').dataset.count);
    });
    return compteurs;
}

function memoriserChangementStatut(event) {
    const formulaire = event.detail.elt;
    if (!(formulaire instanceof HTMLElement) || !formulaire.classList.contains('statut-form')) return;

    const ligne = formulaire.closest('tr.cadeau-ligne');
    const choix = formulaire.querySelector('select[name=statut]');
    changementStatut = {
        id: ligne.id, depart: ligne.dataset.statut, arrivee: choix.value, choix, anciens: lireCompteurs()
    };
}

function annulerChangementStatut(event) {
    if (!changementStatut || !event.detail.elt.classList?.contains('statut-form')) return;

    changementStatut.choix.value = changementStatut.depart;
    changementStatut = null;
}

function pulser(element) {
    element.animate([{ transform: 'scale(1)' }, { transform: 'scale(1.6)' }, { transform: 'scale(1)' }],
        { duration: 400, easing: 'ease-out' });
}

function animerChangementStatut() {
    const changement = changementStatut;
    if (!changement) return;
    changementStatut = null;

    const ligne = document.getElementById(changement.id);
    const etapeCible = document.querySelector(`#workflow li[data-statut="${CSS.escape(changement.arrivee)}"]`);
    // La ligne rendue par le serveur fait foi : sans elle, le changement n'est pas confirmé.
    if (!ligne || !etapeCible || ligne.dataset.statut !== changement.arrivee) return;

    const libelle = etapeCible.querySelector('.workflow-libelle').textContent;
    const annonce = document.getElementById('workflow-annonce');
    setTimeout(() => { annonce.textContent = `Cadeau déplacé vers ${libelle}`; }, 50);

    if (mouvementReduit() || !ligne.animate || changement.depart === changement.arrivee) return;

    const compteurDepart = document.querySelector(
        `#workflow li[data-statut="${CSS.escape(changement.depart)}"] .workflow-compteur`);
    const compteurArrivee = etapeCible.querySelector('.workflow-compteur');
    const nouveaux = lireCompteurs();
    compteurDepart.textContent = changement.anciens[changement.depart];
    compteurArrivee.textContent = changement.anciens[changement.arrivee];

    ligne.classList.add('cadeau-maj');
    ligne.addEventListener('animationend', () => ligne.classList.remove('cadeau-maj'), { once: true });

    const source = ligne.getBoundingClientRect();
    const cible = etapeCible.getBoundingClientRect();
    const jeton = document.createElement('span');
    jeton.className = `jeton-cadeau st-${changement.arrivee}`;
    jeton.textContent = ligne.querySelector('.cadeau-nom').textContent;
    jeton.setAttribute('aria-hidden', 'true');
    document.body.append(jeton);

    const depart = `translate(${source.left + 16}px, ${source.top + source.height / 2}px) scale(1)`;
    const arrivee = `translate(${cible.left + cible.width / 2}px, ${cible.top + cible.height / 2}px) scale(.45)`;
    jeton.animate([{ transform: depart, opacity: 1 }, { transform: arrivee, opacity: .35 }],
        { duration: 700, easing: 'ease-in-out' }).finished.then(() => {
        jeton.remove();
        compteurDepart.textContent = nouveaux[changement.depart];
        compteurArrivee.textContent = nouveaux[changement.arrivee];
        pulser(compteurDepart);
        pulser(compteurArrivee);
    });
}

document.body.addEventListener('htmx:beforeRequest', memoriserChangementStatut);
document.body.addEventListener('htmx:afterSettle', animerChangementStatut);
document.body.addEventListener('htmx:responseError', annulerChangementStatut);
document.body.addEventListener('htmx:sendError', annulerChangementStatut);

// Wishlist publique : popup du nom d'invité, partage et copie du lien.
const CLE_NOM_INVITE = 'lutin-nom-invite';

function lireNomInvite() {
    try {
        return localStorage.getItem(CLE_NOM_INVITE) || '';
    } catch (e) {
        return '';
    }
}

function memoriserNomInvite(nom) {
    try {
        localStorage.setItem(CLE_NOM_INVITE, nom);
    } catch (e) {
        // Navigation privée : le nom sera simplement redemandé.
    }
}

function confirmerSurBouton(bouton, message) {
    const libelle = bouton.dataset.libelle || bouton.textContent;
    bouton.dataset.libelle = libelle;
    bouton.textContent = message;
    setTimeout(() => { bouton.textContent = libelle; }, 2000);
}

function copierLien(bouton) {
    const lien = bouton.dataset.lien;
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(lien).then(() => confirmerSurBouton(bouton, 'Lien copié'));
    } else {
        window.prompt('Copiez ce lien :', lien);
    }
}

document.addEventListener('click', (evenement) => {
    const prendre = evenement.target.closest('[data-prendre-url]');
    if (prendre) {
        const dialogue = document.getElementById('dialogue-nom');
        const formulaire = document.getElementById('formulaire-nom');
        formulaire.action = prendre.dataset.prendreUrl;
        formulaire.elements.nom.value = lireNomInvite();
        dialogue.showModal();
        formulaire.elements.nom.focus();
        return;
    }
    if (evenement.target.closest('[data-fermer-dialogue]')) {
        document.getElementById('dialogue-nom').close();
        return;
    }
    const partager = evenement.target.closest('[data-partager]');
    if (partager) {
        if (navigator.share) {
            navigator.share({ title: partager.dataset.titre, url: partager.dataset.lien }).catch(() => {});
        } else {
            copierLien(partager);
        }
        return;
    }
    const copier = evenement.target.closest('[data-copier]');
    if (copier) {
        copierLien(copier);
    }
});

document.addEventListener('submit', (evenement) => {
    if (evenement.target.id === 'formulaire-nom') {
        memoriserNomInvite(evenement.target.elements.nom.value.trim());
    }
});

// Sous-menus d'actions : un seul ouvert à la fois, refermé au clic ailleurs et sur Échap.
function fermerMenus(sauf) {
    document.querySelectorAll('details.menu-actions[open]').forEach((menu) => {
        if (menu !== sauf) {
            menu.removeAttribute('open');
        }
    });
}

document.addEventListener('click', (evenement) => fermerMenus(evenement.target.closest('details.menu-actions')));
document.addEventListener('keydown', (evenement) => {
    if (evenement.key === 'Escape') {
        fermerMenus(null);
    }
});

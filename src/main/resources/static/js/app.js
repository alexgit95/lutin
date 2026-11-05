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

package com.alexgit95.lutin.service;

import com.alexgit95.lutin.domain.ApiKey;
import com.alexgit95.lutin.repository.ApiKeyRepository;
import com.alexgit95.lutin.vue.VueCleApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/** Clés d'API de sauvegarde : la clé en clair n'existe que dans la réponse à la génération. */
@Service
@Transactional
public class CleApiService {

    /** {@code cle} est la valeur en clair, à ne jamais journaliser ni conserver. */
    public record CleGeneree(String nom, String cle) {
    }

    public static final int MAX_CLES = 20;
    public static final int NOM_MAX = 100;
    static final String PREFIXE = "lutin_";
    static final int JETON_MAX = 128;
    private static final int OCTETS = 32;
    private static final SecureRandom ALEA = new SecureRandom();
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ApiKeyRepository cles;
    private final Clock horloge;
    private final ZoneId fuseau;

    public CleApiService(ApiKeyRepository cles, Clock horloge,
                         @Value("${lutin.fuseau-horaire:Europe/Paris}") ZoneId fuseau) {
        this.cles = cles;
        this.horloge = horloge;
        this.fuseau = fuseau;
    }

    public CleGeneree generer(String nomSaisi) {
        String nom = nomValide(nomSaisi);
        if (cles.count() >= MAX_CLES) {
            throw new RequeteInvalideException("Nombre maximal de clés atteint (" + MAX_CLES + "), révoquez-en une");
        }
        if (cles.existsByNomCle(ApiKey.cle(nom))) {
            throw new RequeteInvalideException("Une clé porte déjà ce nom");
        }
        byte[] octets = new byte[OCTETS];
        ALEA.nextBytes(octets);
        String cle = PREFIXE + Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        cles.save(new ApiKey(nom, empreinte(cle), horloge.instant()));
        return new CleGeneree(nom, cle);
    }

    @Transactional(readOnly = true)
    public List<VueCleApi> lister() {
        return cles.findAllByOrderByCreeLeAscIdAsc().stream()
                .map(k -> new VueCleApi(k.getId(), k.getNom(), formater(k.getCreeLe()),
                        k.getDerniereUtilisation() == null ? null : formater(k.getDerniereUtilisation())))
                .toList();
    }

    public void revoquer(Long id) {
        cles.findById(id).ifPresent(cles::delete);
    }

    /** Renvoie le nom de la clé et date son utilisation ; ne modifie rien si le jeton est inconnu ou révoqué. */
    public Optional<String> authentifier(String jeton) {
        if (jeton == null || jeton.isEmpty() || jeton.length() > JETON_MAX) {
            return Optional.empty();
        }
        return cles.findByEmpreinte(empreinte(jeton)).map(k -> {
            cles.marquerUtilisee(k.getId(), horloge.instant());
            return k.getNom();
        });
    }

    static String empreinte(String cle) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cle.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    static String nomValide(String saisi) {
        String propre = saisi == null ? "" : saisi.replaceAll("[\\p{Cntrl}\\p{Cf}]", " ").replaceAll(" +", " ").trim();
        if (propre.isEmpty()) {
            throw new RequeteInvalideException("Le nom de la clé est obligatoire");
        }
        if (propre.length() > NOM_MAX) {
            throw new RequeteInvalideException("Le nom est trop long (" + NOM_MAX + " caractères maximum)");
        }
        return propre;
    }

    private String formater(Instant instant) {
        return FORMAT.format(instant.atZone(fuseau));
    }
}

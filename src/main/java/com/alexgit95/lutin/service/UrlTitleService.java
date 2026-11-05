package com.alexgit95.lutin.service;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Récupère le titre d'une page web saisie par l'utilisateur, avec protection SSRF. */
@Service
public class UrlTitleService {

    @FunctionalInterface
    interface Resolveur {
        InetAddress[] resoudre(String hote) throws UnknownHostException;
    }

    private static final int MAX_OCTETS = 512 * 1024;
    private static final int MAX_REDIRECTIONS = 3;
    private static final int MAX_TITRE = 255;
    private static final Pattern TITRE = Pattern.compile("<title[^>]*>(.*?)</title>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final HttpClient client;
    private final Resolveur resolveur;
    private final Predicate<InetAddress> interdite;

    public UrlTitleService() {
        this(InetAddress::getAllByName, UrlTitleService::estAdresseInterne);
    }

    UrlTitleService(Resolveur resolveur, Predicate<InetAddress> interdite) {
        this.resolveur = resolveur;
        this.interdite = interdite;
        // Redirections suivies à la main pour revalider chaque destination.
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /** Schéma http/https uniquement, sans identifiants : utilisé aussi pour valider l'URL enregistrée. */
    public static boolean estUrlHttpValide(String url) {
        try {
            URI uri = new URI(url.trim());
            return estSchemaAutorise(uri) && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public Optional<String> titre(String url) {
        try {
            URI uri = valider(url);
            for (int i = 0; i <= MAX_REDIRECTIONS; i++) {
                HttpRequest requete = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(5))
                        .header("User-Agent", "Mozilla/5.0 (compatible; Lutin/1.0)")
                        .header("Accept", "text/html")
                        .GET().build();
                HttpResponse<InputStream> reponse = client.send(requete, HttpResponse.BodyHandlers.ofInputStream());
                try (InputStream corps = reponse.body()) {
                    int code = reponse.statusCode();
                    if (code >= 300 && code < 400) {
                        Optional<String> location = reponse.headers().firstValue("Location");
                        if (location.isEmpty()) {
                            return Optional.empty();
                        }
                        uri = valider(uri.resolve(location.get()).toString());
                        continue;
                    }
                    if (code != 200 || !estHtml(reponse)) {
                        return Optional.empty();
                    }
                    String html = new String(corps.readNBytes(MAX_OCTETS), StandardCharsets.UTF_8);
                    return extraire(html);
                }
            }
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException | RuntimeException | URISyntaxException e) {
            return Optional.empty();
        }
    }

    static Optional<String> extraire(String html) {
        Matcher m = TITRE.matcher(html);
        if (!m.find()) {
            return Optional.empty();
        }
        String titre = HtmlUtils.htmlUnescape(m.group(1)).replaceAll("\\s+", " ").trim();
        if (titre.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(titre.length() > MAX_TITRE ? titre.substring(0, MAX_TITRE) : titre);
    }

    private static boolean estHtml(HttpResponse<?> reponse) {
        return reponse.headers().firstValue("Content-Type")
                .map(t -> t.toLowerCase().startsWith("text/html")).orElse(true);
    }

    private static boolean estSchemaAutorise(URI uri) {
        return "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
    }

    private URI valider(String url) throws URISyntaxException, UnknownHostException {
        URI uri = new URI(url.trim());
        if (!estSchemaAutorise(uri) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL non autorisée");
        }
        for (InetAddress adresse : resolveur.resoudre(uri.getHost())) {
            if (interdite.test(adresse)) {
                throw new IllegalArgumentException("Adresse non autorisée");
            }
        }
        return uri;
    }

    static boolean estAdresseInterne(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        if (b.length == 4) {
            int b0 = b[0] & 0xff;
            int b1 = b[1] & 0xff;
            return b0 == 0                                  // 0.0.0.0/8
                    || (b0 == 100 && b1 >= 64 && b1 <= 127) // 100.64.0.0/10 (CGNAT)
                    || (b0 == 198 && (b1 == 18 || b1 == 19)) // 198.18.0.0/15
                    || b0 >= 240;                           // réservé
        }
        return (b[0] & 0xfe) == 0xfc;                       // fc00::/7 (ULA)
    }
}

package com.alexgit95.lutin.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UrlTitleServiceTest {

    private HttpServer serveur;
    private int port;

    // Résolution et filtrage permissifs pour joindre le serveur local, sauf l'adresse 10.0.0.1.
    private final UrlTitleService permissif = new UrlTitleService(
            hote -> new InetAddress[]{hote.equals("10.0.0.1") ? InetAddress.getByAddress(new byte[]{10, 0, 0, 1})
                    : InetAddress.getLoopbackAddress()},
            adresse -> adresse.getHostAddress().equals("10.0.0.1"));

    private final UrlTitleService strict = new UrlTitleService();

    @BeforeEach
    void demarrer() throws IOException {
        serveur = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        port = serveur.getAddress().getPort();
        reponse("/ok", 200, "text/html; charset=utf-8", "<html><head><TITLE>\n  Super &amp; cadeau  </TITLE></head></html>");
        reponse("/sans-titre", 200, "text/html", "<html><body>rien</body></html>");
        reponse("/json", 200, "application/json", "<title>pas html</title>");
        reponse("/erreur", 500, "text/html", "<title>x</title>");
        reponse("/gros", 200, "text/html", "x".repeat(600 * 1024) + "<title>trop loin</title>");
        serveur.createContext("/vers-interne", ex -> {
            ex.getResponseHeaders().add("Location", "http://10.0.0.1/secret");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        serveur.createContext("/vers-ok", ex -> {
            ex.getResponseHeaders().add("Location", "/ok");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        serveur.createContext("/boucle", ex -> {
            ex.getResponseHeaders().add("Location", "/boucle");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        serveur.start();
    }

    @AfterEach
    void arreter() {
        serveur.stop(0);
    }

    private void reponse(String chemin, int code, String type, String corps) {
        serveur.createContext(chemin, ex -> {
            byte[] octets = corps.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", type);
            ex.sendResponseHeaders(code, octets.length);
            ex.getResponseBody().write(octets);
            ex.close();
        });
    }

    private String url(String chemin) {
        return "http://localhost:" + port + chemin;
    }

    @Test
    void recupereEtNettoieLeTitre() {
        assertThat(permissif.titre(url("/ok"))).contains("Super & cadeau");
    }

    @Test
    void suitUneRedirectionVersUneAdresseAutorisee() {
        assertThat(permissif.titre(url("/vers-ok"))).contains("Super & cadeau");
    }

    @Test
    void refuseUneRedirectionVersUneAdresseInterne() {
        assertThat(permissif.titre(url("/vers-interne"))).isEmpty();
    }

    @Test
    void limiteLeNombreDeRedirections() {
        assertThat(permissif.titre(url("/boucle"))).isEmpty();
    }

    @Test
    void ignoreLesReponsesSansTitreNonHtmlOuEnErreur() {
        assertThat(permissif.titre(url("/sans-titre"))).isEmpty();
        assertThat(permissif.titre(url("/json"))).isEmpty();
        assertThat(permissif.titre(url("/erreur"))).isEmpty();
    }

    @Test
    void neLitQuUneTailleLimiteeDeLaPage() {
        assertThat(permissif.titre(url("/gros"))).isEmpty();
    }

    @Test
    void refuseLesSchemasNonHttp() {
        assertThat(strict.titre("file:///etc/passwd")).isEmpty();
        assertThat(strict.titre("ftp://example.com/x")).isEmpty();
        assertThat(strict.titre("javascript:alert(1)")).isEmpty();
    }

    @Test
    void refuseLesAdressesInternes() {
        assertThat(strict.titre("http://127.0.0.1/")).isEmpty();
        assertThat(strict.titre("http://localhost/")).isEmpty();
        assertThat(strict.titre("http://10.1.2.3/")).isEmpty();
        assertThat(strict.titre("http://192.168.0.1/")).isEmpty();
        assertThat(strict.titre("http://169.254.169.254/latest/meta-data")).isEmpty();
        assertThat(strict.titre("http://[::1]/")).isEmpty();
        assertThat(strict.titre("http://[fc00::1]/")).isEmpty();
        assertThat(strict.titre("http://0.0.0.0/")).isEmpty();
    }

    @Test
    void refuseLesIdentifiantsDansLUrl() {
        assertThat(permissif.titre("http://user:pass@localhost:" + port + "/ok")).isEmpty();
    }

    @Test
    void refuseUneUrlMalformee() {
        assertThat(strict.titre("pas une url")).isEmpty();
        assertThat(strict.titre("")).isEmpty();
    }

    @Test
    void classeLesAdressesInternes() throws Exception {
        assertThat(UrlTitleService.estAdresseInterne(InetAddress.getByName("100.64.0.1"))).isTrue();
        assertThat(UrlTitleService.estAdresseInterne(InetAddress.getByName("198.18.0.1"))).isTrue();
        assertThat(UrlTitleService.estAdresseInterne(InetAddress.getByName("240.0.0.1"))).isTrue();
        assertThat(UrlTitleService.estAdresseInterne(InetAddress.getByName("8.8.8.8"))).isFalse();
        assertThat(UrlTitleService.estAdresseInterne(InetAddress.getByName("2606:4700:4700::1111"))).isFalse();
    }

    @Test
    void validePourLEnregistrementSeulementHttpEtHttps() {
        assertThat(UrlTitleService.estUrlHttpValide("https://exemple.fr/a?b=c")).isTrue();
        assertThat(UrlTitleService.estUrlHttpValide("http://exemple.fr")).isTrue();
        assertThat(UrlTitleService.estUrlHttpValide("javascript:alert(1)")).isFalse();
        assertThat(UrlTitleService.estUrlHttpValide("data:text/html,x")).isFalse();
        assertThat(UrlTitleService.estUrlHttpValide("https://u:p@exemple.fr")).isFalse();
        assertThat(UrlTitleService.estUrlHttpValide("n'importe quoi")).isFalse();
    }

    @Test
    void extraitLeTitreEtTronque() {
        assertThat(UrlTitleService.extraire("<title>a</title>")).isEqualTo(Optional.of("a"));
        assertThat(UrlTitleService.extraire("<title>   </title>")).isEmpty();
        assertThat(UrlTitleService.extraire("<title>" + "y".repeat(400) + "</title>").orElseThrow()).hasSize(255);
    }
}

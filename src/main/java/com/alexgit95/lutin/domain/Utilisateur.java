package com.alexgit95.lutin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String login;

    // Pseudo en minuscules : unicité sans tenir compte de la casse, renseigné par setLogin.
    @Column(name = "login_cle", nullable = false, unique = true, length = 50)
    private String loginCle;

    // Absent pour un compte Cloudflare Access.
    @Column(name = "password_hash")
    private String passwordHash;

    // HMAC-SHA256 hexadécimal de l'email normalisé ; l'adresse elle-même n'est jamais conservée.
    @Column(name = "email_hmac", unique = true, length = 64)
    private String emailHmac;

    // Non nul (true) pour l'unique administrateur : la contrainte unique garantit qu'il n'y en a qu'un.
    @Column(name = "admin_marker", unique = true)
    private Boolean adminMarker;

    public Utilisateur(String login, String passwordHash) {
        setLogin(login);
        this.passwordHash = passwordHash;
    }

    public void setLogin(String login) {
        this.login = login;
        this.loginCle = login == null ? null : login.toLowerCase(java.util.Locale.ROOT);
    }

    @Transient
    public boolean isAdmin() {
        return adminMarker != null;
    }

    public void setAdmin(boolean admin) {
        this.adminMarker = admin ? Boolean.TRUE : null;
    }
}

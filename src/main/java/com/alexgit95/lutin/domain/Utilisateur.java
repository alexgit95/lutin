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

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // Non nul (true) pour l'unique administrateur : la contrainte unique garantit qu'il n'y en a qu'un.
    @Column(name = "admin_marker", unique = true)
    private Boolean adminMarker;

    public Utilisateur(String login, String passwordHash) {
        this.login = login;
        this.passwordHash = passwordHash;
    }

    @Transient
    public boolean isAdmin() {
        return adminMarker != null;
    }

    public void setAdmin(boolean admin) {
        this.adminMarker = admin ? Boolean.TRUE : null;
    }
}

package com.alexgit95.lutin.service;

import com.alexgit95.lutin.config.LutinProperties;
import com.alexgit95.lutin.domain.Utilisateur;
import com.alexgit95.lutin.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UtilisateurRepository utilisateurs;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PlatformTransactionManager transactionManager;

    private RegistrationService service;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(utilisateurs, passwordEncoder, new LutinProperties("secret"), transactionManager);
    }

    @Test
    void refuseUneCleInvalide() {
        assertThatThrownBy(() -> service.register("alice", "motdepasse", "mauvaise"))
                .isInstanceOfSatisfying(InscriptionException.class,
                        e -> assertThat(e.getMotif()).isEqualTo(InscriptionException.Motif.CLE_INVALIDE));
        verify(utilisateurs, never()).saveAndFlush(any());
    }

    @Test
    void refuseUneCleNulle() {
        assertThatThrownBy(() -> service.register("alice", "motdepasse", null))
                .isInstanceOf(InscriptionException.class);
    }

    @Test
    void refuseTouteCleSiLaCleConfigureeEstVide() {
        RegistrationService sansCle = new RegistrationService(utilisateurs, passwordEncoder, new LutinProperties(""), transactionManager);
        assertThatThrownBy(() -> sansCle.register("alice", "motdepasse", ""))
                .isInstanceOf(InscriptionException.class);
    }

    @Test
    void refuseUnLoginDejaUtilise() {
        when(utilisateurs.existsByLogin("alice")).thenReturn(true);

        assertThatThrownBy(() -> service.register("alice", "motdepasse", "secret"))
                .isInstanceOfSatisfying(InscriptionException.class,
                        e -> assertThat(e.getMotif()).isEqualTo(InscriptionException.Motif.LOGIN_UTILISE));
    }

    @Test
    void lePremierInscritEstAdministrateurEtLeMotDePasseEstHache() {
        when(passwordEncoder.encode("motdepasse")).thenReturn("HASH");
        when(utilisateurs.count()).thenReturn(0L);
        when(utilisateurs.saveAndFlush(any(Utilisateur.class))).thenAnswer(i -> i.getArgument(0));

        Utilisateur cree = service.register("  alice  ", "motdepasse", "secret");

        assertThat(cree.isAdmin()).isTrue();
        assertThat(cree.getLogin()).isEqualTo("alice");
        assertThat(cree.getPasswordHash()).isEqualTo("HASH");
    }

    @Test
    void lesInscritsSuivantsNeSontPasAdministrateurs() {
        when(passwordEncoder.encode("motdepasse")).thenReturn("HASH");
        when(utilisateurs.count()).thenReturn(1L);
        when(utilisateurs.saveAndFlush(any(Utilisateur.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.register("bob", "motdepasse", "secret").isAdmin()).isFalse();
    }

    @Test
    void reessaieSansRoleAdminQuandUneInscriptionConcurrenteLaPris() {
        when(passwordEncoder.encode("motdepasse")).thenReturn("HASH");
        when(utilisateurs.count()).thenReturn(0L);
        when(utilisateurs.saveAndFlush(any(Utilisateur.class)))
                .thenThrow(new DataIntegrityViolationException("admin_marker"))
                .thenAnswer(i -> i.getArgument(0));

        Utilisateur cree = service.register("bob", "motdepasse", "secret");

        assertThat(cree.isAdmin()).isFalse();
    }

    @Test
    void loginDupliqueDetecteParLaContrainteUnique() {
        when(passwordEncoder.encode("motdepasse")).thenReturn("HASH");
        when(utilisateurs.count()).thenReturn(1L);
        when(utilisateurs.existsByLogin("bob")).thenReturn(false, true);
        when(utilisateurs.saveAndFlush(any(Utilisateur.class))).thenThrow(new DataIntegrityViolationException("login"));

        assertThatThrownBy(() -> service.register("bob", "motdepasse", "secret"))
                .isInstanceOfSatisfying(InscriptionException.class,
                        e -> assertThat(e.getMotif()).isEqualTo(InscriptionException.Motif.LOGIN_UTILISE));
    }
}

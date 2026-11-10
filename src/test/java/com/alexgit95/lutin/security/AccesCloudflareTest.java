package com.alexgit95.lutin.security;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/** Application complète en mode cloudflare sur la base de test ; contexte partagé entre les classes de test. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "lutin.acces.mode=cloudflare",
        "lutin.cloudflare.team-domain=" + JetonsCloudflare.EQUIPE,
        "lutin.cloudflare.audience=" + JetonsCloudflare.AUDIENCE,
        "lutin.cloudflare.email-hmac-secret=" + JetonsCloudflare.SECRET})
@Import(JetonsCloudflareConfig.class)
public @interface AccesCloudflareTest {
}

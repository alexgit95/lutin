package com.alexgit95.lutin.security;

import com.alexgit95.lutin.service.CleApiService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;

/** Chaîne de l'API de sauvegarde, commune aux deux modes : clé d'API seule, ni session, ni CSRF, ni Cloudflare Access. */
@Configuration
public class SecurityApiConfig {

    @Bean
    @Order(1)
    SecurityFilterChain apiFilterChain(HttpSecurity http, CleApiService cles) throws Exception {
        http
                .securityMatcher("/api/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(contexte -> contexte.securityContextRepository(
                        new RequestAttributeSecurityContextRepository()))
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(new CleApiFilter(cles), AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/sauvegarde").hasAuthority(CleApiFilter.ROLE)
                        .anyRequest().denyAll())
                .exceptionHandling(erreurs -> erreurs
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable());
        return http.build();
    }
}

package com.alexgit95.lutin.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Actif seulement quand l'accès est assuré par Cloudflare Access (profil prod). */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ConditionalOnProperty(name = "lutin.acces.mode", havingValue = "cloudflare")
public @interface ModeCloudflare {
}

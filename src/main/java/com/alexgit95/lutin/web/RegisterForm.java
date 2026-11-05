package com.alexgit95.lutin.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterForm {

    @NotBlank(message = "Le login est obligatoire")
    @Size(max = 50, message = "50 caractères maximum")
    @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "Lettres, chiffres, point, tiret et underscore uniquement")
    private String login;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, max = 72, message = "Entre 8 et 72 caractères")
    private String password;

    @NotBlank(message = "La clé d'invitation est obligatoire")
    private String invitationKey;
}

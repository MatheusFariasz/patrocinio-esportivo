package br.ifsp.edu.scl.patrocinioesportivo.security.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record RegisterUserRequest(
        @Schema(description = "Name", example = "John")
        @NotBlank String name,
        @Schema(description = "Lastname", example = "Snow")
        @NotBlank String lastname,
        @Schema(description = "Email to be used as login", example = "know.nothing@snow.com")
        @NotBlank @Email String email,
        @Schema(description = "Password", example = "n3243#kFdj$")
        @NotBlank String password
) {}

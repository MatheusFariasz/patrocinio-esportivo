package br.ifsp.edu.scl.patrocinioesportivo.security.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

public record AuthRequest(
        @Schema(description = "Email to be used as login", example = "know.nothing@snow.com")
        @NotBlank @Email String username,
        @Schema(description = "Password", example = "n3243#kFdj$")
        @NotBlank String password
) { }

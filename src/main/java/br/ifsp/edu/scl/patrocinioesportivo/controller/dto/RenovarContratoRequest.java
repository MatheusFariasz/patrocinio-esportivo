package br.ifsp.edu.scl.patrocinioesportivo.controller.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RenovarContratoRequest(
        @NotNull @Positive Integer duracaoMeses,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal novaMeta
) {}

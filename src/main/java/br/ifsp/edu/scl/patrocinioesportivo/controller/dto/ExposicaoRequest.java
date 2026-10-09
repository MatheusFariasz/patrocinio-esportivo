package br.ifsp.edu.scl.patrocinioesportivo.controller.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ExposicaoRequest(
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal valor
) {}

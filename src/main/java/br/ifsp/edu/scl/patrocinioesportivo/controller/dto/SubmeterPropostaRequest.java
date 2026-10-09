package br.ifsp.edu.scl.patrocinioesportivo.controller.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SubmeterPropostaRequest(
        @NotNull @Positive Long clubeId,
        @NotNull @Positive Long patrocinadorId,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal valor,
        @NotNull LocalDate inicio,
        @NotNull LocalDate termino,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal meta
) {}

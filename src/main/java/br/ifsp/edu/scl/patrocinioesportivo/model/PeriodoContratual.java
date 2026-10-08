package br.ifsp.edu.scl.patrocinioesportivo.model;

import br.ifsp.edu.scl.patrocinioesportivo.exception.PeriodoInvalidoError;

import java.time.LocalDate;

public record PeriodoContratual(LocalDate inicio, LocalDate termino) {

    public PeriodoContratual {
        if (inicio == null || termino == null
                || !termino.isAfter(inicio)) {
            throw new PeriodoInvalidoError("O período contratual é inválido.");
        }
    }

    public PeriodoContratual comInicio(LocalDate novoInicio) {
        return new PeriodoContratual(novoInicio, termino);
    }

    public PeriodoContratual comTermino(LocalDate novoTermino) {
        return new PeriodoContratual(inicio, novoTermino);
    }
}

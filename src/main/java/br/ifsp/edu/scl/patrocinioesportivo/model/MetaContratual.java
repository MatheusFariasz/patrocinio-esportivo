package br.ifsp.edu.scl.patrocinioesportivo.model;

import br.ifsp.edu.scl.patrocinioesportivo.exception.MetaInvalidaError;
import br.ifsp.edu.scl.patrocinioesportivo.exception.MetaObrigatoriaError;

import java.math.BigDecimal;

public record MetaContratual(BigDecimal valor) {

    public MetaContratual {
        if (valor == null) {
            throw new MetaObrigatoriaError("A meta contratual é obrigatória.");
        }

        if (valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new MetaInvalidaError("A meta contratual deve ser maior que zero.");
        }

        valor = valor.stripTrailingZeros();
    }
}

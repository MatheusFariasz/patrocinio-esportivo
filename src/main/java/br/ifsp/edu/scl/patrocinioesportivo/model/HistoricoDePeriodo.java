package br.ifsp.edu.scl.patrocinioesportivo.model;

import java.math.BigDecimal;
import java.util.List;

public record HistoricoDePeriodo(
        PeriodoContratual periodo,
        MetaContratual meta,
        BigDecimal exposicaoAcumulada,
        List<ParcelaDePagamento> parcelas
) {
    public HistoricoDePeriodo {
        parcelas = List.copyOf(parcelas);
    }
}

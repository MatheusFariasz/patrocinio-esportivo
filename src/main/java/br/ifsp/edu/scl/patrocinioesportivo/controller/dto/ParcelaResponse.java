package br.ifsp.edu.scl.patrocinioesportivo.controller.dto;

import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelaResponse(int numero, BigDecimal valor, LocalDate vencimento,
                              boolean paga, LocalDate dataPagamento, boolean emAtraso) {
    public static ParcelaResponse de(ParcelaDePagamento parcela) {
        return new ParcelaResponse(parcela.getNumero(), parcela.getValor(), parcela.getVencimento(),
                parcela.isPaga(), parcela.getDataPagamento(), parcela.isEmAtraso(LocalDate.now()));
    }
}

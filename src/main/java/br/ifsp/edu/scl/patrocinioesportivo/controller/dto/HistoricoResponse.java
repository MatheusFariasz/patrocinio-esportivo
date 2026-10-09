package br.ifsp.edu.scl.patrocinioesportivo.controller.dto;

import br.ifsp.edu.scl.patrocinioesportivo.model.HistoricoDePeriodo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record HistoricoResponse(LocalDate inicio, LocalDate termino, BigDecimal meta,
                                BigDecimal exposicaoAcumulada, List<ParcelaResponse> parcelas) {
    public static HistoricoResponse de(HistoricoDePeriodo historico) {
        return new HistoricoResponse(historico.periodo().inicio(), historico.periodo().termino(),
                historico.meta().valor(), historico.exposicaoAcumulada(),
                historico.parcelas().stream().map(ParcelaResponse::de).toList());
    }
}

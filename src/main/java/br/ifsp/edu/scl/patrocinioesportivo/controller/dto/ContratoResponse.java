package br.ifsp.edu.scl.patrocinioesportivo.controller.dto;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ContratoResponse(Long id, Long clubeId, Long patrocinadorId, StatusContrato status,
                               LocalDate inicio, LocalDate termino, BigDecimal meta, BigDecimal valorTotal,
                               BigDecimal exposicaoAcumulada, BigDecimal multaRescisoria,
                               List<ParcelaResponse> parcelas, List<HistoricoResponse> historico) {
    public static ContratoResponse de(ContratoDePatrocinio contrato) {
        return new ContratoResponse(contrato.getId(), contrato.getClubeId(), contrato.getPatrocinadorId(),
                contrato.getStatus(), contrato.getPeriodoContratual().inicio(), contrato.getPeriodoContratual().termino(),
                contrato.getMetaContratual().valor(), contrato.getValorTotal(), contrato.getExposicaoAcumulada(),
                contrato.getMultaRescisoria(), contrato.getParcelas().stream().map(ParcelaResponse::de).toList(),
                contrato.getHistorico().stream().map(HistoricoResponse::de).toList());
    }
}

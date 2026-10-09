package br.ifsp.edu.scl.patrocinioesportivo.repository.jdbc;

import br.ifsp.edu.scl.patrocinioesportivo.exception.ContratoInexistenteError;
import br.ifsp.edu.scl.patrocinioesportivo.model.*;
import br.ifsp.edu.scl.patrocinioesportivo.repository.ContratoDePatrocinioRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcContratoDePatrocinioRepository implements ContratoDePatrocinioRepository {
    private final JdbcTemplate jdbc;

    public JdbcContratoDePatrocinioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ContratoDePatrocinio> buscarPorId(Long id) {
        return jdbc.query("SELECT * FROM contrato_patrocinio WHERE id = ?", (rs, row) ->
                ContratoDePatrocinio.reconstituir(rs.getLong("id"),
                        StatusContrato.valueOf(rs.getString("status")),
                        new PeriodoContratual(LocalDate.parse(rs.getString("inicio")),
                                LocalDate.parse(rs.getString("termino"))),
                        new MetaContratual(new BigDecimal(rs.getString("meta"))),
                        new BigDecimal(rs.getString("valor_total")), rs.getLong("clube_id"),
                        rs.getLong("patrocinador_id"), new BigDecimal(rs.getString("exposicao_acumulada")),
                        new BigDecimal(rs.getString("multa_rescisoria")),
                        buscarParcelas(id, 0), buscarHistorico(id)), id).stream().findFirst();
    }

    private List<ParcelaDePagamento> buscarParcelas(Long contratoId, int ciclo) {
        return jdbc.query("SELECT * FROM parcela_pagamento WHERE contrato_id = ? AND ciclo = ? ORDER BY numero",
                (rs, row) -> ParcelaDePagamento.reconstituir(rs.getInt("numero"),
                        new BigDecimal(rs.getString("valor")), date(rs.getString("vencimento")),
                        rs.getBoolean("paga"), date(rs.getString("data_pagamento"))), contratoId, ciclo);
    }

    private List<HistoricoDePeriodo> buscarHistorico(Long contratoId) {
        return jdbc.query("SELECT * FROM historico_periodo WHERE contrato_id = ? ORDER BY ciclo",
                (rs, row) -> new HistoricoDePeriodo(
                        new PeriodoContratual(LocalDate.parse(rs.getString("inicio")),
                                LocalDate.parse(rs.getString("termino"))),
                        new MetaContratual(new BigDecimal(rs.getString("meta"))),
                        new BigDecimal(rs.getString("exposicao_acumulada")),
                        buscarParcelas(contratoId, rs.getInt("ciclo"))), contratoId);
    }

    @Override
    @Transactional
    public ContratoDePatrocinio salvar(ContratoDePatrocinio contrato) {
        Object[] values = {contrato.getClubeId(), contrato.getPatrocinadorId(), contrato.getStatus().name(),
                contrato.getPeriodoContratual().inicio().toString(), contrato.getPeriodoContratual().termino().toString(),
                contrato.getMetaContratual().valor().toPlainString(), contrato.getValorTotal().toPlainString(),
                contrato.getExposicaoAcumulada().toPlainString(), contrato.getMultaRescisoria().toPlainString()};
        Long id = contrato.getId();
        if (id == null) {
            id = jdbc.queryForObject("""
                    INSERT INTO contrato_patrocinio
                    (clube_id, patrocinador_id, status, inicio, termino, meta, valor_total, exposicao_acumulada, multa_rescisoria)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, values);
        } else {
            int updated = jdbc.update("""
                    UPDATE contrato_patrocinio SET clube_id = ?, patrocinador_id = ?, status = ?,
                    inicio = ?, termino = ?, meta = ?, valor_total = ?, exposicao_acumulada = ?, multa_rescisoria = ?
                    WHERE id = ?
                    """, values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8], id);
            if (updated == 0) {
                throw new ContratoInexistenteError("Contrato não encontrado.");
            }
        }

        jdbc.update("DELETE FROM parcela_pagamento WHERE contrato_id = ?", id);
        jdbc.update("DELETE FROM historico_periodo WHERE contrato_id = ?", id);
        salvarParcelas(id, 0, contrato.getParcelas());
        int ciclo = 1;
        for (HistoricoDePeriodo historico : contrato.getHistorico()) {
            jdbc.update("""
                    INSERT INTO historico_periodo (contrato_id, ciclo, inicio, termino, meta, exposicao_acumulada)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, ciclo, historico.periodo().inicio().toString(), historico.periodo().termino().toString(),
                    historico.meta().valor().toPlainString(), historico.exposicaoAcumulada().toPlainString());
            salvarParcelas(id, ciclo++, historico.parcelas());
        }
        contrato.definirId(id);
        return contrato;
    }

    private void salvarParcelas(Long contratoId, int ciclo, List<ParcelaDePagamento> parcelas) {
        for (ParcelaDePagamento parcela : parcelas) {
            jdbc.update("""
                    INSERT INTO parcela_pagamento (contrato_id, ciclo, numero, valor, vencimento, paga, data_pagamento)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, contratoId, ciclo, parcela.getNumero(), parcela.getValor().toPlainString(),
                    text(parcela.getVencimento()), parcela.isPaga(), text(parcela.getDataPagamento()));
        }
    }

    private static LocalDate date(String value) {
        return value == null ? null : LocalDate.parse(value);
    }

    private static String text(LocalDate value) {
        return value == null ? null : value.toString();
    }
}

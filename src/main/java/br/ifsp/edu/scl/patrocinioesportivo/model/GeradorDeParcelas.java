package br.ifsp.edu.scl.patrocinioesportivo.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class GeradorDeParcelas {

    private GeradorDeParcelas() {
    }

    public static List<ParcelaDePagamento> gerar(
            BigDecimal valorTotal,
            PeriodoContratual periodo
    ) {
        long quantidade = Math.max(1, ChronoUnit.MONTHS.between(
                periodo.inicio(),
                periodo.termino()
        ));

        BigDecimal valorDaParcela = valorTotal.divide(
                BigDecimal.valueOf(quantidade), 2, RoundingMode.DOWN
        );

        BigDecimal somaDasAnteriores = valorDaParcela.multiply(
                BigDecimal.valueOf(quantidade - 1)
        );

        List<ParcelaDePagamento> parcelas = new ArrayList<>();

        for (int numero = 1; numero <= quantidade; numero++) {
            BigDecimal valor = numero == quantidade
                    ? valorTotal.subtract(somaDasAnteriores)
                    : valorDaParcela;

            LocalDate vencimento = periodo.inicio().plusMonths(numero);

            if (vencimento.isAfter(periodo.termino())) {
                vencimento = periodo.termino();
            }

            parcelas.add(new ParcelaDePagamento(numero, valor, vencimento));
        }

        return parcelas;
    }
}
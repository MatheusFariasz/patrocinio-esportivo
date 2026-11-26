package br.ifsp.edu.scl.patrocinioesportivo.support;

import br.ifsp.edu.scl.patrocinioesportivo.model.ContratoDePatrocinio;
import br.ifsp.edu.scl.patrocinioesportivo.model.HistoricoDePeriodo;
import br.ifsp.edu.scl.patrocinioesportivo.model.MetaContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.ParcelaDePagamento;
import br.ifsp.edu.scl.patrocinioesportivo.model.PeriodoContratual;
import br.ifsp.edu.scl.patrocinioesportivo.model.StatusContrato;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ContratoTestFixtures {
    public static final long CLUBE_DEMO_ID = 10L;
    public static final long PATROCINADOR_DEMO_ID = 20L;

    private ContratoTestFixtures() {}

    public static DataSource createInitializedSqliteDataSource() {
        var dataSource = new DriverManagerDataSource(SqliteTestDatabase.create() + "?foreign_keys=on");
        new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"),
                new ClassPathResource("data-demo.sql")
        ).execute(dataSource);
        return dataSource;
    }

    public static ContratoDePatrocinio contratoPendente(BigDecimal valorTotal, BigDecimal meta) {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate termino = LocalDate.of(2026, 4, 1);
        return new ContratoDePatrocinio(
                StatusContrato.PENDENTE,
                new PeriodoContratual(inicio, termino),
                new MetaContratual(meta),
                valorTotal,
                CLUBE_DEMO_ID,
                PATROCINADOR_DEMO_ID
        );
    }

    public static ContratoDePatrocinio contratoAtivoComParcelas(
            Long id,
            List<ParcelaDePagamento> parcelas,
            List<HistoricoDePeriodo> historico
    ) {
        LocalDate inicio = LocalDate.of(2026, 5, 1);
        LocalDate termino = LocalDate.of(2026, 8, 1);
        return ContratoDePatrocinio.reconstituir(
                id,
                StatusContrato.ATIVO,
                new PeriodoContratual(inicio, termino),
                new MetaContratual(new BigDecimal("500.00")),
                new BigDecimal("12000.00"),
                CLUBE_DEMO_ID,
                PATROCINADOR_DEMO_ID,
                new BigDecimal("125.50"),
                BigDecimal.ZERO,
                parcelas,
                historico
        );
    }
}

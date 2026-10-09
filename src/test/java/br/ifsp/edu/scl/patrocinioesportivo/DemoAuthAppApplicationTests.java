package br.ifsp.edu.scl.patrocinioesportivo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import br.ifsp.edu.scl.patrocinioesportivo.support.SqliteTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("IntegrationTest")
@SpringBootTest
class DemoAuthAppApplicationTests {
    private static final String DATABASE_URL = SqliteTestDatabase.create();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> DATABASE_URL);
    }


    @Test
    void contextLoads() {
    }

}

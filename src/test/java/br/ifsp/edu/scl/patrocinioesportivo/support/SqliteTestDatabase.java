package br.ifsp.edu.scl.patrocinioesportivo.support;

import java.io.IOException;
import java.nio.file.Files;

public final class SqliteTestDatabase {
    private SqliteTestDatabase() {}

    public static String create() {
        try {
            var file = Files.createTempFile("patrocinio-integracao-", ".db");
            file.toFile().deleteOnExit();
            return "jdbc:sqlite:" + file.toAbsolutePath();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o banco temporário", e);
        }
    }
}

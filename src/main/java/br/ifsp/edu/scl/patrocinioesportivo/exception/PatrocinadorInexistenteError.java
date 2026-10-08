package br.ifsp.edu.scl.patrocinioesportivo.exception;

public class PatrocinadorInexistenteError extends RuntimeException {

    public PatrocinadorInexistenteError(String message) {
        super(message);
    }
}

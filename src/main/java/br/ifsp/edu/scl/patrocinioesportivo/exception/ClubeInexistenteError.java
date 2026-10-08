package br.ifsp.edu.scl.patrocinioesportivo.exception;

public class ClubeInexistenteError extends RuntimeException {

    public ClubeInexistenteError(String message) {
        super(message);
    }
}

package br.ifsp.edu.scl.patrocinioesportivo.exception;

public class EntityAlreadyExistsException extends RuntimeException {
    public EntityAlreadyExistsException(String message) {
        super(message);
    }
}

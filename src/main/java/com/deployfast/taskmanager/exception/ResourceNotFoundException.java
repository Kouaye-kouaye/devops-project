package com.deployfast.taskmanager.exception;
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " avec l'identifiant " + id + " est introuvable.");
    }
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

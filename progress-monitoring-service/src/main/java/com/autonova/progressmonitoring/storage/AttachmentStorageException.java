package com.autonova.progressmonitoring.storage;

/**
 * Unchecked exception used to indicate storage failures in {@link AttachmentStorage} implementations.
 */
public class AttachmentStorageException extends RuntimeException {
    public AttachmentStorageException(String message) { super(message); }
    public AttachmentStorageException(String message, Throwable cause) { super(message, cause); }
}


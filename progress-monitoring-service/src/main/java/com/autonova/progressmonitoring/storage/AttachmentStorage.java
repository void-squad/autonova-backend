package com.autonova.progressmonitoring.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction for storing binary attachments (e.g. images) and returning their metadata.
 * <p>Implementations MUST either:
 * <ul>
 *   <li>Wrap any {@link java.io.IOException} or environment related failures (disk full, permission denied)
 *       in an unchecked {@link com.autonova.progressmonitoring.storage.AttachmentStorageException}, OR</li>
 *   <li>Convert the error into a domain specific unchecked exception.</li>
 * </ul>
 * This interface intentionally does not declare checked exceptions to keep calling code simple.
 * Callers are expected to handle {@link com.autonova.progressmonitoring.storage.AttachmentStorageException} when they need to surface
 * a user-friendly error message or fallback behavior.
 */
public interface AttachmentStorage {
    /**
     * Store the given multipart file and return immutable metadata describing the stored attachment.
     * @param file multipart file to persist (must not be null or empty)
     * @return metadata of stored attachment (never null)
     * @throws IllegalArgumentException if file is null or empty
     * @throws com.autonova.progressmonitoring.storage.AttachmentStorageException wrapping low-level storage failures
     */
    StoredAttachment store(MultipartFile file);
}

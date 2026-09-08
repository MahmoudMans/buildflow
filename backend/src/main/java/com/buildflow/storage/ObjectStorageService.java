package com.buildflow.storage;

public interface ObjectStorageService {

    void put(
        String objectKey,
        String mimeType,
        byte[] content
    );

    StoredObject get(
        String objectKey
    );

    void delete(
        String objectKey
    );

    record StoredObject(
        byte[] content,
        String mimeType
    ) {
    }
}

package com.buildflow.storage;

import com.buildflow.shared.error.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class MinioObjectStorageService
    implements ObjectStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioObjectStorageService(
        @Value(
            "${buildflow.storage.endpoint:http://localhost:9000}"
        )
        String endpoint,

        @Value(
            "${buildflow.storage.access-key:buildflow-local}"
        )
        String accessKey,

        @Value(
            "${buildflow.storage.secret-key:buildflow-local-secret}"
        )
        String secretKey,

        @Value(
            "${buildflow.storage.bucket:buildflow}"
        )
        String bucket
    ) {
        this.minioClient =
            MinioClient.builder()
                .endpoint(endpoint)
                .credentials(
                    accessKey,
                    secretKey
                )
                .build();

        this.bucket =
            bucket;
    }

    @Override
    public void put(
        String objectKey,
        String mimeType,
        byte[] content
    ) {
        try {
            ensureBucket();

            try (
                ByteArrayInputStream input =
                    new ByteArrayInputStream(
                        content
                    )
            ) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .contentType(mimeType)
                        .stream(
                            input,
                            content.length,
                            -1
                        )
                        .build()
                );
            }
        } catch (Exception exception) {
            throw storageFailure(
                "Could not store the file.",
                exception
            );
        }
    }

    @Override
    public StoredObject get(
        String objectKey
    ) {
        try {
            ensureBucket();

            try (
                var stream =
                    minioClient.getObject(
                        GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
                    )
            ) {
                return new StoredObject(
                    stream.readAllBytes(),
                    null
                );
            }
        } catch (Exception exception) {
            throw storageFailure(
                "Could not read the file.",
                exception
            );
        }
    }

    @Override
    public void delete(
        String objectKey
    ) {
        try {
            ensureBucket();

            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build()
            );
        } catch (Exception exception) {
            throw storageFailure(
                "Could not remove the file.",
                exception
            );
        }
    }

    private void ensureBucket()
        throws Exception {

        boolean exists =
            minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build()
            );

        if (!exists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build()
            );
        }
    }

    private BusinessException storageFailure(
        String message,
        Exception exception
    ) {
        return new BusinessException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "OBJECT_STORAGE_FAILURE",
            message
                + " "
                + exception.getClass()
                    .getSimpleName()
        );
    }
}

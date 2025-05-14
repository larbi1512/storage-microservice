package com.example.storage_service.service;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    // Upload a file to MinIO
    public String uploadFile(MultipartFile file) throws Exception {
        String key = file.getOriginalFilename();

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .contentType(file.getContentType())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .build());
        } catch (Exception e) {
            throw new Exception("Error uploading file to MinIO", e);
        }

        // Manually build the URL for MinIO (replace with your actual MinIO endpoint)
        String endpoint = "http://localhost:9000"; // Update this to your MinIO server URL
        return String.format("%s/%s/%s", endpoint, bucket, key);
    }

    // List all files in the MinIO bucket
    public List<String> listFiles() {
        List<String> files = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder().bucket(bucket).build());
            for (Result<Item> result : results) {
                files.add(result.get().objectName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error listing files in MinIO bucket", e);
        }
        return files;
    }

    // Download a file from MinIO
    public byte[] downloadFile(String key) throws IOException {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build())) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error downloading file from MinIO", e);
        }
    }
}
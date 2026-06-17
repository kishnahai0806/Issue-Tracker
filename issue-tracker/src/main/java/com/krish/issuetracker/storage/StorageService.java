package com.krish.issuetracker.storage;

import java.io.IOException;
import java.time.Duration;

import com.krish.issuetracker.exception.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@Slf4j
public class StorageService {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final StorageProperties storageProperties;

	public StorageService(S3Client s3Client, S3Presigner s3Presigner, StorageProperties storageProperties) {
		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.storageProperties = storageProperties;
	}

	public void uploadFile(MultipartFile file, String storageKey) {
		try {
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(storageProperties.bucketName())
					.key(storageKey)
					.contentType(file.getContentType())
					.build();
			s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
		} catch (IOException | S3Exception ex) {
			throw new StorageException("Failed to upload file: " + storageKey, ex);
		}
	}

	public String generatePresignedUrl(String storageKey) {
		try {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(storageProperties.bucketName())
					.key(storageKey)
					.responseContentDisposition("attachment; filename=\"" + extractFilename(storageKey) + "\"")
					.build();
			GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
					.signatureDuration(Duration.ofMinutes(storageProperties.presignedUrlExpiryMinutes()))
					.getObjectRequest(getObjectRequest)
					.build();

			return s3Presigner.presignGetObject(presignRequest).url().toString();
		} catch (S3Exception ex) {
			throw new StorageException("Failed to generate presigned URL: " + storageKey, ex);
		}
	}

	public void deleteFile(String storageKey) {
		try {
			DeleteObjectRequest request = DeleteObjectRequest.builder()
					.bucket(storageProperties.bucketName())
					.key(storageKey)
					.build();
			s3Client.deleteObject(request);
		} catch (S3Exception ex) {
			log.warn("Failed to delete stored file {}", storageKey, ex);
		}
	}

	private String extractFilename(String storageKey) {
		int index = storageKey.lastIndexOf('/');
		if (index < 0 || index == storageKey.length() - 1) {
			return storageKey;
		}
		return storageKey.substring(index + 1);
	}
}

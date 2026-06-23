package com.krish.issuetracker.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import com.krish.issuetracker.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

	@Mock
	private S3Client s3Client;

	@Mock
	private S3Presigner s3Presigner;

	@Mock
	private StorageProperties storageProperties;

	@Mock
	private PresignedGetObjectRequest presignedGetObjectRequest;

	@InjectMocks
	private StorageService storageService;

	@Test
	void uploadFile_shouldUploadSuccessfully_whenS3Succeeds() {
		MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[] {0x25, 0x50});
		when(storageProperties.bucketName()).thenReturn("test-bucket");
		when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
				.thenReturn(PutObjectResponse.builder().build());

		assertThatNoException().isThrownBy(() -> storageService.uploadFile(file, "attachments/key/test.pdf"));

		verify(s3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
	}

	@Test
	void uploadFile_shouldThrowStorageException_whenS3ExceptionOccurs() {
		MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[] {0x25, 0x50});
		when(storageProperties.bucketName()).thenReturn("test-bucket");
		when(s3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
				.thenThrow(S3Exception.builder().message("S3 unavailable").build());

		assertThatThrownBy(() -> storageService.uploadFile(file, "attachments/key/test.pdf"))
				.isInstanceOf(StorageException.class);
	}

	@Test
	void generatePresignedUrl_shouldReturnUrl_whenS3Succeeds() throws Exception {
		when(storageProperties.bucketName()).thenReturn("test-bucket");
		when(storageProperties.presignedUrlExpiryMinutes()).thenReturn(60L);
		when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);
		when(presignedGetObjectRequest.url()).thenReturn(URI.create("https://example.com/file.pdf").toURL());

		String url = storageService.generatePresignedUrl("attachments/key/file.pdf");

		assertThat(url).isEqualTo("https://example.com/file.pdf");
	}

	@Test
	void generatePresignedUrl_shouldThrowStorageException_whenS3ExceptionOccurs() {
		when(storageProperties.bucketName()).thenReturn("test-bucket");
		when(storageProperties.presignedUrlExpiryMinutes()).thenReturn(60L);
		when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
				.thenThrow(S3Exception.builder().message("S3 unavailable").build());

		assertThatThrownBy(() -> storageService.generatePresignedUrl("attachments/key/file.pdf"))
				.isInstanceOf(StorageException.class);
	}

	@Test
	void generatePresignedUrl_shouldSetContentDispositionHeader_whenBuildingRequest() throws Exception {
		when(storageProperties.bucketName()).thenReturn("test-bucket");
		when(storageProperties.presignedUrlExpiryMinutes()).thenReturn(60L);
		when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);
		when(presignedGetObjectRequest.url()).thenReturn(URI.create("https://example.com/file.pdf").toURL());
		ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);

		storageService.generatePresignedUrl("attachments/key/file.pdf");

		verify(s3Presigner).presignGetObject(captor.capture());
		GetObjectRequest capturedRequest = captor.getValue().getObjectRequest();
		assertThat(capturedRequest.responseContentDisposition()).isEqualTo("attachment; filename=\"file.pdf\"");
	}

	@Test
	void deleteFile_shouldDeleteSuccessfully_whenS3Succeeds() {
		when(storageProperties.bucketName()).thenReturn("test-bucket");
		when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

		assertThatNoException().isThrownBy(() -> storageService.deleteFile("attachments/key/file.pdf"));

		verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
	}

	@Test
	void deleteFile_shouldSwallowException_whenS3ExceptionOccurs() {
		when(storageProperties.bucketName()).thenReturn("test-bucket");
		when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
				.thenThrow(S3Exception.builder().message("S3 unavailable").build());

		assertThatNoException().isThrownBy(() -> storageService.deleteFile("attachments/key/file.pdf"));
	}
}

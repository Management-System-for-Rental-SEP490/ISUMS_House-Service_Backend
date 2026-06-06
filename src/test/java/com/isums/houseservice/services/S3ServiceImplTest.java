package com.isums.houseservice.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3ServiceImpl")
class S3ServiceImplTest {

    @Mock private S3Client s3Client;
    @Mock private S3Presigner s3Presigner;

    @InjectMocks private S3ServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(service, "bucket", "test-bucket");
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("uploads file to S3 and returns generated key with correct extension")
        void uploadsFile() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.PNG", "image/png",
                    "payload".getBytes(StandardCharsets.UTF_8));

            String key = service.upload(file, "houses");

            assertThat(key).startsWith("media/houses/");
            assertThat(key).endsWith(".png"); // extension lowercased

            ArgumentCaptor<PutObjectRequest> cap = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(cap.capture(), any(RequestBody.class));
            assertThat(cap.getValue().bucket()).isEqualTo("test-bucket");
            assertThat(cap.getValue().contentType()).isEqualTo("image/png");
            assertThat(cap.getValue().contentLength()).isEqualTo(file.getSize());
        }

        @Test
        @DisplayName("defaults extension to 'jpg' when original filename has none")
        void noExtensionDefaultsJpg() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "noext", "application/octet-stream", "x".getBytes());

            String key = service.upload(file, "f");
            assertThat(key).endsWith(".jpg");
        }

        @Test
        @DisplayName("defaults extension to 'jpg' when original filename is null")
        void nullFilenameDefaultsJpg() {
            MultipartFile file = new MockMultipartFile(
                    "file", null, "image/jpeg", "x".getBytes());

            String key = service.upload(file, "f");
            assertThat(key).endsWith(".jpg");
        }

        @Test
        @DisplayName("wraps IOException into RuntimeException")
        void ioExceptionWrapped() throws IOException {
            MultipartFile broken = new BrokenMultipartFile();

            assertThatThrownBy(() -> service.upload(broken, "f"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to upload");
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("issues DeleteObjectRequest with bucket and key")
        void deletes() {
            service.delete("media/x/abc.png");

            ArgumentCaptor<DeleteObjectRequest> cap = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            verify(s3Client).deleteObject(cap.capture());
            assertThat(cap.getValue().bucket()).isEqualTo("test-bucket");
            assertThat(cap.getValue().key()).isEqualTo("media/x/abc.png");
        }

        @Test
        @DisplayName("swallows S3 failures so callers are not broken")
        void swallowsFailure() {
            doThrow(AwsServiceException.builder().message("boom").build())
                    .when(s3Client).deleteObject(any(DeleteObjectRequest.class));

            // should not throw
            service.delete("k");
        }
    }

    @Nested
    @DisplayName("getImageUrl")
    class GetImageUrl {

        @Test
        @DisplayName("returns an S3 presigned URL")
        void buildsUrl() throws Exception {
            PresignedGetObjectRequest presigned = org.mockito.Mockito.mock(
                    PresignedGetObjectRequest.class);
            when(s3Presigner.presignGetObject(
                    any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class)))
                    .thenReturn(presigned);
            when(presigned.url()).thenReturn(
                    URI.create("https://signed.example.com/media/x/img.jpg").toURL());

            assertThat(service.getImageUrl("media/x/img.jpg"))
                    .isEqualTo("https://signed.example.com/media/x/img.jpg");
        }
    }

    /**
     * Helper: MultipartFile that throws IOException on getInputStream(),
     * to exercise the catch branch in upload().
     */
    private static class BrokenMultipartFile implements MultipartFile {
        @Override public String getName() { return "broken"; }
        @Override public String getOriginalFilename() { return "x.txt"; }
        @Override public String getContentType() { return "text/plain"; }
        @Override public boolean isEmpty() { return false; }
        @Override public long getSize() { return 1L; }
        @Override public byte[] getBytes() { return new byte[]{1}; }
        @Override public InputStream getInputStream() throws IOException {
            throw new IOException("simulated");
        }
        @Override public void transferTo(java.io.File dest) {}
    }
}

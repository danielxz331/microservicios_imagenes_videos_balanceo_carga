package com.example.webcam_app.service;

import com.example.webcam_app.model.Photo;
import com.example.webcam_app.repository.PhotoRepository;
import com.github.sarxos.webcam.Webcam;
import io.minio.*;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PhotoService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private PhotoRepository photoRepository;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket}")
    private String bucketName;

    @Scheduled(fixedRate = 5000)
    public void captureAndUploadPhoto() {
        try {
            // Asegúrate de que el bucket existe
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            Webcam webcam = Webcam.getDefault();
            webcam.open();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(webcam.getImage(), "PNG", baos);
            byte[] bytes = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

            String objectName = "photos/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".png";
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                                    bais, bytes.length, -1)
                            .contentType("image/png")
                            .build()
            );

            String url = String.format("%s/%s/%s", minioUrl, bucketName, objectName);

            Photo photo = new Photo();
            photo.setUrl(url);
            photo.setName(objectName);
            photoRepository.save(photo);

        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            Webcam webcam = Webcam.getDefault();
            if (webcam != null && webcam.isOpen()) {
                webcam.close();
            }
        }
    }

    public Page<Photo> getPhotos(int page, int size) {
        return photoRepository.findAll(PageRequest.of(page, size));
    }

    public Photo getPhotoById(Long id) {
        return photoRepository.findById(id).orElse(null);
    }

    public List<String> getAllPhotoUrls() {
        return photoRepository.findAll().stream()
                .map(Photo::getUrl)
                .collect(Collectors.toList());
    }

    public InputStream getPhoto(String objectName) throws MinioException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
    }
}

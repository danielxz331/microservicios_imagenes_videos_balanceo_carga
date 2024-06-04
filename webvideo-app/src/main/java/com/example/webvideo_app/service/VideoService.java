package com.example.webvideo_app.service;

import com.example.webvideo_app.model.Video;
import com.example.webvideo_app.repository.VideosRepository;
import io.minio.*;
import io.minio.errors.MinioException;
import org.bytedeco.javacv.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VideoService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private VideosRepository photoRepository;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket}")
    private String bucketName;

    @Scheduled(fixedRate = 10000) // Cada 10 segundos
    public void captureAndUploadVideo() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            String videoFileName = "video_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".mp4";
            File videoFile = new File(videoFileName);

            captureVideo(videoFile, 10);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStream videoInputStream = new FileInputStream(videoFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = videoInputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            videoInputStream.close();
            byte[] bytes = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

            String objectName = "videos/" + videoFileName;
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(
                                    bais, bytes.length, -1)
                            .contentType("video/mp4")
                            .build()
            );

            String url = String.format("%s/%s/%s", minioUrl, bucketName, objectName);

            Video video = new Video();
            video.setUrl(url);
            video.setName(objectName);
            photoRepository.save(video);

            videoFile.delete();

        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void captureVideo(File videoFile, int durationSeconds) throws FrameGrabber.Exception, FrameRecorder.Exception {
        FrameGrabber grabber = FrameGrabber.createDefault(0);
        grabber.start();

        FrameRecorder recorder = FrameRecorder.createDefault(videoFile.getAbsolutePath(), grabber.getImageWidth(), grabber.getImageHeight());
        recorder.setFormat("mp4");
        recorder.start();

        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) < durationSeconds * 1000) {
            Frame frame = grabber.grab();
            recorder.record(frame);
        }

        recorder.stop();
        grabber.stop();
    }

    public Page<Video> getPhotos(int page, int size) {
        return photoRepository.findAll(PageRequest.of(page, size));
    }

    public Video getPhotoById(Long id) {
        return photoRepository.findById(id).orElse(null);
    }

    public List<String> getAllPhotoUrls() {
        return photoRepository.findAll().stream()
                .map(Video::getUrl)
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

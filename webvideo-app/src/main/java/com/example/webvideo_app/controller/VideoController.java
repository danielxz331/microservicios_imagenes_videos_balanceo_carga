package com.example.webvideo_app.controller;

import com.example.webvideo_app.model.Video;
import com.example.webvideo_app.service.VideoService;
import io.minio.errors.MinioException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/videos")
public class VideoController {

    @Autowired
    private VideoService videoService;

    @GetMapping("/capture")
    public String captureVideo() {
        videoService.captureAndUploadVideo();
        return "Video captured and uploaded!";
    }

    @GetMapping
    public Page<Video> listVideoUrls(@RequestParam int page, @RequestParam int size) {
        return videoService.getPhotos(page, size);
    }

    @GetMapping("/{objectName}")
    public ResponseEntity<InputStreamResource> getVideo(@PathVariable String objectName) {
        try {
            InputStream videoStream = videoService.getPhoto(objectName); // Reutilizamos el método existente
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // Cambiamos el tipo de contenido a genérico
                    .body(new InputStreamResource(videoStream));
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/url")
    public ResponseEntity<String> getVideoUrlById(@PathVariable Long id) {
        Video video = videoService.getPhotoById(id);
        if (video != null) {
            return ResponseEntity.ok(video.getUrl());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

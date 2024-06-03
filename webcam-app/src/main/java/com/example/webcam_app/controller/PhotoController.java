package com.example.webcam_app.controller;

import com.example.webcam_app.model.Photo;
import com.example.webcam_app.service.PhotoService;
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
@RequestMapping("/photos")
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @GetMapping("/capture")
    public String capturePhoto() {
        photoService.captureAndUploadPhoto();
        return "Photo captured and uploaded!";
    }

    @GetMapping
    public Page<Photo> listPhotoUrls(@RequestParam int page, @RequestParam int size) {
        return photoService.getPhotos(page, size);
    }

    @GetMapping("/{objectName}")
    public ResponseEntity<InputStreamResource> getPhoto(@PathVariable String objectName) {
        try {
            InputStream photoStream = photoService.getPhoto(objectName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"")
                    .contentType(MediaType.IMAGE_PNG)
                    .body(new InputStreamResource(photoStream));
        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/url")
    public ResponseEntity<String> getPhotoUrlById(@PathVariable Long id) {
        Photo photo = photoService.getPhotoById(id);
        if (photo != null) {
            return ResponseEntity.ok(photo.getUrl());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}

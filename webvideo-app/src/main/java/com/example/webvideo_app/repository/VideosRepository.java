package com.example.webvideo_app.repository;

import com.example.webvideo_app.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideosRepository extends JpaRepository<Video, Long> {
}

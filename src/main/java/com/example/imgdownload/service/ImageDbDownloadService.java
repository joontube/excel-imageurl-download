package com.example.imgdownload.service;

import com.example.imgdownload.entity.NlImageTag;
import com.example.imgdownload.repository.NlImageTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class ImageDbDownloadService {
    @Autowired
    private NlImageTagRepository repository;

    // 모든 이미지 다운로드
    public void downloadAllImages(String outputDirectory) {
        // 데이터베이스에서 모든 데이터 가져오기
        List<NlImageTag> imageTags = repository.findAll();

        // 다운로드 경로 설정
        Path directoryPath = Paths.get(outputDirectory);

        // 디렉토리 생성
        try {
            Files.createDirectories(directoryPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // 각 이미지 다운로드
        for (NlImageTag imageTag : imageTags) {
            String imageUrl = imageTag.getSrc();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                downloadImage(imageUrl, directoryPath);
            }
        }
    }

    // 이미지 다운로드 메서드
    public void downloadImage(String imageUrl, Path directoryPath) {
        try {
            // URL을 도메인과 경로로 분리
            URL url = new URL(imageUrl);
            String domain = url.getProtocol() + "://" + url.getHost();
            String path = url.getPath();

            // 경로와 파일명을 분리
            int lastSlashIndex = path.lastIndexOf("/");
            String directoryPathInUrl = path.substring(0, lastSlashIndex + 1); // 마지막 "/"까지 포함
            String fileName = path.substring(lastSlashIndex + 1); // 파일명만 추출

            // 파일명만 URL 디코딩
            String decodedFileName = URLDecoder.decode(fileName, "UTF-8");

            // 파일명이 이미 디코딩된 경우, 그대로 사용
            boolean isEncoded = !fileName.equals(decodedFileName);
            String encodedFileName = isEncoded ? fileName : URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");

            // 다운로드할 파일 경로
            Path targetPath = directoryPath.resolve(decodedFileName);

            // 파일이 이미 존재하는 경우 로그 기록 후 건너뛰기
            if (Files.exists(targetPath)) {
                System.out.println("파일이 이미 존재합니다: " + targetPath);
                return;
            }

            // 최종 URL 구성
            String encodedUrl = domain + directoryPathInUrl + encodedFileName;

            // 파일 다운로드
            try (InputStream in = new URL(encodedUrl).openStream()) {
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("다운로드 완료: " + targetPath);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("이미지 다운로드 실패: " + imageUrl);
        }
    }

}

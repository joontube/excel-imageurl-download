package com.example.imgdownload.controller;



import com.example.imgdownload.service.ImageDbDownloadService;
import com.example.imgdownload.service.ImageExcelDownloadService;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageDownloadController {

    private final ImageExcelDownloadService imageExcelDownloadService;
    private final ImageDbDownloadService imageDbDownloadService;

    @Autowired
    public ImageDownloadController(ImageExcelDownloadService imageExcelDownloadService, ImageDbDownloadService imageDbDownloadService) {
        this.imageExcelDownloadService = imageExcelDownloadService;
        this.imageDbDownloadService = imageDbDownloadService;
    }

    // 엑셀 파일 업로드 및 처리
    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcelFile(@RequestParam("file") MultipartFile file) {
        try {
            // 엑셀 파일에서 디렉토리와 URL 정보 읽기
            Map<String, List<String>> directoryToUrlsMap = imageExcelDownloadService.readDirectoryAndUrlsFromExcel(file);

            // 이미지 다운로드
            imageExcelDownloadService.downloadImagesAsync(directoryToUrlsMap);

            return ResponseEntity.ok("이미지 다운로드가 완료되었습니다.");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 처리 중 오류 발생: " + e.getMessage());
        }
    }


}

package com.example.imgdownload.service;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class ImageExcelDownloadService {

    // 엑셀 파일에서 디렉토리와 URL 매핑 읽기
    public Map<String, List<String>> readDirectoryAndUrlsFromExcel(MultipartFile file) throws IOException {
        Map<String, List<String>> directoryToUrlsMap = new HashMap<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트 사용

            // 첫 번째 행 (A1, B1, C1 등)을 읽어 디렉토리 이름을 추출
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return directoryToUrlsMap;

            // 열 단위로 디렉토리 이름 및 URL 읽기
            for (int colIndex = 0; colIndex < headerRow.getPhysicalNumberOfCells(); colIndex++) {
                String directoryName = headerRow.getCell(colIndex).getStringCellValue(); // 디렉토리 이름
                List<String> urls = new ArrayList<>();

                // 해당 열의 URL들을 읽음 (2번째 행부터 시작)
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row != null) {
                        Cell cell = row.getCell(colIndex); // 열의 데이터를 읽음
                        if (cell != null && cell.getCellType() == CellType.STRING) {
                            String url = cell.getStringCellValue().trim();
                            if (!url.isEmpty()) {
                                urls.add(url);
                            }
                        }
                    }
                }

                // URL 목록이 비어있지 않으면 매핑 추가
                if (!urls.isEmpty()) {
                    directoryToUrlsMap.put(directoryName, urls);
                }
            }
        }

        return directoryToUrlsMap;
    }

    // 이미지 다운로드
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

            // 파일명만 URL 인코딩
            String encodedFileName = isEncoded(fileName)
                    ? fileName : URLEncoder.encode(fileName, "UTF-8").replace("+", "%20");


            // 다운로드할 파일 경로
            Path targetPath = directoryPath.resolve(fileName);

            // 파일이 이미 존재하는 경우 건너뛰기
//            if (Files.exists(targetPath)) {
////                System.out.println("파일이 이미 존재합니다: " + targetPath);
//                return;
//            }


            // 최종 URL 구성
            String encodedUrl = domain + directoryPathInUrl + encodedFileName;

            // 파일 다운로드
            try (InputStream in = new URL(encodedUrl).openStream()) {
                Files.copy(in, directoryPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("이미지 다운로드 실패: " + imageUrl);
        }
    }

    private boolean isEncoded(String url) {
        try {
            return !url.equals(java.net.URLDecoder.decode(url, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            return false; // 기본적으로 false로 처리
        }
    }

    // 디렉토리와 URL 매핑된 이미지 다운로드 처리
    public void downloadImages(Map<String, List<String>> directoryToUrlsMap) throws IOException {
        for (Map.Entry<String, List<String>> entry : directoryToUrlsMap.entrySet()) {
            String directoryName = entry.getKey();
            List<String> urls = entry.getValue();

            Path directoryPath = Paths.get(directoryName);
            Files.createDirectories(directoryPath); // 디렉토리 생성

            for (String url : urls) {
                downloadImage(url, directoryPath);
                System.out.println("하나 다운로드 완료: "+url);
            }
        }
    }


    public void downloadImagesAsync(Map<String, List<String>> directoryToUrlsMap) throws IOException {
        for (Map.Entry<String, List<String>> entry : directoryToUrlsMap.entrySet()) {
            String directoryName = entry.getKey();
            List<String> urls = entry.getValue();

            Path directoryPath = Paths.get(directoryName);
            Files.createDirectories(directoryPath); // 디렉토리 생성

            // 비동기로 URL 다운로드 작업 시작
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            int cnt = 0;
            for (String url : urls) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    System.out.println("다운로드 중: " + url);
                    downloadImage(url, directoryPath);
                    System.out.println("하나 다운로드 완료: " + url);
                });
                futures.add(future);
            }

            // 모든 작업이 완료될 때까지 기다림
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }
}

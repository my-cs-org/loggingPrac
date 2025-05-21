package com.ohgiraffers.loggingprac.log;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* 설명. 로그 분석 결과를 제공하는 REST API를 정의 */
@RestController
@RequestMapping("/api/logs")
public class LogAnalyzerController {

    private final LogAnalyzerService logAnalyzerService;
    Logger logger = LoggerFactory.getLogger(LogAnalyzerController.class);

    public LogAnalyzerController(LogAnalyzerService logAnalyzerService) {
        this.logAnalyzerService = logAnalyzerService;
    }

    @GetMapping("/health")
    public String healthCheck() {
        System.out.println("단순 콘솔 출력");
        logger.debug("health 핸들러 메소드 실행");
        return "I'm OK";
    }

    @GetMapping("/analyze/{date}")
    public Map<String, Object> analyzeLog(@PathVariable String date) {
        logger.debug("analyze 핸들러 메소드 실행");
        List<LogEntry> entries = logAnalyzerService.parseLogFile(date);

        Map<String, Object> analysis = new HashMap<>();
        if (entries.isEmpty()) {
            analysis.put("message", date + " 날짜의 로그 데이터가 없습니다.");
            analysis.put("totalRequests", 0);
        } else {
            analysis.put("totalRequests", entries.size());
            analysis.put("requestsByIp", logAnalyzerService.getRequestsByIp(entries));
            analysis.put("requestsByUri", logAnalyzerService.getRequestsByUri(entries));
            analysis.put("requestsByMethod", logAnalyzerService.getRequestsByMethod(entries));
        }

        return analysis;
    }

    @GetMapping("/download/{date}")
    public ResponseEntity<Resource> downloadExcel(@PathVariable String date) {
        logger.debug("download 핸들러 메소드 실행");
        try {
            List<LogEntry> entries = logAnalyzerService.parseLogFile(date);
            logAnalyzerService.exportToExcel(entries, date);

            Path file = Paths.get("logs/client-requests-" + date + ".xlsx");
            Resource resource = new UrlResource(file.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"client-requests-" + date + ".xlsx\"")
                    .body(resource);
        } catch (Exception e) {
            throw new RuntimeException("파일 다운로드 실패", e);
        }
    }
}

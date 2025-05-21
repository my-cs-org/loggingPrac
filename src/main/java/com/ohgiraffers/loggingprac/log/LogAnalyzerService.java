package com.ohgiraffers.loggingprac.log;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class LogAnalyzerService {

    private static final Pattern LOG_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}).*Client Request - IP: ([^,]+), Method: ([^,]+), URI: ([^,]+), User-Agent: (.+)");

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    Logger logger = LoggerFactory.getLogger(LogAnalyzerController.class);

    /**
     * 설명. 지정된 날짜의 로그 파일을 파싱하여 접속 이력 LogEntry 객체 목록을 반환
     *      파일이 존재하지 않으면 빈 목록을 반환
     *
     * @param date 로그 파일 날짜 (yyyy-MM-dd 형식)
     * @return 파싱된 로그 항목 목록
     */
    public List<LogEntry> parseLogFile(String date) {
        String logFile = String.format("logs/client-requests.%s.log", date);
        Path logPath = Paths.get(logFile);

        if (!Files.exists(logPath)) {
            return new ArrayList<>();  // 해당 날짜의 로그 파일이 없으면 빈 리스트 반환
        }

        try {
            return Files.lines(logPath)
                    .map(this::parseLine)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("로그 파일 읽기 실패: " + logFile, e);
        }
    }

    /**
     * 설명. 로그 라인을 파싱하여 접속 client에 대한 정보를 담은 로그들(Pattern과 Matcher를 활용하여)만
     *      LogEntry 객체로 변환
     *      (현재 로그 파일에 접속 이력이 없다면 null을 반환)
     *
     * @param line 파싱할 로그 라인
     * @return 파싱된 LogEntry 객체 또는 null
     */
    private LogEntry parseLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            logger.debug("로그 패턴과 일치하는 로그: {}", matcher.group());
            logger.debug("로그 패턴과 일치하는 요청 url: {}", matcher.group(2));
            return new LogEntry(
                    LocalDateTime.parse(matcher.group(1), DATE_FORMAT),
                    matcher.group(2).trim(),
                    matcher.group(3).trim(),
                    matcher.group(4).trim(),
                    matcher.group(5).trim()
            );
        }
        return null;
    }

    /**
     * 설명. 로그 항목 목록에서 IP별 요청 수를 계산하여 반환
     *
     * @param entries 로그 항목 목록
     * @return IP 주소를 키로, 요청 수를 값으로 가지는 맵
     */
    public Map<String, Long> getRequestsByIp(List<LogEntry> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(
                        LogEntry::getClientIp,
                        Collectors.counting()
                ));
    }

    /**
     * 설명. 로그 항목 목록에서 URI별 요청 수를 계산하여 반환
     *
     * @param entries 로그 항목 목록
     * @return URI를 키로, 요청 수를 값으로 가지는 맵
     */
    public Map<String, Long> getRequestsByUri(List<LogEntry> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(
                        LogEntry::getUri,
                        Collectors.counting()
                ));
    }

    /**
     * 설명. 로그 항목 목록에서 HTTP 메소드별 요청 수를 계산하여 반환
     *
     * @param entries 로그 항목 목록
     * @return HTTP 메소드를 키로, 요청 수를 값으로 가지는 맵
     */
    public Map<String, Long> getRequestsByMethod(List<LogEntry> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(
                        LogEntry::getMethod,
                        Collectors.counting()
                ));
    }

    /**
     * 설명. 로그 항목 목록을 Excel 파일로 내보냄
     *      파일은 요청 상세 시트와 통계 시트(IP별, URI별, 메소드별)로 구성
     *
     * @param entries 내보낼 로그 항목 목록
     * @param date 파일명에 포함될 날짜 문자열
     */
    public void exportToExcel(List<LogEntry> entries, String date) {
        try (Workbook workbook = new XSSFWorkbook()) {

            /* 설명. 요청 상세 시트 생성 */
            Sheet detailSheet = workbook.createSheet("요청 상세");

            /* 설명. 헤더 생성 */
            Row headerRow = detailSheet.createRow(0);
            headerRow.createCell(0).setCellValue("시간");
            headerRow.createCell(1).setCellValue("IP");
            headerRow.createCell(2).setCellValue("메소드");
            headerRow.createCell(3).setCellValue("URI");
            headerRow.createCell(4).setCellValue("User-Agent");

            /* 설명. 데이터 입력 */
            int rowNum = 1;
            for (LogEntry entry : entries) {
                Row row = detailSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                row.createCell(1).setCellValue(entry.getClientIp());
                row.createCell(2).setCellValue(entry.getMethod());
                row.createCell(3).setCellValue(entry.getUri());
                row.createCell(4).setCellValue(entry.getUserAgent());
            }

            /* 설명. 통계 시트 생성 */
            Sheet statsSheet = workbook.createSheet("통계");

            /* 설명. IP별 통계 */
            createStatisticsSection(statsSheet, 0, "IP별 요청 수", getRequestsByIp(entries));

            /* 설명. URI별 통계 */
            createStatisticsSection(statsSheet, getRequestsByIp(entries).size() + 2,
                    "URI별 요청 수", getRequestsByUri(entries));

            /* 설명. 메소드별 통계 */
            createStatisticsSection(statsSheet, getRequestsByIp(entries).size() +
                            getRequestsByUri(entries).size() + 4,
                    "메소드별 요청 수", getRequestsByMethod(entries));

            /* 설명. 컬럼 너비 자동 조정 */
            for (int i = 0; i < 5; i++) {
                detailSheet.autoSizeColumn(i);
                statsSheet.autoSizeColumn(i);
            }

            /* 설명. 파일 저장 */
            String fileName = String.format("logs/client-requests-%s.xlsx", date);
            try (FileOutputStream fileOut = new FileOutputStream(fileName)) {
                workbook.write(fileOut);
            }
        } catch (IOException e) {
            throw new RuntimeException("엑셀 파일 생성 실패", e);
        }
    }

    /**
     * 설명. 통계 데이터를 Excel 시트에 추가
     *
     * @param sheet 데이터를 추가할 시트
     * @param startRow 데이터 추가를 시작할 행 인덱스
     * @param title 통계 섹션의 제목
     * @param data 통계 데이터 맵 (키-구분, 값-요청 수)
     */
    private void createStatisticsSection(Sheet sheet, int startRow, String title,
                                         Map<String, Long> data) {
        Row titleRow = sheet.createRow(startRow);
        titleRow.createCell(0).setCellValue(title);

        Row headerRow = sheet.createRow(startRow + 1);
        headerRow.createCell(0).setCellValue("구분");
        headerRow.createCell(1).setCellValue("요청 수");

        int rowNum = startRow + 2;
        for (Map.Entry<String, Long> entry : data.entrySet()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }
    }
}

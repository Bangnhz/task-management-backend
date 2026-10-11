package com.example.task_management.service.impl;

import com.example.task_management.service.FileParseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Slf4j
@Service("fileParserService")
public class FileParseServiceImpl implements FileParseService {

    @Override
    public String extractContent(MultipartFile[] files) throws Exception {
        if (files == null || files.length == 0) {
            return "";
        }

        StringBuilder combinedText = new StringBuilder();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String content = extractContent(file);
            if (content != null && !content.isBlank()) {
                combinedText.append("\n=== NỘI DUNG TỪ FILE (")
                        .append(file.getOriginalFilename())
                        .append(") ===\n")
                        .append(content)
                        .append("\n");
            }
        }
        return combinedText.toString();
    }

    @Override
    public String extractContent(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return "";
        }
        String filename = file.getOriginalFilename();
        if (filename == null)
            return "";

        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return parseExcel(file.getInputStream());
        } else if (filename.endsWith(".docx")) {
            return parseWord(file.getInputStream());
        }
        throw new IllegalArgumentException("Định dạng file không được hỗ trợ. Chỉ nhận .xlsx, .docx: " + filename);
    }

    private String parseExcel(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder("DỮ LIỆU TỪ FILE EXCEL:\n");
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0); // Đọc sheet đầu tiên
            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {
                for (Cell cell : row) {
                    sb.append(formatter.formatCellValue(cell)).append(" | ");
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String parseWord(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder("DỮ LIỆU TỪ FILE WORD:\n");
        try (XWPFDocument doc = new XWPFDocument(is)) {
            // Đọc các đoạn văn bản
            for (XWPFParagraph p : doc.getParagraphs()) {
                if (!p.getText().isBlank()) {
                    sb.append(p.getText()).append("\n");
                }
            }
            // Đọc các bảng nếu có
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    row.getTableCells().forEach(cell -> sb.append(cell.getText()).append(" | "));
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }
}

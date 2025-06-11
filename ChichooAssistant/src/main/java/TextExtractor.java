// package com.example.chichooassistant; // 你的包名

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.InputStream;

public class TextExtractor {

    public static String extractText(InputStream inputStream, String fileName) throws Exception {
        String fileExtension = getFileExtension(fileName);

        if ("pdf".equalsIgnoreCase(fileExtension)) {
            return extractTextFromPdf(inputStream);
        } else if ("docx".equalsIgnoreCase(fileExtension)) {
            return extractTextFromDocx(inputStream);
        } else {
            // 如果是不支持的文件类型，可以抛出异常或返回提示信息
            throw new IllegalArgumentException("不支持的文件类型: " + fileExtension);
        }
    }

    private static String extractTextFromPdf(InputStream inputStream) throws Exception {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    private static String extractTextFromDocx(InputStream inputStream) throws Exception {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
}
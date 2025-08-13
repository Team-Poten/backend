//package org.poten.backend.ocr.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.poten.backend.clova.dto.response.OcrResponseDto;
//import org.poten.backend.clova.service.OcrService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//
//@RestController
//@RequestMapping("/api/v1/ocr")
//@RequiredArgsConstructor
//public class OcrController {
//
//    private final OcrService ocrService;
//
//    @PostMapping
//    public ResponseEntity<String> extractTextFromImage(@RequestParam("image") MultipartFile image) {
//        try {
//            OcrResponseDto ocrResponse = ocrService.extractTextFromImage(image);
//            return ResponseEntity.ok(ocrResponse.getFullText());
//        } catch (IOException e) {
//            return ResponseEntity.status(500).body("Error " + e.getMessage());
//        }
//    }
//}

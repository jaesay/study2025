package com.jaesay.openaidemo.imageprocessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.jaesay.openaidemo.services.OpenAiService;

@Controller
public class ImageAnalyzerController {

    private static final String UPLOAD_DIR = "/Users/jaesaythippireddy/Documents/springai/images/uploads/";
    
    @Autowired
    private OpenAiService service;

    // Display the image upload form
    @GetMapping("showImageAnalyzer")
    public String showUploadForm() {
        return "imageAnalyzer";
    }

    @PostMapping("/imageAnalyzer")
    public String uploadImage(String prompt, @RequestParam("file") MultipartFile file, Model model, RedirectAttributes redirectAttributes) {


        return "imageAnalyzer";
    }
}
package com.jaesay.openaidemo.imageprocessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jaesay.openaidemo.services.OpenAiService;

@Controller
public class DietHelperController {

    // Define the folder where images will be saved
    private static final String UPLOAD_DIR = "/Users/jaesaythippireddy/Documents/springai/images/uploads/";
    
    @Autowired
    private OpenAiService service;

    // Display the image upload form
    @GetMapping("/showDietHelper")
    public String showUploadForm() {
        return "dietHelper";
    }

    @PostMapping("/dietHelper")
    public String dietHelper(String prompt, @RequestParam("file1") MultipartFile file1,@RequestParam("file2") MultipartFile file2, Model model) {

        return "dietHelper";
    }
}
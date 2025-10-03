package com.jaesay.openaidemo.imageprocessing;

import com.jaesay.openaidemo.services.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ImageGenerationController {

	private final OpenAiService service;

	@GetMapping("/showImageGenerator")
	public String showImageGenerator() {
		return "imageGenerator";

	}

	@PostMapping("/imageGenerator")
	public String imageGenerator(@RequestParam String prompt, Model model) {
        String response = service.generateImage(prompt);
        model.addAttribute("response", response);
		return "imageGenerator";
	}

}
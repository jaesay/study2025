package com.jaesay.openaidemo.embeddings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.jaesay.openaidemo.services.OpenAiService;

@Controller
public class SimilarityFinder {

	@Autowired
	private OpenAiService service;
	
	@GetMapping("/showSimilarityFinder")
	public String showSimilarityFinder() {
		return "similarityFinder";

	}

	@PostMapping("/similarityFinder")
	public String findSimilarity(@RequestParam String text1,@RequestParam String text2,Model model) {
        double response = service.findSimilarity(text1, text2);
        model.addAttribute("response", response);
		return "similarityFinder";
	}

}
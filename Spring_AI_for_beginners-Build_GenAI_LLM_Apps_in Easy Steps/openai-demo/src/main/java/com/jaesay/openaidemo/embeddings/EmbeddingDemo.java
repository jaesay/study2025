package com.jaesay.openaidemo.embeddings;

import com.jaesay.openaidemo.services.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class EmbeddingDemo {

	private final OpenAiService service;

	@GetMapping("/showEmbedding")
	public String showEmbedDemo() {
		return "embedDemo";

	}

	@PostMapping("/embedding")
	public String embed(@RequestParam String text,Model model) {
        float[] response = service.embed(text);
        model.addAttribute("response", response);
        return "embedDemo";
	}

}
package com.jaesay.openaidemo.tools;

import com.jaesay.openaidemo.services.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class WeatherAgentController {

	private final OpenAiService service;

	@GetMapping("/showWeatherAgent")
	public String showWeatherAgent() {
		return "weatherTool";
	}

	@PostMapping("/weatherAgent")
	public String weatherAgent(@RequestParam("query") String query, Model model) {
        String response = service.callAgent(query);
        model.addAttribute("weatherInfo", response);
		return "weatherTool";
	}
}
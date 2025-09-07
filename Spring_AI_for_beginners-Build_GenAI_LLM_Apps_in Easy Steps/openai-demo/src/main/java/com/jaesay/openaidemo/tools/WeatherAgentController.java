package com.jaesay.openaidemo.tools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.jaesay.openaidemo.services.OpenAiService;

@Controller
public class WeatherAgentController {

	@Autowired
	private OpenAiService service;

	@GetMapping("/showWeatherAgent")
	public String showWeatherAgent() {
		return "weatherTool";
	}

	@PostMapping("/weatherAgent")
	public String weatherAgent(@RequestParam("query") String query, Model model) {
		return "weatherTool";
	}
}
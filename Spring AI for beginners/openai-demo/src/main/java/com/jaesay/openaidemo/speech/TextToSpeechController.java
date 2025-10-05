package com.jaesay.openaidemo.speech;

import java.io.IOException;

import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.jaesay.openaidemo.services.OpenAiService;

@Controller
public class TextToSpeechController {

	@Autowired
	private OpenAiService service;

	// Display the image upload form
	@GetMapping("/showTextToSpeech")
	public String showUploadForm() throws IOException {
		return "textToSpeech";
	}

	@GetMapping("/textToSpeech")
	public ResponseEntity<byte[]> streamAudio(@RequestParam String text) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "audio/mpeg");
        httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=output.mp3");

        return new ResponseEntity<>(service.textToSpeech(text), httpHeaders, HttpStatus.OK);
	}
}
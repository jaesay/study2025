package com.jaesay.openaidemo.text.prompttemplate;

import com.jaesay.openaidemo.services.OpenAiService;
import com.jaesay.openaidemo.text.prompttemplate.dto.CountryCuisines;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class CuisineHelperController {

    private final OpenAiService chatService;

    @GetMapping("/showCuisineHelper")
    public String showChatPage() {
         return "cuisineHelper";
    }

    @PostMapping("/cuisineHelper")
    public String getChatResponse(@RequestParam("country") String country, @RequestParam("numCuisines") String numCuisines,@RequestParam("language") String language,Model model) {
        CountryCuisines countryCuisines = chatService.getCuisines(country, numCuisines, language);

        model.addAttribute("countryCuisines", countryCuisines);
        return "cuisineHelper";
    }
}

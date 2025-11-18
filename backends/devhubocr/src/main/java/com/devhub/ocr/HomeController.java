package com.devhub.ocr;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    @GetMapping("/")
    public String home() {
        return "html/home";
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "html/dashboard";
    }

   
}


package com.devhub.ocr.init;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class init {

    @GetMapping("/init")
    public String init() {
        return "html/init";
    }
    
    @GetMapping("/")
    public String home() {
        return "html/home";
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        return "html/dashboard";
    }
}
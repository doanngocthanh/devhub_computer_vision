package com.devhub.ocr.init;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class init {

    @GetMapping("/init")
    public String home() {
        return "html/init";
    }
}
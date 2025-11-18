package com.devhub.ocr.app.systems.trx;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.devhub.ocr.app.systems.mod.FileService;

@Controller
@RequestMapping("/app/systems/file")
public class systems_file {
    @GetMapping(value = "/api/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<String> listFilesJson() {
        FileService fileService = new FileService();
        return fileService.getAllFiles();
    }
}

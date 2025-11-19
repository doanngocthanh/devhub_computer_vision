package com.devhub.ocr.app.systems.pages;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

/**
 * Generic page resolver for path-based templates.
 *
 * Maps requests such as /AA/A0/AAA0_0100 to the Thymeleaf template
 * templates/html/AA/A0/AAA0_0100/AAA0_0100.html when the file exists.
 */
@Controller
public class PathPageController {

    private final ResourceLoader resourceLoader;

    public PathPageController(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @GetMapping("/{p1}/{p2}/{p3}")
    public String threeSegment(@PathVariable String p1,
                               @PathVariable String p2,
                               @PathVariable String p3) {
        String tpl = String.format("classpath:/templates/html/%s/%s/%s/%s.html", p1, p2, p3, p3);
        Resource res = resourceLoader.getResource(tpl);
        try {
            if (res.exists() && res.getFile().isFile()) {
                return String.format("html/%s/%s/%s/%s", p1, p2, p3, p3);
            }
        } catch (IOException e) {
            // fall through to 404
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Page not found");
    }

    @GetMapping("/{p1}/{p2}/{p3}/**")
    public String threeSegmentWildcard(@PathVariable String p1,
                                       @PathVariable String p2,
                                       @PathVariable String p3) {
        // same behaviour for deeper paths under the page
        return threeSegment(p1, p2, p3);
    }
}

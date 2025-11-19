package com.devhub.ocr.app.systems.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Central error controller that renders friendly HTML error pages for common
 * HTTP status codes (404, 403, 500). Templates are located under
 * `templates/error/` (e.g. `error/404.html`).
 */
@Controller
public class AppErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Integer statusCode = statusObj instanceof Integer ? (Integer) statusObj : null;
        Object ex = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        String message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE) == null ? "" : String.valueOf(request.getAttribute(RequestDispatcher.ERROR_MESSAGE));

        model.addAttribute("status", statusCode == null ? "" : statusCode);
        model.addAttribute("errorMessage", message == null ? "" : message);
        model.addAttribute("exception", ex == null ? null : ex);

        if (statusCode != null) {
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                return "error/404";
            } else if (statusCode == HttpStatus.FORBIDDEN.value()) {
                return "error/403";
            } else if (statusCode >= 500) {
                return "error/500";
            }
        }

        return "error/error";
    }

}

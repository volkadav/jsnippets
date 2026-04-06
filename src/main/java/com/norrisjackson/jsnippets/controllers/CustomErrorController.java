package com.norrisjackson.jsnippets.controllers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

@Controller
@Slf4j
public class CustomErrorController implements ErrorController {
    private final ErrorAttributes errorAttributes;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    public CustomErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    /**
     * Handle error requests and display a custom error page.
     *
     * @param request the HTTP servlet request
     * @param model   the Spring MVC model
     * @return the error view name
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        ServletWebRequest webRequest = new ServletWebRequest(request);
        Map<String, Object> errorAttrs = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.EXCEPTION, ErrorAttributeOptions.Include.MESSAGE));

        int status = (int) errorAttrs.getOrDefault("status", 500);
        String error = (String) errorAttrs.getOrDefault("error", "Unknown Error");
        String message = (String) errorAttrs.getOrDefault("message", "");
        Throwable exception = errorAttributes.getError(webRequest);

        log.warn("ErrorController triggered for URI {}: status={}, error={}, message={}", request.getRequestURI(), status, error, message, exception);

        model.addAttribute("status", status);
        model.addAttribute("error", error);
        if (!isProductionProfile()) {
            log.debug("Non-production profile detected, showing detailed error message");
            if (exception != null && exception.getMessage() != null) {
                model.addAttribute("message", exception.getMessage());
            } else if (message != null && !message.isEmpty()) {
                model.addAttribute("message", message);
            }
        }
        return "error";
    }

    /**
     * Check if any active profile indicates a production environment.
     * Handles both "prod" and "production" names, and comma-separated profile lists.
     */
    private boolean isProductionProfile() {
        if (activeProfile == null || activeProfile.isEmpty()) {
            return false;
        }
        for (String profile : activeProfile.split(",")) {
            String trimmed = profile.trim().toLowerCase();
            if ("prod".equals(trimmed) || "production".equals(trimmed)) {
                return true;
            }
        }
        return false;
    }
}

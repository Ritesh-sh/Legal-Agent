package com.legalai.service;

import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class ScholarService {

    /**
     * Generate Google Scholar link for a legal query.
     */
    public String getScholarLink(String query) {
        String encoded = URLEncoder.encode(query + " Indian law", StandardCharsets.UTF_8);
        return "https://scholar.google.com/scholar?q=" + encoded;
    }
}

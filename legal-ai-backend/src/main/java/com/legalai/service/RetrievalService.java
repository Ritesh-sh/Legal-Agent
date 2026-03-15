package com.legalai.service;

import com.legalai.model.LegalDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetrievalService {

    private final BM25Service bm25Service;
    private final IndexBuilderService indexBuilderService;

    public RetrievalService(BM25Service bm25Service, IndexBuilderService indexBuilderService) {
        this.bm25Service = bm25Service;
        this.indexBuilderService = indexBuilderService;
    }

    /**
     * Search sections using PageIndex + BM25.
     * Returns top 3 relevant sections.
     */
    public List<LegalDocument> searchSections(String query) {
        return bm25Service.search(query, 3);
    }

    /**
     * Direct section lookup by section number.
     */
    public LegalDocument getSection(String sectionNumber) {
        String docId = indexBuilderService.searchBySection(sectionNumber);
        if (docId != null) {
            return indexBuilderService.getDocument(docId);
        }
        return null;
    }

    /**
     * Generate Google Scholar search link.
     */
    public String searchScholar(String query) {
        String encoded = query.replaceAll("\\s+", "+");
        return "https://scholar.google.com/scholar?q=" + encoded + "+Indian+law";
    }
}

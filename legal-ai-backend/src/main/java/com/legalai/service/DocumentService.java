package com.legalai.service;

import com.legalai.model.LegalDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final IndexBuilderService indexBuilderService;

    public DocumentService(IndexBuilderService indexBuilderService) {
        this.indexBuilderService = indexBuilderService;
    }

    public List<LegalDocument> getAllDocuments() {
        return indexBuilderService.getAllDocuments();
    }

    public LegalDocument getDocumentById(String id) {
        return indexBuilderService.getDocument(id);
    }

    public int getDocumentCount() {
        return indexBuilderService.getAllDocuments().size();
    }
}

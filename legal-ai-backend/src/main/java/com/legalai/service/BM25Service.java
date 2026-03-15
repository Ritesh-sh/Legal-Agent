package com.legalai.service;

import com.legalai.model.LegalDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BM25Service {

    private static final Logger log = LoggerFactory.getLogger(BM25Service.class);

    private final IndexBuilderService indexBuilderService;

    // BM25 parameters
    private static final double K1 = 1.5;
    private static final double B = 0.75;

    public BM25Service(IndexBuilderService indexBuilderService) {
        this.indexBuilderService = indexBuilderService;
    }

    /**
     * Compute BM25 scores for candidate documents against a query.
     *
     * @param query    the search query
     * @param topK     number of top results to return
     * @return list of documents sorted by BM25 score (descending)
     */
    public List<LegalDocument> search(String query, int topK) {
        Set<String> queryTokens = indexBuilderService.tokenize(query);
        Set<String> candidateIds = indexBuilderService.getCandidateDocIds(query);

        if (candidateIds.isEmpty()) {
            log.debug("No candidate documents found for query: {}", query);
            return Collections.emptyList();
        }

        // Compute average document length
        List<LegalDocument> allDocs = indexBuilderService.getAllDocuments();
        int totalDocs = allDocs.size();
        double avgDocLength = allDocs.stream()
                .mapToInt(d -> tokenize(d).size())
                .average()
                .orElse(100.0);

        // Compute BM25 scores
        Map<String, Double> scores = new HashMap<>();

        for (String docId : candidateIds) {
            LegalDocument doc = indexBuilderService.getDocument(docId);
            if (doc == null) continue;

            double score = computeBM25Score(queryTokens, doc, totalDocs, avgDocLength);
            scores.put(docId, score);
        }

        // Sort by score descending and return top K
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> indexBuilderService.getDocument(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private double computeBM25Score(Set<String> queryTokens, LegalDocument doc, int totalDocs, double avgDocLength) {
        Set<String> docTokens = tokenize(doc);
        int docLength = docTokens.size();
        double score = 0.0;

        for (String queryTerm : queryTokens) {
            // Term frequency in document
            int tf = countOccurrences(doc, queryTerm);

            // Number of documents containing the term
            int df = indexBuilderService.searchByKeyword(queryTerm).size();

            if (df == 0 || tf == 0) continue;

            // IDF component
            double idf = Math.log((totalDocs - df + 0.5) / (df + 0.5) + 1.0);

            // TF normalization with BM25
            double tfNorm = (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * (docLength / avgDocLength)));

            score += idf * tfNorm;
        }

        // Boost for exact section number match
        for (String token : queryTokens) {
            if (token.matches("\\d+[a-z]?") && doc.getSectionNumber().equalsIgnoreCase(token)) {
                score += 10.0; // High boost for section number match
            }
        }

        // Boost for act name match
        String docActLower = doc.getActName().toLowerCase();
        for (String token : queryTokens) {
            if (docActLower.contains(token)) {
                score += 2.0;
            }
        }

        return score;
    }

    private int countOccurrences(LegalDocument doc, String term) {
        String text = (doc.getTitle() + " " + doc.getContent() + " " + doc.getActName()).toLowerCase();
        String[] words = text.split("\\W+");
        int count = 0;
        for (String word : words) {
            if (word.equals(term)) count++;
        }
        return count;
    }

    private Set<String> tokenize(LegalDocument doc) {
        return indexBuilderService.tokenize(doc.getTitle() + " " + doc.getContent());
    }
}

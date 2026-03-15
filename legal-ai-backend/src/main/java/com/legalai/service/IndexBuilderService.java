package com.legalai.service;

import com.legalai.model.LegalDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IndexBuilderService {

    private static final Logger log = LoggerFactory.getLogger(IndexBuilderService.class);

    private final PDFParserService pdfParserService;

    // PageIndex: three main indexes
    private final Map<String, Set<String>> keywordIndex = new HashMap<>();
    private final Map<String, String> sectionIndex = new HashMap<>();
    private final Map<String, Set<String>> actIndex = new HashMap<>();

    // Document store for quick lookup
    private final Map<String, LegalDocument> documentStore = new HashMap<>();

    // Stopwords for filtering
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "shall", "can", "must", "need", "dare",
            "to", "of", "in", "for", "on", "with", "at", "by", "from", "as",
            "into", "through", "during", "before", "after", "above", "below",
            "between", "under", "over", "out", "up", "down", "off", "about",
            "and", "but", "or", "nor", "not", "so", "yet", "both", "either",
            "neither", "each", "every", "all", "any", "few", "more", "most",
            "other", "some", "such", "no", "only", "own", "same", "than",
            "too", "very", "just", "because", "if", "when", "while", "where",
            "how", "what", "which", "who", "whom", "this", "that", "these",
            "those", "it", "its", "he", "she", "they", "them", "we", "you",
            "i", "me", "my", "your", "his", "her", "our", "their"
    );

    public IndexBuilderService(PDFParserService pdfParserService) {
        this.pdfParserService = pdfParserService;
    }

    @PostConstruct
    public void buildIndex() {
        List<LegalDocument> documents = pdfParserService.getAllDocuments();
        log.info("Building PageIndex for {} documents...", documents.size());

        for (LegalDocument doc : documents) {
            // Store document
            documentStore.put(doc.getId(), doc);

            // Build section index: sectionNumber -> docId
            sectionIndex.put(doc.getSectionNumber(), doc.getId());

            // Build act index: actAbbrev -> Set<docId>
            String actKey = extractActKey(doc.getId());
            actIndex.computeIfAbsent(actKey, k -> new LinkedHashSet<>()).add(doc.getId());

            // Build keyword index
            Set<String> keywords = tokenize(doc.getTitle() + " " + doc.getContent() + " " + doc.getActName());
            for (String keyword : keywords) {
                keywordIndex.computeIfAbsent(keyword, k -> new LinkedHashSet<>()).add(doc.getId());
            }
        }

        log.info("PageIndex built: {} keywords, {} sections, {} acts",
                keywordIndex.size(), sectionIndex.size(), actIndex.size());
    }

    public Set<String> searchByKeyword(String keyword) {
        return keywordIndex.getOrDefault(keyword.toLowerCase(), Collections.emptySet());
    }

    public String searchBySection(String sectionNumber) {
        return sectionIndex.get(sectionNumber);
    }

    public Set<String> searchByAct(String actKey) {
        return actIndex.getOrDefault(actKey, Collections.emptySet());
    }

    public LegalDocument getDocument(String docId) {
        return documentStore.get(docId);
    }

    public List<LegalDocument> getAllDocuments() {
        return new ArrayList<>(documentStore.values());
    }

    public Set<String> getCandidateDocIds(String query) {
        Set<String> tokens = tokenize(query);
        Set<String> candidates = new LinkedHashSet<>();

        for (String token : tokens) {
            // Search keyword index
            Set<String> keywordHits = keywordIndex.getOrDefault(token, Collections.emptySet());
            candidates.addAll(keywordHits);

            // Try section lookup (e.g., "420" -> "IPC_420")
            String sectionHit = sectionIndex.get(token);
            if (sectionHit != null) {
                candidates.add(sectionHit);
            }

            // Try act lookup
            Set<String> actHits = actIndex.getOrDefault(token.toUpperCase(), Collections.emptySet());
            candidates.addAll(actHits);
        }

        return candidates;
    }

    public Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", " ")
                        .split("\\s+"))
                .filter(t -> t.length() > 1)
                .filter(t -> !STOPWORDS.contains(t))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String extractActKey(String docId) {
        int underscoreIdx = docId.indexOf('_');
        if (underscoreIdx > 0) {
            return docId.substring(0, underscoreIdx);
        }
        return docId;
    }

    public Map<String, Set<String>> getKeywordIndex() { return keywordIndex; }
    public Map<String, String> getSectionIndex() { return sectionIndex; }
    public Map<String, Set<String>> getActIndex() { return actIndex; }
}

package com.legalai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
public class IntentService {

    private static final Logger log = LoggerFactory.getLogger(IntentService.class);

    // Legal keywords and phrases
    private static final Set<String> LEGAL_KEYWORDS = Set.of(
            "law", "laws", "legal", "act", "acts", "section", "sections", "article", "articles", "clause", "provision",
            "statute", "legislation", "ordinance", "regulation", "rule", "amendment",
            "constitution", "constitutional", "fundamental", "rights",
            
            // Criminal law
            "crime", "criminal", "offence", "offenses", "offense", "punishment", "penalty",
            "imprisonment", "fine", "bail", "arrest", "fir", "cognizable",
            "non-cognizable", "bailable", "non-bailable", "murder", "theft", "stolen", "steal",
            "robbery", "cheating", "fraud", "forgery", "kidnapping", "assault",
            "rape", "dowry", "cruelty", "defamation", "conspiracy", "police",
            "abetment", "attempt", "culpable", "homicide",
            
            // Civil law
            "civil", "suit", "petition", "appeal", "plea", "writ", "injunction",
            "damages", "compensation", "contract", "agreement", "breach",
            "negligence", "liability", "indemnity", "guarantee", "surety",
            
            // Family law
            "marriage", "divorce", "custody", "maintenance", "alimony",
            "adoption", "guardianship", "succession", "inheritance", "will",
            "hindu", "muslim", "christian",
            
            // Property law
            "property", "land", "transfer", "sale", "mortgage", "lease",
            "tenant", "landlord", "eviction", "possession", "title",
            "registration", "stamp", "easement",
            
            // Company law
            "company", "corporate", "director", "shareholder", "board",
            "incorporation", "merger", "acquisition", "winding",
            "insolvency", "bankruptcy",
            
            // Cyber law
            "cyber", "cybercrime", "hacking", "data", "privacy",
            "electronic", "digital", "information technology",
            
            // Courts and judiciary
            "court", "judge", "magistrate", "tribunal", "supreme",
            "high court", "district", "sessions", "jurisdiction",
            "hearing", "trial", "verdict", "judgment", "order",
            "decree", "stay", "contempt",
            
            // Legal procedures
            "evidence", "witness", "testimony", "confession", "statement",
            "prosecution", "defense", "accused", "complainant", "plaintiff",
            "defendant", "respondent", "petitioner", "advocate", "lawyer",
            "affidavit", "summons", "warrant", "subpoena",
            
            // Specific acts (short forms)
            "ipc", "crpc", "cpc", "bns", "bnss", "bsa",
            "ndps", "rti", "pocso", "posh",
            
            // Consumer
            "consumer", "complaint", "deficiency", "defect",
            
            // Labour
            "labour", "labor", "employment", "wages", "workman",
            "industrial", "dispute", "strike", "lockout",
            
            // Arbitration
            "arbitration", "mediation", "conciliation", "adr",
            "alternative dispute",
            
            // Motor vehicles
            "motor", "vehicle", "accident", "insurance", "claim",
            "driving", "licence", "license",
            
            // Negotiable instruments
            "cheque", "check", "dishonour", "dishonor", "bounced",
            "negotiable", "promissory", "bill of exchange",
            
            // Rights
            "fundamental rights", "directive", "habeas corpus",
            "mandamus", "certiorari", "prohibition",
            
            // Legal maxims
            "mens rea", "actus reus", "res judicata", "stare decisis",
            "ultra vires", "locus standi",

            // General
            "illegal", "unlawful", "lawful", "legality", "judicial",
            "justice", "equity", "relief", "remedy", "limitation",
            "prescribed", "cognizance",

            // New acts from user's PDF collection
            "rera", "real estate", "promoter", "builder", "allottee",
            "ibc", "cirp", "resolution",
            "nclt", "nclat", "liquidation", "creditor", "debtor",
            "pmla", "money laundering", "proceeds of crime",
            "banking", "bank", "rbi", "reserve bank", "deposit",
            "mediator", "settlement",
            "partnership", "partner", "firm",
            "goods", "buyer", "seller", "delivery",
            "special marriage", "coparcener",

            // Common conversational legal follow-ups
            "documents", "document", "case", "cases", "procedure", "applicable"
    );

    // Patterns that strongly indicate legal queries
    private static final Pattern LEGAL_PATTERN = Pattern.compile(
            "(?i)(section\\s+\\d+|article\\s+\\d+|ipc\\s+\\d+|" +
            "what\\s+is\\s+.*?(law|act|section|punishment|offence|crime)|" +
            "explain\\s+.*?(law|act|section|right|provision)|" +
            "punishment\\s+for|penalty\\s+for|" +
            "under\\s+which\\s+(section|act)|" +
            "according\\s+to\\s+(law|act|section)|" +
            "is\\s+it\\s+(legal|illegal|lawful|unlawful|crime)|" +
            "can\\s+i\\s+(sue|file|appeal|claim)|" +
            "how\\s+to\\s+(file|register|appeal|sue)|" +
            "rights\\s+of|duties\\s+of|" +
            "what\\s+are\\s+.*?(rights|duties|obligations|penalties)|" +
            "tell\\s+me\\s+about\\s+.*?(law|act|section|crime|right)|" +
            "discuss\\s+.*?(law|act|contract|crime|property|cyber))"
    );

    /**
     * Determines if a query is related to Indian law.
     *
     * @param query the user's query
     * @return true if the query is legal-related
     */
    public boolean isLegalQuery(String query) {
        if (query == null || query.trim().isEmpty()) return false;

        String lowerQuery = query.toLowerCase().trim();

        // Check against pattern first (strong signal)
        if (LEGAL_PATTERN.matcher(lowerQuery).find()) {
            log.debug("Query matched legal pattern: {}", query);
            return true;
        }

        // Check for legal keywords
        String[] words = lowerQuery.split("\\W+");
        int legalWordCount = 0;

        for (String word : words) {
            if (LEGAL_KEYWORDS.contains(word)) {
                legalWordCount++;
            }
        }

        // Also check for multi-word legal terms
        for (String keyword : LEGAL_KEYWORDS) {
            if (keyword.contains(" ") && lowerQuery.contains(keyword)) {
                legalWordCount += 2;
            }
        }

        // Consider legal if at least one legal keyword found
        boolean isLegal = legalWordCount >= 1;

        log.debug("Query '{}' - legal keywords found: {}, isLegal: {}", query, legalWordCount, isLegal);
        return isLegal;
    }

    public static final String REJECTION_MESSAGE =
            "I'm designed to assist only with legal questions related to Indian law.";
}

package com.legalai.service;

import com.legalai.model.LegalDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PDFParserService {

    private static final Logger log = LoggerFactory.getLogger(PDFParserService.class);

    @Value("${legal.acts.folder:legal_acts}")
    private String actsFolder;

    private final List<LegalDocument> allDocuments = new ArrayList<>();

    // ======================================================================
    // EXACT FILENAME → (Act Abbreviation , Full Act Name)
    // Matches the 23 PDFs in YOUR legal_acts/ folder
    // ======================================================================
    private record ActInfo(String abbreviation, String fullName) {}

    private static final Map<String, ActInfo> FILE_TO_ACT = new LinkedHashMap<>();
    static {
        FILE_TO_ACT.put("ipc_act",                                                      new ActInfo("IPC",          "Indian Penal Code, 1860"));
        FILE_TO_ACT.put("the_code_of_criminal_procedure,_1973",                          new ActInfo("CrPC",         "Code of Criminal Procedure, 1973"));
        FILE_TO_ACT.put("iea_1872",                                                      new ActInfo("IEA",          "Indian Evidence Act, 1872"));
        FILE_TO_ACT.put("193003",                                                        new ActInfo("SaleOfGoods",  "Sale of Goods Act, 1930"));
        FILE_TO_ACT.put("A187209",                                                       new ActInfo("Contract",     "Indian Contract Act, 1872"));
        FILE_TO_ACT.put("A1882-04",                                                      new ActInfo("TPA",          "Transfer of Property Act, 1882"));
        FILE_TO_ACT.put("A1908-16",                                                      new ActInfo("CPC",          "Code of Civil Procedure, 1908"));
        FILE_TO_ACT.put("A195443",                                                       new ActInfo("SMA",          "Special Marriage Act, 1954"));
        FILE_TO_ACT.put("A1955-25",                                                      new ActInfo("HMA",          "Hindu Marriage Act, 1955"));
        FILE_TO_ACT.put("AAA1956suc___30",                                               new ActInfo("HSA",          "Hindu Succession Act, 1956"));
        FILE_TO_ACT.put("A1963-47",                                                      new ActInfo("Limitation",   "Limitation Act, 1963"));
        FILE_TO_ACT.put("A2003-12",                                                      new ActInfo("PMLA",         "Prevention of Money Laundering Act, 2002"));
        FILE_TO_ACT.put("A2005-43",                                                      new ActInfo("RTI",          "Right to Information Act, 2005"));
        FILE_TO_ACT.put("A2013-18",                                                      new ActInfo("Companies",    "Companies Act, 2013"));
        FILE_TO_ACT.put("A2016-16",                                                      new ActInfo("RERA",         "Real Estate (Regulation and Development) Act, 2016"));
        FILE_TO_ACT.put("a1949-54",                                                      new ActInfo("BRA",          "Banking Regulation Act, 1949"));
        FILE_TO_ACT.put("a2023-22",                                                      new ActInfo("Mediation",    "Mediation Act, 2023"));
        FILE_TO_ACT.put("eng201935",                                                     new ActInfo("Consumer",     "Consumer Protection Act, 2019"));
        FILE_TO_ACT.put("indian_partnership_act_1932",                                   new ActInfo("Partnership",  "Indian Partnership Act, 1932"));
        FILE_TO_ACT.put("it_act_2000_updated",                                           new ActInfo("IT",           "Information Technology Act, 2000"));
        FILE_TO_ACT.put("it_amendment_act2008",                                          new ActInfo("ITAmend",      "Information Technology (Amendment) Act, 2008"));
        FILE_TO_ACT.put("the_consumer_protection_act,_2019_no._35_of_2019_date_09.08.2019", new ActInfo("Consumer2019", "Consumer Protection Act, 2019"));
        FILE_TO_ACT.put("the_insolvency_and_bankruptcy_code,_2016",                      new ActInfo("IBC",          "Insolvency and Bankruptcy Code, 2016"));
    }

    // Fallback generic abbreviation map (for any PDFs not in the exact map)
    private static final Map<String, String> GENERIC_ACT_MAP = new LinkedHashMap<>();
    static {
        GENERIC_ACT_MAP.put("ipc",          "Indian Penal Code");
        GENERIC_ACT_MAP.put("crpc",         "Code of Criminal Procedure");
        GENERIC_ACT_MAP.put("cpc",          "Code of Civil Procedure");
        GENERIC_ACT_MAP.put("evidence",     "Indian Evidence Act");
        GENERIC_ACT_MAP.put("contract",     "Indian Contract Act");
        GENERIC_ACT_MAP.put("companies",    "Companies Act");
        GENERIC_ACT_MAP.put("consumer",     "Consumer Protection Act");
        GENERIC_ACT_MAP.put("it_act",       "Information Technology Act");
        GENERIC_ACT_MAP.put("partnership",  "Indian Partnership Act");
        GENERIC_ACT_MAP.put("transfer",     "Transfer of Property Act");
        GENERIC_ACT_MAP.put("limitation",   "Limitation Act");
        GENERIC_ACT_MAP.put("arbitration",  "Arbitration and Conciliation Act");
        GENERIC_ACT_MAP.put("insolvency",   "Insolvency and Bankruptcy Code");
        GENERIC_ACT_MAP.put("bankruptcy",   "Insolvency and Bankruptcy Code");
        GENERIC_ACT_MAP.put("hindu",        "Hindu Marriage Act");
        GENERIC_ACT_MAP.put("sale",         "Sale of Goods Act");
        GENERIC_ACT_MAP.put("rti",          "Right to Information Act");
        GENERIC_ACT_MAP.put("rera",         "Real Estate (Regulation and Development) Act");
        GENERIC_ACT_MAP.put("mediation",    "Mediation Act");
        GENERIC_ACT_MAP.put("banking",      "Banking Regulation Act");
        GENERIC_ACT_MAP.put("pmla",         "Prevention of Money Laundering Act");
        GENERIC_ACT_MAP.put("motor",        "Motor Vehicles Act");
        GENERIC_ACT_MAP.put("ndps",         "NDPS Act");
        GENERIC_ACT_MAP.put("constitution", "Constitution of India");
    }

    @PostConstruct
    public void init() {
        File folder = new File(actsFolder);
        if (!folder.exists() || !folder.isDirectory()) {
            folder = new File(System.getProperty("user.dir"), actsFolder);
        }
        if (!folder.exists() || !folder.isDirectory()) {
            log.warn("Legal acts folder not found at: {}. Creating sample documents.", actsFolder);
            createSampleDocuments();
            return;
        }

        File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
        if (pdfFiles == null || pdfFiles.length == 0) {
            log.warn("No PDF files found in: {}. Creating sample documents.", folder.getAbsolutePath());
            createSampleDocuments();
            return;
        }

        log.info("========================================");
        log.info("  PDF PARSING STARTED — {} files found", pdfFiles.length);
        log.info("========================================");

        for (File pdf : pdfFiles) {
            try {
                List<LegalDocument> docs = parsePdf(pdf);
                allDocuments.addAll(docs);
                log.info("  ✓ {} — {} sections extracted", pdf.getName(), docs.size());
            } catch (Exception e) {
                log.error("  ✗ {} — FAILED: {}", pdf.getName(), e.getMessage());
            }
        }

        // Also add sample enrichment documents for better query coverage
        createSampleDocuments();

        log.info("========================================");
        log.info("  TOTAL DOCUMENTS INDEXED: {}", allDocuments.size());
        log.info("========================================");
    }

    public List<LegalDocument> parsePdf(File pdfFile) throws IOException {
        List<LegalDocument> documents = new ArrayList<>();
        String fileNameNoExt = pdfFile.getName()
                .replaceAll("(?i)\\.pdf$", "");

        // Look up exact filename in our mapping
        ActInfo actInfo = FILE_TO_ACT.get(fileNameNoExt);
        String actAbbrev;
        String actName;

        if (actInfo != null) {
            actAbbrev = actInfo.abbreviation();
            actName   = actInfo.fullName();
        } else {
            actAbbrev = deriveActAbbreviation(fileNameNoExt);
            actName   = deriveActName(fileNameNoExt);
        }

        log.debug("Parsing {} → abbreviation={}, name={}", pdfFile.getName(), actAbbrev, actName);

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);

            // Try structured section extraction first
            List<LegalDocument> structured = extractSections(fullText, actAbbrev, actName, pdfFile.getName());

            // If we extracted very few sections relative to the document size, extraction likely failed.
            if (structured.size() > 15 || (document.getNumberOfPages() <= 10 && !structured.isEmpty())) {
                documents.addAll(structured);
            } else {
                log.warn("  --> Structured extraction poor for {} (got {} sections). Falling back to pages.", pdfFile.getName(), structured.size());
                documents.addAll(extractByPages(document, actAbbrev, actName, pdfFile.getName()));
            }
        }

        return documents;
    }

    // ======================================================================
    //  Section extraction — tries multiple regex patterns common in Indian Acts
    // ======================================================================
    private List<LegalDocument> extractSections(String text, String actAbbrev, String actName, String sourceFile) {
        List<LegalDocument> bestDocs = new ArrayList<>();

        // Multiple patterns that appear in Indian legal act PDFs
        Pattern[] patterns = {
            // "Section 420. Cheating …"  or  "Section 420 \n Cheating …"
            Pattern.compile("(?mi)(?:^|\\n)\\s*(?:Section|Sec\\.)\\s+(\\d+[A-Za-z]?)[\\.\\s:\\-–—\\n]+\\s*([A-Z][^\\n]{3,})"),
            // "420. Cheating and …"  (number-dot-title)
            Pattern.compile("(?m)(?:^|\\n)\\s*(\\d+[A-Za-z]?)\\.\\s+([A-Z][^\\n]{3,})"),
            // "Article 14. Equality …"  (Constitution)
            Pattern.compile("(?m)(?:^|\\n)\\s*(?:Article|ARTICLE)\\s+(\\d+[A-Za-z]?)[\\.\\s:\\-–—]+\\s*([^\\n]{3,})"),
            // "S. 420 Cheating"
            Pattern.compile("(?mi)(?:^|\\n)\\s*(?:S\\.|CHAPTER)\\s*(\\d+[A-Za-z]?)[\\.\\s:\\-–—]+\\s*([^\\n]{3,})")
        };

        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(text);
            List<int[]> matchPositions = new ArrayList<>();
            List<String[]> matchGroups = new ArrayList<>();
            Set<String> seenSections = new HashSet<>();

            while (matcher.find()) {
                String sectionNum = matcher.group(1);
                String title = matcher.group(2).trim();

                // Skip dubious matches (too-short titles, page numbers, etc.)
                if (title.length() < 3 || title.matches("^\\d+$")) continue;

                if (!seenSections.contains(sectionNum)) {
                    seenSections.add(sectionNum);
                    matchPositions.add(new int[]{matcher.start(), matcher.end()});
                    matchGroups.add(new String[]{sectionNum, title});
                }
            }

            List<LegalDocument> currentDocs = new ArrayList<>();
            // Extract content between section headings
            for (int i = 0; i < matchPositions.size(); i++) {
                int start = matchPositions.get(i)[1]; // end of heading
                int end = (i + 1 < matchPositions.size()) ? matchPositions.get(i + 1)[0] : Math.min(start + 2500, text.length());

                String content = text.substring(start, end).trim()
                        .replaceAll("\\r", "")
                        .replaceAll("\\n{3,}", "\n\n");

                // Limit content length
                if (content.length() > 2000) {
                    content = content.substring(0, 2000) + "...";
                }

                String sectionNum = matchGroups.get(i)[0];
                String title = matchGroups.get(i)[1].replaceAll("[.\\-–—]+$", "").trim();

                String id = actAbbrev + "_" + sectionNum;
                currentDocs.add(new LegalDocument(id, actName, sectionNum, title, content, sourceFile));
            }

            // We want the pattern that matches the MOST distinct sections. 
            // This prevents a single random match from overriding the page fallbacks.
            if (currentDocs.size() > bestDocs.size()) {
                bestDocs = currentDocs;
            }
        }

        return bestDocs;
    }

    // ======================================================================
    //  Page-based fallback when no section headings are detected
    // ======================================================================
    private List<LegalDocument> extractByPages(PDDocument document, String actAbbrev, String actName, String sourceFile) throws IOException {
        List<LegalDocument> docs = new ArrayList<>();
        int totalPages = document.getNumberOfPages();
        PDFTextStripper stripper = new PDFTextStripper();

        for (int page = 1; page <= totalPages; page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);
            String pageText = stripper.getText(document).trim();

            if (pageText.length() < 50) continue;

            // Clean
            pageText = pageText.replaceAll("\\r", "");
            if (pageText.length() > 1500) {
                pageText = pageText.substring(0, 1500) + "...";
            }

            String id = actAbbrev + "_page_" + page;
            String title = actName + " — Page " + page;
            LegalDocument doc = new LegalDocument(id, actName, "page_" + page, title, pageText, sourceFile);
            docs.add(doc);
        }

        return docs;
    }

    // ======================================================================
    //  Fallback name derivation when filename is not in FILE_TO_ACT
    // ======================================================================
    private String deriveActAbbreviation(String fileName) {
        String lower = fileName.toLowerCase().replaceAll("[_\\-\\s]+", "");
        for (Map.Entry<String, String> entry : GENERIC_ACT_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getKey().toUpperCase();
            }
        }
        return fileName.replaceAll("[^A-Za-z0-9]", "");
    }

    private String deriveActName(String fileName) {
        String lower = fileName.toLowerCase().replaceAll("[_\\-\\s]+", "");
        for (Map.Entry<String, String> entry : GENERIC_ACT_MAP.entrySet()) {
            if (lower.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return fileName.replaceAll("([a-z])([A-Z])", "$1 $2")
                       .replaceAll("[_\\-]", " ")
                       .trim();
    }

    public List<LegalDocument> getAllDocuments() {
        return Collections.unmodifiableList(allDocuments);
    }

    // ======================================================================
    //  SAMPLE DOCUMENTS — enrichment so common queries always find results
    //  These supplement whatever is parsed from your 23 PDFs.
    // ======================================================================
    private void createSampleDocuments() {
        log.info("Adding enrichment sample documents for better query coverage...");

        // Keep a set of existing IDs to avoid duplicates with PDF-parsed content
        Set<String> existingIds = new HashSet<>();
        for (LegalDocument doc : allDocuments) {
            existingIds.add(doc.getId());
        }

        List<LegalDocument> samples = new ArrayList<>();

        // === INDIAN PENAL CODE, 1860 ===
        samples.add(new LegalDocument("IPC_302", "Indian Penal Code, 1860", "302",
                "Punishment for murder",
                "Whoever commits murder shall be punished with death, or imprisonment for life, and shall also be liable to fine. Murder is defined under Section 300 of the Indian Penal Code. The court considers the gravity of the offence, the manner in which it was committed, and the motive behind the act while awarding the sentence.",
                "ipc_act.pdf"));

        samples.add(new LegalDocument("IPC_304", "Indian Penal Code, 1860", "304",
                "Punishment for culpable homicide not amounting to murder",
                "Whoever commits culpable homicide not amounting to murder shall be punished with imprisonment for life, or imprisonment of either description for a term which may extend to ten years, and shall also be liable to fine, if the act by which the death is caused is done with the intention of causing death, or of causing such bodily injury as is likely to cause death.",
                "ipc_act.pdf"));

        samples.add(new LegalDocument("IPC_307", "Indian Penal Code, 1860", "307",
                "Attempt to murder",
                "Whoever does any act with such intention or knowledge, and under such circumstances that, if he by that act caused death, he would be guilty of murder, shall be punished with imprisonment of either description for a term which may extend to ten years, and shall also be liable to fine; and if hurt is caused to any person by such act, the offender shall be liable either to imprisonment for life, or to such punishment as is hereinbefore mentioned.",
                "ipc_act.pdf"));

        samples.add(new LegalDocument("IPC_376", "Indian Penal Code, 1860", "376",
                "Punishment for rape",
                "Whoever commits rape shall be punished with rigorous imprisonment of either description for a term which shall not be less than ten years, but which may extend to imprisonment for life, and shall also be liable to fine.",
                "ipc_act.pdf"));

        samples.add(new LegalDocument("IPC_379", "Indian Penal Code, 1860", "379",
                "Punishment for theft",
                "Whoever commits theft shall be punished with imprisonment of either description for a term which may extend to three years, or with fine, or with both.",
                "ipc_act.pdf"));

        samples.add(new LegalDocument("IPC_392", "Indian Penal Code, 1860", "392",
                "Punishment for robbery",
                "Whoever commits robbery shall be punished with rigorous imprisonment for a term which may extend to ten years, and shall also be liable to fine; and, if the robbery be committed on the highway between sunset and sunrise, the imprisonment may be of either description, and may extend to fourteen years.",
                "ipc_act.pdf"));

        samples.add(new LegalDocument("IPC_420", "Indian Penal Code, 1860", "420",
                "Cheating and dishonestly inducing delivery of property",
                "Whoever cheats and thereby dishonestly induces the person deceived to deliver any property to any person, or to make, alter or destroy the whole or any part of a valuable security, or anything which is signed or sealed, and which is capable of being converted into a valuable security, shall be punished with imprisonment of either description for a term which may extend to seven years, and shall also be liable to fine.",
                "ipc_act.pdf"));

        samples.add(new LegalDocument("IPC_498A", "Indian Penal Code, 1860", "498A",
                "Husband or relative of husband of a woman subjecting her to cruelty",
                "Whoever, being the husband or the relative of the husband of a woman, subjects such woman to cruelty shall be punished with imprisonment for a term which may extend to three years and shall also be liable to fine.",
                "ipc_act.pdf"));

        samples.add(new LegalDocument("IPC_34", "Indian Penal Code, 1860", "34",
                "Acts done by several persons in furtherance of common intention",
                "When a criminal act is done by several persons in furtherance of the common intention of all, each of such persons is liable for that act in the same manner as if it were done by him alone.",
                "ipc_act.pdf"));

        samples.add(new LegalDocument("IPC_120B", "Indian Penal Code, 1860", "120B",
                "Punishment of criminal conspiracy",
                "Whoever is a party to a criminal conspiracy to commit an offence punishable with death, imprisonment for life or rigorous imprisonment for a term of two years or upwards, shall be punished in the same manner as if he had abetted such offence.",
                "ipc_act.pdf"));

        // === CODE OF CRIMINAL PROCEDURE, 1973 ===
        samples.add(new LegalDocument("CrPC_41", "Code of Criminal Procedure, 1973", "41",
                "When police may arrest without warrant",
                "Any police officer may without an order from a Magistrate and without a warrant, arrest any person who has been concerned in any cognizable offence, or against whom a reasonable complaint has been made.",
                "the_code_of_criminal_procedure,_1973.pdf"));

        samples.add(new LegalDocument("CrPC_125", "Code of Criminal Procedure, 1973", "125",
                "Order for maintenance of wives, children and parents",
                "If any person having sufficient means neglects or refuses to maintain his wife, unable to maintain herself, or his legitimate or illegitimate minor child, a Magistrate of the first class may order such person to make a monthly allowance.",
                "the_code_of_criminal_procedure,_1973.pdf"));

        samples.add(new LegalDocument("CrPC_144", "Code of Criminal Procedure, 1973", "144",
                "Power to issue order in urgent cases of nuisance or apprehended danger",
                "A District Magistrate or Sub-divisional Magistrate may by written order direct any person to abstain from a certain act if immediate prevention or speedy remedy is desirable.",
                "the_code_of_criminal_procedure,_1973.pdf"));

        samples.add(new LegalDocument("CrPC_154", "Code of Criminal Procedure, 1973", "154",
                "Information in cognizable cases (FIR)",
                "Every information relating to the commission of a cognizable offence, if given orally to an officer in charge of a police station, shall be reduced to writing by him or under his direction, and be read over to the informant.",
                "the_code_of_criminal_procedure,_1973.pdf"));

        samples.add(new LegalDocument("CrPC_438", "Code of Criminal Procedure, 1973", "438",
                "Direction for grant of bail to person apprehending arrest (Anticipatory Bail)",
                "Where any person has reason to believe that he may be arrested on accusation of having committed a non-bailable offence, he may apply to the High Court or the Court of Session for a direction that in the event of such arrest, he shall be released on bail.",
                "the_code_of_criminal_procedure,_1973.pdf"));

        // === INDIAN EVIDENCE ACT, 1872 ===
        samples.add(new LegalDocument("IEA_25", "Indian Evidence Act, 1872", "25",
                "Confession to police officer not to be proved",
                "No confession made to a police officer shall be proved as against a person accused of any offence.",
                "iea_1872.pdf"));

        samples.add(new LegalDocument("IEA_45", "Indian Evidence Act, 1872", "45",
                "Opinions of experts",
                "When the Court has to form an opinion upon a point of foreign law or of science or art, or as to identity of handwriting or finger impressions, the opinions upon that point of persons specially skilled are relevant facts.",
                "iea_1872.pdf"));

        samples.add(new LegalDocument("IEA_65B", "Indian Evidence Act, 1872", "65B",
                "Admissibility of electronic records",
                "Any information contained in an electronic record which is printed on paper, stored, recorded or copied in optical or magnetic media produced by a computer shall be deemed to be a document and shall be admissible in any proceedings.",
                "iea_1872.pdf"));

        // === INDIAN CONTRACT ACT, 1872 ===
        samples.add(new LegalDocument("Contract_10", "Indian Contract Act, 1872", "10",
                "What agreements are contracts",
                "All agreements are contracts if they are made by the free consent of parties competent to contract, for a lawful consideration and with a lawful object, and are not hereby expressly declared to be void.",
                "A187209.pdf"));

        samples.add(new LegalDocument("Contract_14", "Indian Contract Act, 1872", "14",
                "Free consent",
                "Consent is said to be free when it is not caused by coercion, undue influence, fraud, misrepresentation, or mistake.",
                "A187209.pdf"));

        samples.add(new LegalDocument("Contract_56", "Indian Contract Act, 1872", "56",
                "Agreement to do impossible act — Doctrine of Frustration",
                "An agreement to do an act impossible in itself is void. A contract to do an act which becomes impossible or unlawful becomes void. This embodies the doctrine of frustration.",
                "A187209.pdf"));

        samples.add(new LegalDocument("Contract_73", "Indian Contract Act, 1872", "73",
                "Compensation for loss or damage caused by breach of contract",
                "When a contract has been broken, the party who suffers by such breach is entitled to receive compensation for any loss or damage caused thereby.",
                "A187209.pdf"));

        // === COMPANIES ACT, 2013 ===
        samples.add(new LegalDocument("Companies_149", "Companies Act, 2013", "149",
                "Company to have Board of Directors",
                "Every company shall have a Board of Directors consisting of individuals as directors. A public company shall have a minimum of three directors.",
                "A2013-18.pdf"));

        samples.add(new LegalDocument("Companies_166", "Companies Act, 2013", "166",
                "Duties of directors",
                "A director of a company shall act in accordance with the articles of the company, act in good faith to promote the objects of the company for the benefit of its members as a whole.",
                "A2013-18.pdf"));

        samples.add(new LegalDocument("Companies_447", "Companies Act, 2013", "447",
                "Punishment for fraud",
                "Any person found guilty of fraud involving at least ten lakh rupees shall be punishable with imprisonment for a term of six months to ten years and shall also be liable to fine.",
                "A2013-18.pdf"));

        // === INFORMATION TECHNOLOGY ACT, 2000 ===
        samples.add(new LegalDocument("IT_43", "Information Technology Act, 2000", "43",
                "Penalty and compensation for damage to computer, computer system, etc.",
                "If any person without permission accesses or secures access to any computer or computer network, he shall be liable to pay damages by way of compensation.",
                "it_act_2000_updated.pdf"));

        samples.add(new LegalDocument("IT_66", "Information Technology Act, 2000", "66",
                "Computer related offences",
                "If any person dishonestly or fraudulently does any act referred to in section 43, he shall be punishable with imprisonment up to three years or fine up to five lakh rupees or both.",
                "it_act_2000_updated.pdf"));

        samples.add(new LegalDocument("IT_67", "Information Technology Act, 2000", "67",
                "Punishment for publishing obscene material in electronic form",
                "Whoever publishes or transmits obscene material in electronic form shall be punished on first conviction with imprisonment up to three years and fine up to five lakh rupees.",
                "it_act_2000_updated.pdf"));

        // === CONSUMER PROTECTION ACT, 2019 ===
        samples.add(new LegalDocument("Consumer_2", "Consumer Protection Act, 2019", "2",
                "Definitions",
                "'Consumer' means any person who buys any goods or hires any services for a consideration which has been paid or promised. 'Defect' means any fault, imperfection or shortcoming in quality, quantity, potency, purity or standard.",
                "the_consumer_protection_act,_2019_no._35_of_2019_date_09.08.2019.pdf"));

        samples.add(new LegalDocument("Consumer_35", "Consumer Protection Act, 2019", "35",
                "Jurisdiction of District Commission",
                "The District Commission shall have jurisdiction to entertain complaints where the value of goods or services does not exceed one crore rupees.",
                "the_consumer_protection_act,_2019_no._35_of_2019_date_09.08.2019.pdf"));

        // === TRANSFER OF PROPERTY ACT, 1882 ===
        samples.add(new LegalDocument("TPA_54", "Transfer of Property Act, 1882", "54",
                "Sale defined",
                "'Sale' is a transfer of ownership in exchange for a price paid or promised or part-paid and part-promised.",
                "A1882-04.pdf"));

        samples.add(new LegalDocument("TPA_58", "Transfer of Property Act, 1882", "58",
                "Mortgage defined",
                "A mortgage is the transfer of an interest in specific immovable property for the purpose of securing the payment of money advanced or to be advanced by way of loan.",
                "A1882-04.pdf"));

        // === HINDU MARRIAGE ACT, 1955 ===
        samples.add(new LegalDocument("HMA_5", "Hindu Marriage Act, 1955", "5",
                "Conditions for a Hindu marriage",
                "A marriage may be solemnized between any two Hindus if: neither party has a spouse living; at the time of marriage neither party is incapable of giving valid consent; the bridegroom has completed 21 years and the bride 18 years.",
                "A1955-25.pdf"));

        samples.add(new LegalDocument("HMA_13", "Hindu Marriage Act, 1955", "13",
                "Divorce",
                "A marriage may be dissolved by a decree of divorce on grounds including adultery, cruelty, desertion for two years, conversion, unsoundness of mind, leprosy, venereal disease, renunciation, or presumption of death.",
                "A1955-25.pdf"));

        samples.add(new LegalDocument("HMA_13B", "Hindu Marriage Act, 1955", "13B",
                "Divorce by mutual consent",
                "A petition for dissolution of marriage may be presented by both parties on the ground that they have been living separately for one year or more and have mutually agreed that the marriage should be dissolved.",
                "A1955-25.pdf"));

        // === HINDU SUCCESSION ACT, 1956 ===
        samples.add(new LegalDocument("HSA_6", "Hindu Succession Act, 1956", "6",
                "Devolution of interest in coparcenary property",
                "On and from the commencement of the Hindu Succession (Amendment) Act, 2005, the daughter of a coparcener shall by birth become a coparcener in her own right in the same manner as the son.",
                "AAA1956suc___30.pdf"));

        // === LIMITATION ACT, 1963 ===
        samples.add(new LegalDocument("Limitation_3", "Limitation Act, 1963", "3",
                "Bar of limitation",
                "Every suit instituted, appeal preferred, and application made after the prescribed period shall be dismissed although limitation has not been set up as a defence.",
                "A1963-47.pdf"));

        // === RTI ACT, 2005 ===
        samples.add(new LegalDocument("RTI_3", "Right to Information Act, 2005", "3",
                "Right to information",
                "Subject to the provisions of this Act, all citizens shall have the right to information.",
                "A2005-43.pdf"));

        samples.add(new LegalDocument("RTI_6", "Right to Information Act, 2005", "6",
                "Request for obtaining information",
                "A person who desires to obtain any information under this Act shall make a request in writing or through electronic means to the Central or State Public Information Officer.",
                "A2005-43.pdf"));

        // === INSOLVENCY AND BANKRUPTCY CODE, 2016 ===
        samples.add(new LegalDocument("IBC_7", "Insolvency and Bankruptcy Code, 2016", "7",
                "Initiation of CIRP by financial creditor",
                "A financial creditor either by itself or jointly with other financial creditors or any other person on behalf of the financial creditor may file an application for initiating corporate insolvency resolution process against a corporate debtor before the Adjudicating Authority.",
                "the_insolvency_and_bankruptcy_code,_2016.pdf"));

        samples.add(new LegalDocument("IBC_9", "Insolvency and Bankruptcy Code, 2016", "9",
                "Application by operational creditor",
                "After the expiry of the period of ten days from the date of delivery of the notice or invoice demanding payment, if the operational creditor does not receive payment from the corporate debtor or notice of the dispute, the operational creditor may file an application before the Adjudicating Authority for initiating a corporate insolvency resolution process.",
                "the_insolvency_and_bankruptcy_code,_2016.pdf"));

        // === SPECIAL MARRIAGE ACT, 1954 ===
        samples.add(new LegalDocument("SMA_4", "Special Marriage Act, 1954", "4",
                "Conditions relating to solemnization of special marriages",
                "A marriage between any two persons may be solemnized under this Act if neither party has a spouse living; neither party is an idiot or a lunatic; the male has completed the age of twenty-one years and the female the age of eighteen years; the parties are not within the degrees of prohibited relationship.",
                "A195443.pdf"));

        // === PREVENTION OF MONEY LAUNDERING ACT, 2002 ===
        samples.add(new LegalDocument("PMLA_3", "Prevention of Money Laundering Act, 2002", "3",
                "Offence of money-laundering",
                "Whosoever directly or indirectly attempts to indulge or knowingly assists or knowingly is a party or is actually involved in any process or activity connected with the proceeds of crime including its concealment, possession, acquisition or use and projecting or claiming it as untainted property shall be guilty of offence of money-laundering.",
                "A2003-12.pdf"));

        samples.add(new LegalDocument("PMLA_5", "Prevention of Money Laundering Act, 2002", "5",
                "Attachment of property involved in money-laundering",
                "Where the Director or any other officer not below the rank of Deputy Director has reason to believe that any person has committed an offence under section 3, he may provisionally attach any property believed to be the proceeds of crime.",
                "A2003-12.pdf"));

        // === REAL ESTATE (REGULATION AND DEVELOPMENT) ACT, 2016 ===
        samples.add(new LegalDocument("RERA_3", "Real Estate (Regulation and Development) Act, 2016", "3",
                "Registration of real estate project",
                "No promoter shall advertise, market, book, sell or offer for sale, or invite persons to purchase in any manner any plot, apartment or building, in any real estate project or part of it, in any planning area, without registering the real estate project with the Real Estate Regulatory Authority.",
                "A2016-16.pdf"));

        // === INDIAN PARTNERSHIP ACT, 1932 ===
        samples.add(new LegalDocument("Partnership_4", "Indian Partnership Act, 1932", "4",
                "Definition of Partnership",
                "'Partnership' is the relation between persons who have agreed to share the profits of a business carried on by all or any of them acting for all. Persons who have entered into partnership with one another are called individually 'partners' and collectively 'a firm'.",
                "indian_partnership_act_1932.pdf"));

        // === SALE OF GOODS ACT, 1930 ===
        samples.add(new LegalDocument("SaleOfGoods_4", "Sale of Goods Act, 1930", "4",
                "Sale and agreement to sell",
                "A contract of sale of goods is a contract whereby the seller transfers or agrees to transfer the property in goods to the buyer for a price. Where under a contract of sale the property in the goods is transferred from the seller to the buyer, the contract is called a sale.",
                "193003.pdf"));

        // === CODE OF CIVIL PROCEDURE, 1908 ===
        samples.add(new LegalDocument("CPC_9", "Code of Civil Procedure, 1908", "9",
                "Courts to try all civil suits unless barred",
                "The Courts shall (subject to the provisions herein contained) have jurisdiction to try all suits of a civil nature excepting suits of which their cognizance is either expressly or impliedly barred.",
                "A1908-16.pdf"));

        samples.add(new LegalDocument("CPC_89", "Code of Civil Procedure, 1908", "89",
                "Settlement of disputes outside the Court",
                "Where it appears to the Court that there exist elements of a settlement which may be acceptable to the parties, the Court shall formulate the terms of settlement and give them to the parties for their observations.",
                "A1908-16.pdf"));

        // === BANKING REGULATION ACT, 1949 ===
        samples.add(new LegalDocument("BRA_5", "Banking Regulation Act, 1949", "5",
                "Interpretation",
                "'Banking' means the accepting, for the purpose of lending or investment, of deposits of money from the public, repayable on demand or otherwise, and withdrawable by cheque, draft, order or otherwise. 'Banking company' means any company which transacts the business of banking in India.",
                "a1949-54.pdf"));

        // === MEDIATION ACT, 2023 ===
        samples.add(new LegalDocument("Mediation_3", "Mediation Act, 2023", "3",
                "Mediation",
                "Mediation means a process, whether referred to by the expression mediation, pre-litigation mediation, online mediation, community mediation, conciliation or an expression of similar import, whereby parties attempt to reach an amicable settlement of their dispute with the assistance of a third person referred to as the mediator.",
                "a2023-22.pdf"));

        // Add only samples whose IDs don't already exist from PDF parsing
        int added = 0;
        for (LegalDocument sample : samples) {
            if (!existingIds.contains(sample.getId())) {
                allDocuments.add(sample);
                existingIds.add(sample.getId());
                added++;
            }
        }

        log.info("Added {} enrichment sample documents (skipped {} duplicates)",
                added, samples.size() - added);
    }
}

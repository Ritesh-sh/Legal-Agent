package com.legalai.model;

public class LegalDocument {
    private String id;
    private String actName;
    private String sectionNumber;
    private String title;
    private String content;
    private String sourceFile;

    public LegalDocument() {}

    public LegalDocument(String id, String actName, String sectionNumber, String title, String content, String sourceFile) {
        this.id = id;
        this.actName = actName;
        this.sectionNumber = sectionNumber;
        this.title = title;
        this.content = content;
        this.sourceFile = sourceFile;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getActName() { return actName; }
    public void setActName(String actName) { this.actName = actName; }
    public String getSectionNumber() { return sectionNumber; }
    public void setSectionNumber(String sectionNumber) { this.sectionNumber = sectionNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSourceFile() { return sourceFile; }
    public void setSourceFile(String sourceFile) { this.sourceFile = sourceFile; }

    @Override
    public String toString() {
        return "LegalDocument{id='" + id + "', actName='" + actName + "', section='" + sectionNumber + "'}";
    }
}

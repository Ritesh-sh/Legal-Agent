package com.legalai.model;

import java.util.List;

public class QueryResponse {
    private String answer;
    private List<String> sources;
    private String scholarLink;

    public QueryResponse() {}

    public QueryResponse(String answer, List<String> sources, String scholarLink) {
        this.answer = answer;
        this.sources = sources;
        this.scholarLink = scholarLink;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
    public String getScholarLink() { return scholarLink; }
    public void setScholarLink(String scholarLink) { this.scholarLink = scholarLink; }
}

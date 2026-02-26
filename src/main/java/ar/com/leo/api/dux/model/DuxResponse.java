package ar.com.leo.api.dux.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DuxResponse {

    @JsonProperty("paging")
    private Paging paging;

    @JsonProperty("results")
    private List<Item> results;

    public Paging getPaging() { return paging; }
    public void setPaging(Paging paging) { this.paging = paging; }
    public List<Item> getResults() { return results; }
    public void setResults(List<Item> results) { this.results = results; }
}

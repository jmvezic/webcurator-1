package org.webcurator.core.visualization.networkmap.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkMapNodeDTO extends NetworkMapCommonNode{
    public static final int SEED_TYPE_PRIMARY = 0;
    public static final int SEED_TYPE_SECONDARY = 1;
    public static final int SEED_TYPE_OTHER = 2;
    
    protected String url;

    protected long parentId = -1;
    protected long offset;
    protected long fetchTimeMs; //ms: time used to download the page
    protected String fileName;

    protected List<Long> outlinks = new ArrayList<>();
    protected List<NetworkMapNodeDTO> children = new ArrayList<>();

    protected String title;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public long getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = parentId;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public long getFetchTimeMs() {
        return fetchTimeMs;
    }

    public void setFetchTimeMs(long fetchTimeMs) {
        this.fetchTimeMs = fetchTimeMs;
    }

    public List<Long> getOutlinks() {
        return outlinks;
    }

    public void setOutlinks(List<Long> outlinks) {
        this.outlinks = outlinks;
    }

    public List<NetworkMapNodeDTO> getChildren() {
        return children;
    }

    public void setChildren(List<NetworkMapNodeDTO> children) {
        this.children = children;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @JsonIgnore
    public void clear() {
        this.outlinks.clear();
        this.children.forEach(NetworkMapNodeDTO::clear);
        this.children.clear();
    }

    @JsonIgnore
    public void putChild(NetworkMapNodeDTO e) {
        this.children.add(e);
    }

    @JsonIgnore
    public String getUnlString() {
        String strOutlinks = outlinks.stream().map(outlink -> Long.toString(outlink)).collect(Collectors.joining(","));
        return String.format("%d %s %d %d %d %d %d %d %d %s %s %d %d %d %s %b [%s]",
                id, url, seedType, totUrls, totSuccess, totFailed, totSize,
                domainId, contentLength, contentType, statusCode, parentId, offset, fetchTimeMs,
                fileName, isSeed, strOutlinks);
    }
}

package org.webcurator.core.visualization.networkmap.metadata;

public class NetworkMapTreeNodeDTO {
    private boolean categorizedByDomain =true;
    private String title;

    private boolean isSeed = false; //true: if url equals seed or domain contains seed url.
    private int seedType = -1;

    /////////////////////////////////////////////////////////////////////////////////////////
    // 1. Domain: the total items of all urls contained in this domain.
    // 2. URL: the total items of all urls directly link to this url and the url itself
    private int totUrls = 0;
    private int totSuccess = 0;
    private int totFailed = 0;
    private long totSize = 0;
    ///////////////////////////////////////////////////////////////////////////////////////////

    private long domainId = -1; //default: no domain
    private long contentLength;
    private String contentType;
    private int statusCode;

    public boolean isCategorizedByDomain() {
        return categorizedByDomain;
    }

    public void setCategorizedByDomain(boolean categorizedByDomain) {
        this.categorizedByDomain = categorizedByDomain;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isSeed() {
        return isSeed;
    }

    public void setSeed(boolean seed) {
        isSeed = seed;
    }

    public int getSeedType() {
        return seedType;
    }

    public void setSeedType(int seedType) {
        this.seedType = seedType;
    }

    public int getTotUrls() {
        return totUrls;
    }

    public void setTotUrls(int totUrls) {
        this.totUrls = totUrls;
    }

    public int getTotSuccess() {
        return totSuccess;
    }

    public void setTotSuccess(int totSuccess) {
        this.totSuccess = totSuccess;
    }

    public int getTotFailed() {
        return totFailed;
    }

    public void setTotFailed(int totFailed) {
        this.totFailed = totFailed;
    }

    public long getTotSize() {
        return totSize;
    }

    public void setTotSize(long totSize) {
        this.totSize = totSize;
    }

    public long getDomainId() {
        return domainId;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}

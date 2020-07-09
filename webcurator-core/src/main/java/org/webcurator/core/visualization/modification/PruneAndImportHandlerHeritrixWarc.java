package org.webcurator.core.visualization.modification;

import org.archive.format.warc.WARCConstants;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.ArchiveRecordHeader;
import org.archive.io.arc.ARCReader;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCRecordInfo;
import org.archive.io.warc.WARCWriter;
import org.archive.io.warc.WARCWriterPoolSettings;
import org.archive.io.warc.WARCWriterPoolSettingsData;
import org.archive.io.warc.WARCRecord;
import org.archive.uid.UUIDGenerator;
import org.archive.util.anvl.ANVLRecord;
import org.webcurator.core.visualization.VisualizationProgressBar;
import org.webcurator.core.visualization.modification.metadata.PruneAndImportCommandRowMetadata;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PruneAndImportHandlerHeritrixWarc extends PruneAndImportHandler {
    public static final String ARCHIVE_TYPE = "WARC";
    private WarcFilenameTemplate warcFilenameTemplate = null;
    private final List<String> impArcHeader = new ArrayList<String>();
    private final AtomicInteger aint = new AtomicInteger();
    private List<File> dirs;
    private boolean compressed;


    /**
     * Post 1.6.1 code.
     * <p>
     * Problem:
     * The correct number of bytes/characters are being read from the header record, and saved in the
     * buffer array. But the input stream appears (for some unknown reason) to read or mark one character further
     * than the length that was read into the array.
     * <p>
     * For example, with content-length: 398, the stream should be stopping at the <|> below. So the next character read
     * would be a carriage return "\r". This is what the WarcReader (line 65 - gotoEOR()) is expecting in order to move
     * the marker to the start of the next record.
     * <p>
     * http-header-from: youremail@yourdomain.com\r\n
     * \r\n<|>
     * \r\n
     * \r\n
     * WARC/0.18\r\n
     * <p>
     * Instead the stream is reading up until the marker in the following example, and throwing a runtime error.
     * <p>
     * http-header-from: youremail@yourdomain.com\r\n
     * \r\n
     * \r<|>\n
     * \r\n
     * WARC/0.18\r\n
     * <p>
     * <p>
     * Workaround/Fix:
     * Create a duplicate ArchiveReader (headerRecordIt) for just the warc header metadata, that is then closed after
     * the metadata is read. The archiveRecordsIt ArchiveReader is still used to read the rest of the records. However
     * the first record (which we read with the other ArchiveReader) still has an issue with the iterator hasNext()
     * call. So it is skipped before entering the loop that copies each record.
     */
    @Override
    protected void copyArchiveRecords(File fileFrom, List<String> urisToDelete, Map<String, PruneAndImportCommandRowMetadata> hrsToImport, int newHarvestResultNumber) throws Exception {
        if (!fileFrom.getName().toUpperCase().endsWith(ARCHIVE_TYPE)) {
            log.warn("Unsupported file format: {}", fileFrom.getAbsolutePath());
            return;
        }

        // Get the reader for this ARC File
        ArchiveReader reader = ArchiveReaderFactory.get(fileFrom);
        if (!(reader instanceof WARCReader)) {
            log.warn("Unsupported file format: {}", fileFrom.getAbsolutePath());
            return;
        }
        this.writeLog(String.format("Start to copy and prune a WARC file: %s size: %d", fileFrom.getName(), fileFrom.length()));

        // Use the original filename
        String strippedImpArcFilename = reader.getStrippedFileName();
        if (this.warcFilenameTemplate == null) {
            this.warcFilenameTemplate = new WarcFilenameTemplate(strippedImpArcFilename);
        }

        if (urisToDelete.size() == 0) {
            //Copy file directly
            File destDir = this.dirs.get(0);
            File fileTo = new File(destDir, fileFrom.getName());
            org.apache.hadoop.thirdparty.guava.common.io.Files.copy(fileFrom, fileTo);
            return;
        }

        //Summary
        StatisticItem statisticItem = new StatisticItem();
        statisticItems.add(statisticItem);
        statisticItem.setFromFileName(fileFrom.getName());
        statisticItem.setFromFileLength(fileFrom.length());

        //Progress
        VisualizationProgressBar.ProgressItem progressItem = this.progressBar.getProgressItem(fileFrom.getName());

        compressed = reader.isCompressed();
        Iterator<ArchiveRecord> archiveRecordsIt = reader.iterator();

        // Get a another reader for the warc header metadata
        ArchiveReader headerReader = ArchiveReaderFactory.get(fileFrom);
        Iterator<ArchiveRecord> headerRecordIt = headerReader.iterator();

        // Read the Meta Data
        WARCRecord headerRec = (WARCRecord) headerRecordIt.next();
        byte[] buff = new byte[BYTE_BUFF_SIZE];
        StringBuilder metaData = new StringBuilder();
        int bytesRead = 0;
        while ((bytesRead = headerRec.read(buff)) != -1) {
            metaData.append(new String(buff, 0, bytesRead));
        }

        List<String> l = new ArrayList<String>();
        l.add(metaData.toString());

        if (impArcHeader.isEmpty()) {
            impArcHeader.add(metaData.toString());
        }

        headerRec.close();
        headerReader.close();

        // Bypass warc header metadata as it has been read above from a different ArchiveReader
        archiveRecordsIt.next();

        // Create a WARC Writer
        WARCWriterPoolSettings settings = new WARCWriterPoolSettingsData(strippedImpArcFilename + "~" + newHarvestResultNumber, "${prefix}",
                ARCReader.DEFAULT_MAX_ARC_FILE_SIZE, compressed, dirs, l, new UUIDGenerator());
        WARCWriter writer = new WARCWriter(aint, settings);

        this.writeLog("Create a new WARC file, file name: " + writer.getFile());

        // Iterate through all the records, skipping deleted or imported URLs.
        while (archiveRecordsIt.hasNext()) {
            this.tryBlock();

            WARCRecord record = (WARCRecord) archiveRecordsIt.next();
            ArchiveRecordHeader header = record.getHeader();
            String WARCType = (String) header.getHeaderValue(org.archive.format.warc.WARCConstants.HEADER_KEY_TYPE);
            String strRecordId = (String) header.getHeaderValue(org.archive.format.warc.WARCConstants.HEADER_KEY_ID);
            URI recordId = new URI(strRecordId.substring(strRecordId.indexOf("<") + 1, strRecordId.lastIndexOf(">") - 1));
            long contentLength = header.getLength() - header.getContentBegin();

            if (WARCType.equals(org.archive.format.warc.WARCConstants.WARCRecordType.warcinfo.toString())) {
                this.writeLog("Skip [warcinfo] record");
                statisticItem.increaseSkippedRecords();
                continue;
            }

            //TODO: to confirm should imported urls to be pruned: hrsToImport.containsKey(header.getUrl())
            if (urisToDelete.contains(header.getUrl())) {
                this.writeLog(String.format("Prune [%s] record: %s", WARCType, header.getUrl()));
                statisticItem.increasePrunedRecords();
                continue;
            }

            ANVLRecord namedFields = new ANVLRecord();
            header.getHeaderFields().forEach((key, value) -> {
                if (key.equals(org.archive.format.warc.WARCConstants.ABSOLUTE_OFFSET_KEY)) {
                    value = Long.toString(writer.getPosition());
                }
                // we exclude all but three fields to avoid duplication / erroneous data
                if (key.equals("WARC-IP-Address") || key.equals("WARC-Payload-Digest") || key.equals("WARC-Concurrent-To")) {
                    namedFields.addLabelValue(key, value.toString());
                }
            });

            WARCRecordInfo warcRecordInfo = new WARCRecordInfo();
            switch (org.archive.format.warc.WARCConstants.WARCRecordType.valueOf(WARCType)) {
                case warcinfo:
                    warcRecordInfo.setType(org.archive.format.warc.WARCConstants.WARCRecordType.warcinfo);
                    break;
                case response:
                    warcRecordInfo.setType(org.archive.format.warc.WARCConstants.WARCRecordType.response);
                    warcRecordInfo.setUrl(header.getUrl());
                    break;
                case metadata:
                    warcRecordInfo.setType(org.archive.format.warc.WARCConstants.WARCRecordType.metadata);
                    warcRecordInfo.setUrl(header.getUrl());
                    break;
                case request:
                    warcRecordInfo.setType(org.archive.format.warc.WARCConstants.WARCRecordType.request);
                    warcRecordInfo.setUrl(header.getUrl());
                    break;
                case resource:
                    warcRecordInfo.setType(org.archive.format.warc.WARCConstants.WARCRecordType.resource);
                    warcRecordInfo.setUrl(header.getUrl());
                    break;
                case revisit:
                    warcRecordInfo.setType(WARCConstants.WARCRecordType.revisit);
                    warcRecordInfo.setUrl(header.getUrl());
                    break;
                default:
                    log.warn("Ignoring unrecognised type for WARCRecord: " + WARCType);
                    this.writeLog(String.format("Skip unrecognised [%s] record: %s", WARCType, header.getUrl()));
                    statisticItem.increaseFailedRecords();
                    continue;
            }

            warcRecordInfo.setCreate14DigitDate(header.getDate());
            warcRecordInfo.setMimetype(header.getMimetype());
            warcRecordInfo.setRecordId(recordId);
            warcRecordInfo.setExtraHeaders(namedFields);
            warcRecordInfo.setContentStream(record);
            warcRecordInfo.setContentLength(contentLength);
            writer.writeRecord(warcRecordInfo);
            statisticItem.increaseCopiedRecords();
            this.writeLog(String.format("Copy [%s] record: %s", WARCType, header.getUrl()));

            progressItem.setCurLength(header.getOffset()); //Increase Progress
        }

        this.writeLog(String.format("End to copy and prune from: %s size: %d", fileFrom.getName(), fileFrom.length()));
        if (writer.getFile() != null) {
            this.writeLog(String.format("End to copy and prune to: %s size: %d", writer.getFile().getName(), writer.getFile().length()));

            statisticItem.setToFileName(writer.getFile().getName());
            statisticItem.setToFileLength(writer.getFile().length());
        }
        writer.close();
        reader.close();
    }

    @Override
    protected void importFromFile(long job, int harvestResultNumber, int newHarvestResultNumber, Map<String, PruneAndImportCommandRowMetadata> hrsToImport) throws IOException {
        this.writeLog("Start to import from file");
        StatisticItem statisticItem = new StatisticItem();
        statisticItems.add(statisticItem);

        VisualizationProgressBar.ProgressItem progressItemFileImported = progressBar.getProgressItem("ImportedFiles");

        // Create a WARC Writer
        LocalDateTime timestamp = LocalDateTime.now();
        this.warcFilenameTemplate.setTimestamp(timestamp.format(fTimestamp17));
        this.warcFilenameTemplate.setSerialNo(aint.getAndIncrement());
        this.warcFilenameTemplate.setHeritrixInfo("mod~import~file");
        String strippedImpArcFilename = this.warcFilenameTemplate.toString();

        // Create a WARC Writer
        // Somewhat arbitrarily use the last filename from the list of original filenames
        // Compress the file if the (last) original file was compressed
        WARCWriterPoolSettings settings = new WARCWriterPoolSettingsData(strippedImpArcFilename + "~" + newHarvestResultNumber, "${prefix}",
                WARCReader.DEFAULT_MAX_WARC_FILE_SIZE, compressed, dirs, impArcHeader, new UUIDGenerator());
        WARCWriter warcWriter = new WARCWriter(aint, settings);

        hrsToImport.values().stream().filter(f -> {
            return f.getLength() > 0L;
        }).forEach(fProps -> {
            try {
                this.tryBlock();

                Date warcDate = new Date();
                if (fProps.getModifiedMode().equalsIgnoreCase("FILE") || fProps.getModifiedMode().equalsIgnoreCase("CUSTOM")) {
                    warcDate.setTime(fProps.getLastModified());
                }

                log.debug("WARC-Date: {}", writerDF.format(warcDate));

                File tempFile = this.modificationDownloadFile(job, harvestResultNumber, fProps);
                InputStream fin = Files.newInputStream(tempFile.toPath());
                URI recordId = new URI("urn:uuid:" + tempFile.getName());
                ANVLRecord namedFields = new ANVLRecord();
                namedFields.addLabelValue(WARCConstants.HEADER_KEY_IP, "0.0.0.0");
                WARCRecordInfo warcRecordInfo = new WARCRecordInfo();
                warcRecordInfo.setUrl(fProps.getUrl());
                warcRecordInfo.setCreate14DigitDate(writerDF.format(warcDate));
                warcRecordInfo.setMimetype(fProps.getContentType());
                warcRecordInfo.setRecordId(recordId);
                warcRecordInfo.setExtraHeaders(namedFields);
                warcRecordInfo.setContentStream(fin);
                warcRecordInfo.setContentLength(tempFile.length());
                warcRecordInfo.setType(WARCConstants.WARCRecordType.response);
                warcWriter.writeRecord(warcRecordInfo);

                Files.deleteIfExists(tempFile.toPath());

                this.writeLog(String.format("Imported a record from file, name: %s, size: %d", tempFile.getName(), tempFile.length()));
                statisticItem.increaseCopiedRecords();

                progressItemFileImported.setCurLength(progressItemFileImported.getCurLength() + fProps.getLength());
            } catch (IOException | URISyntaxException e) {
                log.error(e.getMessage());
                statisticItem.increaseFailedRecords();
            }
        });
        if (warcWriter.getFile() != null) {
            this.writeLog(String.format("End to import files, to: %s size: %d", warcWriter.getFile().getName(), warcWriter.getFile().length()));
            statisticItem.setToFileName(warcWriter.getFile().getName());
            statisticItem.setToFileLength(warcWriter.getFile().length());
        } else {
            this.writeLog("End to import files");
            statisticItems.remove(statisticItem);
        }
        warcWriter.close();
    }


    @Override
    protected void importFromPatchHarvest(File fileFrom, List<String> urisToDelete, List<String> urisToImportByUrl, int newHarvestResultNumber) throws IOException, URISyntaxException, InterruptedException {
        if (!fileFrom.getName().toUpperCase().endsWith(ARCHIVE_TYPE)) {
            log.warn("Unsupported file format: {}", fileFrom.getAbsolutePath());
            return;
        }

        // Get the reader for this ARC File
        ArchiveReader reader = ArchiveReaderFactory.get(fileFrom);
        if (!(reader instanceof WARCReader)) {
            log.warn("Unsupported file format: {}", fileFrom.getAbsolutePath());
            return;
        }
        this.writeLog(String.format("Start to import from source URLs, a source WARC file: %s size: %d", fileFrom.getName(), fileFrom.length()));

        //Summary
        StatisticItem statisticItem = new StatisticItem();
        statisticItems.add(statisticItem);
        statisticItem.setFromFileName(fileFrom.getName());
        statisticItem.setFromFileLength(fileFrom.length());

        //Progress
        VisualizationProgressBar.ProgressItem progressItem = this.progressBar.getProgressItem(fileFrom.getName());

        String strippedImpArcFilename = reader.getStrippedFileName();
        Iterator<ArchiveRecord> archiveRecordsIt = reader.iterator();

        // Get a another reader for the warc header metadata
        ArchiveReader headerReader = ArchiveReaderFactory.get(fileFrom);
        Iterator<ArchiveRecord> headerRecordIt = headerReader.iterator();

        // Read the Meta Data
        WARCRecord headerRec = (WARCRecord) headerRecordIt.next();
        byte[] buff = new byte[BYTE_BUFF_SIZE];
        StringBuilder metaData = new StringBuilder();
        int bytesRead = 0;
        while ((bytesRead = headerRec.read(buff)) != -1) {
            metaData.append(new String(buff, 0, bytesRead));
        }

        List<String> l = new ArrayList<String>();
        l.add(metaData.toString());

        if (impArcHeader.isEmpty()) {
            impArcHeader.add(metaData.toString());
        }

        headerRec.close();
        headerReader.close();

        // Bypass warc header metadata as it has been read above from a different ArchiveReader
        archiveRecordsIt.next();

        WARCWriterPoolSettings settings = new WARCWriterPoolSettingsData(strippedImpArcFilename + "~" + newHarvestResultNumber, "${prefix}",
                ARCReader.DEFAULT_MAX_ARC_FILE_SIZE, compressed, dirs, l, new UUIDGenerator());
        WARCWriter writer = new WARCWriter(aint, settings);

        // Iterate through all the records, skipping deleted URLs.
        while (archiveRecordsIt.hasNext()) {
            this.tryBlock();

            WARCRecord record = (WARCRecord) archiveRecordsIt.next();
            ArchiveRecordHeader header = record.getHeader();
            String WARCType = (String) header.getHeaderValue(org.archive.format.warc.WARCConstants.HEADER_KEY_TYPE);
            String strRecordId = (String) header
                    .getHeaderValue(org.archive.format.warc.WARCConstants.HEADER_KEY_ID);
            URI recordId = new URI(strRecordId.substring(
                    strRecordId.indexOf("<") + 1,
                    strRecordId.lastIndexOf(">") - 1));
            long contentLength = header.getLength() - header.getContentBegin();

            if (WARCType.equals(org.archive.format.warc.WARCConstants.WARCRecordType.warcinfo.toString())) {
                this.writeLog("Skip [warcinfo] record");
                statisticItem.increaseSkippedRecords();
                continue;
            }

            /*If the url is to be pruned, but not to be imported*/
            if (urisToDelete.contains(header.getUrl()) && !urisToImportByUrl.contains(header.getUrl())) {
                this.writeLog(String.format("Prune [%s] record: %s", WARCType, header.getUrl()));
                continue;
            }

            ANVLRecord namedFields = new ANVLRecord();
            header.getHeaderFields().forEach((key, value) -> {
                if (key.equals(org.archive.format.warc.WARCConstants.ABSOLUTE_OFFSET_KEY)) {
                    value = Long.toString(writer.getPosition());
                }
                // we exclude all but three fields to avoid
                // duplication / erroneous data
                if (key.equals("WARC-IP-Address") || key.equals("WARC-Payload-Digest") || key.equals("WARC-Concurrent-To")) {
                    namedFields.addLabelValue(key, value.toString());
                }
            });

            WARCRecordInfo warcRecordInfo = new WARCRecordInfo();
            switch (org.archive.format.warc.WARCConstants.WARCRecordType.valueOf(WARCType)) {
                case warcinfo:
                    warcRecordInfo.setType(org.archive.format.warc.WARCConstants.WARCRecordType.warcinfo);
                    break;
                case response:
                    warcRecordInfo.setType(org.archive.format.warc.WARCConstants.WARCRecordType.response);
                    warcRecordInfo.setUrl(header.getUrl());
                    break;
                case metadata:
                    warcRecordInfo.setType(org.archive.format.warc.WARCConstants.WARCRecordType.metadata);
                    warcRecordInfo.setUrl(header.getUrl());
                    break;
                case request:
                    warcRecordInfo.setType(org.archive.format.warc.WARCConstants.WARCRecordType.request);
                    warcRecordInfo.setUrl(header.getUrl());
                    break;
                case resource:
                    warcRecordInfo.setType(org.archive.format.warc.WARCConstants.WARCRecordType.resource);
                    warcRecordInfo.setUrl(header.getUrl());
                    break;
                case revisit:
                    warcRecordInfo.setType(WARCConstants.WARCRecordType.revisit);
                    warcRecordInfo.setUrl(header.getUrl());
                    break;
                default:
                    log.warn("Ignoring unrecognised type for WARCRecord: " + WARCType);
                    this.writeLog(String.format("Skip unrecognised [%s] record: %s", WARCType, header.getUrl()));
                    statisticItem.increaseFailedRecords();
                    continue;
            }
            warcRecordInfo.setCreate14DigitDate(header.getDate());
            warcRecordInfo.setMimetype(header.getMimetype());
            warcRecordInfo.setRecordId(recordId);
            warcRecordInfo.setExtraHeaders(namedFields);
            warcRecordInfo.setContentStream(record);
            warcRecordInfo.setContentLength(contentLength);
            writer.writeRecord(warcRecordInfo);

            this.writeLog(String.format("Import [%s] record: %s", WARCType, header.getUrl()));
            statisticItem.increaseCopiedRecords();

            progressItem.setCurLength(header.getOffset());
        }

        this.writeLog(String.format("End to import URLs, from: %s size: %d", fileFrom.getName(), fileFrom.length()));
        if (writer.getFile() != null) {
            this.writeLog(String.format("End to import URLs, to: %s size: %d", writer.getFile().getName(), writer.getFile().length()));
            statisticItem.setToFileName(writer.getFile().getName());
            statisticItem.setToFileLength(writer.getFile().length());
        }
        writer.close();
        reader.close();
    }

    private WARCRecordInfo createWarcRecordInfo(WARCRecord record, ArchiveRecordHeader header, ANVLRecord namedFields, String targetUrl) throws URISyntaxException {
        String WARCType = (String) header.getHeaderValue(org.archive.format.warc.WARCConstants.HEADER_KEY_TYPE);
        URI recordId = new URI(String.format("urn:uuid:%s", UUID.randomUUID().toString()));
        long contentLength = header.getLength() - header.getContentBegin();

        WARCRecordInfo warcRecordInfo = new WARCRecordInfo();
        switch (org.archive.format.warc.WARCConstants.WARCRecordType.valueOf(WARCType)) {
            case warcinfo:
                warcRecordInfo.setType(WARCConstants.WARCRecordType.warcinfo);
                break;
            case response:
                warcRecordInfo.setType(WARCConstants.WARCRecordType.response);
                warcRecordInfo.setUrl(targetUrl);
                break;
            case metadata:
                warcRecordInfo.setType(WARCConstants.WARCRecordType.metadata);
                warcRecordInfo.setUrl(targetUrl);
                break;
            case request:
                warcRecordInfo.setType(WARCConstants.WARCRecordType.request);
                warcRecordInfo.setUrl(targetUrl);
                break;
            case resource:
                warcRecordInfo.setType(WARCConstants.WARCRecordType.resource);
                warcRecordInfo.setUrl(targetUrl);
                break;
            case revisit:
                warcRecordInfo.setType(WARCConstants.WARCRecordType.revisit);
                warcRecordInfo.setUrl(targetUrl);
                break;
            default:
                log.warn("Ignoring unrecognised type for WARCRecord: " + WARCType);
        }
        warcRecordInfo.setCreate14DigitDate(header.getDate());
        warcRecordInfo.setMimetype(header.getMimetype());
        warcRecordInfo.setRecordId(recordId);
        warcRecordInfo.setExtraHeaders(namedFields);
        warcRecordInfo.setContentStream(record);
        warcRecordInfo.setContentLength(contentLength);

        return warcRecordInfo;
    }

    @Override
    protected String archiveType() {
        return ARCHIVE_TYPE;
    }

    public List<String> getImpArcHeader() {
        return impArcHeader;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public void setDirs(List<File> dirs) {
        this.dirs = dirs;
    }


    static class WarcFilenameTemplate {
        private String prefix;
        private String timestamp;
        private int serialNo;
        private String heritrixInfo;

        public WarcFilenameTemplate(String strippedImpArcFilename) throws Exception {
            if (strippedImpArcFilename == null || strippedImpArcFilename.indexOf('-') < 0) {
                throw new Exception("Unsupported file template: " + strippedImpArcFilename);
            }

            int idx = strippedImpArcFilename.indexOf('-');
            this.prefix = strippedImpArcFilename.substring(0, idx);
        }

        public String toString() {
            return String.format("%s-%s-%05d-%s", this.prefix, this.timestamp, this.serialNo, this.heritrixInfo);
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public int getSerialNo() {
            return serialNo;
        }

        public void setSerialNo(int serialNo) {
            this.serialNo = serialNo;
        }

        public String getHeritrixInfo() {
            return heritrixInfo;
        }

        public void setHeritrixInfo(String heritrixInfo) {
            this.heritrixInfo = heritrixInfo;
        }
    }
}

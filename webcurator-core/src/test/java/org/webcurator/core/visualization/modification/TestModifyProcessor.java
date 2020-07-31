package org.webcurator.core.visualization.modification;

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;
import org.webcurator.core.exceptions.DigitalAssetStoreException;
import org.webcurator.core.util.PatchUtil;
import org.webcurator.core.visualization.BaseVisualizationTest;
import org.webcurator.core.visualization.VisualizationProgressBar;
import org.webcurator.core.visualization.modification.metadata.ModifyApplyCommand;
import org.webcurator.core.visualization.modification.processor.ModifyProcessor;
import org.webcurator.core.visualization.modification.metadata.ModifyRowMetadata;
import org.webcurator.core.visualization.modification.processor.ModifyProcessorWarc;
import org.webcurator.domain.model.core.HarvestResult;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

import static org.junit.Assert.*;

public class TestModifyProcessor extends BaseVisualizationTest {
    private ModifyProcessor warcProcessor = null;

    @Before
    public void initTest() throws IOException, DigitalAssetStoreException {
        super.initTest();

        List<File> dirs = new LinkedList<>();
        File destDir = new File(directoryManager.getBaseDir(), targetInstanceId + File.separator + newHarvestResultNumber);
        dirs.add(destDir);

        ModifyApplyCommand cmd = getApplyCommand();
        warcProcessor = new ModifyProcessorWarc(cmd);
        warcProcessor.init(this.processorManager, this.directoryManager, this.wctClient);
    }

    @Test
    public void testCopyAndPrune() throws Exception {
        File warcFileFrom = getOneWarcFile();
        assert warcFileFrom != null;

        List<String> listToBePrunedUrl = getRandomUrlsFromWarcFile(warcFileFrom);

        Map<String, ModifyRowMetadata> hrsToImport = new HashMap<>();

        warcProcessor.copyArchiveRecords(warcFileFrom, listToBePrunedUrl, hrsToImport, newHarvestResultNumber);

        File destDirectory = new File(directoryManager.getBaseDir(), String.format("%d%s%d", targetInstanceId, File.separator, newHarvestResultNumber));
        File warcFileNew = new File(destDirectory, getModifiedWarcFileName(warcFileFrom));
        assert warcFileNew.exists();
        assert warcFileNew.isFile();

        assertFalse("URLs not be pruned completely.", isUrlExistInWarcFile(warcFileNew, listToBePrunedUrl));
    }

    @Test
    public void testImportByFile() throws Exception {
        String targetUrl = String.format("http://www.import.file/%s/", UUID.randomUUID().toString());
        ModifyApplyCommand cmd = getApplyCommand();
        ModifyRowMetadata m = new ModifyRowMetadata();
        m.setOption("file");
        m.setUrl(targetUrl);
        m.setName("expand.png");
        m.setModifiedMode("TBC");
        m.setContentType("image/png");
        cmd.getDataset().add(m);
        Map<String, ModifyRowMetadata> hrsToImport = new HashMap<>();
        hrsToImport.put(targetUrl, m);
        warcProcessor.importFromFile(targetInstanceId, harvestResultNumber, newHarvestResultNumber, hrsToImport);

        List<String> importedUrl = new ArrayList<>();
        importedUrl.add(targetUrl);

        File warcFileNew = getOneImportedWarcFile("File");
        assert warcFileNew != null;

        assertTrue(isUrlExistInWarcFile(warcFileNew, importedUrl));
    }

    @Test
    public void testImportByUrl() throws InterruptedException, IOException, URISyntaxException {
        File fileFrom = getOnePatchHarvestedWarcFile();

        String targetUrl = String.format("https://www.import.url/%s/", UUID.randomUUID().toString());
        ModifyApplyCommand cmd = getApplyCommand();
        ModifyRowMetadata m = new ModifyRowMetadata();
        m.setOption("url");
        m.setUrl(targetUrl);
        cmd.getDataset().add(m);

        List<String> importedUrl = new ArrayList<>();
        importedUrl.add(targetUrl);

        List<String> urisToDelete = new ArrayList<>();

        warcProcessor.importFromPatchHarvest(fileFrom, urisToDelete, importedUrl, newHarvestResultNumber);

        String option = FilenameUtils.removeExtension(fileFrom.getName());
        File warcFileNew = getOneImportedWarcFile(option);
        assert warcFileNew != null;
    }

    @Test
    public void testProcessInternal() throws Exception {
        warcProcessor.processInternal();

        //To test progress
        VisualizationProgressBar progressBar = warcProcessor.getProgress();
        assertEquals(HarvestResult.STATE_MODIFYING, progressBar.getState());
        assertEquals(HarvestResult.STATUS_FINISHED, progressBar.getStatus());
        assertEquals(100, progressBar.getProgressPercentage());

        warcProcessor.close();

        //To test logs
        String strLogDir = directoryManager.getPatchLogDir(HarvestResult.PATCH_STAGE_TYPE_MODIFYING, targetInstanceId, newHarvestResultNumber);
        File fileLog = new File(strLogDir, "running.log");
        assert fileLog.exists();
        assert fileLog.length() > 0;

        //To test reports
        String strReportDir = directoryManager.getPatchReportDir(HarvestResult.PATCH_STAGE_TYPE_MODIFYING, targetInstanceId, newHarvestResultNumber);
        File fileReport = new File(strReportDir, "report.txt");
        assert fileReport.exists();
        assert fileReport.length() > 0;
    }

    private File getOneWarcFile() {
        File directory = new File(directoryManager.getBaseDir(), String.format("%d%s%d", targetInstanceId, File.separator, harvestResultNumber));
        List<File> fileList = PatchUtil.listWarcFiles(directory);
        assert fileList.size() > 0;

        return fileList.get(0);
    }

    private File getOnePatchHarvestedWarcFile() {
        String jobName = PatchUtil.getPatchJobName(targetInstanceId, newHarvestResultNumber);
        File directory = new File(directoryManager.getBaseDir(), String.format("%s%s%d", jobName, File.separator, 1));
        List<File> fileList = PatchUtil.listWarcFiles(directory);
        assert fileList.size() > 0;

        return fileList.get(0);
    }

    private File getOneImportedWarcFile(String option) {
        File directory = new File(directoryManager.getBaseDir(), String.format("%d%s%d", targetInstanceId, File.separator, newHarvestResultNumber));
        List<File> fileList = PatchUtil.listWarcFiles(directory);
        assert fileList.size() > 0;

        if (option.equalsIgnoreCase("FILE")) {
            for (File f : fileList) {
                if (f.getName().contains("mod~import~file")) {
                    return f;
                }
            }
        } else {
            for (File f : fileList) {
                if (f.getName().contains(option)) {
                    return f;
                }
            }
        }
        return null;
    }
}

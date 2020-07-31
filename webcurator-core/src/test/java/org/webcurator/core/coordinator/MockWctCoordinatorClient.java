package org.webcurator.core.coordinator;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.webcurator.core.exceptions.DigitalAssetStoreException;
import org.webcurator.core.rest.AbstractRestClient;
import org.webcurator.core.visualization.modification.metadata.ModifyRowMetadata;
import org.webcurator.domain.model.core.HarvestResult;
import org.webcurator.domain.model.core.HarvestResultDTO;
import org.webcurator.domain.model.core.SeedHistoryDTO;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MockWctCoordinatorClient extends AbstractRestClient implements WctCoordinatorClient {
    private HarvestResultManager hrManager = new HarvestResultManagerImpl();

    public MockWctCoordinatorClient(String scheme, String host, int port, RestTemplateBuilder restTemplateBuilder) {
        super(scheme, host, port, restTemplateBuilder);
    }

    @Override
    public void completeArchiving(Long targetInstanceOid, String archiveIID) {

    }

    @Override
    public void failedArchiving(Long targetInstanceOid, String message) {

    }

    @Override
    public void dasFinaliseModify(long targetInstanceId, int harvestResultNumber) {

    }

    @Override
    public Set<SeedHistoryDTO> dasGetSeedUrls(long targetInstanceId, int harvestResultNumber) {
        Set<SeedHistoryDTO> seeds = new HashSet<>();
        SeedHistoryDTO seedHistoryPrimary = new SeedHistoryDTO(1, "http://www.google.com/", targetInstanceId, true);
        SeedHistoryDTO seedHistorySecondary = new SeedHistoryDTO(2, "http://www.baidu.com/", targetInstanceId, false);
        seeds.add(seedHistoryPrimary);
        seeds.add(seedHistorySecondary);
        return seeds;
    }

    @Override
    public void dasHeartBeat(List<HarvestResultDTO> list) {

    }

    @Override
    public void dasFinaliseIndex(long targetInstanceId, int harvestNumber) {

    }

    @Override
    public void dasUpdateHarvestResultStatus(HarvestResultDTO hrDTO) {

    }

    @Override
    public File dasDownloadFile(String fileDir, long job, int harvestResultNumber, ModifyRowMetadata metadata) throws IOException, DigitalAssetStoreException {
        String tempFileName = UUID.randomUUID().toString();
        File dirFile = new File(fileDir);

        if (!dirFile.exists() && !dirFile.mkdirs()) {
            String err = String.format("Make dir failed: %s", fileDir);
            throw new DigitalAssetStoreException(err);
        }
        File downloadedFile = new File(fileDir, tempFileName);

        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadedFile));
        outputStream.write("This is a test file".getBytes());
        outputStream.close();

        return downloadedFile;
    }
}

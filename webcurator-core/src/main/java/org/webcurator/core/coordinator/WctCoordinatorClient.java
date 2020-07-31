package org.webcurator.core.coordinator;

import org.webcurator.core.exceptions.DigitalAssetStoreException;
import org.webcurator.core.visualization.modification.metadata.ModifyRowMetadata;
import org.webcurator.domain.model.core.HarvestResultDTO;
import org.webcurator.domain.model.core.SeedHistoryDTO;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface WctCoordinatorClient {
    void completeArchiving(Long targetInstanceOid, String archiveIID);

    void failedArchiving(Long targetInstanceOid, String message);

    void dasFinaliseModify(long targetInstanceId, int harvestResultNumber);

    Set<SeedHistoryDTO> dasGetSeedUrls(long targetInstanceId, int harvestResultNumber);

    void dasHeartBeat(List<HarvestResultDTO> list);

    void dasFinaliseIndex(long targetInstanceId, int harvestNumber);

    void dasUpdateHarvestResultStatus(HarvestResultDTO hrDTO);

    File dasDownloadFile(String fileDir, long job, int harvestResultNumber, ModifyRowMetadata metadata) throws IOException, DigitalAssetStoreException;
}

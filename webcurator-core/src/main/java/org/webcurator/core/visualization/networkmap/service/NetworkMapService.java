package org.webcurator.core.visualization.networkmap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.webcurator.core.visualization.VisualizationServiceInterface;
import org.webcurator.core.visualization.networkmap.metadata.NetworkDbVersionDTO;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapNodeDTO;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public interface NetworkMapService extends VisualizationServiceInterface {
    int MAX_URL_UNL_FIELDS_COUNT = 17;

    NetworkMapResult initialIndex(long job, int harvestResultNumber);

    NetworkMapResult getDbVersion(long job, int harvestResultNumber);

    NetworkMapResult get(long job, int harvestResultNumber, String key);

    NetworkMapResult getNode(long job, int harvestResultNumber, long id);

    NetworkMapResult getOutlinks(long job, int harvestResultNumber, long id);

    NetworkMapResult getChildren(long job, int harvestResultNumber, long id);

    NetworkMapResult getAllDomains(long job, int harvestResultNumber);

    NetworkMapResult getSeedUrls(long job, int harvestResultNumber);

    NetworkMapResult getMalformedUrls(long job, int harvestResultNumber);

    NetworkMapResult searchUrl(long job, int harvestResultNumber, NetworkMapServiceSearchCommand searchCommand);

    NetworkMapResult searchUrl2CascadePaths(long job, int harvestResultNumber, NetworkMapServiceSearchCommand searchCommand);

    //List<String>
    NetworkMapResult searchUrlNames(long job, int harvestResultNumber, String substring);

    NetworkMapResult getHopPath(long job, int harvestResultNumber, long id);

    NetworkMapResult getHierarchy(long job, int harvestResultNumber, List<Long> ids);

    NetworkMapResult getUrlByName(long job, int harvestResultNumber, String urlName);

    NetworkMapResult getUrlsByNames(long job, int harvestResultNumber, List<String> urlNameList);

    NetworkMapResult getProgress(long job, int harvestResultNumber);

    NetworkMapResult getProcessingHarvestResultDTO(long job, int harvestResultNumber);

    default NetworkMapNodeDTO getNodeEntity(String json) {
        if (json == null) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(json, NetworkMapNodeDTO.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    default NetworkDbVersionDTO getDbVersionDTO(String json) {
        if (json == null) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(json, NetworkDbVersionDTO.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    default NetworkMapNodeDTO unlString2NetworkMapNode(String s) {
        if (s == null) {
            return null;
        }
        String[] items = s.split("\n");
        if (items.length != MAX_URL_UNL_FIELDS_COUNT) {
            return null;
        }

        NetworkMapNodeDTO n = new NetworkMapNodeDTO();
        n.setId(Long.parseLong(items[0]));
        n.setUrl(items[1]);
        n.setSeedType(Integer.parseInt(items[2]));
        n.setTotUrls(Integer.parseInt(items[3]));
        n.setTotSuccess(Integer.parseInt(items[4]));
        n.setTotFailed(Integer.parseInt(items[5]));
        n.setTotSize(Integer.parseInt(items[6]));
        n.setDomainId(Integer.parseInt(items[7]));
        n.setContentLength(Long.parseLong(items[8]));
        n.setContentType(items[9]);
        n.setStatusCode(Integer.parseInt(items[10]));
        n.setParentId(Long.parseLong(items[11]));
        n.setOffset(Long.parseLong(items[12]));
        n.setFetchTimeMs(Long.parseLong(items[13]));
        n.setFileName(items[14]);
        n.setSeed(Boolean.parseBoolean(items[15]));

        String strOutlinks = items[16];
        List<Long> outlinks = new ArrayList<>();
        if (strOutlinks.length() > 2) {
            strOutlinks = strOutlinks.substring(1, strOutlinks.length() - 1);
            outlinks = Arrays.stream(strOutlinks.split(",")).map(Long::parseLong).collect(Collectors.toList());
        }
        n.setOutlinks(outlinks);
        return n;
    }
}

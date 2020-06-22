package org.webcurator.core.visualization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapNode;

import java.io.IOException;
import java.util.List;

public interface VisualizationServiceInterface {
    default public String obj2Json(Object obj) {
        String json = "{}";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            json = objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return json;
    }


    default public List<Long> getArrayList(String json) {
        if (json == null) {
            return null;
        }

//        log.debug(json);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    default public List<String> getArrayListOfNetworkMapNode(String json) {
        if (json == null) {
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    VisualizationProgressBar getProgress(long targetInstanceId, int harvestResultNumber);
}

package org.webcurator.core.visualization.networkmap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webcurator.common.util.Utils;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapTreeNodeDTO;
import org.webcurator.core.visualization.networkmap.service.NetworkMapClientLocal;
import org.webcurator.core.visualization.networkmap.service.NetworkMapServiceSearchCommand;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkMapUtil {
    private static final Logger log = LoggerFactory.getLogger(NetworkMapUtil.class);
    private static final int PATH_TREE_MAX = 16;
    private static final Map<String, ClassifiedPathTree> PATH_TREE_QUEUE = new HashMap<>();

    public static NetworkMapTreeNodeDTO getMapTreeNode(NetworkMapClientLocal localClient, long targetInstanceId, int harvestResultNumber, NetworkMapServiceSearchCommand searchCommand, String title) {
        String key = getKey(targetInstanceId, harvestResultNumber, searchCommand);
        final ClassifiedPathTree classifiedPathTree = PATH_TREE_QUEUE.containsKey(key) ? PATH_TREE_QUEUE.get(key) : new ClassifiedPathTree();
        if (PATH_TREE_QUEUE.containsKey(key)) {
            return classifiedPathTree.getTreeNodeByTitle(title);
        } else {
            while (PATH_TREE_QUEUE.size() >= PATH_TREE_MAX) {
                Optional<ClassifiedPathTree> oldestClassifiedPathTree = PATH_TREE_QUEUE.values().stream().min((a, b) -> (int) (a.getCreatedTime() - b.getCreatedTime()));
                oldestClassifiedPathTree.ifPresent(pathTree -> {
                    pathTree.getRootTreeNode().destroy();
                    PATH_TREE_QUEUE.remove(pathTree.getKey());
                });
            }
            PATH_TREE_QUEUE.put(key, classifiedPathTree);
        }

        final NetworkMapTreeNodeDTO rootTreeNode = localClient.searchUrlTreeNodes(targetInstanceId, harvestResultNumber, searchCommand);
        classifyTreeViewByPathNames(rootTreeNode);
        classifiedPathTree.setKey(key);
        classifiedPathTree.setRootTreeNode(rootTreeNode);
        return rootTreeNode;
    }

    public static void classifyTreeViewByPathNames(final NetworkMapTreeNodeDTO rootTreeNode) {
        log.debug("classifyTreeViewByPathNames: title={}, url={}", rootTreeNode.getTitle(), rootTreeNode.getUrl());

        if (rootTreeNode.isHandled()) {
            return;
        }
        rootTreeNode.setHandled(true);

        //The terminal node
        if (rootTreeNode.getChildren().size() == 0) {
            rootTreeNode.setTitle(rootTreeNode.getUrl());
            return;
        }

        //Summarize total capacity
        statisticTreeNodes(rootTreeNode);

        final long domainId = rootTreeNode.getDomainId();
        Map<String, List<NetworkMapTreeNodeDTO>> mapClassifiedByTitle = new HashMap<>();
        while (true) {
            //Calculate the title
            final String currentParentTitle = Utils.isEmpty(rootTreeNode.getTitle()) ? "" : rootTreeNode.getTitle();

            rootTreeNode.getChildren().forEach(node -> {
                String title = getNextTitle(currentParentTitle, node.getUrl());
                node.setTitle(title);
            });

            mapClassifiedByTitle.clear();
            mapClassifiedByTitle = rootTreeNode.getChildren().stream().collect(Collectors.groupingBy(NetworkMapTreeNodeDTO::getTitle));
            if (mapClassifiedByTitle.size() > 1) {
                rootTreeNode.getChildren().clear();
                for (String subTreeNodeTitle : mapClassifiedByTitle.keySet()) {
                    List<NetworkMapTreeNodeDTO> subTreeNodeChildren = mapClassifiedByTitle.get(subTreeNodeTitle);

                    NetworkMapTreeNodeDTO subTreeNode = new NetworkMapTreeNodeDTO();
                    subTreeNode.setTitle(subTreeNodeTitle);
                    subTreeNode.setChildren(subTreeNodeChildren);
                    subTreeNode.setDomainId(domainId);
                    statisticTreeNodes(subTreeNode);
                    rootTreeNode.getChildren().add(subTreeNode);
                }
                break;
            }

            //Size == 1
            String subTitle = (String) mapClassifiedByTitle.keySet().toArray()[0];
            NetworkMapTreeNodeDTO subTreeNode = mapClassifiedByTitle.get(subTitle).get(0);
            if (subTitle.equals(rootTreeNode.getTitle())) {
                rootTreeNode.copy(subTreeNode);
                rootTreeNode.getChildren().clear();
                if (subTreeNode.getChildren().size() == 0) {
                    rootTreeNode.setTitle(subTreeNode.getUrl());
                } else {
                    rootTreeNode.setChildren(subTreeNode.getChildren());
                }
                break;
            } else {
                rootTreeNode.setTitle(subTitle);
            }
        }
        mapClassifiedByTitle.clear();

        //Sort the return result
        rootTreeNode.getChildren().sort(Comparator.comparing(NetworkMapTreeNodeDTO::getTitle));
    }

    public static String getNextTitle(String parentTile, String url) {
        int lenParentTitle;
        if (Utils.isEmpty(parentTile)) {
            lenParentTitle = url.indexOf("://");
            if (lenParentTitle > 0) {
                lenParentTitle += 3;
            } else {
                lenParentTitle = 0;
            }
        } else {
            lenParentTitle = parentTile.length();
        }

        int nextSlashPosition = url.indexOf('/', lenParentTitle + 1);
        if (nextSlashPosition > 0) {
            return url.substring(0, nextSlashPosition + 1);
        }
        int questionMarkPosition = url.indexOf('?', lenParentTitle + 1);
        if (questionMarkPosition > 0) {
            return url.substring(0, questionMarkPosition + 1);
        }
        int requestParamSepPosition = url.indexOf('&', lenParentTitle + 1);
        if (requestParamSepPosition > 0) {
            return url.substring(0, requestParamSepPosition + 1);
        }

        return url;
    }

    public static void statisticTreeNodes(NetworkMapTreeNodeDTO node) {
        node.setZero();
        node.getChildren().forEach(node::accumulate);
    }

    private static String getKey(long targetInstanceId, int harvestResultNumber, NetworkMapServiceSearchCommand searchCommand) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonSearchCommand = objectMapper.writeValueAsString(searchCommand);
            return String.format("%d_%d_%s", targetInstanceId, harvestResultNumber, jsonSearchCommand);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialized search command", e);
            return null;
        }
    }


    static class ClassifiedPathTree {
        private long createdTime = System.currentTimeMillis();
        private String key;
        private NetworkMapTreeNodeDTO rootTreeNode;

        public NetworkMapTreeNodeDTO getTreeNodeByTitle(String title) {
            NetworkMapTreeNodeDTO found = findTreeNode(this.rootTreeNode, title);
            if (found != null) {
                NetworkMapUtil.classifyTreeViewByPathNames(found);
            }
            return found;
        }

        private NetworkMapTreeNodeDTO findTreeNode(NetworkMapTreeNodeDTO curNode, String title) {
            if (Utils.isEmpty(curNode.getTitle()) && Utils.isEmpty(title)) {
                return curNode;
            } else if (Utils.isEmpty(title)) {
                return null;
            }

            if (!Utils.isEmpty(curNode.getTitle()) && curNode.getTitle().equals(title)) {
                return curNode;
            }

            for (NetworkMapTreeNodeDTO subNode : curNode.getChildren()) {
                NetworkMapTreeNodeDTO found = findTreeNode(subNode, title);
                if (found != null) {
                    return found;
                }
            }

            return null;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public long getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(long createdTime) {
            this.createdTime = createdTime;
        }

        public NetworkMapTreeNodeDTO getRootTreeNode() {
            return rootTreeNode;
        }

        public void setRootTreeNode(NetworkMapTreeNodeDTO rootTreeNode) {
            this.rootTreeNode = rootTreeNode;
        }
    }
}



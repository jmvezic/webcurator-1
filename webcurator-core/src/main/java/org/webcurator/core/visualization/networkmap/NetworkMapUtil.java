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
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class NetworkMapUtil {
    private static final Logger log = LoggerFactory.getLogger(NetworkMapUtil.class);
    private static final int PATH_TREE_MAX = 16;
    private static final Map<String, ClassifiedPathTree> PATH_TREE_QUEUE = new HashMap<>();
    private static final int FIRST_LEVEL = 0;

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
        classifiedPathTree.setKey(key);
        classifiedPathTree.setRootTreeNode(rootTreeNode);
        Thread processor = new Thread(() -> {
            classifyTreeViewByPathNames(rootTreeNode, classifiedPathTree, FIRST_LEVEL);
            classifiedPathTree.getFirstLevelLock().release(); //For some special condition
            classifiedPathTree.getAllLock().release();
        });
        processor.start(); //To ignore block

        if (Utils.isEmpty(title)) {
            if (!classifiedPathTree.getFirstLevelLock().tryAcquire()) {
                try {
                    classifiedPathTree.getFirstLevelLock().acquire();
                } catch (InterruptedException e) {
                    log.error("Failed to try lock 1st level", e);
                    return null;
                }
            }
            return classifiedPathTree.getRootTreeNode();
        } else {
            if (!classifiedPathTree.getAllLock().tryAcquire()) {
                try {
                    classifiedPathTree.getAllLock().acquire();
                } catch (InterruptedException e) {
                    log.error("Failed to try lock 1st level", e);
                    return null;
                }
            }
            return classifiedPathTree.getTreeNodeByTitle(title);
        }
    }

    public static void classifyTreeViewByPathNames(final NetworkMapTreeNodeDTO rootTreeNode, final ClassifiedPathTree classifiedPathTree, int level) {
        log.debug("classifyTreeViewByPathNames: {}, {}", rootTreeNode.getTitle(), level);

        log.debug("Title: {}", rootTreeNode.getTitle());
        final long domainId = rootTreeNode.getDomainId();
        final String parentTitle = rootTreeNode.getTitle() == null ? "" : rootTreeNode.getTitle();
        if (rootTreeNode.getChildren().size() == 0) {
            rootTreeNode.setTitle(rootTreeNode.getUrl());
            return;
        } else if (rootTreeNode.getChildren().size() == 1) {
//            rootTreeNode.setTitle(rootTreeNode.getChildren().get(0).getTitle());
            rootTreeNode.copy(rootTreeNode.getChildren().get(0));
            rootTreeNode.setChildren(rootTreeNode.getChildren().get(0).getChildren());
            classifyTreeViewByPathNames(rootTreeNode, classifiedPathTree, level);
            return;
        }


        final int lenParentTitle = Utils.isEmpty(parentTitle) ? 8 : parentTitle.length();

        //Calculate the title
        rootTreeNode.getChildren().forEach(node -> {
            String title = getNextTitle(lenParentTitle, node.getUrl());
            node.setTitle(title);
        });

        Map<String, List<NetworkMapTreeNodeDTO>> mapClassifiedByTitle = rootTreeNode.getChildren().stream().collect(Collectors.groupingBy(NetworkMapTreeNodeDTO::getTitle));
        rootTreeNode.getChildren().clear();
        for (String subTreeNodeTitle : mapClassifiedByTitle.keySet()) {
            List<NetworkMapTreeNodeDTO> subTreeNodeChildren = mapClassifiedByTitle.get(subTreeNodeTitle);

            NetworkMapTreeNodeDTO subTreeNode = new NetworkMapTreeNodeDTO();
            subTreeNode.setTitle(subTreeNodeTitle);
            subTreeNode.setChildren(subTreeNodeChildren);
            subTreeNode.setDomainId(domainId);

            classifyTreeViewByPathNames(subTreeNode, classifiedPathTree, level + 1);

            rootTreeNode.getChildren().add(subTreeNode);
        }
        mapClassifiedByTitle.clear();

        //Uplift
        while (rootTreeNode.getChildren().size() == 1) {
            NetworkMapTreeNodeDTO subTreeNode = rootTreeNode.getChildren().get(0);
//            rootTreeNode.setTitle(subTreeNode.getTitle());
            rootTreeNode.copy(subTreeNode);
            rootTreeNode.setChildren(subTreeNode.getChildren());
        }

        //Sort the return result
        rootTreeNode.getChildren().sort(Comparator.comparing(NetworkMapTreeNodeDTO::getTitle));

        if (rootTreeNode.getChildren().size() > 0) {
            rootTreeNode.setFolder(true);
            rootTreeNode.setLazy(true);
        } else {
            rootTreeNode.setFolder(false);
            rootTreeNode.setLazy(false);
        }

        // Finished the first level
        if (level == FIRST_LEVEL) {
            classifiedPathTree.getFirstLevelLock().release();
        }

        //Need to recurse because the title changed
//        for (NetworkMapTreeNodeDTO subTreeNode : rootTreeNode.getChildren()) {
//            classifyTreeViewByPathNames(subTreeNode, classifiedPathTree, level + 1);
//        }

        //Summarize total capacity
        statisticTreeNodes(rootTreeNode);
    }

    public static String getNextTitle(int lenParentTitle, String url) {
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
        private final Semaphore firstLevelLock = new Semaphore(0);
        private final Semaphore allLock = new Semaphore(0);

        private long createdTime = System.currentTimeMillis();
        private String key;
        private NetworkMapTreeNodeDTO rootTreeNode;

        public NetworkMapTreeNodeDTO getTreeNodeByTitle(String title) {
            return findTreeNode(this.rootTreeNode, title);
        }

        private NetworkMapTreeNodeDTO findTreeNode(NetworkMapTreeNodeDTO curNode, String title) {
            if (Utils.isEmpty(curNode.getTitle()) && Utils.isEmpty(title)) {
                return curNode;
            } else if (Utils.isEmpty(curNode.getTitle()) || Utils.isEmpty(title)) {
                return null;
            }

            if (curNode.getTitle().equals(title)) {
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

        public Semaphore getFirstLevelLock() {
            return firstLevelLock;
        }


        public Semaphore getAllLock() {
            return allLock;
        }

    }
}



package org.webcurator.core.visualization.networkmap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapTreeNodeDTO;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkMapUtil {
    private static final Logger log = LoggerFactory.getLogger(NetworkMapUtil.class);

    public static void classifyTreeViewByPathNames(final NetworkMapTreeNodeDTO rootTreeNode) {
        log.debug("Title: {}", rootTreeNode.getTitle());

        final long domainId = rootTreeNode.getDomainId();
        final String parentTitle = rootTreeNode.getTitle() == null ? "" : rootTreeNode.getTitle();
        if (rootTreeNode.getChildren().size() == 0) {
            rootTreeNode.setTitle(rootTreeNode.getUrl());
            return;
        } else if (rootTreeNode.getChildren().size() == 1) {
            rootTreeNode.copy(rootTreeNode.getChildren().get(0));
            rootTreeNode.setChildren(rootTreeNode.getChildren().get(0).getChildren());
            classifyTreeViewByPathNames(rootTreeNode);
            return;
        }

        final int lenParentTitle = parentTitle.length() == 0 ? 8 : parentTitle.length();

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

            //Need to recurse because the title changed
            classifyTreeViewByPathNames(subTreeNode);

            rootTreeNode.getChildren().add(subTreeNode);
        }
        mapClassifiedByTitle.clear();

        //Uplift
        if (rootTreeNode.getChildren().size() == 1) {
            NetworkMapTreeNodeDTO subTreeNode = rootTreeNode.getChildren().get(0);
            rootTreeNode.copy(subTreeNode);
            rootTreeNode.setChildren(subTreeNode.getChildren());
        }

        //Summarize total capacity
        statisticTreeNodes(rootTreeNode);

        //Sort the return result
        rootTreeNode.getChildren().sort(Comparator.comparing(NetworkMapTreeNodeDTO::getTitle));

        if (rootTreeNode.getChildren().size()>0){
            rootTreeNode.setFolder(true);
            rootTreeNode.setLazy(false);
        }else{
            rootTreeNode.setFolder(false);
            rootTreeNode.setLazy(false);
        }
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
}

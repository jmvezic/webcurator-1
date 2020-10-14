package org.webcurator.core.visualization.networkmap;

import org.webcurator.core.visualization.networkmap.bdb.BDBNetworkMap;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapNodeDTO;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapTreeNodeDTO;
import org.webcurator.core.visualization.networkmap.service.NetworkMapService;

import java.util.*;
import java.util.stream.Collectors;

public class NetworkMapUtil {
    public static NetworkMapTreeNodeDTO classifyTreeViewByPathNames(final BDBNetworkMap db, final NetworkMapService service, final NetworkMapTreeNodeDTO rootTreeNode) {
        final long domainId = rootTreeNode.getDomainId();
        final String parentTitle = rootTreeNode.getTitle() == null ? "" : rootTreeNode.getTitle();
        final List<NetworkMapTreeNodeDTO> children = rootTreeNode.getChildren();
        if (children.size() <= 1) {
            return rootTreeNode;
        }

        final int lenParentTitle = parentTitle.length() == 0 ? 8 : parentTitle.length();

        //Calculate the title
        children.forEach(node -> {
            String title = getNextTitle(lenParentTitle, node.getUrl());
            node.setTitle(title);
        });

        Map<String, List<NetworkMapTreeNodeDTO>> mapClassifiedByTitle = children.stream().collect(Collectors.groupingBy(NetworkMapTreeNodeDTO::getTitle));
        if (mapClassifiedByTitle.size() == 1) {
            String title = "";
            for (String s : mapClassifiedByTitle.keySet()) {
                title = s;
            }

            rootTreeNode.setTitle(title);
            rootTreeNode.setChildren(mapClassifiedByTitle.get(title));

            classifyTreeViewByPathNames(db, service, rootTreeNode);
        } else {
            final List<NetworkMapTreeNodeDTO> returnedTreeNodes = new ArrayList<>();
            mapClassifiedByTitle.forEach((title, list) -> {
                NetworkMapTreeNodeDTO treeNodeDTO = statisticTreeNodes(list);
                treeNodeDTO.setTitle(title);
                treeNodeDTO.setDomainId(domainId);

                if (list.size() == 1) {
                    NetworkMapTreeNodeDTO child = list.get(0);
                    treeNodeDTO.setTitle(child.getUrl());
                    treeNodeDTO.setFolder(false);
                    treeNodeDTO.setLazy(false);
                } else {
                    treeNodeDTO.setTitle(title);
                    treeNodeDTO.setFolder(true);
                    treeNodeDTO.setLazy(false);
                    treeNodeDTO.setChildren(list);
                    classifyTreeViewByPathNames(db, service, treeNodeDTO);
                }

                //Check is the title an existing URL
                String urlId = db.get(treeNodeDTO.getTitle());
                if (urlId != null) {
                    String urlUnl = db.get(urlId);
                    NetworkMapNodeDTO networkMapNodeDTO = service.unlString2NetworkMapNode(urlUnl);
                    treeNodeDTO.setContentType(networkMapNodeDTO.getContentType());
                    treeNodeDTO.setContentLength(networkMapNodeDTO.getContentLength());
                    treeNodeDTO.setStatusCode(networkMapNodeDTO.getStatusCode());
                    treeNodeDTO.setSeed(networkMapNodeDTO.isSeed());
                    treeNodeDTO.setSeedType(networkMapNodeDTO.getSeedType());
                    treeNodeDTO.setVirtual(false);
                } else {
                    treeNodeDTO.setVirtual(true);
                }

                returnedTreeNodes.add(treeNodeDTO);
            });
            mapClassifiedByTitle.clear();
            //Filter the node that equals to the parent node
            returnedTreeNodes.removeIf(node -> parentTitle.equals(node.getTitle()));

            //Sort the return result
            returnedTreeNodes.sort(Comparator.comparing(NetworkMapTreeNodeDTO::getTitle));
            rootTreeNode.setChildren(returnedTreeNodes);
        }

        return rootTreeNode;
    }

    public static String getNextTitle(int lenParentTitle, String url) {
        int nextSlashPosition = url.indexOf('/', lenParentTitle + 1);
        if (nextSlashPosition > 0) {
            return url.substring(0, nextSlashPosition + 1);
        }
        int requestParamSepPosition = url.indexOf('&', lenParentTitle + 1);
        if (requestParamSepPosition > 0) {
            return url.substring(0, requestParamSepPosition + 1);
        }

        return url;
    }

    private static NetworkMapTreeNodeDTO statisticTreeNodes(List<NetworkMapTreeNodeDTO> treeNodeDTOS) {
        final NetworkMapTreeNodeDTO result = new NetworkMapTreeNodeDTO();
        treeNodeDTOS.forEach(node -> {
            result.accumulate(node.getStatusCode(), node.getContentLength(), node.getContentType());
        });

        return result;
    }
}

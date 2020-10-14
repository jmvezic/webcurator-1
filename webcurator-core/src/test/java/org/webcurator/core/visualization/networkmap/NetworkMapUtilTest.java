package org.webcurator.core.visualization.networkmap;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.webcurator.core.visualization.BaseVisualizationTest;
import org.webcurator.core.visualization.networkmap.bdb.BDBNetworkMap;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapNode;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapNodeDTO;
import org.webcurator.core.visualization.networkmap.metadata.NetworkMapTreeNodeDTO;
import org.webcurator.core.visualization.networkmap.processor.IndexProcessor;
import org.webcurator.core.visualization.networkmap.processor.IndexProcessorWarc;
import org.webcurator.core.visualization.networkmap.service.NetworkMapClientLocal;
import org.webcurator.core.visualization.networkmap.service.NetworkMapServiceSearchCommand;

import java.io.File;
import java.util.List;

public class NetworkMapUtilTest extends BaseVisualizationTest {
    private IndexProcessor indexer;

    @Before
    public void initTest() throws Exception {
        super.initTest();

        NetworkMapDomainSuffix suffixParser = new NetworkMapDomainSuffix();
        Resource resource = new ClassPathResource("public_suffix_list.dat");

        try {
            suffixParser.init(resource.getFile());
        } catch (Exception e) {
            log.error("Load domain suffix file failed.", e);
        }
        NetworkMapNode.setTopDomainParse(suffixParser);

        String dbPath = pool.getDbPath(targetInstanceId, harvestResultNumber);
        File f = new File(dbPath);
        f.deleteOnExit(); //Clear the existing db

        indexer = new IndexProcessorWarc(pool, targetInstanceId, harvestResultNumber);
        indexer.init(processorManager, directoryManager, wctClient);

        indexer.processInternal();
    }

    @Test
    public void testClassifyTreeViewByPathName() {
        NetworkMapClientLocal localClient = new NetworkMapClientLocal(pool, processorManager);
        BDBNetworkMap db = pool.getInstance(targetInstanceId, harvestResultNumber);

        NetworkMapServiceSearchCommand searchCommand = new NetworkMapServiceSearchCommand();
        List<NetworkMapNodeDTO> networkMapNodeDTOList = localClient.searchUrlDTOs(db, targetInstanceId, harvestResultNumber, searchCommand);

        NetworkMapTreeNodeDTO rootTreeNodeDTO = new NetworkMapTreeNodeDTO();
        networkMapNodeDTOList.forEach(node -> {
            NetworkMapTreeNodeDTO treeNodeDTO = new NetworkMapTreeNodeDTO();
            treeNodeDTO.setUrl(node.getUrl());
            treeNodeDTO.setContentType(node.getContentType());
            treeNodeDTO.setStatusCode(node.getStatusCode());
            treeNodeDTO.setContentLength(node.getContentLength());

            rootTreeNodeDTO.getChildren().add(treeNodeDTO);
        });

        NetworkMapUtil.classifyTreeViewByPathNames(db, localClient, rootTreeNodeDTO);

        log.debug("Size: {}", rootTreeNodeDTO.getChildren().size());
        assert true;
    }

}

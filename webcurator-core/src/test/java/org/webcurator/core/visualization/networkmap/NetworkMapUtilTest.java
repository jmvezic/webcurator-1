package org.webcurator.core.visualization.networkmap;

import org.junit.Before;
import org.junit.Ignore;
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

    @Ignore
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

        NetworkMapUtil.classifyTreeViewByPathNames(rootTreeNodeDTO);

        log.debug("Size: {}", rootTreeNodeDTO.getChildren().size());
        assert true;
    }


    @Test
    public void testClassifyTreeViewByPathName2() {
        NetworkMapTreeNodeDTO rootTreeNodeDTO = new NetworkMapTreeNodeDTO();
        NetworkMapTreeNodeDTO t1 = new NetworkMapTreeNodeDTO();
        t1.setUrl("http://a.b.c/d/e/f?x=1");
        t1.setContentType("T1");
        t1.setStatusCode(200);
        t1.setContentLength(3);
        t1.accumulate(200, 3, "T1");
        rootTreeNodeDTO.getChildren().add(t1);

        NetworkMapTreeNodeDTO t2 = new NetworkMapTreeNodeDTO();
        t2.setUrl("http://a.b.c/d/u/v?x=1");
        t2.setContentType("T2");
        t2.setStatusCode(200);
        t2.setContentLength(5);
        t2.accumulate(200, 5, "T2");
        rootTreeNodeDTO.getChildren().add(t2);

        NetworkMapUtil.classifyTreeViewByPathNames(rootTreeNodeDTO);


        assert rootTreeNodeDTO.getChildren().size() == 2;
        assert rootTreeNodeDTO.getTotSize() == 8;

        NetworkMapTreeNodeDTO t3 = new NetworkMapTreeNodeDTO();
        t3.setUrl("http://a.b.c/");
        t3.setContentType("T3");
        t3.setStatusCode(200);
        t3.setContentLength(5);
        t3.accumulate(200, 5, "T3");
        rootTreeNodeDTO.getChildren().add(t3);

        NetworkMapUtil.classifyTreeViewByPathNames(rootTreeNodeDTO);
        assert rootTreeNodeDTO.getChildren().size() == 3;
        assert rootTreeNodeDTO.getTotSize() == 13;


        NetworkMapTreeNodeDTO t4 = new NetworkMapTreeNodeDTO();
        t4.setUrl("http://www.b.c/");
        t4.setContentType("T4");
        t4.setStatusCode(200);
        t4.setContentLength(100);
        t4.accumulate(200, 100, "T4");
        rootTreeNodeDTO.getChildren().add(t4);
        rootTreeNodeDTO.setTitle(null);
        NetworkMapUtil.classifyTreeViewByPathNames(rootTreeNodeDTO);
        assert rootTreeNodeDTO.getChildren().size() == 2;
        assert rootTreeNodeDTO.getTotSize() == 113;

        log.debug("Size: {}", rootTreeNodeDTO.getChildren().size());
    }

    @Test
    public void testGetNextTitle() {
        {
            String url = "http://a.b.c/d/u/v?x=1";
            String parentTitle = null;
            int lenParentTitle = parentTitle == null ? 8 : parentTitle.length();

            String title = NetworkMapUtil.getNextTitle(lenParentTitle, url);

            assert title.equals("http://a.b.c/");
        }

        {
            String url = "http://a.b.c/d/u/v?x=1";
            String parentTitle = "http://a.b.c/";
            int lenParentTitle = parentTitle == null ? 8 : parentTitle.length();

            String title = NetworkMapUtil.getNextTitle(lenParentTitle, url);

            assert title.equals("http://a.b.c/d/");
        }

        {
            String url = "http://www.b.c/d/u/v?x=1";
            String parentTitle = "http://www.b.c/";
            int lenParentTitle = parentTitle == null ? 8 : parentTitle.length();

            String title = NetworkMapUtil.getNextTitle(lenParentTitle, url);

            assert title.equals("http://www.b.c/d/");
        }
    }
}

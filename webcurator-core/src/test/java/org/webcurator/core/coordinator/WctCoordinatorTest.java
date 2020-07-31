package org.webcurator.core.coordinator;

import com.google.common.collect.Lists;
import org.apache.tools.ant.taskdefs.Tar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.webcurator.app.TestApplication;
import org.webcurator.core.exceptions.WCTRuntimeException;
import org.webcurator.core.harvester.HarvesterType;
import org.webcurator.core.harvester.agent.HarvestAgentFactory;
import org.webcurator.core.harvester.agent.MockHarvestAgent;
import org.webcurator.core.harvester.agent.MockHarvestAgentFactory;
import org.webcurator.core.harvester.coordinator.*;
import org.webcurator.core.reader.MockLogReader;
import org.webcurator.core.scheduler.MockTargetInstanceManager;
import org.webcurator.core.scheduler.TargetInstanceManager;
import org.webcurator.core.scheduler.TargetInstanceManagerImpl;
import org.webcurator.core.store.DigitalAssetStore;
import org.webcurator.core.store.DigitalAssetStoreFactory;
import org.webcurator.core.store.MockDigitalAssetStore;
import org.webcurator.core.store.MockDigitalAssetStoreFactory;
import org.webcurator.core.targets.TargetManager;
import org.webcurator.domain.TargetInstanceDAO;
import org.webcurator.domain.TargetInstanceDAOImpl;
import org.webcurator.domain.model.core.*;
import org.webcurator.domain.model.core.harvester.agent.HarvestAgentStatusDTO;
import org.webcurator.domain.model.core.harvester.agent.HarvesterStatusDTO;
import org.webcurator.domain.model.dto.QueuedTargetInstanceDTO;

import java.io.File;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestApplication.class})
@SpringBootTest
public class WctCoordinatorTest {
    @Autowired
    private WctCoordinatorImpl wctCoordinator;

    @Autowired
    @Qualifier(value = "targetInstanceDao")
    private TargetInstanceDAOImpl tiDao;

    private WctCoordinatorImpl testInstance;

    private HarvestBandwidthManager mockHarvestBandwidthManager;

    private HarvesterStatusDTO getStatusDTO(String status) {
        HarvesterStatusDTO dto = new HarvesterStatusDTO();
        dto.setStatus(status);
        return dto;
    }

    @Before
    public void setup() {
        testInstance = new WctCoordinatorImpl();
        testInstance.setHarvestLogManager(wctCoordinator.getHarvestLogManager());
        testInstance.setHarvestQaManager(wctCoordinator.getHarvestQaManager());
        testInstance.setHarvestResultManager(wctCoordinator.getHarvestResultManager());
        testInstance.setInTrayManager(wctCoordinator.getInTrayManager());
        testInstance.setTargetInstanceDao(wctCoordinator.getTargetInstanceDao());
        testInstance.setTargetManager(wctCoordinator.getTargetManager());
        testInstance.setTargetInstanceManager(wctCoordinator.getTargetInstanceManager());

        HarvestAgentManagerImpl harvestAgentManager = new HarvestAgentManagerImpl();
        HarvestAgentFactory mockHarvestAgentFactory = mock(HarvestAgentFactory.class);
        when(mockHarvestAgentFactory.getHarvestAgent(any(HarvestAgentStatusDTO.class))).thenReturn(new MockHarvestAgent());
        harvestAgentManager.setHarvestAgentFactory(mockHarvestAgentFactory);
        harvestAgentManager.setHarvestResultManager(wctCoordinator.getHarvestResultManager());
        harvestAgentManager.setTargetInstanceDao(wctCoordinator.getTargetInstanceDao());
        harvestAgentManager.setWctCoordinator(testInstance);
        harvestAgentManager.setTargetInstanceManager(wctCoordinator.getTargetInstanceManager());
        testInstance.setHarvestAgentManager(harvestAgentManager);

        MockDigitalAssetStoreFactory mockDigitalAssetStoreFactory = new MockDigitalAssetStoreFactory();
        testInstance.setDigitalAssetStoreFactory(mockDigitalAssetStoreFactory);

        mockHarvestBandwidthManager = mock(HarvestBandwidthManager.class);
        when(mockHarvestBandwidthManager.getCurrentGlobalMaxBandwidth()).thenReturn(100L);
        when(mockHarvestBandwidthManager.isHarvestOptimizationAllowed()).thenReturn(true);
        testInstance.setHarvestBandwidthManager(mockHarvestBandwidthManager);
    }

    @Test
    public final void testHeartbeatQueuedRunning() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_QUEUED);
        tiDao.save(ti);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5001", getStatusDTO("Running"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        testInstance.heartbeat(aStatus);

        ti = tiDao.load(5001L);
        assert TargetInstance.STATE_RUNNING.equals(ti.getState());
    }

    @Test
    public final void testHeartbeatPausedRunning() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_PAUSED);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5001", getStatusDTO("Running"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        testInstance.heartbeat(aStatus);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_RUNNING, ti.getState());
    }

    @Test
    public final void testHeartbeatRunningPaused() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_RUNNING);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5001", getStatusDTO("Paused"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        testInstance.heartbeat(aStatus);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_PAUSED, ti.getState());
    }

    @Test
    public final void testHeartbeatRunningFinished() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_RUNNING);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5001", getStatusDTO("Finished"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        testInstance.heartbeat(aStatus);

        ti = tiDao.load(5001L);
        assertEquals(ti.getState(), TargetInstance.STATE_STOPPING);
    }

    @Test
    public final void testHeartbeatRunningAborted() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_RUNNING);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5001", getStatusDTO("Could not launch job - Fatal InitializationException"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        testInstance.heartbeat(aStatus);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_ABORTED, ti.getState());
    }

    @Test
    public final void testHarvestOrQueue() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_SCHEDULED);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5001", getStatusDTO("Running"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        aStatus.setMaxHarvests(2);
        aStatus.setHarvesterType(HarvesterType.HERITRIX1.name());
        testInstance.heartbeat(aStatus);

        QueuedTargetInstanceDTO dto = new QueuedTargetInstanceDTO(ti.getOid(), ti.getScheduledTime(), ti.getPriority(),
                ti.getState(), ti.getBandwidthPercent(), ti.getOwningUser().getAgency().getName());
        when(mockHarvestBandwidthManager.isMiniumBandwidthAvailable(dto)).thenReturn(true);
        testInstance.harvestOrQueue(dto);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_RUNNING, ti.getState());
    }

    @Test
    public final void testHarvestOrQueuePaused() {
        testInstance.pauseQueue();

        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_SCHEDULED);

        QueuedTargetInstanceDTO dto = new QueuedTargetInstanceDTO(ti.getOid(), ti.getScheduledTime(), ti.getPriority(),
                ti.getState(), ti.getBandwidthPercent(), ti.getOwningUser().getAgency().getName());

        testInstance.harvestOrQueue(dto);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_QUEUED, ti.getState());
    }

    @Test
    public final void testHarvestOrQueueMemoryWarning() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_SCHEDULED);
        tiDao.save(ti);

        Target target=(Target)ti.getTarget();
        target.setRunOnApproval(true);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5001", getStatusDTO("Running"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        aStatus.setMaxHarvests(2);
        aStatus.setMemoryWarning(true);
        testInstance.heartbeat(aStatus);

        QueuedTargetInstanceDTO dto = new QueuedTargetInstanceDTO(ti.getOid(), ti.getScheduledTime(), ti.getPriority(),
                ti.getState(), ti.getBandwidthPercent(), ti.getOwningUser().getAgency().getName());

        testInstance.harvestOrQueue(dto);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_QUEUED, ti.getState());
    }

    @Test
    public final void testAgentPaused() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_SCHEDULED);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5002", getStatusDTO("Running"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        aStatus.setMaxHarvests(2);
        aStatus.setAcceptTasks(false);
        aStatus.setHarvesterType(HarvesterType.HERITRIX1.name());
        testInstance.heartbeat(aStatus);

        QueuedTargetInstanceDTO dto = new QueuedTargetInstanceDTO(ti.getOid(), ti.getScheduledTime(), ti.getPriority(),
                ti.getState(), ti.getBandwidthPercent(), ti.getOwningUser().getAgency().getName());
        when(mockHarvestBandwidthManager.isMiniumBandwidthAvailable(dto)).thenReturn(true);

        testInstance.harvestOrQueue(dto);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_QUEUED, ti.getState());

        aStatus.setAcceptTasks(true);
        testInstance.harvestOrQueue(dto);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_RUNNING, ti.getState());
    }

    @Test
    public final void testHarvest() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_SCHEDULED);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5002", getStatusDTO("Running"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        aStatus.setMaxHarvests(2);
        testInstance.heartbeat(aStatus);

        HarvestAgentStatusDTO has = (HarvestAgentStatusDTO) testInstance.getHarvestAgents().get("Test Agent");

        testInstance.harvest(ti, has);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_RUNNING, ti.getState());
    }

    @Test
    public final void testHarvestStoreSeedHistory() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_SCHEDULED);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();
        aHarvesterStatus.put("Target-5002", getStatusDTO("Running"));

        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        aStatus.setMaxHarvests(2);
        testInstance.heartbeat(aStatus);

        HarvestAgentStatusDTO has = (HarvestAgentStatusDTO) testInstance.getHarvestAgents().get("Test Agent");

//        mockTargetInstanceManager.setStoreSeedHistory(true);

        testInstance.harvest(ti, has);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_RUNNING, ti.getState());
        assertNotNull(ti.getSeedHistory());
        assertTrue(ti.getSeedHistory().size() > 0);
    }

    @Test
    public final void testHarvestDontStoreSeedHistory() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_QUEUED);
        tiDao.save(ti);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5001", getStatusDTO("Running"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        aStatus.setMaxHarvests(2);
        testInstance.heartbeat(aStatus);

        HarvestAgentStatusDTO has = (HarvestAgentStatusDTO) testInstance.getHarvestAgents().get("Test Agent");

        TargetInstanceManagerImpl targetInstanceManager = (TargetInstanceManagerImpl) testInstance.getTargetInstanceManager();
        targetInstanceManager.setStoreSeedHistory(false);

        testInstance.harvest(ti, has);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_RUNNING, ti.getState());
        assertNotNull(ti.getSeedHistory());
        assertEquals(ti.getSeedHistory().size(), 0);
    }

    @Test
    public final void testHarvestPaused() {
        testInstance.pauseQueue();

        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_SCHEDULED);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5002", getStatusDTO("Running"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        aStatus.setMaxHarvests(2);
        testInstance.heartbeat(aStatus);

        HarvestAgentStatusDTO has = (HarvestAgentStatusDTO) testInstance.getHarvestAgents().get("Test Agent");

        testInstance.harvest(ti, has);

        ti = tiDao.load(5001L);
        assertEquals(TargetInstance.STATE_SCHEDULED, ti.getState());
    }

    @Test
    public final void testHarvestMemoryWarning() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_SCHEDULED);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5002", getStatusDTO("Running"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        aStatus.setMaxHarvests(2);
        aStatus.setMemoryWarning(true);
        testInstance.heartbeat(aStatus);

        HarvestAgentStatusDTO has = (HarvestAgentStatusDTO) testInstance.getHarvestAgents().get("Test Agent");

        testInstance.harvest(ti, has);

        ti = tiDao.load(5001L);
        assertEquals(ti.getState(), TargetInstance.STATE_SCHEDULED);
    }

    @Test
    public final void testHarvestProfilePrefix() {
        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_SCHEDULED);

        HashMap<String, HarvesterStatusDTO> aHarvesterStatus = new HashMap<>();

        aHarvesterStatus.put("Target-5002", getStatusDTO("Running"));
        HarvestAgentStatusDTO aStatus = new HarvestAgentStatusDTO();
        aStatus.setName("Test Agent");
        aStatus.setHarvesterStatus(aHarvesterStatus);
        aStatus.setMaxHarvests(2);
        testInstance.heartbeat(aStatus);

        HarvestAgentStatusDTO has = (HarvestAgentStatusDTO) testInstance.getHarvestAgents().get("Test Agent");

        testInstance.harvest(ti, has);

        MockHarvestAgentFactory mockHarvestAgentFactory = new MockHarvestAgentFactory();
        MockHarvestAgent agent = mockHarvestAgentFactory.getMockHarvestAgent();
        String profile = agent.getProfileString();
        String ti_oid = ti.getOid().toString();

        ti = tiDao.load(5001L);
        assertTrue(ti.getTarget().getProfile().getProfile().contains("IAH-${TI_OID}"));
        assertFalse(profile.contains("IAH-${TI_OID}"));
        assertTrue(profile.contains("IAH-" + ti_oid));
    }

    @Test
    public final void testCompleteArchiving() {
        String archiveId = "12345";
        MockDigitalAssetStoreFactory factory = new MockDigitalAssetStoreFactory(new MockDigitalAssetStore());
        testInstance.setDigitalAssetStoreFactory(factory);

        TargetInstance ti = tiDao.load(5000L);
        ti.setState(TargetInstance.STATE_ENDORSED);
        assertNull(ti.getArchivedTime());

        testInstance.completeArchiving(5000L, archiveId);

        ti = tiDao.load(5001L);
        assertEquals(ti.getState(), TargetInstance.STATE_ARCHIVED);
        assertNotNull(ti.getArchivedTime());
        assertEquals(ti.getArchiveIdentifier(), archiveId);
        MockDigitalAssetStore store = (MockDigitalAssetStore) factory.getDAS();
        assertEquals(1, store.getRemovedIndexes().size());
    }

    @Test
    public final void testReIndexHarvestResult() {
        MockDigitalAssetStore store = (MockDigitalAssetStore) testInstance.getDigitalAssetStoreFactory().getDAS();


//        testInstance.setDigitalAssetStoreFactory(new MockDigitalAssetStoreFactory(store));
        {
            TargetInstance ti = tiDao.load(5000L);
            ti.setState(TargetInstance.STATE_HARVESTED);
            tiDao.save(ti);

            List<HarvestResult> results = ti.getHarvestResults();

            int numResults = results.size();
            assertTrue(numResults > 0);

            HarvestResult result = results.get(numResults - 1);
            assertNotNull(result);

            result.setState(HarvestResult.STATE_INDEXING);
            tiDao.save(result);

            store.setCheckIndexingReturn(true);
            assertFalse(testInstance.reIndexHarvestResult(result));

            ti = tiDao.load(5000L);
            assertEquals(ti.getState(), TargetInstance.STATE_HARVESTED);

            result = ti.getHarvestResult(0);
            assertEquals(result.getState(), HarvestResult.STATE_INDEXING);

            results = ti.getHarvestResults();
            assertEquals(results.size(), numResults);
            assertEquals(results.get(results.size() - 1).getState(), HarvestResult.STATE_INDEXING);
        }
        {
            TargetInstance ti = tiDao.load(5000L);
            ti.setState(TargetInstance.STATE_HARVESTED);

            List<HarvestResult> results = ti.getHarvestResults();

            int numResults = results.size();
            assertTrue(numResults > 0);

            HarvestResult result = results.get(numResults - 1);
            assertNotNull(result);

            result.setState(HarvestResult.STATE_INDEXING);
            store.setCheckIndexingReturn(false);
            assertTrue(testInstance.reIndexHarvestResult(result));

            ti = tiDao.load(5000L);
            assertEquals(ti.getState(), TargetInstance.STATE_HARVESTED);

            result = ti.getHarvestResult(0);
            assertEquals(result.getState(), HarvestResult.STATE_ABORTED);

            results = ti.getHarvestResults();
            assertEquals(results.size(), numResults + 1);
            assertEquals(results.get(results.size() - 1).getState(), HarvestResult.STATE_INDEXING);
        }
    }

    @Test
    public final void testHarvestComplete() {
        MockDigitalAssetStore store = new MockDigitalAssetStore();

//        testInstance.setDigitalAssetStoreFactory(new MockDigitalAssetStoreFactory(store));

        TargetInstance ti = tiDao.load(5001L);
        ti.setState(TargetInstance.STATE_STOPPING);
        HarvestResultDTO ahr = new HarvestResultDTO();
        ahr.setCreationDate(new Date());
        ahr.setTargetInstanceOid(ti.getOid());
        ahr.setProvenanceNote("Original Harvest");

        List<HarvestResult> results = ti.getHarvestResults();
        assertTrue(results.isEmpty());

        testInstance.harvestComplete(ahr);

        assertEquals(ti.getState(), TargetInstance.STATE_HARVESTED);
        results = ti.getHarvestResults();
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());

        testInstance.harvestComplete(ahr);

        assertEquals(ti.getState(), TargetInstance.STATE_HARVESTED);
        results = ti.getHarvestResults();
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
    }

    @Test
    public void testStop() {
        HarvestAgentManager mockHarvestAgentManager = mock(HarvestAgentManager.class);
        testInstance.setHarvestAgentManager(mockHarvestAgentManager);
        TargetInstance mockTi = mock(TargetInstance.class);
        testInstance.stop(mockTi);
        verify(mockHarvestAgentManager).stop(mockTi);

    }

    @Test
    public void testQueueOptimizableInstancesNoUpcoming() {
        TargetInstance ti = tiDao.load(5000);
        ti.setState(TargetInstance.STATE_SCHEDULED);
        tiDao.save(ti);
        when(mockHarvestBandwidthManager.isHarvestOptimizationAllowed()).thenReturn(true);
        testInstance.setHarvestOptimizationEnabled(true);
        testInstance.queueOptimisableInstances();
        List<QueuedTargetInstanceDTO> arrayList = tiDao.getUpcomingJobs(3600 * 1000);
        assert arrayList.size() > 0;
    }

    @Test
    public void testQueueOptimizableInstances() {
        when(mockHarvestBandwidthManager.isHarvestOptimizationAllowed()).thenReturn(true);

        long tiOid = 1234L;
        long abstractTargetOid = 4312L;

        ArrayList<QueuedTargetInstanceDTO> queuedTiList = Lists.newArrayList();
        QueuedTargetInstanceDTO mockQueued = mock(QueuedTargetInstanceDTO.class);
        when(mockQueued.getOid()).thenReturn(tiOid);
        queuedTiList.add(mockQueued);
        when(tiDao.getUpcomingJobs(anyLong())).thenReturn(queuedTiList);

        TargetInstance mockTi = mock(TargetInstance.class);
        when(tiDao.load(tiOid)).thenReturn(mockTi);
        when(tiDao.populate(mockTi)).thenReturn(mockTi);
        Target mockTarget = mock(Target.class);
        when(mockTarget.isAllowOptimize()).thenReturn(true);
        AbstractTarget mockAbstractTarget = mock(AbstractTarget.class);
        when(mockTi.getTarget()).thenReturn(mockAbstractTarget);
        when(mockAbstractTarget.getObjectType()).thenReturn(AbstractTarget.TYPE_TARGET);
        when(mockAbstractTarget.getOid()).thenReturn(abstractTargetOid);

        TargetManager mockTargetManager = mock(TargetManager.class);
        when(mockTargetManager.load(abstractTargetOid)).thenReturn(mockTarget);

        testInstance.setHarvestOptimizationEnabled(true);
        testInstance.setTargetInstanceDao(tiDao);
        testInstance.setTargetManager(mockTargetManager);
        testInstance.setHarvestBandwidthManager(mockHarvestBandwidthManager);
        testInstance.queueOptimisableInstances();
        verify(tiDao).getUpcomingJobs(anyLong());
        verify(tiDao).load(tiOid);
        verify(tiDao).populate(mockTi);
        verify(mockTargetManager).load(abstractTargetOid);

    }

    @Test
    public void testCheckBandwidthTransition() {
        testInstance.checkForBandwidthTransition();
        verify(mockHarvestBandwidthManager).checkForBandwidthTransition();
    }

    @Test
    public void testCurrentGlobalMaxBandwidth() {
        testInstance.getCurrentGlobalMaxBandwidth();
        verify(mockHarvestBandwidthManager).getCurrentGlobalMaxBandwidth();
    }

    @Test
    public void testHarvestOptimizationAllowed() {
        testInstance.isHarvestOptimizationAllowed();
        verify(mockHarvestBandwidthManager).isHarvestOptimizationAllowed();
    }

    @Test
    public void testSetMinimumBandwidth() {
        testInstance.setMinimumBandwidth(123);
        verify(mockHarvestBandwidthManager).setMinimumBandwidth(123);
    }

    @Test
    public void testSetMaxBandwidthPercent() {
        testInstance.setMaxBandwidthPercent(21);
        verify(mockHarvestBandwidthManager).setMaxBandwidthPercent(21);
    }

    @Test
    public void testResume() {
        HarvestAgentManager mockHarvestAgentManager = mock(HarvestAgentManager.class);
        testInstance.setHarvestAgentManager(mockHarvestAgentManager);
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        testInstance.resume(mockTargetInstance);
        verify(mockHarvestAgentManager).resume(mockTargetInstance);
        verify(mockHarvestBandwidthManager).sendBandWidthRestrictions();
    }

    @Test
    public void testPause() {
        HarvestAgentManager mockHarvestAgentManager = mock(HarvestAgentManager.class);
        testInstance.setHarvestAgentManager(mockHarvestAgentManager);
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        testInstance.pause(mockTargetInstance);
        verify(mockHarvestAgentManager).pause(mockTargetInstance);
    }

    @Test
    public void testResumeAll() {
        HarvestAgentManager mockHarvestAgentManager = mock(HarvestAgentManager.class);
        testInstance.setHarvestAgentManager(mockHarvestAgentManager);
        testInstance.resumeAll();
        verify(mockHarvestAgentManager).resumeAll();
    }

    @Test
    public void testPauseAll() {
        HarvestAgentManager mockHarvestAgentManager = mock(HarvestAgentManager.class);
        testInstance.setHarvestAgentManager(mockHarvestAgentManager);
        testInstance.pauseAll();
        verify(mockHarvestAgentManager).pauseAll();
    }

    @Test
    public void testAbort() {
        HarvestAgentManager mockHarvestAgentManager = mock(HarvestAgentManager.class);
        testInstance.setHarvestAgentManager(mockHarvestAgentManager);
        HarvestBandwidthManager mockHarvestBandwidthManager = mock(HarvestBandwidthManager.class);
        testInstance.setHarvestBandwidthManager(mockHarvestBandwidthManager);
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        testInstance.abort(mockTargetInstance);
        verify(mockHarvestAgentManager).abort(mockTargetInstance);
        verify(mockHarvestBandwidthManager).sendBandWidthRestrictions();
    }

    @Test
    public void testGetBandwidthRestrictions() {
        HarvestBandwidthManager mockHarvestBandwidthManager = mock(HarvestBandwidthManager.class);
        testInstance.setHarvestBandwidthManager(mockHarvestBandwidthManager);
        testInstance.getBandwidthRestrictions();
        verify(mockHarvestBandwidthManager).getBandwidthRestrictions();
    }

    @Test
    public void testGetBandwidthRestriction() {
        HarvestBandwidthManager mockHarvestBandwidthManager = mock(HarvestBandwidthManager.class);
        testInstance.setHarvestBandwidthManager(mockHarvestBandwidthManager);
        testInstance.getBandwidthRestriction(123L);
        verify(mockHarvestBandwidthManager).getBandwidthRestriction(anyLong());
    }

    @Test
    public void testGetBandwidthRestrictionForDay() {
        HarvestBandwidthManager mockHarvestBandwidthManager = mock(HarvestBandwidthManager.class);
        testInstance.setHarvestBandwidthManager(mockHarvestBandwidthManager);
        testInstance.getBandwidthRestriction("test", new Date());
        verify(mockHarvestBandwidthManager).getBandwidthRestriction(anyString(), any(Date.class));
    }

    @Test
    public void testSaveOrUpdate() {
        BandwidthRestriction mockBandwidthRestriction = mock(BandwidthRestriction.class);
        HarvestBandwidthManager mockHarvestBandwidthManager = mock(HarvestBandwidthManager.class);
        testInstance.setHarvestBandwidthManager(mockHarvestBandwidthManager);
        testInstance.saveOrUpdate(mockBandwidthRestriction);
        verify(mockHarvestBandwidthManager).saveOrUpdate(mockBandwidthRestriction);
    }

    @Test
    public void testDelete() {
        BandwidthRestriction mockBandwidthRestriction = mock(BandwidthRestriction.class);
        HarvestBandwidthManager mockHarvestBandwidthManager = mock(HarvestBandwidthManager.class);
        testInstance.setHarvestBandwidthManager(mockHarvestBandwidthManager);
        testInstance.delete(mockBandwidthRestriction);
        verify(mockHarvestBandwidthManager).delete(mockBandwidthRestriction);
    }


    @Test
    public void testPurgeDigitalAssetsNone() {
        TargetInstanceDAO mockTiDao = mock(TargetInstanceDAO.class);
        List<TargetInstance> purgeableTargetInstances = Arrays.<TargetInstance>asList();
        when(mockTiDao.findPurgeableTargetInstances(any(Date.class))).thenReturn(purgeableTargetInstances);
        testInstance.setTargetInstanceDao(mockTiDao);
        testInstance.purgeDigitalAssets();

    }

    @Test
    public void testPurgeDigitalAssetsOne() throws Exception {
        TargetInstanceManager mockTargetInstanceManager = mock(TargetInstanceManager.class);
        testInstance.setTargetInstanceManager(mockTargetInstanceManager);
        TargetInstanceDAO mockTiDao = mock(TargetInstanceDAO.class);
        testInstance.setTargetInstanceDao(mockTiDao);
        DigitalAssetStoreFactory mockDasFactory = mock(DigitalAssetStoreFactory.class);
        testInstance.setDigitalAssetStoreFactory(mockDasFactory);
        DigitalAssetStore mockDas = mock(DigitalAssetStore.class);
        when(mockDasFactory.getDAS()).thenReturn(mockDas);

        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        List<TargetInstance> purgeableTargetInstances = Arrays.asList(mockTargetInstance);
        when(mockTiDao.findPurgeableTargetInstances(any(Date.class))).thenReturn(purgeableTargetInstances);
        testInstance.purgeDigitalAssets();
        verify(mockTargetInstanceManager).purgeTargetInstance(mockTargetInstance);
        verify(mockDas).purge(any(List.class));
    }

    @Test
    public void testPurgeDigitalAssetsMultiple() throws Exception {
        TargetInstanceManager mockTargetInstanceManager = mock(TargetInstanceManager.class);
        testInstance.setTargetInstanceManager(mockTargetInstanceManager);
        TargetInstanceDAO mockTiDao = mock(TargetInstanceDAO.class);
        testInstance.setTargetInstanceDao(mockTiDao);
        DigitalAssetStoreFactory mockDasFactory = mock(DigitalAssetStoreFactory.class);
        testInstance.setDigitalAssetStoreFactory(mockDasFactory);
        DigitalAssetStore mockDas = mock(DigitalAssetStore.class);
        when(mockDasFactory.getDAS()).thenReturn(mockDas);

        TargetInstance mockTargetInstance1 = mock(TargetInstance.class);
        TargetInstance mockTargetInstance2 = mock(TargetInstance.class);
        List<TargetInstance> purgeableTargetInstances = Arrays.asList(mockTargetInstance1, mockTargetInstance2);
        when(mockTiDao.findPurgeableTargetInstances(any(Date.class))).thenReturn(purgeableTargetInstances);
        testInstance.purgeDigitalAssets();
        verify(mockTargetInstanceManager).purgeTargetInstance(mockTargetInstance1);
        verify(mockTargetInstanceManager).purgeTargetInstance(mockTargetInstance2);
        verify(mockDas).purge(any(List.class));
    }

    @Test
    public void testPurgeAbortedTargetInstancesNone() throws Exception {
        HarvestAgentManager mockHarvestAgentManager = mock(HarvestAgentManager.class);
        testInstance.setHarvestAgentManager(mockHarvestAgentManager);
        TargetInstanceManager mockTargetInstanceManager = mock(TargetInstanceManager.class);
        testInstance.setTargetInstanceManager(mockTargetInstanceManager);
        TargetInstanceDAO mockTiDao = mock(TargetInstanceDAO.class);
        testInstance.setTargetInstanceDao(mockTiDao);
        DigitalAssetStoreFactory mockDasFactory = mock(DigitalAssetStoreFactory.class);
        testInstance.setDigitalAssetStoreFactory(mockDasFactory);
        DigitalAssetStore mockDas = mock(DigitalAssetStore.class);
        when(mockDasFactory.getDAS()).thenReturn(mockDas);

        List<TargetInstance> purgeableTargetInstances = Arrays.asList();
        when(mockTiDao.findPurgeableAbortedTargetInstances(any(Date.class))).thenReturn(purgeableTargetInstances);
        testInstance.purgeAbortedTargetInstances();
        verifyNoMoreInteractions(mockHarvestAgentManager, mockDas, mockTargetInstanceManager);
    }

    @Test
    public void testPurgeAbortedTargetInstances() throws Exception {
        HarvestAgentManager mockHarvestAgentManager = mock(HarvestAgentManager.class);
        testInstance.setHarvestAgentManager(mockHarvestAgentManager);
        TargetInstanceManager mockTargetInstanceManager = mock(TargetInstanceManager.class);
        testInstance.setTargetInstanceManager(mockTargetInstanceManager);
        TargetInstanceDAO mockTiDao = mock(TargetInstanceDAO.class);
        testInstance.setTargetInstanceDao(mockTiDao);
        DigitalAssetStoreFactory mockDasFactory = mock(DigitalAssetStoreFactory.class);
        testInstance.setDigitalAssetStoreFactory(mockDasFactory);
        DigitalAssetStore mockDas = mock(DigitalAssetStore.class);
        when(mockDasFactory.getDAS()).thenReturn(mockDas);

        TargetInstance mockTargetInstance1 = mock(TargetInstance.class);
        TargetInstance mockTargetInstance2 = mock(TargetInstance.class);
        List<TargetInstance> purgeableTargetInstances = Arrays.asList(mockTargetInstance1, mockTargetInstance2);
        when(mockTiDao.findPurgeableAbortedTargetInstances(any(Date.class))).thenReturn(purgeableTargetInstances);
        testInstance.purgeAbortedTargetInstances();
        verify(mockHarvestAgentManager).purgeAbortedTargetInstances(any(List.class));
        verify(mockDas).purgeAbortedTargetInstances(any(List.class));
        verify(mockTargetInstanceManager).purgeTargetInstance(mockTargetInstance1);
        verify(mockTargetInstanceManager).purgeTargetInstance(mockTargetInstance2);
    }

    @Test
    public void testMinimumBandwidthAvailable() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        testInstance.isMiniumBandwidthAvailable(mockTargetInstance);
        verify(mockHarvestBandwidthManager).isMiniumBandwidthAvailable(mockTargetInstance);
    }

    @Test
    public void testListLogFiles() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        testInstance.listLogFiles(mockTargetInstance);

        HarvestLogManager mockHarvestLogManager = mock(HarvestLogManager.class);
        when(mockHarvestLogManager.listLogFiles(mockTargetInstance)).thenReturn(new ArrayList<>());
        testInstance.setHarvestLogManager(mockHarvestLogManager);

        verify(mockHarvestLogManager).listLogFiles(mockTargetInstance);
    }

    @Test(expected = WCTRuntimeException.class)
    public void testListLogFilesException() {
        testInstance.listLogFiles(null);
    }

    @Test
    public void testTailLog() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        int numLines = 123;
        String fileName = "testFile";

        HarvestLogManager mockHarvestLogManager = mock(HarvestLogManager.class);
        when(mockHarvestLogManager.tailLog(mockTargetInstance, fileName, numLines)).thenReturn(new ArrayList<>());
        testInstance.setHarvestLogManager(mockHarvestLogManager);
        testInstance.tailLog(mockTargetInstance, fileName, numLines);
        verify(mockHarvestLogManager).tailLog(mockTargetInstance, fileName, numLines);
    }

    @Test
    public void testCountLogLines() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        String fileName = "testFile";

        HarvestLogManager mockHarvestLogManager = mock(HarvestLogManager.class);
        when(mockHarvestLogManager.countLogLines(mockTargetInstance, fileName)).thenReturn(0);
        testInstance.setHarvestLogManager(mockHarvestLogManager);

        testInstance.countLogLines(mockTargetInstance, fileName);
        verify(mockHarvestLogManager).countLogLines(mockTargetInstance, fileName);
    }

    @Test
    public void testListLogFileAttributes() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);

        HarvestLogManager mockHarvestLogManager = mock(HarvestLogManager.class);
        when(mockHarvestLogManager.listLogFileAttributes(mockTargetInstance)).thenReturn(new ArrayList<>());
        testInstance.setHarvestLogManager(mockHarvestLogManager);

        testInstance.listLogFileAttributes(mockTargetInstance);
        verify(mockHarvestLogManager).listLogFileAttributes(mockTargetInstance);
    }

    @Test
    public void testHeadLog() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        int numLines = 123;
        String fileName = "testFile";

        HarvestLogManager mockHarvestLogManager = mock(HarvestLogManager.class);
        when(mockHarvestLogManager.headLog(mockTargetInstance, fileName, numLines)).thenReturn(new ArrayList<>());
        testInstance.setHarvestLogManager(mockHarvestLogManager);

        testInstance.headLog(mockTargetInstance, fileName, numLines);
        verify(mockHarvestLogManager).headLog(mockTargetInstance, fileName, numLines);
    }

    @Test
    public void testGetLog() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        int startLine = 1;
        int numLines = 123;
        String fileName = "testFile";

        HarvestLogManager mockHarvestLogManager = mock(HarvestLogManager.class);
        when(mockHarvestLogManager.getLog(mockTargetInstance, fileName, startLine, numLines)).thenReturn(new ArrayList<>());
        testInstance.setHarvestLogManager(mockHarvestLogManager);

        testInstance.getLog(mockTargetInstance, fileName, startLine, numLines);
        verify(mockHarvestLogManager).getLog(mockTargetInstance, fileName, startLine, numLines);
    }

    @Test
    public void testGetFirstLogLineContaining() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        String fileName = "testFile";
        String match = "match";

        HarvestLogManager mockHarvestLogManager = mock(HarvestLogManager.class);
        when(mockHarvestLogManager.getFirstLogLineContaining(mockTargetInstance, fileName, match)).thenReturn(0);
        testInstance.setHarvestLogManager(mockHarvestLogManager);

        testInstance.getFirstLogLineContaining(mockTargetInstance, fileName, match);
        verify(mockHarvestLogManager).getFirstLogLineContaining(mockTargetInstance, fileName, match);
    }

    @Test
    public void testGetFirstLogLineBeginning() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        String fileName = "testFile";
        String match = "match";

        HarvestLogManager mockHarvestLogManager = mock(HarvestLogManager.class);
        when(mockHarvestLogManager.getFirstLogLineBeginning(mockTargetInstance, fileName, match)).thenReturn(0);
        testInstance.setHarvestLogManager(mockHarvestLogManager);

        testInstance.getFirstLogLineBeginning(mockTargetInstance, fileName, match);
        verify(mockHarvestLogManager).getFirstLogLineBeginning(mockTargetInstance, fileName, match);
    }

    @Test
    public void testGetFirstLogLineAfterTimestamp() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        String fileName = "testFile";
        Long timestamp = 123456789L;

        HarvestLogManager mockHarvestLogManager = mock(HarvestLogManager.class);
        when(mockHarvestLogManager.getFirstLogLineAfterTimeStamp(mockTargetInstance, fileName, timestamp)).thenReturn(0);
        testInstance.setHarvestLogManager(mockHarvestLogManager);

        testInstance.getFirstLogLineAfterTimeStamp(mockTargetInstance, fileName, timestamp);
        verify(mockHarvestLogManager).getFirstLogLineAfterTimeStamp(mockTargetInstance, fileName, timestamp);
    }

    @Test
    public void testGetLogFile() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        String fileName = "testFile";

        HarvestLogManager mockHarvestLogManager = mock(HarvestLogManager.class);
        when(mockHarvestLogManager.getLogfile(mockTargetInstance, fileName)).thenReturn(new File(fileName));
        testInstance.setHarvestLogManager(mockHarvestLogManager);

        testInstance.getLogfile(mockTargetInstance, fileName);
        verify(mockHarvestLogManager).getLogfile(mockTargetInstance, fileName);
    }

    @Test
    public void testGetHopPath() {
        TargetInstance mockTargetInstance = mock(TargetInstance.class);
        String fileName = "testFile";
        String match = "match";
        testInstance.getHopPath(mockTargetInstance, fileName, match);
//        verify(mockHarvestLogManager).getHopPath(mockTargetInstance, fileName, match);
    }
}

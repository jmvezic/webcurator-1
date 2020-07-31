package org.webcurator.app;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.webcurator.auth.AuthorityManagerImpl;
import org.webcurator.common.util.DateUtils;
import org.webcurator.core.admin.PermissionTemplateManagerImpl;
import org.webcurator.core.agency.AgencyUserManagerImpl;
import org.webcurator.core.archive.ArchiveAdapterImpl;
import org.webcurator.core.archive.SipBuilder;
import org.webcurator.core.check.BandwidthChecker;
import org.webcurator.core.check.CheckProcessor;
import org.webcurator.core.check.Checker;
import org.webcurator.core.check.CoreCheckNotifier;
import org.webcurator.core.common.Environment;
import org.webcurator.core.common.EnvironmentFactory;
import org.webcurator.core.common.EnvironmentImpl;
import org.webcurator.core.coordinator.HarvestResultManager;
import org.webcurator.core.coordinator.HarvestResultManagerImpl;
import org.webcurator.core.coordinator.WctCoordinator;
import org.webcurator.core.coordinator.WctCoordinatorImpl;
import org.webcurator.core.harvester.agent.HarvestAgentFactoryImpl;
import org.webcurator.core.harvester.coordinator.*;
import org.webcurator.core.notification.InTrayManagerImpl;
import org.webcurator.core.notification.MailServerImpl;
import org.webcurator.core.permissionmapping.HierPermMappingDAOImpl;
import org.webcurator.core.permissionmapping.HierarchicalPermissionMappingStrategy;
import org.webcurator.core.permissionmapping.PermMappingSiteListener;
import org.webcurator.core.permissionmapping.PermissionMappingStrategy;
import org.webcurator.core.profiles.PolitenessOptions;
import org.webcurator.core.profiles.ProfileManager;
import org.webcurator.core.reader.LogReader;
import org.webcurator.core.reader.LogReaderClient;
import org.webcurator.core.reader.LogReaderImpl;
import org.webcurator.core.report.LogonDurationDAOImpl;
import org.webcurator.core.rules.QaRecommendationServiceImpl;
import org.webcurator.core.scheduler.TargetInstanceManager;
import org.webcurator.core.scheduler.TargetInstanceManagerImpl;
import org.webcurator.core.sites.SiteManagerImpl;
import org.webcurator.core.sites.SiteManagerListener;
import org.webcurator.core.store.DigitalAssetStoreClient;
import org.webcurator.core.store.DigitalAssetStoreFactoryImpl;
import org.webcurator.core.targets.TargetManagerImpl;
import org.webcurator.core.util.ApplicationContextFactory;
import org.webcurator.core.util.AuditDAOUtil;
import org.webcurator.core.util.LockManager;
import org.webcurator.core.visualization.VisualizationDirectoryManager;
import org.webcurator.core.visualization.networkmap.service.NetworkMapClient;
import org.webcurator.core.visualization.networkmap.service.NetworkMapClientRemote;
import org.webcurator.domain.*;
import org.webcurator.domain.model.core.BusinessObjectFactory;
import org.webcurator.domain.model.core.HarvestResult;

import javax.annotation.PostConstruct;
import java.util.*;

@SuppressWarnings("all")
@TestConfiguration
public class TestConfigBasic {
    private RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${digitalAssetStore.scheme}")
    private String digitalAssetStoreScheme;

    @Value("${digitalAssetStore.host}")
    private String digitalAssetStoreHost;

    @Value("${digitalAssetStore.port}")
    private String digitalAssetStorePort;

    @Value("${wctCoordinator.minimumBandwidth}")
    private int minimumBandwidth;

    @Value("${wctCoordinator.maxBandwidthPercent}")
    private int maxBandwidthPercent;

    @Value("${wctCoordinator.autoQAUrl}")
    private String autoQAUrl;

    @Value("${queueController.enableQaModule}")
    private boolean enableQaModule;

    @Value("${queueController.autoPrunedNote}")
    private String autoPrunedNote;

    @Value("${targetInstanceManager.storeSeedHistory}")
    private boolean storeSeedHistory;

    @Value("${targetManager.allowMultiplePrimarySeeds}")
    private boolean allowMultiplePrimarySeeds;

    @Value("${groupTypes.subgroup}")
    private String groupTypesSubgroup;

    @Value("${harvestAgentFactory.daysToSchedule}")
    private int harvestAgentDaysToSchedule;

    @Value("${createNewTargetInstancesTrigger.schedulesPerBatch}")
    private int targetInstancesTriggerSchedulesPerBatch;

    @Value("${project.version}")
    private String projectVersion;

    @Value("${heritrix.version}")
    private String heritrixVersion;

    @Value("${processScheduleTrigger.startDelay}")
    private long processScheduleTriggerStartDelay;

    @Value("${processScheduleTrigger.repeatInterval}")
    private long processScheduleTriggerRepeatInterval;

    @Value("${bandwidthCheckTrigger.startDelay}")
    private long bandwidthCheckTriggerStartDelay;

    @Value("${bandwidthCheckTrigger.repeatInterval}")
    private long bandwidthCheckTriggerRepeatInterval;

    @Value("${mail.protocol}")
    private String mailProtocol;

    @Value("${mailServer.smtp.host}")
    private String mailServerSmtpHost;

    @Value("${mail.smtp.port}")
    private String mailSmtpPort;

    @Value("${bandwidthChecker.warnThreshold}")
    private long bandwidthCheckerWarnThreshold;

    @Value("${bandwidthChecker.errorThreshold}")
    private long bandwidthCheckerErrorThreshold;

    @Value("${checkProcessorTrigger.startDelay}")
    private long checkProcessorTriggerStartDelay;

    @Value("${checkProcessorTrigger.repeatInterval}")
    private long checkProcessorTriggerRepeatInterval;

    @Value("${purgeDigitalAssetsTrigger.repeatInterval}")
    private long purgeDigitalAssetsTriggerRepeatInterval;

    @Value("${purgeAbortedTargetInstancesTrigger.repeatInterval}")
    private long purgeAbortedTargetInstancesTriggerRepeatInterval;

    @Value("${inTrayManager.sender}")
    private String inTrayManagerSender;

    @Value("${inTrayManager.wctBaseUrl}")
    private String inTrayManagerWctBaseUrl;

    @Value("${groupExpiryJobTrigger.startDelay}")
    private long groupExpiryJobTriggerStartDelay;

    @Value("${groupExpiryJobTrigger.repeatInterval}")
    private long groupExpiryJobTriggerRepeatInterval;

    @Value("${createNewTargetInstancesTrigger.startDelay}")
    private long createNewTargetInstancesTriggerStartDelay;

    @Value("${createNewTargetInstancesTrigger.repeatInterval}")
    private long createNewTargetInstancesTriggerRepeatInterval;

    @Value("${archiveAdapter.targetReferenceMandatory}")
    private boolean archiveAdapterTargetReferenceMandatory;

    @Value("${groupSearchController.defaultSearchOnAgencyOnly}")
    private boolean groupSearchControllerDefaultSearchOnAgencyOnly;

    @Value("${groupTypes.subgroupSeparator}")
    private String groupTypesSubgroupSeparator;

    @Value("${harvestResourceUrlMapper.urlMap}")
    private String harvestResourceUrlMapperUrlMap;

    @Value("${qualityReviewToolController.enableAccessTool}")
    private boolean qualityReviewToolControllerEnableAccessTool;

    @Value("${digitalAssetStoreServer.uploadedFilesDir}")
    private String digitalAssetStoreServerUploadedFilesDir;

    @Value("${wctCoordinator.autoQAUrl}")
    private String harvestCoordinatorAutoQAUrl;

    @Value("${qualityReviewToolController.archiveUrl}")
    private String qualityReviewToolControllerArchiveUrl;

    @Value("${qualityReviewToolController.archiveName}")
    private String qualityReviewToolControllerArchiveName;

    @Value("${qualityReviewToolController.archive.alternative}")
    private String qualityReviewToolControllerArchiveUrlAlternative;

    @Value("${qualityReviewToolController.archive.alternative.name}")
    private String qualityReviewToolControllerArchiveAlternativeName;

    @Value("${qualityReviewToolController.enableBrowseTool}")
    private boolean qualityReviewToolControllerEnableBrowseTool;

    @Value("${qualityReviewToolController.webArchiveTarget}")
    private String qualityReviewToolControllerWebArchiveTarget;

    @Value("${core.base.dir}")
    private String baseDir;

    @Autowired
    private TestConfigLists listsConfig;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private WctCoordinator wctCoordinator;

    @PostConstruct
    public void init() {
        ApplicationContextFactory.setApplicationContext(applicationContext);
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    public VisualizationDirectoryManager visualizationManager() {
        return new VisualizationDirectoryManager(baseDir, "", "");
    }

//    @Bean(name = "wctCoordinator")
//    public WctCoordinator wctCoordinator {
//        WctCoordinatorImpl bean = new WctCoordinatorImpl();
//        return bean;
//    }

    @Bean
    public HarvestResultManager harvestResultManager() {
        HarvestResultManagerImpl bean = new HarvestResultManagerImpl();
        return bean;
    }

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource bean = new ResourceBundleMessageSource();
        bean.setBasename("messages");

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public LogReader logReader() {
        LogReader bean = new LogReaderImpl();
        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public DigitalAssetStoreClient digitalAssetStore() {
        DigitalAssetStoreClient bean = new DigitalAssetStoreClient(digitalAssetStoreScheme, digitalAssetStoreHost, Integer.parseInt(digitalAssetStorePort), restTemplateBuilder);
        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public DigitalAssetStoreFactoryImpl digitalAssetStoreFactory() {
        DigitalAssetStoreFactoryImpl bean = new DigitalAssetStoreFactoryImpl();
        bean.setDAS(digitalAssetStore());
        bean.setLogReader(new LogReaderClient(digitalAssetStoreScheme, digitalAssetStoreHost, Integer.parseInt(digitalAssetStorePort), restTemplateBuilder));
        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public NetworkMapClient networkMapClientReomote() {
        return new NetworkMapClientRemote(digitalAssetStoreScheme, digitalAssetStoreHost, Integer.parseInt(digitalAssetStorePort), restTemplateBuilder);
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public QaRecommendationServiceImpl qaRecommendationService() {
        QaRecommendationServiceImpl bean = new QaRecommendationServiceImpl();
        // The state that will be used to denote a failure within the Rules Engine (eg: an unexpected exception).
        // This state will be returned to the user as the state of the failed indicator along with the exception.
        bean.setStateFailed("Failed");

        // The advice priority is the QA recommendation in rank order, the value of each Map entry being the rank.
        Map<String, Integer> advicePriorityMap = new HashMap<>();
        advicePriorityMap.put("None", 0);
        advicePriorityMap.put("Running", 1);
        advicePriorityMap.put("Archive", 2);
        advicePriorityMap.put("Investigate", 3);
        // Delist has highest priority for a valid indicator since we know that nothing has changed (precluding any other advice).
        advicePriorityMap.put("Delist", 4);
        advicePriorityMap.put("Reject", 5);
        // Failed has the highest priority overall since any failures are unexpected.
        advicePriorityMap.put("Failed", 6);
        bean.setAdvicePriority(advicePriorityMap);

        // Globals objects used by the rules engine.
        Map<String, String> globalsMap = new HashMap<>();
        globalsMap.put("MSG_WITHIN_TOLERANCE", "The {0} indicator value of {1} is within {2}% and {3}% of reference crawl tolerance ({4} &lt;= {5} &lt;= {6})");
        globalsMap.put("MSG_OUTSIDE_TOLERANCE", "The {0} indicator value of {1} is outside {2}% and {3}% of reference crawl tolerance ({5} &lt; {4} or {5} &gt; {6})");
        globalsMap.put("MSG_EXCEEDED_UPPER_LIMIT", "The {0} indicator value of {1} has exceeded its upper limit of {2}");
        globalsMap.put("MSG_FALLEN_BELOW_LOWER_LIMIT", "The {0} indicator value of {1} has fallen below its lower limit of {2}");
        // Advice that will be returned on an indicator.
        globalsMap.put("REJECT", "Reject");
        globalsMap.put("INVESTIGATE", "Investigate");
        globalsMap.put("ARCHIVE", "Archive");
        bean.setGlobals(globalsMap);

        bean.setRulesFileName("rules.drl");
//        bean.setQualityReviewFacade(qualityReviewFacade());
        //bean.setHarvestCoordinator(wctCoordinator);
        bean.setTargetInstanceManager(targetInstanceManager());

        return bean;
    }

    @Bean(name = "targetInstanceDao")
    public TargetInstanceDAO targetInstanceDao() {
        TargetInstanceDAOImpl bean = new TargetInstanceDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);
        bean.setAuditor(audit());

        return bean;
    }

    @Bean
    public UserRoleDAO userRoleDAO() {
        UserRoleDAOImpl bean = new UserRoleDAOImpl();
        bean.setSessionFactory(sessionFactory);
        //bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public RejReasonDAO rejReasonDAO() {
        RejReasonDAOImpl bean = new RejReasonDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public IndicatorDAO indicatorDAO() {
        IndicatorDAOImpl bean = new IndicatorDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public IndicatorCriteriaDAO indicatorCriteriaDAO() {
        IndicatorCriteriaDAOImpl bean = new IndicatorCriteriaDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public IndicatorReportLineDAO indicatorReportLineDAO() {
        IndicatorReportLineDAOImpl bean = new IndicatorReportLineDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public FlagDAO flagDAO() {
        FlagDAOImpl bean = new FlagDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public TargetDAO targetDao() {
        TargetDAOImpl bean = new TargetDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public SiteDAO siteDao() {
        SiteDAOImpl bean = new SiteDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public PermissionDAO permissionDAO() {
        PermissionDAOImpl bean = new PermissionDAOImpl();
        bean.setSessionFactory(sessionFactory);
        return bean;
    }

    @Bean
    public ProfileDAO profileDao() {
        ProfileDAOImpl bean = new ProfileDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public InTrayDAO inTrayDao() {
        InTrayDAOImpl bean = new InTrayDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean(name = "harvestCoordinatorDao")
    public HarvestCoordinatorDAO harvestCoordinatorDao() {
        HarvestCoordinatorDAOImpl bean = new HarvestCoordinatorDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);
        return bean;
    }

    @Bean
    public HeatmapDAO heatmapConfigDao() {
        HeatmapDAOImpl bean = new HeatmapDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public PermissionTemplateDAO permissionTemplateDao() {
        PermissionTemplateDAOImpl bean = new PermissionTemplateDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public SipBuilder sipBuilder() {
        SipBuilder bean = new SipBuilder();
        bean.setTargetInstanceManager(targetInstanceManager());
        bean.setTargetManager(targetManager());

        return bean;
    }

    @Bean
    public HarvestAgentManagerImpl harvestAgentManager() {
        HarvestAgentManagerImpl bean = new HarvestAgentManagerImpl();
        bean.setHarvestAgentFactory(harvestAgentFactory());
        bean.setTargetInstanceManager(targetInstanceManager());
        bean.setTargetInstanceDao(targetInstanceDao());
        bean.setWctCoordinator(wctCoordinator);
        bean.setHarvestResultManager(harvestResultManager());
        return bean;
    }

    @Bean
    public HarvestLogManager harvestLogManager() {
        HarvestLogManagerImpl bean = new HarvestLogManagerImpl();
        bean.setHarvestAgentManager(harvestAgentManager());
        bean.setDigitalAssetStoreFactory(digitalAssetStoreFactory());

        return bean;
    }


    @Bean(name = HarvestResult.PATCH_STAGE_TYPE_CRAWLING)
    public PatchingHarvestLogManager patchingHarvestLogManagerNormal() {
        PatchingHarvestLogManagerImpl bean = new PatchingHarvestLogManagerImpl();
        bean.setHarvestAgentManager(harvestAgentManager());
        bean.setDigitalAssetStoreFactory(digitalAssetStoreFactory());
        bean.setType(HarvestResult.PATCH_STAGE_TYPE_CRAWLING);
        return bean;
    }

    @Bean(name = HarvestResult.PATCH_STAGE_TYPE_MODIFYING)
    public PatchingHarvestLogManager patchingHarvestLogManagerModification() {
        PatchingHarvestLogManagerImpl bean = new PatchingHarvestLogManagerImpl();
        bean.setHarvestAgentManager(harvestAgentManager());
        bean.setDigitalAssetStoreFactory(digitalAssetStoreFactory());
        bean.setType(HarvestResult.PATCH_STAGE_TYPE_MODIFYING);
        return bean;
    }

    @Bean(name = HarvestResult.PATCH_STAGE_TYPE_INDEXING)
    public PatchingHarvestLogManager patchingHarvestLogManagerIndex() {
        PatchingHarvestLogManagerImpl bean = new PatchingHarvestLogManagerImpl();
        bean.setHarvestAgentManager(harvestAgentManager());
        bean.setDigitalAssetStoreFactory(digitalAssetStoreFactory());
        bean.setType(HarvestResult.PATCH_STAGE_TYPE_INDEXING);
        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    public HarvestBandwidthManager harvestBandwidthManager() {
        HarvestBandwidthManagerImpl bean = new HarvestBandwidthManagerImpl();
        bean.setHarvestAgentManager(harvestAgentManager());
        bean.setTargetInstanceDao(targetInstanceDao());
        bean.setHarvestCoordinatorDao(harvestCoordinatorDao());
        bean.setMinimumBandwidth(minimumBandwidth);
        bean.setMaxBandwidthPercent(maxBandwidthPercent);
        bean.setAuditor(audit());

        return bean;
    }

    @Bean
    public HarvestQaManager harvestQaManager() {
        HarvestQaManagerImpl bean = new HarvestQaManagerImpl();
        bean.setTargetInstanceManager(targetInstanceManager());
        bean.setTargetInstanceDao(targetInstanceDao());
        bean.setAutoQAUrl(autoQAUrl);
        //bean.setQaRecommendationService(qaRecommendationService());
//        bean.setQualityReviewFacade(qualityReviewFacade());
        bean.setEnableQaModule(enableQaModule);
        bean.setAutoPrunedNote(autoPrunedNote);

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public TargetInstanceManager targetInstanceManager() {
        TargetInstanceManagerImpl bean = new TargetInstanceManagerImpl();
        bean.setTargetInstanceDao(targetInstanceDao());
        bean.setAuditor(audit());
        bean.setAnnotationDAO(annotationDao());
        bean.setIndicatorDAO(indicatorDAO());
        bean.setIndicatorCriteriaDAO(indicatorCriteriaDAO());
        bean.setIndicatorReportLineDAO(indicatorReportLineDAO());
        bean.setProfileDAO(profileDao());
        bean.setInTrayManager(inTrayManager());
        bean.setStoreSeedHistory(storeSeedHistory);

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public LockManager lockManager() {
        return new LockManager();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public SiteManagerImpl siteManager() {
        SiteManagerImpl bean = new SiteManagerImpl();
        bean.setSiteDao(siteDao());
        bean.setAnnotationDAO(annotationDao());

        PermMappingSiteListener permMappingSiteListener = new PermMappingSiteListener();
        permMappingSiteListener.setStrategy(permissionMappingStrategy());
        List<SiteManagerListener> permMappingSiteListenerList = new ArrayList<>(
                Arrays.asList(permMappingSiteListener)
        );
        bean.setListeners(permMappingSiteListenerList);

        bean.setIntrayManager(inTrayManager());
        bean.setAuditor(audit());
        bean.setAgencyUserManager(agencyUserManager());

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public TargetManagerImpl targetManager() {
        TargetManagerImpl bean = new TargetManagerImpl();
        bean.setTargetDao(targetDao());
        bean.setSiteDao(siteDao());
        bean.setAnnotationDAO(annotationDao());
        bean.setAuthMgr(authorityManager());
        bean.setTargetInstanceDao(targetInstanceDao());
        bean.setInstanceManager(targetInstanceManager());
        bean.setIntrayManager(inTrayManager());
        bean.setMessageSource(messageSource());
        bean.setAuditor(audit());
        bean.setBusinessObjectFactory(businessObjectFactory());
        bean.setAllowMultiplePrimarySeeds(allowMultiplePrimarySeeds);
        bean.setSubGroupParentTypesList(listsConfig.subGroupParentTypesList());
        bean.setSubGroupTypeName(groupTypesSubgroup);

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public ProfileManager profileManager() {
        ProfileManager bean = new ProfileManager();
        bean.setProfileDao(profileDao());
        bean.setAuthorityManager(authorityManager());
        bean.setAuditor(audit());

        return bean;
    }

    @Bean
    public AuditDAOUtil audit() {
        AuditDAOUtil bean = new AuditDAOUtil();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    public LogonDurationDAOImpl logonDuration() {
        LogonDurationDAOImpl bean = new LogonDurationDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public HarvestAgentFactoryImpl harvestAgentFactory() {
        return new HarvestAgentFactoryImpl();
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    public Environment environmentWCT() {
        EnvironmentImpl bean = new EnvironmentImpl();
        bean.setDaysToSchedule(harvestAgentDaysToSchedule);
        bean.setSchedulesPerBatch(targetInstancesTriggerSchedulesPerBatch);
        bean.setApplicationVersion(projectVersion);
        bean.setHeritrixVersion("Heritrix " + heritrixVersion);

        //Init environment
        EnvironmentFactory.setEnvironment(bean);
        return bean;
    }

//    @Bean
//    public SpringSchedulePatternFactory schedulePatternFactory() {
//        SpringSchedulePatternFactory bean = new SpringSchedulePatternFactory();
//
//        SchedulePattern schedulePattern = new SchedulePattern();
//        schedulePattern.setScheduleType(1);
//        schedulePattern.setDescription("Every Monday at 9:00pm");
//        schedulePattern.setCronPattern("00 00 21 ? * MON *");
//
//        List<SchedulePattern> schedulePatternList = new ArrayList<>(Arrays.asList(schedulePattern));
//
//        schedulePatternList.add(schedulePattern);
//
//        bean.setPatterns(schedulePatternList);
//
//        return bean;
//    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    public HierarchicalPermissionMappingStrategy permissionMappingStrategy() {
        HierarchicalPermissionMappingStrategy bean = new HierarchicalPermissionMappingStrategy();
        bean.setDao(permMappingDao());
        bean.setPermissionDAO(permissionDAO());
        PermissionMappingStrategy.setStrategy(bean);
        return bean;
    }

    @Bean
    public HierPermMappingDAOImpl permMappingDao() {
        HierPermMappingDAOImpl bean = new HierPermMappingDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

//    @Bean
//    @Scope(BeanDefinition.SCOPE_SINGLETON)
//    public JobDetail processScheduleJob() {
//        JobDataMap jobDataMap = new JobDataMap();
//        jobDataMap.put("wctCoordinator", wctCoordinator);
//
//        JobDetail bean = JobBuilder.newJob(ScheduleJob.class)
//                .withIdentity("ProcessSchedule", "ProcessScheduleGroup")
//                .usingJobData(jobDataMap)
//                .storeDurably(true)
//                .build();
//
//        return bean;
//    }


    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    public MethodInvokingJobDetailFactoryBean checkBandwidthTransitionsJob() {
        MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();
        bean.setTargetObject(wctCoordinator);
        bean.setTargetMethod("checkForBandwidthTransition");

        return bean;
    }


    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public AgencyUserManagerImpl agencyUserManager() {
        AgencyUserManagerImpl bean = new AgencyUserManagerImpl();
        bean.setUserRoleDAO(userRoleDAO());
        bean.setRejReasonDAO(rejReasonDAO());
        bean.setIndicatorCriteriaDAO(indicatorCriteriaDAO());
        bean.setFlagDAO(flagDAO());
        bean.setAuditor(audit());
        bean.setAuthorityManager(authorityManager());
        bean.setProfileManager(profileManager());

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public AuthorityManagerImpl authorityManager() {
        return new AuthorityManagerImpl();
    }

    @Bean
    public BusinessObjectFactory businessObjectFactory() {
        BusinessObjectFactory bean = new BusinessObjectFactory();
        bean.setProfileManager(profileManager());

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public AnnotationDAOImpl annotationDao() {
        AnnotationDAOImpl bean = new AnnotationDAOImpl();
        bean.setSessionFactory(sessionFactory);
        bean.setTxTemplate(transactionTemplate);

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public MailServerImpl mailServer() {
        Properties properties = new Properties();
        properties.put("mail.transport.protocol", mailProtocol);
        properties.put("mail.smtp.host", mailServerSmtpHost);
        properties.put("mail.smtp.port", mailSmtpPort);

        MailServerImpl bean = new MailServerImpl(properties);

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public BandwidthChecker bandwidthChecker() {
        BandwidthChecker bean = new BandwidthChecker();
        bean.setWarnThreshold(bandwidthCheckerWarnThreshold);
        bean.setErrorThreshold(bandwidthCheckerErrorThreshold);
        bean.setNotificationSubject("Core");
        bean.setCheckType("Bandwidth");
        bean.setHarvestCoordinator(wctCoordinator);
        bean.setNotifier(checkNotifier());

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public CoreCheckNotifier checkNotifier() {
        CoreCheckNotifier bean = new CoreCheckNotifier();
        bean.setInTrayManager(inTrayManager());

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public CheckProcessor checkProcessor() {
        CheckProcessor bean = new CheckProcessor();

        List<Checker> checksList = new ArrayList<>();
        checksList.add(bandwidthChecker());

        bean.setChecks(checksList);

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    public MethodInvokingJobDetailFactoryBean checkProcessorJob() {
        MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();
        bean.setTargetObject(checkProcessor());
        bean.setTargetMethod("check");

        return bean;
    }


    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    public MethodInvokingJobDetailFactoryBean purgeDigitalAssetsJob() {
        MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();
        bean.setTargetObject(wctCoordinator);
        bean.setTargetMethod("purgeDigitalAssets");

        return bean;
    }


    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    public MethodInvokingJobDetailFactoryBean purgeAbortedTargetInstancesJob() {
        MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();
        bean.setTargetObject(wctCoordinator);
        bean.setTargetMethod("purgeAbortedTargetInstances");

        return bean;
    }


    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public InTrayManagerImpl inTrayManager() {
        InTrayManagerImpl bean = new InTrayManagerImpl();
        bean.setInTrayDAO(inTrayDao());
        bean.setUserRoleDAO(userRoleDAO());
        bean.setAgencyUserManager(agencyUserManager());
        bean.setMailServer(mailServer());
        bean.setAudit(audit());
        bean.setSender(inTrayManagerSender);
        bean.setMessageSource(messageSource());
        bean.setWctBaseUrl(inTrayManagerWctBaseUrl);

        return bean;
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public PermissionTemplateManagerImpl permissionTemplateManager() {
        PermissionTemplateManagerImpl bean = new PermissionTemplateManagerImpl();
        bean.setPermissionTemplateDAO(permissionTemplateDao());
        bean.setAuthorityManager(authorityManager());

        return bean;
    }

//    @Bean
//    @Scope(BeanDefinition.SCOPE_SINGLETON)
//    public MethodInvokingJobDetailFactoryBean groupExpiryJob() {
//        MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();
//        bean.setTargetObject(targetManager());
//        bean.setTargetMethod("endDateGroups");
//
//        return bean;
//    }


//    @Bean
//    @Scope(BeanDefinition.SCOPE_SINGLETON)
//    public MethodInvokingJobDetailFactoryBean createNewTargetInstancesJob() {
//        MethodInvokingJobDetailFactoryBean bean = new MethodInvokingJobDetailFactoryBean();
//        bean.setTargetObject(targetManager());
//        bean.setTargetMethod("processSchedulesJob");
//
//        return bean;
//    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public ArchiveAdapterImpl archiveAdapter() {
        ArchiveAdapterImpl bean = new ArchiveAdapterImpl();
        bean.setDigitalAssetStore(digitalAssetStore());
        bean.setTargetInstanceManager(targetInstanceManager());
        bean.setTargetManager(targetManager());
        bean.setAccessStatusMap(listsConfig.accessStatusMap());
        bean.setTargetReferenceMandatory(archiveAdapterTargetReferenceMandatory);

        return bean;
    }

    @Bean
    public DateUtils dateUtils() {
        return DateUtils.get();
    }


    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public PolitenessOptions politePolitenessOptions() {
        // Delay Factor, Min Delay milliseconds, Max Delay milliseconds,
        // Respect crawl delay up to seconds, Max per host bandwidth usage kb/sec
        return new PolitenessOptions(10.0, 9000L, 90000L, 180L, 400L);
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public PolitenessOptions mediumPolitenessOptions() {
        // Delay Factor, Min Delay milliseconds, Max Delay milliseconds,
        // Respect crawl delay up to seconds, Max per host bandwidth usage kb/sec
        return new PolitenessOptions(5.0, 3000L, 30000L, 30L, 800L);
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public PolitenessOptions aggressivePolitenessOptions() {
        // Delay Factor, Min Delay milliseconds, Max Delay milliseconds,
        // Respect crawl delay up to seconds, Max per host bandwidth usage kb/sec
        return new PolitenessOptions(1.0, 1000L, 10000L, 2L, 2000L);
    }

    @Bean
    @Scope(BeanDefinition.SCOPE_SINGLETON)
    @Lazy(false)
    public VisualizationImportedFileDAO getVisualizationImportedFileDAO() {
        VisualizationImportedFileDAOImpl visualizationImportedFileDAO = new VisualizationImportedFileDAOImpl();
        visualizationImportedFileDAO.setSessionFactory(sessionFactory);
        return visualizationImportedFileDAO;
    }

    @Bean
    public BandwidthCalculator bandwidthCalculator() {
        BandwidthCalculator bean = new BandwidthCalculatorImpl();
        return bean;
    }
}

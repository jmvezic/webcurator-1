package org.webcurator.ui.target.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.webcurator.common.ui.Constants;
import org.webcurator.core.harvester.coordinator.HarvestCoordinator;
import org.webcurator.core.harvester.coordinator.PatchingHarvestLogManager;
import org.webcurator.core.visualization.modification.PruneAndImportProcessor;
import org.webcurator.core.visualization.modification.metadata.PruneAndImportCommandApply;
import org.webcurator.core.visualization.modification.metadata.PruneAndImportCommandProgress;
import org.webcurator.core.visualization.modification.metadata.PruneAndImportCommandRowMetadata;
import org.webcurator.domain.TargetInstanceDAO;
import org.webcurator.domain.model.core.HarvestResult;
import org.webcurator.domain.model.core.LogFilePropertiesDTO;
import org.webcurator.domain.model.core.TargetInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * The PatchingViewHarvestResultController is responsible for displaying the overall, progress, logs, to be pruned urls and to be imported urls.
 *
 * @author frank lee
 */
@Controller
@Scope(BeanDefinition.SCOPE_SINGLETON)
@RequestMapping(path = "/curator/target/patching-view-hr.html")
public class PatchingViewHarvestResultController {
    @Autowired
    private TargetInstanceDAO targetInstanceDAO;

    @Autowired
    private HarvestCoordinator harvestCoordinator;

    @Autowired
    @Qualifier(HarvestResult.PATCH_STAGE_TYPE_NORMAL)
    private PatchingHarvestLogManager patchingHarvestLogManager;

    @Autowired
    @Qualifier(HarvestResult.PATCH_STAGE_TYPE_MODIFYING)
    private PatchingHarvestLogManager patchingHarvestLogManagerModification;

    @Autowired
    @Qualifier(HarvestResult.PATCH_STAGE_TYPE_INDEXING)
    private PatchingHarvestLogManager patchingHarvestLogManagerIndex;

    @GetMapping
    public ModelAndView getHandle(@RequestParam("targetInstanceOid") long targetInstanceId, @RequestParam("harvestResultId") long harvestResultId, @RequestParam("harvestNumber") int harvestResultNumber) throws Exception {
        ModelAndView mav = new ModelAndView("patching-view-hr");
        TargetInstance ti = targetInstanceDAO.load(targetInstanceId);
        if (ti == null) {
//            bindingResult.reject("Could not find Target Instance with ID: " + targetInstanceId);
//            mav.addObject(Constants.GBL_ERRORS, bindingResult);
            return mav;
        }

        HarvestResult hr = ti.getHarvestResult(harvestResultNumber);
        if (hr == null) {
//            bindingResult.reject("Could not find Harvest Number with Number: " + harvestResultNumber);
//            mav.addObject(Constants.GBL_ERRORS, bindingResult);
            return mav;
        }

        PruneAndImportCommandProgress progress = new PruneAndImportCommandProgress();
        if (hr.getState() >= 50) {
            progress.setPercentageSchedule(100);
        }
        if (hr.getState() >= HarvestResult.STATE_PATCH_HARVEST_FINISHED) {
            progress.setPercentageHarvest(100);
        } else if (hr.getState() >= HarvestResult.STATE_PATCH_HARVEST_RUNNING) {
            progress.setPercentageHarvest(50);
        }

        if (hr.getState() >= HarvestResult.STATE_PATCH_MOD_FINISHED) {
            progress.setPercentageModify(100);
        } else if (hr.getState() >= HarvestResult.STATE_PATCH_MOD_RUNNING) {
            progress.setPercentageModify(50);
        }

        if (hr.getState() == HarvestResult.STATE_INDEXING) {
            progress.setPercentageIndex(50);
        } else if (hr.getState() < HarvestResult.STATE_PATCH_SCHEDULED) {
            progress.setPercentageHarvest(100);
        }

        PruneAndImportCommandApply pruneAndImportCommandApply = harvestCoordinator.getPruneAndImportCommandApply(ti);
        List<PruneAndImportCommandRowMetadata> listToBePruned = new ArrayList<>();
        List<PruneAndImportCommandRowMetadata> listToBeImportedByFile = new ArrayList<>();
        List<PruneAndImportCommandRowMetadata> listToBeImportedByURL = new ArrayList<>();

        pruneAndImportCommandApply.getDataset().forEach(e -> {
            if (e.getOption().equalsIgnoreCase("prune")) {
                listToBePruned.add(e);
            } else if (e.getOption().equalsIgnoreCase("file")) {
                listToBeImportedByFile.add(e);
            } else if (e.getOption().equalsIgnoreCase("url")) {
                listToBeImportedByURL.add(e);
            }
        });

        List<LogFilePropertiesDTO> logsCrawling = patchingHarvestLogManager.listLogFileAttributes(ti, hr);
        List<LogFilePropertiesDTO> logsModifying = patchingHarvestLogManagerModification.listLogFileAttributes(ti, hr);
        List<LogFilePropertiesDTO> logsIndexing = patchingHarvestLogManagerIndex.listLogFileAttributes(ti, hr);

        mav.addObject("ti", ti);
        mav.addObject("hr", hr);
        mav.addObject("progress", progress);
        mav.addObject("listToBePruned", listToBePruned);
        mav.addObject("listToBeImportedByFile", listToBeImportedByFile);
        mav.addObject("listToBeImportedByURL", listToBeImportedByURL);
        mav.addObject("logsCrawling", logsCrawling);
        mav.addObject("logsModifying", logsModifying);
        mav.addObject("logsIndexing", logsIndexing);
        return mav;
    }
}
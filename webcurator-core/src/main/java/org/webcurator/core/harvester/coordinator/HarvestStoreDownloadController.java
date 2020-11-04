package org.webcurator.core.harvester.coordinator;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.webcurator.core.exceptions.DigitalAssetStoreException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
public class HarvestStoreDownloadController {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @RequestMapping(path = HarvestCoordinatorPaths.DOWNLOAD, method = {RequestMethod.POST, RequestMethod.GET}, produces = "application/octet-stream")
    public void externalDownload(@RequestParam("filePath") String filePath,
                                 HttpServletRequest req,
                                 HttpServletResponse rsp) throws DigitalAssetStoreException, IOException {
        log.debug("Get file download request, filePath: {}", filePath);

        File file = new File(filePath);
        IOUtils.copy(Files.newInputStream(file.toPath()), rsp.getOutputStream());
    }
}
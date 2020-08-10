package org.kgrid.shelf.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.net.URI;

@Service
public class ImportService {
    @Autowired
    CompoundDigitalObjectStore cdoStore;
    @Autowired
    ApplicationContext applicationContext;

    public URI importZip(URI zipUri) throws IOException {
        Resource zipResource = applicationContext.getResource(zipUri.toString());

        ZipUtil.unpack(zipResource.getInputStream(), new File(cdoStore.getAbsoluteLocation()));
        return null;
    }

}

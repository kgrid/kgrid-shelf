package org.kgrid.shelf.repository;

import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.kgrid.shelf.domain.ArkId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.NameMapper;
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
    @Autowired
    KnowledgeObjectRepository repo;
    @Autowired
    ZipImportService zipImportService;
    private ObjectMapper mapper = new ObjectMapper();

    public URI importZip(URI zipUri) throws IOException {
        Resource zipResource = applicationContext.getResource(zipUri.toString());

//        ArkId ark = repo.importZip(zipResource.getInputStream());

        final InputStream zipStream = zipResource.getInputStream();
        ArkId ark = zipImportService.importKO(zipStream, cdoStore);
//        repo.refreshObjectMap();

//        ZipUtil.unpack(zipResource.getInputStream(), new File(cdoStore.getAbsoluteLocation()));

        return URI.create(ark.getSlashArkVersion());
    }

    public JsonNode getMetadata(InputStream zipStream) throws IOException {

        byte[] metadata = ZipUtil.unpackEntry(zipStream, "metadata.json");

        return mapper.readTree(metadata);
    }

}

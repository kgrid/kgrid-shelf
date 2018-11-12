package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.kgrid.shelf.domain.CompoundDigitalObject;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ZipUtil;

public class ZipFileProcessor {

  private final org.slf4j.Logger log = LoggerFactory.getLogger(ZipFileProcessor.class);

  /**
   * Create KO object, must add Knowledge Object files, Knowledge Object properties and
   * Knowledge Object Implementation properties
   *
   * @param zipFileStream
   * @param cdoStore
   */
  public void createCompoundDigitalObject(String identifier, InputStream zipFileStream,CompoundDigitalObjectStore cdoStore) {

    CompoundDigitalObject cdo = new CompoundDigitalObject(identifier);
    ZipUtil.iterate(zipFileStream, (inputStream, zipEntry ) -> {

      if (zipEntry.getName().endsWith("metadata.json")) {
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
        cdo.setMetadata(new ObjectMapper().readTree(writer.toString()));
      } else if (!zipEntry.isDirectory() && !zipEntry.getName().endsWith("metadata.json")) {
        cdo.getBinaryResources().put(zipEntry.getName(), IOUtils.toByteArray(inputStream));
      }

    });

    cdoStore.save(cdo);

  }



}

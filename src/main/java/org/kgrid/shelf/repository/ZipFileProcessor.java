package org.kgrid.shelf.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.kgrid.shelf.domain.CompoundDigitalObject;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.ByteSource;
import org.zeroturnaround.zip.ZipEntrySource;
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
  public void importCompoundDigitalObject(String identifier, InputStream zipFileStream,CompoundDigitalObjectStore cdoStore) {

    CompoundDigitalObject cdo = new CompoundDigitalObject(identifier);
    ZipUtil.iterate(zipFileStream, (inputStream, zipEntry ) -> {

      if (zipEntry.getName().endsWith("metadata.json")) {
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, StandardCharsets.UTF_8);
        cdo.getContainers().put(zipEntry.getName().substring(0,
            zipEntry.getName().indexOf("metadata.json")-1),new ObjectMapper().readTree(writer.toString()));
      } else if (!zipEntry.isDirectory() && !zipEntry.getName().endsWith("metadata.json")) {
        cdo.getBinaryResources().put(zipEntry.getName(), IOUtils.toByteArray(inputStream));
      }

    });

    cdoStore.save(cdo);

  }

  /**
   *
   * @param identifier
   * @param cdoStore
   * @return
   * @throws IOException
   */
  public OutputStream exportCompoundDigitalObject(String identifier,
      CompoundDigitalObjectStore cdoStore)  throws IOException {

    CompoundDigitalObject cdo = cdoStore.find(identifier);
    List<ZipEntrySource> entries = new ArrayList();
    cdo.getContainers().forEach( (path, jsonNode) ->{
          try {
            entries.add(new ByteSource( Paths.get(
                path, KnowledgeObject.METADATA_FILENAME).toString(), jsonNode.binaryValue()));
          } catch (IOException e) {
            e.printStackTrace();
          }
    });
    entries.add( new ByteSource("metadata.json", cdoStore.getMetadata(identifier).toString().getBytes()) );
    cdo.getBinaryResources().forEach( (path, bytes) ->{
      entries.add(new ByteSource(path, bytes));
    });

    OutputStream outputStream = new ByteArrayOutputStream();

    ZipUtil.pack(entries.toArray(new ZipEntrySource[entries.size()]),outputStream);

    return outputStream;

  }



}

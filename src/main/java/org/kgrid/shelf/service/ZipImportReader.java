package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.zeroturnaround.zip.ZipUtil;

public class ZipImportReader {

  private ObjectMapper jsonMapper = new ObjectMapper();
  private ObjectMapper yamlMapper = new YAMLMapper();
  private File koBase;

  public ZipImportReader(Resource zipResource) throws IOException {
    koBase = createKoBase(zipResource);
  }

  File createKoBase(Resource zipResource) throws IOException {
    String koName = StringUtils.removeEnd(zipResource.getFilename(), ".zip");
    File parentDir = unzipToTemp(zipResource.getInputStream());
    FileUtils.forceDeleteOnExit(parentDir);
    File koBase = new File(parentDir, koName);
    return koBase;
  }


  private File unzipToTemp(InputStream inputStream) {
    try {
      File temp = Files.createTempDirectory("ko").toFile();
      ZipUtil.unpack(inputStream, temp);
      return temp;
    } catch (IOException e) {
      throw new ImportExportException("Cannot create temporary directory to unpack zip", e);
    }
  }

  public JsonNode getMetadata(URI specName) throws IOException {

    final File specFile = new File(koBase, specName.toString());

    JsonNode jsonNode;
    if (specName.getPath().endsWith(".json")) {
      jsonNode = jsonMapper.readTree(specFile);
    } else {
      jsonNode = yamlMapper.readTree(specFile);
    }

    return jsonNode;
  }

  public byte[] getBinary(URI artifact) throws IOException {
    File artifactFile = new File(koBase, artifact.toString());
    final byte[] data = Files.readAllBytes(artifactFile.toPath());
    return data;
  }

  public File getKoBase() {
    return koBase;
  }
}

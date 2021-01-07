package org.kgrid.shelf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.zeroturnaround.zip.ZipUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;

public class ZipImportReader {

  private ObjectMapper jsonMapper = new ObjectMapper();
  private ObjectMapper yamlMapper = new YAMLMapper();
  private File koBase;

  public ZipImportReader(Resource zipResource) throws IOException {
    koBase = createKoBase(zipResource);
  }

  private File createKoBase(Resource zipResource) throws IOException {
    String filename = zipResource.getFilename();
    String koName;
    if (filename != null) {
      koName = StringUtils.removeEnd(filename, ".zip");
    } else {
      koName =
          StringUtils.removeEnd(
              StringUtils.removeStart(zipResource.getDescription(), "Byte array resource ["),
              ".zip]");
    }
    File parentDir = unzipToTemp(zipResource.getInputStream());
    FileUtils.forceDeleteOnExit(parentDir);
    return new File(parentDir, koName);
  }

  private File unzipToTemp(InputStream inputStream) {
    try {
      File temp = Files.createTempDirectory("ko").toFile();
      ZipUtil.unpack(inputStream, temp);
      return temp;
    } catch (Exception e) {
      throw new ImportExportException("Cannot unpack zip to temporary directory", e);
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

  public InputStream getFileStream(URI artifact) throws IOException {
    File artifactFile = new File(koBase, artifact.toString());
    final InputStream data = Files.newInputStream(artifactFile.toPath());
    return data;
  }

  public File getKoBase() {
    return koBase;
  }
}

package org.kgrid.shelf.repository;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.service.ExportService;
import org.kgrid.shelf.service.ImportService;
import org.zeroturnaround.zip.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static org.junit.Assert.assertEquals;

@Category(FedoraIntegrationTest.class)
public class FedoraZipExportServiceTest {

  @ClassRule public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  ImportService service = new ImportService();
  FedoraCDOStore compoundDigitalObjectStore =
      new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");

  @Before
  public void setUp() throws Exception {

    URI helloWorldLoc =
        URI.create("file:src/test/resources/fixtures/import-export/hello-world.zip");
    service.importZip(helloWorldLoc);
  }

  @Test
  public void exportCompoundDigitalObject() throws IOException {

    ExportService exportService = new ExportService();

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    exportService.zipKnowledgeObject(new ArkId("hello", "world"), outputStream);

    writeZip(outputStream);

    List<Path> filesPaths;
    filesPaths =
        Files.walk(
                Paths.get(temporaryFolder.getRoot().toPath().toString(), "export", "hello-world"),
                3,
                FOLLOW_LINKS)
            .filter(Files::isRegularFile)
            .map(Path::toAbsolutePath)
            .collect(Collectors.toList());

    filesPaths.forEach(
        file -> {
          System.out.println(file.toString());
        });

    assertEquals(10, filesPaths.size());
  }

  protected void writeZip(ByteArrayOutputStream zipOutputStream) {

    try {

      Path path = Paths.get(temporaryFolder.getRoot().getAbsolutePath(), "export.zip");
      OutputStream outputStream = new FileOutputStream(path.toString());
      try {
        zipOutputStream.writeTo(outputStream);
      } finally {
        outputStream.close();

        ZipUtil.unpack(
            Paths.get(temporaryFolder.getRoot().getAbsolutePath(), "export.zip").toFile(),
            Paths.get(temporaryFolder.getRoot().getAbsolutePath(), "export").toFile());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @After
  public void teardown() {
    compoundDigitalObjectStore.delete(URI.create("hello-world/"));
  }
}

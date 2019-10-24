package org.kgrid.shelf.repository;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.ArkId;
import org.zeroturnaround.zip.ZipUtil;

public class ZipExportServiceTest {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();
  private FilesystemCDOStore compoundDigitalObjectStore;

  @Before
  public void setUp() throws Exception {

    String connectionURL = "filesystem:" + temporaryFolder.getRoot().toURI();
    compoundDigitalObjectStore = new FilesystemCDOStore(connectionURL);
    Path shelf = Paths.get(compoundDigitalObjectStore.getAbsoluteLocation(""));

    ZipUtil.unpack(Paths.get("src/test/resources/fixtures/import-export/hello-world-v2.zip").toFile(), temporaryFolder.getRoot() );
    ZipUtil.unpack(Paths.get("src/test/resources/fixtures/import-export/kozip.zip").toFile(), temporaryFolder.getRoot() );

    temporaryFolder.newFolder("export");

  }

  @Test
  public void exportKnowledgeObject() throws IOException {

    ZipExportService zipExportService = new ZipExportService();

    ByteArrayOutputStream outputStream = zipExportService.exportObject(
        new ArkId("hello", "world", "v2"), "hello-world-v2", compoundDigitalObjectStore);

    writeZip(outputStream);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(),"export","hello-world-v2"),  3, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    System.out.println("exportKnowledgeObject");

    filesPaths.forEach(file ->{
      System.out.println(file.toAbsolutePath().toString());
    });

    assertEquals(3,filesPaths.size());

  }

  @Test
  public void exportKnowledgeObjectWackyFolderName() throws IOException {

    ZipExportService zipExportService = new ZipExportService();

    ByteArrayOutputStream outputStream = zipExportService.exportObject(
        new ArkId("hello", "world", "v3"), "mycoolko", compoundDigitalObjectStore);

    writeZip(outputStream);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(),"export","hello-world-v3"),  3, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file ->{
      System.out.println(file.toAbsolutePath().toString());
    });

    assertEquals(3,filesPaths.size());

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
            Paths.get(temporaryFolder.getRoot().getAbsolutePath(),"export").toFile());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  @After
  public void cleanUp() throws IOException {

    FileUtils.deleteDirectory( Paths.get(temporaryFolder.getRoot().getAbsolutePath(),"export").toFile());

  }
}

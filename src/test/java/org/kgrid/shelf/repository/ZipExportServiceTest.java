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

    Path helloWorld = Paths.get("src/test/resources/shelf/hello-world");
    Path emptyWorld = Paths.get("src/test/resources/shelf/empty-world");
    FileUtils.copyDirectory(helloWorld.toFile(),
        Paths.get(shelf.toString(),"hello-world").toFile());
    FileUtils.copyDirectory(emptyWorld.toFile(),
        Paths.get(shelf.toString(),"empty-world").toFile());

    temporaryFolder.newFolder("export");

  }


  @Test
  public void exportCompoundDigitalObject() throws IOException {

    ZipExportService zipExportService = new ZipExportService();

    ByteArrayOutputStream outputStream = zipExportService.exportCompoundDigitalObject(
        new ArkId("hello", "world"), compoundDigitalObjectStore);

    writeZip(outputStream);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(),"export","hello-world"),  2, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file ->{
      System.out.println(file.toAbsolutePath().toString());
    });

    assertEquals(9,filesPaths.size());

  }

  @Test
  public void exportNoImplentationCompoundDigitalObject() throws IOException {

    ZipExportService zipExportService = new ZipExportService();

    ByteArrayOutputStream outputStream = zipExportService.exportCompoundDigitalObject(
        new ArkId("empty", "world"), compoundDigitalObjectStore);

    writeZip(outputStream);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(),"export","empty-world"),  2, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file ->{
      System.out.println(file.toAbsolutePath().toString());
    });

    assertEquals(1,filesPaths.size());

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
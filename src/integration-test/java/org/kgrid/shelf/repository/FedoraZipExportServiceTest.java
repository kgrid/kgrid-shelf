package org.kgrid.shelf.repository;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.kgrid.shelf.domain.ArkId;
import org.zeroturnaround.zip.ZipUtil;
@Category(FedoraIntegrationTest.class)
public class FedoraZipExportServiceTest {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  ZipImportService service = new ZipImportService();
  FedoraCDOStore compoundDigitalObjectStore =  new FedoraCDOStore("fedora:http://localhost:8080/fcrepo/rest/");

  @Before
  public void setUp() throws Exception {

    InputStream zipStream = FedoraZipImportServiceTest.class.getResourceAsStream("/fixtures/hello-world-jsonld.zip");

    service.importObject(new ArkId("hello", "world"), zipStream, compoundDigitalObjectStore);

  }

  @Test
  public void exportCompoundDigitalObject() throws IOException {

    ZipExportService zipExportService = new ZipExportService();

    ByteArrayOutputStream outputStream = zipExportService.exportObject(
        new ArkId("hello", "world"), compoundDigitalObjectStore, true);

    writeZip(outputStream);

    List<Path> filesPaths;
    filesPaths = Files.walk(Paths.get(
        temporaryFolder.getRoot().toPath().toString(),"export","hello-world"),  2, FOLLOW_LINKS)
        .filter(Files::isRegularFile)
        .map(Path::toAbsolutePath)
        .collect(Collectors.toList());

    filesPaths.forEach(file ->{
      System.out.println(file.toString());

    });

    assertEquals(9,filesPaths.size());

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
  public void teardown(){
    compoundDigitalObjectStore.delete("hello-world");
  }

}
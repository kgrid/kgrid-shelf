package org.kgrid.shelf.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.ext.com.google.common.io.Files;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.repository.CompoundDigitalObjectStore;
import org.kgrid.shelf.repository.ZipImportExportTestHelper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ImportServiceTest {

    @Spy
    ApplicationContext applicationContext = new ClassPathXmlApplicationContext();
    @Mock
    CompoundDigitalObjectStore cdoStore;

    @InjectMocks
    ImportService importService;

    URI resourceUri;

    @Test
    public void importZip_givenUri_canExtractAndSaveArtifacts() {
        resourceUri = URI.create("file:src/test/resources/fixtures/import-export/mycoolko.zip");

        importService.importZip(resourceUri);

        verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/metadata.json"));
        verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/service.yaml"));
        verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/deployment.yaml"));
        verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/dist/main.js"));
    }

    @Test
    public void importZip_givenUri_canExtractAndSaveMultipleArtifacts() {
        resourceUri = URI.create("file:src/test/resources/fixtures/import-export/artifact-array.zip");

        importService.importZip(resourceUri);

        verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/metadata.json"));
        verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/service.yaml"));
        verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/deployment.yaml"));
        verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/dist/main.js"));
        verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/src/index.js"));
    }

    @Test
    public void importZip_givenUri_noDeploymentLogsThrowsAndSkips() {
        resourceUri =
                URI.create("file:src/test/resources/fixtures/import-export/missing-deployment.zip");

        Throwable cause =
                assertThrows(
                        ImportExportException.class,
                        () -> importService.importZip(resourceUri))
                        .getCause();

        assertEquals(FileNotFoundException.class, cause.getClass());
    }

    @Test
    public void importZip_givenUri_badMetadataLogsThrowsAndSkips() {
        resourceUri = URI.create("file:src/test/resources/fixtures/import-export/bad-kometadata.zip");

        Throwable cause =
                assertThrows(
                        ImportExportException.class,
                        () -> importService.importZip(resourceUri))
                        .getCause();

        assertEquals(FileNotFoundException.class, cause.getClass());
    }

    @Test
    public void importZip_givenResource_noMetadataLogsThrowsAndSkips() throws IOException {

        ByteArrayInputStream funnyZipStream =
                ZipImportExportTestHelper.packZipForImport(
                        null, ZipImportExportTestHelper.DEPLOYMENT_BYTES, null, null);

        final File test = Files.createTempDir();
        File zipTest = new File(test, "naan-name-version.zip");
        Files.write(funnyZipStream.readAllBytes(), zipTest);
        FileUtils.forceDeleteOnExit(test);

        final FileSystemResource zipResource = new FileSystemResource(zipTest);

        Throwable cause =
                assertThrows(
                        ImportExportException.class,
                        () -> importService.importZip(zipResource))
                        .getCause();

        assertEquals(FileNotFoundException.class, cause.getClass());
    }

    @Test
    public void importZip_givenUri_canLoadHelloWorld() {
        resourceUri = URI.create("file:src/test/resources/static/hello-world-v1.3.zip");

        importService.importZip(resourceUri);

        verify(cdoStore, times(4)).saveBinary(any(), any());
        verify(cdoStore).saveBinary(isNotNull(), eq("hello-world/metadata.json"));
    }

    @Test
    public void importZip_givenZipFile_throwsImportExportException_WhenGetBytesFails() throws IOException {
        MultipartFile mockFile = Mockito.mock(MultipartFile.class);
        when(mockFile.getBytes()).thenThrow(new IOException("boom"));
        Throwable cause =
                assertThrows(
                        ImportExportException.class,
                        () -> importService.importZip(mockFile))
                        .getCause();

        assertEquals(IOException.class, cause.getClass());
    }
}

package org.kgrid.shelf.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.shelf.TestHelper;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObjectWrapper;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExportServiceTest {

  @Mock KnowledgeObjectRepository knowledgeObjectRepository;

  ArkId arkId;
  KnowledgeObjectWrapper kow;
  URI resourceUri;

  @InjectMocks ExportService exportService;
  @Spy ApplicationContext applicationContext = new ClassPathXmlApplicationContext();

  @Before
  public void setUp() throws IOException {
    arkId = TestHelper.ARK_ID;

    kow = new KnowledgeObjectWrapper(TestHelper.generateMetadata());
    when(knowledgeObjectRepository.getKoRepoLocation()).thenReturn(URI.create("file:/root/"));
    when(knowledgeObjectRepository.getObjectLocation(arkId))
        .thenReturn(URI.create(TestHelper.KO_PATH));
    when(knowledgeObjectRepository.getKow(arkId)).thenReturn(kow);
  }

  @Test
  public void testZipsCorrectData() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (MockedStatic<ZipUtil> mockZip = Mockito.mockStatic(ZipUtil.class)) {
      exportService.zipKnowledgeObject(arkId, baos);
      mockZip.verify(
          times(1), () -> ZipUtil.pack(any(ZipEntrySource[].class), any(OutputStream.class)));
    }
  }

  @Test
  public void testZipsMissingKO() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (MockedStatic<ZipUtil> mockZip = Mockito.mockStatic(ZipUtil.class)) {
      exportService.zipKnowledgeObject(arkId, baos);
      mockZip.verify(
          times(1), () -> ZipUtil.pack(any(ZipEntrySource[].class), any(OutputStream.class)));
    }
  }
}

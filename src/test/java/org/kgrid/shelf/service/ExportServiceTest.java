package org.kgrid.shelf.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kgrid.shelf.TestHelper;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObjectWrapper;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zeroturnaround.zip.ZipEntrySource;
import org.zeroturnaround.zip.ZipUtil;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ExportServiceTest {

  @Mock KnowledgeObjectRepository knowledgeObjectRepository;

  ArkId arkId;
  KnowledgeObjectWrapper kow;

  @InjectMocks ExportService exportService;

  @Test
  @DisplayName("Zips data correctly")
  public void testZipsCorrectData() {
    arkId = TestHelper.ARK_ID;
    kow = new KnowledgeObjectWrapper(TestHelper.generateMetadata());

    when(knowledgeObjectRepository.getKoRepoLocation()).thenReturn(URI.create("file:/root/"));
    when(knowledgeObjectRepository.getObjectLocation(arkId))
        .thenReturn(URI.create(TestHelper.KO_PATH));
    when(knowledgeObjectRepository.getKow(arkId)).thenReturn(kow);

    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    try (MockedStatic<ZipUtil> mockZip = Mockito.mockStatic(ZipUtil.class)) {
      exportService.zipKnowledgeObject(arkId, outStream);
      mockZip.verify(() -> ZipUtil.pack(any(ZipEntrySource[].class), any(OutputStream.class)));
    }
  }
}

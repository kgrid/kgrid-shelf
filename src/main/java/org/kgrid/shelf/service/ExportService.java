package org.kgrid.shelf.service;

import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.KnowledgeObjectWrapper;
import org.kgrid.shelf.repository.KnowledgeObjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zeroturnaround.zip.FileSource;
import org.zeroturnaround.zip.ZipEntrySource;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.zeroturnaround.zip.ZipUtil.pack;

@Service
public class ExportService {

  @Autowired KnowledgeObjectRepository knowledgeObjectRepository;

  public void zipKnowledgeObject(ArkId arkId, OutputStream outputStream)
      throws ImportExportException {

    KnowledgeObjectWrapper kow = knowledgeObjectRepository.getKow(arkId);

    URI koBase =
        knowledgeObjectRepository
            .getKoRepoLocation()
            .resolve(knowledgeObjectRepository.getObjectLocation(arkId) + "/");

    List<ZipEntrySource> koFiles = new ArrayList<>();
    for (URI uri : kow.getArtifactLocations()) {
      URI location = koBase.resolve(uri);
      koFiles.add(
          new FileSource(arkId.getFullDashArk() + "/" + uri.toString(), new File(location)));
    }
    pack(koFiles.toArray(new ZipEntrySource[0]), outputStream);
  }
}

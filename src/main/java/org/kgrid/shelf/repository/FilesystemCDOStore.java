package org.kgrid.shelf.repository;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.domain.ArkId;
import org.kgrid.shelf.domain.CompoundDigitalObject;
import org.kgrid.shelf.domain.KOIOKnowledgeObject;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

@Qualifier("filesystem")
public class FilesystemCDOStore implements CompoundDigitalObjectStore {

  private URI localStorageURI;

  private final Logger log = LoggerFactory.getLogger(FilesystemCDOStore.class);

  public FilesystemCDOStore(

      @Value("${kgrid.shelf.cdostore.url:filesystem:file://shelf}") String connectionURI) {
    URI uri = URI.create(connectionURI.substring(connectionURI.indexOf(':') + 1));
    if (uri.getHost() == null) {
      this.localStorageURI = uri;
    } else {
      this.localStorageURI = Paths.get(uri.getHost(), uri.getPath()).toUri();
    }
  }

  @Override
  public List<String> getChildren(String filePath) {
    Path path = Paths.get(localStorageURI);
    if (filePath != null) {
      path = path.resolve(filePath);
    }
    List<String> children = new ArrayList<>();
    try {
      children = Files.walk(path,  1, FOLLOW_LINKS)
          .filter(Files::isDirectory)
          .map(Object::toString)
          .collect(Collectors.toList());
      children.remove(0); // Remove the parent directory
    } catch (IOException ioEx) {
      log.error("Cannot read children at location " + path + " " + ioEx);
    }
    return children;
  }

  // TODO: this method breaks on windows, says directory does not exist. Fix it.
  @Override
  public String getAbsoluteLocation(String relativeFilePath) {
    Path shelf = Paths.get(localStorageURI);
    if (!shelf.toFile().exists()) {
      throw new IllegalStateException(
          "Filesystem shelf location '" + shelf.toAbsolutePath() + "' is not a valid directory."
              + " Make sure the property kgrid.shelf.cdostore.url is set correctly.");
    }
    if (relativeFilePath == null) {
      return shelf.toString();
    }
    return shelf.resolve(relativeFilePath).toString();
  }

  @Override
  public boolean isMetadata(String relativePath) {
    return relativePath.endsWith(KnowledgeObject.METADATA_FILENAME);
  }

  @Override
  public ObjectNode getMetadata(String relativePath) {
    Path shelf = Paths.get(localStorageURI);
    File metadataFile = shelf.resolve(relativePath).toFile();
    if (metadataFile.isDirectory()) {
      metadataFile = shelf.resolve(relativePath).resolve(KnowledgeObject.METADATA_FILENAME)
          .toFile();
    }

    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    JsonNode koMetadata;
    try {
      koMetadata = mapper.readTree(metadataFile);
      if (koMetadata.isArray()) {
        // Parent object in json-ld is array, get first element.
        koMetadata = koMetadata.get(0);
      }
      ArrayNode children = ((ObjectNode) koMetadata).putArray("children");
      getChildren(Paths.get(metadataFile.getAbsolutePath()).getParent().toString()).forEach(filename -> {
        Path path = Paths.get(filename);
        if (path.toFile().isDirectory() && path.toFile().listFiles().length > 0) {
          for (File child : path.toFile().listFiles()) {
            if (!child.getName().startsWith(".")) {
              children.add(child.getAbsolutePath().substring(shelf.toAbsolutePath().toString().length()));
            }
          }
        }
      });

      return ((ObjectNode) koMetadata);
    } catch (IOException ioEx) {
      throw new IllegalArgumentException(
          "Cannot read metadata file at path " + shelf.resolve(metadataFile.getAbsolutePath()), ioEx);
    }
  }


  @Override
  public byte[] getBinary(String relativeFilePath) {
    Path shelf = Paths.get(localStorageURI);
    byte[] bytes = null;
    try {
      bytes = Files.readAllBytes(shelf.resolve(relativeFilePath));
    } catch (IOException ioEx) {
      log.error("Cannot read file at " + relativeFilePath + " " + ioEx);
    }
    return bytes;
  }

  @Override
  public void saveMetadata(String relativePath, JsonNode metadata) {
    File metadataFile = new File(localStorageURI.getPath(), relativePath.toString());
    try {
      ObjectWriter writer = new ObjectMapper().writer().with(SerializationFeature.INDENT_OUTPUT);
      writer.writeValue(metadataFile, metadata);
    } catch (IOException ioEx) {
      log.error("Could not write to file at " + relativePath + " " + ioEx);
    }
  }

  @Override
  public void saveBinary(String relativePath, byte[] output) {
    File dataFile = new File(localStorageURI.getPath(), relativePath);
    try (FileOutputStream fos = new FileOutputStream(dataFile)) {
      fos.write(output);
    } catch (IOException ioEx) {
      log.error("Could not write to file at " + relativePath + " " + ioEx);
    }
  }

  @Override
  public ArkId addCompoundObjectToShelf(ArkId urlArkId, MultipartFile zip) {
    Path shelf = Paths.get(localStorageURI);
    int entries = 0;
    long totalSize = 0;

    try (ZipInputStream zis = new ZipInputStream(zip.getInputStream())) {
      ZipEntry entry;
      ArkId arkId = null;
      String topLevelFolderName = null;

      while ((entry = zis.getNextEntry()) != null) {
        if(topLevelFolderName == null) {
          topLevelFolderName = entry.getName();
          if (topLevelFolderName.contains("/")) {
            arkId = new ArkId(StringUtils.substringBefore(topLevelFolderName, "/"));
          } else if (topLevelFolderName.contains("\\")) {
            arkId = new ArkId(StringUtils.substringBefore(topLevelFolderName, "\\"));
          } else {
            arkId = new ArkId(topLevelFolderName);
          }
          if (!arkId.equals(urlArkId)) {
            throw new InputMismatchException(
                "URL does not match internal id in zip file url ark=" + urlArkId + " zipped ark="
                    + arkId);
          }
        }
        if (!entry.getName().contains("/.") && !entry.getName().contains("__MACOSX")) {
          Path path = shelf.resolve(entry.getName());
          if (!path.toFile().exists()) {
            path.toFile().mkdirs();
          }
          if (!entry.isDirectory()) {
            Files.copy(zis, path, StandardCopyOption.REPLACE_EXISTING);

            // Prevent zip bombs from using all available resources
            totalSize += Files.size(path);
            entries++;
            if (entries > 1024) {
              throw new IllegalStateException(
                  "Zip file " + zip.getName() + " has too many files in it to unzip.");
            }
            if (totalSize > 0x6400000) { // Over 100 MB
              log.error("Zip file " + zip.getName() + " has too many files in it to unzip.");
              throw new IllegalStateException(
                  "Zip file " + zip.getName() + " is too large to unzip.");
            }
          }
        }
      }
      return arkId;
    } catch (IOException ioEx) {
      log.error("IO Error on shelf " + shelf + " " + ioEx, ioEx);
      throw new IllegalArgumentException(ioEx);
    }
  }

  // rm -rf repository/arkId ** dangerous! **
  @Override
  public void removeFile(String filePath) throws IOException {
    Path shelf = Paths.get(localStorageURI);
    Path ko = shelf.resolve(filePath.toString());

    Files.walk(ko)
        .sorted(Comparator
            .reverseOrder()) // Need to reverse the order to delete files before the directory they're in
        .forEach(file -> {
          try {
            Files.delete(file);
          } catch (IOException e) {
            log.error("Could not delete file " + file + " " + e);
          }
        });
  }

  private void createKOFolderStructure(Path resourceLocation, Path serviceLocation) {
    Path shelf = Paths.get(localStorageURI);

    try {
      Path resourceDir = shelf.resolve(resourceLocation);
      Files.createDirectories(resourceDir);
      Path serviceDir = shelf.resolve(serviceLocation);
      Files.createDirectory(serviceDir);
    } catch (IOException ioEx) {
      log.error("Unable to create directories for ko " + ioEx);
    }
  }

  @Override
  public void getCompoundObjectFromShelf(String objectDir, boolean isVersion,
      OutputStream outputStream) throws IOException {
    Path shelf = Paths.get(localStorageURI);
//    try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(shelf.resolve(arkFilename + "-" + version + ".zip")))) {
    ZipOutputStream zs = new ZipOutputStream(outputStream);
    Path parentPath = shelf.resolve(objectDir);

    Files.walk(parentPath)
        .filter(path -> !Files.isDirectory(path))
        .forEach(file -> {
          ZipEntry zipEntry;
          if (isVersion) {
            zipEntry = new ZipEntry(
                parentPath.getParent().getParent().toUri().relativize(file.toUri()).toString());
          } else {
            zipEntry = new ZipEntry(
                parentPath.getParent().toUri().relativize(file.toUri()).toString());
          }
          try {
            zs.putNextEntry(zipEntry);
            Files.copy(file, zs);
            zs.closeEntry();
          } catch (IOException ioEx) {
            log.error("Cannot create zip of ko due to file error.", ioEx);
          }
        });
    zs.close();
  }

  @Override
  public void createContainer(String relativeDestination) {
    Path basePath = Paths.get(localStorageURI);
    Path containerPath = basePath.resolve(relativeDestination);
    if (!containerPath.toFile().exists()) {
      containerPath.toFile().mkdirs();
    }

  }
  @Override
  public void save(CompoundDigitalObject cdo) {

    cdo.getContainers().forEach( (path, container) -> {
      createContainer(path);
    });

    cdo.getBinaryResources().forEach( (name, bytes)-> {
      saveBinary(name, bytes);
    });

    cdo.getContainers().forEach( (path, container) -> {
      saveMetadata(Paths.get(
          path, KnowledgeObject.METADATA_FILENAME).toString(), container);
    });

  }

  @Override
  public CompoundDigitalObject find(String cdoIdentifier) {

    JsonNode metaDate = getMetadata(cdoIdentifier);
    CompoundDigitalObject compoundDigitalObject = new CompoundDigitalObject(cdoIdentifier);
   // compoundDigitalObject.setMetadata(metaDate);

    Path path = Paths.get(getAbsoluteLocation(cdoIdentifier));

    List<Path> binaryPaths;
    try {
      binaryPaths = Files.walk(path,  2, FOLLOW_LINKS)
          .filter(Files::isRegularFile)
          .filter(p -> !p.getFileName().endsWith("metadata.json"))
          .map(Path::toAbsolutePath)
          .collect(Collectors.toList());

      binaryPaths.forEach( filePath ->{
        try {
          compoundDigitalObject.getBinaryResources().put(
              path.relativize(filePath).toString(),
              Files.readAllBytes(filePath));
        } catch (IOException e) {
          log.error("Cannot add binary to cod " + filePath + " " + e);
        }

      });

    } catch (IOException ioEx) {
      log.error("Cannot read children at location " + path + " " + ioEx);
    }

    return compoundDigitalObject;
  }

  @Override
  public void delete(String cdoIdentifier) throws ShelfException {
    Path path = Paths.get(localStorageURI.toString(), cdoIdentifier);
    try {
      FileUtils.deleteDirectory(new File(path.toString()));
    } catch (IOException e) {
      throw new ShelfException("Could not delete cdo " + cdoIdentifier, e);
    }

  }
}

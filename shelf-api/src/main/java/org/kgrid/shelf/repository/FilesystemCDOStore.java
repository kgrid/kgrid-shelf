package org.kgrid.shelf.repository;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.kgrid.shelf.domain.KnowledgeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Primary
@Component
@Qualifier("filesystem")
public class FilesystemCDOStore implements CompoundDigitalObjectStore {

  private String localStoragePath;

  private final Logger log = LoggerFactory.getLogger(FilesystemCDOStore.class);

  public FilesystemCDOStore( @Value("${kgrid.shelf.cdostore.filesystem.location:shelf}") String localStoragePath) {
    this.localStoragePath = localStoragePath;
  }

  @Override
  public List<Path> getChildren(Path filePath) {
    Path path = Paths.get(localStoragePath);
    if(filePath != null) {
      path = path.resolve(filePath);
    }
    List<Path> children = new ArrayList<>();
    try {
      children = Files.walk(path, 1)
          .filter(Files::isDirectory)
          .collect(Collectors.toList());
      children.remove(0); // Remove the parent directory
    } catch(IOException ioEx) {
      log.error("Cannot read children at location " + path + " " + ioEx);
    }
    return children;
  }

  @Override
  public Path getAbsoluteLocation(Path relativeFilePath) {
    Path shelf = Paths.get(localStoragePath);
    if(!shelf.toFile().exists()) {
      throw new IllegalStateException(
          "Filesystem shelf location " + shelf + " is not a valid directory."
              + " Make sure the property kgrid.shelf.cdostore.filesystem.location is set correctly.");
    }
    if(relativeFilePath == null) {
      return shelf;
    }
    return shelf.resolve(relativeFilePath);
  }

  @Override
  public ObjectNode getMetadata(Path relativePath) {
    Path shelf = Paths.get(localStoragePath);
    File metadataFile = shelf.resolve(relativePath).toFile();
    if(metadataFile.isDirectory()) {
      metadataFile = shelf.resolve(relativePath).resolve(KnowledgeObject.METADATA_FILENAME).toFile();
    }
    if(!metadataFile.exists()) {
      log.error("Cannot find metadata file for knowledge object at " + metadataFile);
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    JsonNode koMetadata;
    try {
      koMetadata = mapper.readTree(metadataFile);
      if(koMetadata.isArray()) {
        // Parent object in json-ld is array, get first element.
        koMetadata = koMetadata.get(0);
      }
     ArrayNode children = ((ObjectNode)koMetadata).putArray("children");
      getChildren(Paths.get(metadataFile.getAbsolutePath()).getParent()).forEach(path -> {
        if(path.toFile().isDirectory() && path.toFile().listFiles().length > 0) {
          for (File child : path.toFile().listFiles()) {
            if(!child.getName().startsWith(".")) {
              children.add(child.getAbsolutePath().substring(shelf.toString().length()));
            }
          }
        }
      });

      return ((ObjectNode)koMetadata);
    } catch (IOException ioEx) {
      throw new IllegalArgumentException("Cannot read metadata file at path " + shelf.resolve(metadataFile.getAbsolutePath())+ " " + ioEx);
    }
  }

  @Override
  public byte[] getBinary(Path relativeFilePath) {
    Path shelf = Paths.get(localStoragePath);
    byte[] bytes = null;
    try {
      bytes = Files.readAllBytes(shelf.resolve(relativeFilePath));
    } catch (IOException ioEx) {
      log.error("Cannot read file at " + relativeFilePath + " " + ioEx);
    }
    return bytes;
  }

  @Override
  public void saveMetadata(Path relativePath, JsonNode metadata) {
    File metadataFile = new File(localStoragePath, relativePath.toString());
    try {
      ObjectWriter writer = new ObjectMapper().writer().with(SerializationFeature.INDENT_OUTPUT);
      writer.writeValue(metadataFile, metadata);
    } catch(IOException ioEx) {
      log.error("Could not write to file at " + relativePath + " " + ioEx);
    }
  }

  @Override
  public void saveBinary(Path relativePath, byte[] output) {
    File dataFile = new File(localStoragePath, relativePath.toString());
    try (FileOutputStream fos = new FileOutputStream(dataFile)){
      fos.write(output);
    } catch (IOException ioEx) {
      log.error("Could not write to file at " + relativePath + " " + ioEx);
    }
  }

  @Override
  public ObjectNode addCompoundObjectToShelf(MultipartFile zip) {
    Path shelf = Paths.get(localStoragePath);
    String filename = zip.getOriginalFilename();
    int entries = 0;
    long totalSize = 0;

    if(filename.endsWith(".zip")) {
      filename = filename.substring(0, filename.length()-4);
    }
    String[] parts  = filename.split("-");
    String version = null;
    if(parts.length > 2) {
      version = parts[2];
    }
    try (ZipInputStream zis = new ZipInputStream(zip.getInputStream())) {
      ZipEntry entry;
      while((entry = zis.getNextEntry()) != null) {
        if(!entry.getName().contains("/.") && !entry.getName().contains("__MACOSX")) {
          Path dir = shelf.resolve(entry.getName());
          if(!dir.toFile().exists() && entry.isDirectory()) {
            dir.toFile().mkdirs();
          } else {
            Files.copy(zis, dir, StandardCopyOption.REPLACE_EXISTING);

            // Prevent zip bombs from using all available resources
            totalSize += Files.size(dir);
            entries++;
            if (entries > 1024) {
              throw new IllegalStateException("Zip file " + zip.getName() + " has too many files in it to unzip.");
            }
            if (totalSize > 0x6400000) { // Over 100 MB
              log.error("Zip file " + zip.getName() + " has too many files in it to unzip.");
              throw new IllegalStateException("Zip file " + zip.getName() + " is too large to unzip.");
            }
          }
        }
      }
    } catch (IOException ioEx) {
      log.error("Cannot find file " + shelf + " " + ioEx);
    }
    String objectRoot = parts[0] + "-" + parts[1];

    if(version == null) {
      // TODO: Get default version?
      version = getChildren(Paths.get(objectRoot)).get(0).getFileName().toString();
    }
    Path metadataLocation = Paths.get(objectRoot, version, KnowledgeObject.METADATA_FILENAME);
    return getMetadata(metadataLocation);
  }

  // rm -rf repository/arkId ** dangerous! **
  @Override
  public void removeFile(Path filePath) throws IOException {
    Path shelf = Paths.get(localStoragePath);
    Path ko = shelf.resolve(filePath.toString());

    Files.walk(ko)
        .sorted(Comparator.reverseOrder()) // Need to reverse the order to delete files before the directory they're in
        .forEach(file -> {
          try {
            Files.delete(file);
          } catch (IOException e) {
            log.error("Could not delete file " + file + " " + e);
          }
        });
  }

  private void createKOFolderStructure(Path resourceLocation, Path serviceLocation) {
    Path shelf = Paths.get(localStoragePath);

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
  public void getCompoundObjectFromShelf(Path versionDir, OutputStream outputStream) throws IOException {
    Path shelf = Paths.get(localStoragePath);
//    try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(shelf.resolve(arkFilename + "-" + version + ".zip")))) {
    ZipOutputStream zs = new ZipOutputStream(outputStream);
    Path parentPath = shelf.resolve(versionDir);
    Files.walk(parentPath)
        .filter(path -> !Files.isDirectory(path))
        .forEach(file -> {
          ZipEntry zipEntry = new ZipEntry(parentPath.relativize(file).toString());
          try {
            zs.putNextEntry(zipEntry);
            Files.copy(file, zs);
            zs.closeEntry();
          } catch (IOException ioEx) {
            log.error("Cannot create zip of ko due to error " + ioEx);
          }
        });
    zs.close();
  }
}

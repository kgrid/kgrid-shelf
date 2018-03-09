package edu.umich.lhs.activator.repository;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FilesystemKOStore implements KnowledgeObjectStore {

  @Value("${activator.shelf.path}")
  private String localStoragePath;

  private final Logger log = LoggerFactory.getLogger(FilesystemKOStore.class);

  @Override
  public List<String> getChildren(Path filePath) {
    Path path = Paths.get(localStoragePath);
    if(filePath != null) {
      path = path.resolve(filePath);
    }
    List<String> children = new ArrayList<>();
    try {
      children = Files.walk(path, 1)
          .filter(Files::isDirectory)
          .map(filepath -> filepath.getFileName().toString())
          .collect(Collectors.toList());
      children.remove(0); //Remove the parent directory
    } catch(IOException ioEx) {
      log.error("Cannot read versions in KO at location " + path + " " + ioEx);
    }
    return children;
  }

  @Override
  public Path getAbsolutePath(Path relativeFilePath) {
    Path shelf = Paths.get(localStoragePath);
    return shelf.resolve(relativeFilePath);
  }

  @Override
  public ObjectNode getMetadata(Path relativePath) {
    Path shelf = Paths.get(localStoragePath);
    File metadataFile = shelf.resolve(relativePath).toFile();
    if(!metadataFile.exists()) {
      log.error("Cannot find metadata file for knowledge object at " + metadataFile);
    }
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    ObjectNode koMetadata = null;
    try {
      koMetadata = (ObjectNode)mapper.readTree(metadataFile);
    } catch (IOException ioEx) {
      log.error("Cannot read metadata file at path " + metadataFile + " " + ioEx);
    }
    return koMetadata;
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
      ObjectWriter writer = new ObjectMapper().writer();
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
    if(filename.endsWith(".zip")) {
      filename = filename.substring(0, filename.length()-4);
    }
    String[] parts  = filename.split("-");
    String version = null;
    if(parts.length > 2) {
      version = parts[2];
    }
    try {
      ZipInputStream zis = new ZipInputStream(zip.getInputStream());
      ZipEntry entry;
      while((entry = zis.getNextEntry()) != null) {
        if(!entry.getName().contains("/.") && !entry.getName().contains("__MACOSX")) {
          Path dir = shelf.resolve(entry.getName());
          if(!dir.toFile().exists() && entry.isDirectory()) {
            dir.toFile().mkdirs();
          } else {
            Files.copy(zis, dir, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    } catch (IOException ioEx) {
      log.error("Cannot find file " + shelf + " " + ioEx);
    }
    Path objectRoot = Paths.get(parts[0] + "-" + parts[1]);
    if(version == null) {
      version = getChildren(objectRoot).get(0);
    }
    Path metadataPath = objectRoot.resolve(version).resolve("metadata.json");
    return getMetadata(metadataPath);
  }

  // rm -rf repository/arkId ** dangerous! **
  public void removeKO(String arkFilename) throws IOException {
    Path shelf = Paths.get(localStoragePath);
    Path ko = shelf.resolve(arkFilename);

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

  private void zipKO(String arkFilename, String version) {
    Path shelf = Paths.get(localStoragePath);
    Path versionDir = Paths.get(arkFilename, version);
    try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(shelf.resolve(arkFilename + "-" + version + ".zip")))) {
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
    } catch (IOException ioEx) {
      log.error("Cannot create zip of ko due to error " + ioEx);
    }
  }

}

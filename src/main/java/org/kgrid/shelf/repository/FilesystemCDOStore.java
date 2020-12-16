package org.kgrid.shelf.repository;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.kgrid.shelf.ShelfException;
import org.kgrid.shelf.ShelfResourceNotFound;
import org.kgrid.shelf.domain.KoFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitOption.FOLLOW_LINKS;

@Qualifier("filesystem")
public class FilesystemCDOStore implements CompoundDigitalObjectStore {

  private Path localStorageDir;
  private static int MAX_DEPTH = 3;

  private final Logger log = LoggerFactory.getLogger(FilesystemCDOStore.class);

  public FilesystemCDOStore(
      @Value("${kgrid.shelf.cdostore.url:filesystem:file://shelf}") String connectionURI) {
    connectionURI = connectionURI.replace(" ", "%20");
    URI uri = URI.create(connectionURI.substring(connectionURI.indexOf(':') + 1));
    if (uri.getHost() == null) {
      localStorageDir = Paths.get(uri);
    } else {
      localStorageDir = Paths.get(uri.getHost(), uri.getPath());
    }
    try {
      Path location = localStorageDir;
      Files.createDirectories(location);
    } catch (IOException e) {
      log.error("Unable to find or create shelf at %s", localStorageDir);
    }
  }

  @Override
  public List<URI> getChildren() {
    List<URI> children;
    try {
      children =
          Files.walk(localStorageDir, MAX_DEPTH, FOLLOW_LINKS)
              .filter(this::pathContainsMetadata)
              .map((childPath) -> getChildUri(childPath))
              .collect(Collectors.toList());
    } catch (IOException ioEx) {
      throw new ShelfResourceNotFound("Cannot read children at location " + localStorageDir, ioEx);
    }
    return children;
  }

  private URI getChildUri(Path childPath) {
    URI uri =
        URI.create(
            childPath
                    .toString()
                    .substring(localStorageDir.toString().length() + 1)
                    .replaceAll("\\\\", "/")
                    .replaceAll(" ", "%20")
                + "/");
    return uri;
  }

  @Override
  public URI getAbsoluteLocation(URI relativePath) {
    if (relativePath == null) {
      return localStorageDir.toUri();
    }
    return localStorageDir.resolve(relativePath.toString()).toUri();
  }

  @Override
  public ObjectNode getMetadata(URI relativePath) {
    Path metadataPath = localStorageDir.resolve(relativePath.toString().replaceAll("%20", " "));
    File metadataFile = metadataPath.toFile();
    if (metadataFile.isDirectory()
        || !metadataFile.getPath().endsWith(KoFields.METADATA_FILENAME.asStr())) {
      metadataFile = metadataPath.resolve(KoFields.METADATA_FILENAME.asStr()).toFile();
    }

    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    JsonNode koMetadata;
    try {
      koMetadata = mapper.readTree(metadataFile);

      return ((ObjectNode) koMetadata);
    } catch (Exception ioEx) {
      throw new ShelfResourceNotFound("Metadata resource not found " + metadataPath, ioEx);
    }
  }

  @Override
  public byte[] getBinary(URI relativePath) {
    Path binaryPath = localStorageDir.resolve(relativePath.toString().replaceAll("%20", " "));
    byte[] bytes;
    try {
      bytes = Files.readAllBytes(binaryPath);
    } catch (IOException ioEx) {
      throw new ShelfResourceNotFound("Binary resource not found " + binaryPath, ioEx);
    }
    return bytes;
  }

  @Override
  public InputStream getBinaryStream(URI relativePath) {
    Path binaryPath = localStorageDir.resolve(relativePath.toString().replaceAll("%20", " "));
    try {
      return Files.newInputStream(binaryPath);
    } catch (IOException ioEx) {
      throw new ShelfResourceNotFound("Binary resource not found " + binaryPath, ioEx);
    }
  }

  @Override
  public void saveMetadata(JsonNode metadata, URI relativePath) {
    Path metadataPath = localStorageDir.resolve(relativePath.toString());
    File metadataFile = metadataPath.toFile();
    if (metadataFile.isDirectory()) {
      metadataFile = metadataPath.resolve(KoFields.METADATA_FILENAME.asStr()).toFile();
    }
    try {
      ObjectWriter writer = new ObjectMapper().writer().with(SerializationFeature.INDENT_OUTPUT);
      writer.writeValue(metadataFile, metadata);
    } catch (IOException ioEx) {
      throw new ShelfException("Could not write to file at " + metadataPath, ioEx);
    }
  }

  @Override
  public void saveBinary(byte[] output, URI relativePath) {
    Path dataPath = localStorageDir.resolve(relativePath.toString());
    File dataFile = dataPath.toFile();
    try (FileOutputStream fos = FileUtils.openOutputStream(dataFile)) {
      fos.write(output);
    } catch (IOException ioEx) {
      throw new ShelfException("Could not write to file at " + dataPath, ioEx);
    }
  }

  @Override
  public long getBinarySize(URI relativePath) {
    Path dataPath = localStorageDir.resolve(relativePath.toString());
    try {
      return Files.size(dataPath);
    } catch (IOException e) {
      throw new ShelfException("Cannot get file size for " + relativePath);
    }
  }

  @Override
  public void createContainer(URI relativePath) {
    Path containerPath = localStorageDir.resolve(relativePath.toString());
    if (!containerPath.toFile().exists()) {
      containerPath.toFile().mkdirs();
    }
  }

  @Override
  public void delete(URI relativePath) throws ShelfException {
    Path path = localStorageDir.resolve(relativePath.toString());
    try {
      FileUtils.deleteDirectory(new File(path.toString()));
    } catch (IOException e) {
      throw new ShelfException("Could not delete cdo " + relativePath, e);
    }
  }

  // Simple transaction support:
  @Override
  public String createTransaction() {
    String trxID = "trx-" + UUID.randomUUID().toString();
    createContainer(URI.create(trxID));
    return trxID;
  }

  @Override
  public void commitTransaction(String transactionID) {
    File tempFolder = new File(localStorageDir.toFile(), transactionID);
    try {
      FileUtils.copyDirectory(tempFolder, localStorageDir.toFile());
      FileUtils.deleteDirectory(tempFolder);
    } catch (IOException e) {
      log.warn("Cannot copy files from temp directory to shelf " + e);
    }
  }

  @Override
  public void rollbackTransaction(String transactionID) {
    File tempFolder = new File(localStorageDir.toFile(), transactionID);
    try {
      FileUtils.deleteDirectory(tempFolder);
    } catch (IOException e) {
      log.warn("Cannot rollback failed transaction " + e);
    }
  }

  private boolean pathContainsMetadata(Path path) {
    return path.toFile().isDirectory()
        && path.resolve(KoFields.METADATA_FILENAME.asStr()).toFile().exists();
  }
}

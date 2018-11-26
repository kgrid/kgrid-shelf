package org.kgrid.shelf;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class UriHandlingTest {

  @Test
  public void createPathFromAbsoluteFilePathUri() throws IOException {

    Path originalPath = Files.createTempFile(null, null);

    // Create property with absolute path, typical URI ("filesystem:file:///...")
    String property = "filesystem:" + originalPath.toAbsolutePath().toUri();

    // create file system uri get passed to CdoStore; remove "fileSystem:" scheme
    URI uri = URI.create(property);
    URI fileSystemUri = URI.create(uri.getSchemeSpecificPart());

    // Now that's a real file system URI!
    Path pathFromUri = getPath(fileSystemUri);

    assertTrue(Files.exists(originalPath));
    assertTrue(Files.exists(pathFromUri));
    assertTrue(Files.isSameFile(originalPath, pathFromUri));
  }

  @Test
  public void createPathFromRelativeFileUri() throws IOException {

    Path originalPath = Files.createTempFile(null, null);

    // Create property with relative path, leave out slashes ("filesystem:file:...")
    Path relativePath = Paths.get("").toAbsolutePath().relativize(originalPath);
    String property = "filesystem:" + relativePath.toUri();

    // file system uri get passed to CdoStore; remove "fileSystem:" scheme
    URI uri = URI.create(property);
    URI fileSystemUri = URI.create(uri.getSchemeSpecificPart());

    // That's not really a valid file URI but it might work...
    // file:///../../../../../var/folders/w6/rcfp0mmj7kj97gtx97lw55wjhv1jtr/T/1019684936422797695.tmp
    Path pathFromUri = getPath(fileSystemUri);

    assertTrue(Files.exists(originalPath));
    assertTrue(Files.exists(pathFromUri));
    assertTrue(Files.isSameFile(originalPath, pathFromUri));

  }

  @Test
  public void createPathFromPathUri() throws IOException {
    Path originalPath = Files.createTempFile(null, null);

    // Create property with relative path, leave out slashes ("filesystem:file:...")
    Path relativePath = Paths.get("").toAbsolutePath().relativize(originalPath);
    String property = "filesystem:" + relativePath.toUri();

    // create file system uri to pass to CdoStore, but first remove "fileSystem:" scheme
    URI uri = URI.create(property);
    URI fileSystemUri = URI.create(uri.getSchemeSpecificPart());
    Path pathFromUri = getPath(fileSystemUri);

    assertTrue(Files.exists(originalPath));
    assertTrue(Files.exists(pathFromUri));
    assertTrue(Files.isSameFile(originalPath, pathFromUri));


  }

  private Path getPath(URI fileSystemUri) {
    // That's might be a valid file URI, let's find out...
    Path pathFromUri;

    if (fileSystemUri.getScheme() != null) {
      pathFromUri = Paths.get(fileSystemUri);
    } else {
      pathFromUri = Paths.get(fileSystemUri.getPath());
    }
    return pathFromUri;
  }
}

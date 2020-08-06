package org.kgrid.shelf.repository;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class CompoundDigitalObjectStoreFactoryTest {

  @Test
  public void testCreatesFilesystemCdoStore() throws IOException {
    CompoundDigitalObjectStore store =
        CompoundDigitalObjectStoreFactory.create(
            "filesystem:file://gibberishsadfeojunsdfnjsdfdsoiklmsd");
    assertEquals(FilesystemCDOStore.class, store.getClass());
    Files.delete(Paths.get("gibberishsadfeojunsdfnjsdfdsoiklmsd")); // Cleanup
  }

  @Test
  public void testCreatesFedoraCdoStore() {
    CompoundDigitalObjectStore store =
        CompoundDigitalObjectStoreFactory.create("fedora:http://gibberish");
    assertEquals(FedoraCDOStore.class, store.getClass());
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwsExceptionForUnknownStoreType() {
    CompoundDigitalObjectStore store =
        CompoundDigitalObjectStoreFactory.create("alien:xyz://gibberish");
    assertEquals(FedoraCDOStore.class, store.getClass());
  }
}

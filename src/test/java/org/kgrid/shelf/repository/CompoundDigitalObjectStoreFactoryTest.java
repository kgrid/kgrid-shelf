package org.kgrid.shelf.repository;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompoundDigitalObjectStoreFactoryTest {

  @Test
  public void testCreatesFilesystemCdoStore() {
    CompoundDigitalObjectStore store =
        CompoundDigitalObjectStoreFactory.create("filesystem:file://gibberish");
    assertEquals(FilesystemCDOStore.class, store.getClass());
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

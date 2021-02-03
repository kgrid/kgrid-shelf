package org.kgrid.shelf.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("CDO Store Factory Tests")
public class CompoundDigitalObjectStoreFactoryTest {
  private static final UUID uuid = UUID.randomUUID();

  private static Stream<Arguments> provideConnectionUris() {

    return Stream.of(
        Arguments.of(new Object[]{"filesystem:file://gibberish" + uuid, FilesystemCDOStore.class})
    );
  }

  @ParameterizedTest
  @MethodSource("provideConnectionUris")
  @DisplayName("Creates correct cdo store for connection string")
  public void createsCorrectCdoStore(
      String connectionDetails, Class<CompoundDigitalObjectStore> expectedStoreClass)
      throws IOException {
    CompoundDigitalObjectStore store = CompoundDigitalObjectStoreFactory.create(connectionDetails);
    assertEquals(expectedStoreClass, store.getClass());

    // Cleanup
    if (Files.exists(Paths.get("gibberish" + uuid))) {
      Files.delete(Paths.get("gibberish" + uuid));
    }
  }

  @Test
  @DisplayName("Throws illegal argument ex for unknown connection string")
  public void throwsExceptionForUnknownStoreType() {
    String unknownType = "alien";
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> CompoundDigitalObjectStoreFactory.create(unknownType + ":xyz://gibberish"));
    assertEquals("Cannot find specified CDO store type " + unknownType, e.getMessage());
  }
}

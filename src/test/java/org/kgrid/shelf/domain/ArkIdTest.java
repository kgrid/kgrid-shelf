package org.kgrid.shelf.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.kgrid.shelf.TestHelper.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Ark Id Tests")
public class ArkIdTest {

  private final String arkStringNaanNameVersion =
      String.format("ark:/%s/%s/%s", NAAN, NAME, VERSION_1);
  private final String arkStringNaanNameOnly = String.format("ark:/%s/%s", NAAN, NAME);
  private final ArkId arkIdFromNaanNameVersion = new ArkId(NAAN, NAME, VERSION_1);
  private final ArkId arkIdFromNaanNameOnly = new ArkId(NAAN, NAME);

  @Test
  @DisplayName("Ark Id can be constructed from Ark string with a version")
  public void testArkConstructorFromArkString() {
    ArkId arkIdFromArkString = new ArkId(arkStringNaanNameVersion);
    assertEquals(new ArkId(NAAN, NAME, VERSION_1), arkIdFromArkString);
  }

  @Test
  @DisplayName("Ark Id can be constructed from Ark string with no version")
  public void testArkConstructorFromArkString_noVersion() {
    ArkId arkIdFromArkString = new ArkId(arkStringNaanNameOnly);
    assertEquals(new ArkId(NAAN, NAME), arkIdFromArkString);
  }

  @Test
  @DisplayName("Ark Id from string constructor throws if it cannot create Ark")
  public void testArkStringConstructorThrows() {
    String notAnArk = "MALARKEY";
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new ArkId(notAnArk));
    assertEquals(String.format("Cannot create ark id from %s", notAnArk), exception.getMessage());
  }

  @Test
  @DisplayName("Ark Id from a naan and name")
  public void testArkConstructorFromNaanName() {
    assertAll(
        () -> assertEquals(arkIdFromNaanNameOnly.getNaan(), NAAN),
        () -> assertEquals(arkIdFromNaanNameOnly.getName(), NAME));
  }

  @Test
  @DisplayName("Ark Id from a naan, name, and version")
  public void testArkConstructorFromNaanNameVersion() {
    assertAll(
        () -> assertEquals(arkIdFromNaanNameVersion.getNaan(), NAAN),
        () -> assertEquals(arkIdFromNaanNameVersion.getName(), NAME),
        () -> assertEquals(arkIdFromNaanNameVersion.getVersion(), VERSION_1));
  }

  @Test
  @DisplayName("Get Full Ark with version returns correct string")
  public void testGetFullArkVersion() {
    assertEquals(arkStringNaanNameVersion, arkIdFromNaanNameVersion.getFullArk());
  }

  @Test
  @DisplayName("Get Full Ark with no version returns correct string")
  public void testGetFullArkNoVersion() {
    assertEquals(arkStringNaanNameOnly, arkIdFromNaanNameOnly.getFullArk());
  }

  @Test
  @DisplayName("Get Full Dash Ark returns correct string")
  public void testGetDashArk() {
    assertEquals(String.format("%s-%s", NAAN, NAME), arkIdFromNaanNameVersion.getDashArk());
  }

  @Test
  @DisplayName("Get Full Dash Ark with version returns correct string")
  public void testGetFullDashArkWithVersion() {
    assertEquals(
        String.format("%s-%s-%s", NAAN, NAME, VERSION_1),
        arkIdFromNaanNameVersion.getFullDashArk());
  }

  @Test
  @DisplayName("Get Full Dash Ark with no version returns correct string")
  public void testGetFullDashArkWithNoVersion() {
    assertEquals(String.format("%s-%s", NAAN, NAME), arkIdFromNaanNameOnly.getFullDashArk());
  }

  @Test
  @DisplayName("isArkId can detect a correct arkId String with version")
  public void testIsArkIdWithFullArkString() {
    assertTrue(ArkId.isArkId(arkStringNaanNameVersion));
  }

  @Test
  @DisplayName("isArkId can detect a correct arkId String without version")
  public void testIsArkIdWithArkStringNoVersion() {
    assertTrue(ArkId.isArkId(arkStringNaanNameOnly));
  }

  @Test
  @DisplayName("isArkId can detect an incorrect arkId String")
  public void testIsArkIdWithNonsense() {
    assertFalse(ArkId.isArkId("shmark:/close/butNo/cigar"));
  }

  @Test
  @DisplayName("getSlashArk returns proper string")
  public void testGetSlashArk() {
    assertEquals(String.format("%s/%s", NAAN, NAME), arkIdFromNaanNameVersion.getSlashArk());
  }

  @Test
  @DisplayName("getSlashArkVersion returns proper string")
  public void testGetSlashArkVersion() {
    assertEquals(
        String.format("%s/%s/%s", NAAN, NAME, VERSION_1),
        arkIdFromNaanNameVersion.getSlashArkVersion());
  }

  @Test
  @DisplayName("Ark Id with Version is not equal to same Ark without version")
  public void testEqualsChecksVersion() {
    assertNotEquals(arkIdFromNaanNameVersion, arkIdFromNaanNameOnly);
  }

  @Test
  @DisplayName("toString uses getFullArk")
  public void testToStringUsesGetFullArk() {
    assertEquals(arkIdFromNaanNameVersion.getFullArk(), arkIdFromNaanNameVersion.toString());
  }

  @Test
  @DisplayName("hashCode is consistent")
  public void testHashCode() {
    assertEquals(448828949, arkIdFromNaanNameVersion.hashCode());
  }

  @Test
  @DisplayName("hashCode is consistent No Version")
  public void testHashCodeNoVersion() {
    assertEquals(448828900, arkIdFromNaanNameOnly.hashCode());
  }

  @Test
  @DisplayName("compareTo")
  public void testCompareTo() {
    assertAll(
        () -> assertEquals(0, arkIdFromNaanNameVersion.compareTo(arkIdFromNaanNameVersion)),
        () -> assertEquals(0, arkIdFromNaanNameVersion.compareTo(new ArkId(NAAN, NAME, VERSION_1))),
        () ->
            assertEquals(
                12, arkIdFromNaanNameVersion.compareTo(new ArkId("banana", NAME, VERSION_1))),
        () ->
            assertEquals(
                13, arkIdFromNaanNameVersion.compareTo(new ArkId(NAAN, "apple", VERSION_1))),
        () -> assertEquals(-54, arkIdFromNaanNameVersion.compareTo(new ArkId(NAAN, NAME, "grape"))),
        () -> assertEquals(1, arkIdFromNaanNameVersion.compareTo(arkIdFromNaanNameOnly)));
  }
}

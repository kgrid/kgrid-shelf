package org.kgrid.shelf.domain;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArkIdTest {

  @Test
  public void testArkWithVersion() {

    ArkId arkId = new ArkId("ark:/hello/world/v1");
    assertEquals("hello", arkId.getNaan());
    assertEquals("world", arkId.getName());
    assertEquals("v1", arkId.getVersion());
    assertEquals("hello-world/v1", arkId.getDashArkVersion());

    arkId = new ArkId("ark:/hello/world");
    assertEquals("hello", arkId.getNaan());
    assertEquals("world", arkId.getName());
  }

  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(ArkId.class).verify();
  }

  @Test
  public void testHasVersion() {

    ArkId arkId = new ArkId("ark:/hello/world/v1");
    assertEquals(true, arkId.hasVersion());

    arkId = new ArkId("ark:/hello/world");
    assertEquals(false, arkId.hasVersion());
  }

  @Test
  public void testCreatesArkIdCorrectly() {
    ArkId arkId = new ArkId("ark:/hello/world/v0.1");
    assertEquals("hello", arkId.getNaan());
    assertEquals("world", arkId.getName());
    assertEquals("v0.1", arkId.getVersion());
  }

  @Test
  public void compareToNumericDifferentNaan() {
    ArkId lowest = new ArkId("ark:/1/2/3");
    ArkId highest = new ArkId("ark:/2/2/3");
    assertEquals(-1, lowest.compareTo(highest));
  }

  @Test
  public void compareToNumericDifferentName() {
    ArkId lowest = new ArkId("ark:/1/2/3");
    ArkId highest = new ArkId("ark:/1/3/1");
    assertEquals(-1, lowest.compareTo(highest));
  }

  @Test
  public void compareToNumericDifferentVersion() {
    ArkId lowest = new ArkId("ark:/1/2/3");
    ArkId highest = new ArkId("ark:/1/2/4");
    assertEquals(-1, lowest.compareTo(highest));
  }

  @Test
  public void compareToNumericNoVersion() {
    ArkId lowest = new ArkId("ark:/1/2");
    ArkId highest = new ArkId("ark:/1/2/1");
    assertEquals(-1, lowest.compareTo(highest));
  }

  @Test
  public void compareToComplexVersion() {
    ArkId lowest = new ArkId("ark:/1/2/1.0");
    ArkId highest = new ArkId("ark:/1/2/1.1");
    assertEquals(-1, lowest.compareTo(highest));
  }

  @Test
  public void compareToSame() {
    ArkId lowest = new ArkId("ark:/1/2/1.0");
    assertEquals(0, lowest.compareTo(lowest));
  }

  @Test
  public void compareToIdentical() {
    ArkId lowest = new ArkId("ark:/1/2/1.0");
    ArkId alsoLowest = new ArkId("ark:/1/2/1.0");
    assertEquals(0, lowest.compareTo(alsoLowest));
  }
}

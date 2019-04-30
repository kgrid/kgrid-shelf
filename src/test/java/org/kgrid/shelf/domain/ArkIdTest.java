package org.kgrid.shelf.domain;

import static org.junit.Assert.*;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class ArkIdTest {

  @Test
  public void testArkWithImplentation(){

    ArkId arkId = new ArkId("hello-world/v1");
    assertEquals("hello", arkId.getNaan());
    assertEquals("world", arkId.getName());
    assertEquals("v1", arkId.getImplementation());
    assertEquals("hello-world/v1", arkId.getDashArkImplementation());

    arkId = new ArkId("hello-world");
    assertEquals("hello", arkId.getNaan());
    assertEquals("world", arkId.getName());


  }
  @Test
  public void equalsContract() {
    EqualsVerifier.forClass(ArkId.class).verify();
  }

  @Test
  public void testIsImplentation(){

    ArkId arkId = new ArkId("hello-world/v1");
    assertEquals(true, arkId.isImplementation());

    arkId = new ArkId("hello-world");
    assertEquals(false,arkId.isImplementation());


  }

}
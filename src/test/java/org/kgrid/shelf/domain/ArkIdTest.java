package org.kgrid.shelf.domain;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArkIdTest {

  @Test
  public void testArkWithImplentation(){

    ArkId arkId = new ArkId("hello-world/v1");
    assertEquals("hello", arkId.getNaan());
    assertEquals("world", arkId.getName());
    assertEquals("v1", arkId.getImplementation());

    arkId = new ArkId("hello-world");
    assertEquals("hello", arkId.getNaan());
    assertEquals("world", arkId.getName());


  }

}
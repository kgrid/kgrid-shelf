package org.kgrid.shelf.domain;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ArkIdTest {

    @Test
    public void testArkWithVersion() {

        ArkId arkId = new ArkId("hello-world/v1");
        assertEquals("hello", arkId.getNaan());
        assertEquals("world", arkId.getName());
        assertEquals("v1", arkId.getVersion());
        assertEquals("hello-world/v1", arkId.getDashArkVersion());

        arkId = new ArkId("hello-world");
        assertEquals("hello", arkId.getNaan());
        assertEquals("world", arkId.getName());


    }

    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ArkId.class).verify();
    }

    @Test
    public void testHasVersion() {

        ArkId arkId = new ArkId("hello-world/v1");
        assertEquals(true, arkId.hasVersion());

        arkId = new ArkId("hello-world");
        assertEquals(false, arkId.hasVersion());


    }

    @Test
    public void testCreatesArkIdCorrectly() {
        ArkId arkId = new ArkId("ark:/hello/world/v0.1");
        assertEquals("hello", arkId.getNaan());
        assertEquals("world", arkId.getName());
        assertEquals("v0.1", arkId.getVersion());

    }
}

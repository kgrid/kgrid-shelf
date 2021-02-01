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

    private final String arkStringNaanNameVersion = String.format("ark:/%s/%s/%s", NAAN, NAME, VERSION);
    private final String arkStringNaanNameOnly = String.format("ark:/%s/%s", NAAN, NAME);
    private final ArkId arkIdFromNaanNameVersion = new ArkId(NAAN, NAME, VERSION);
    private final ArkId arkIdFromNaanNameOnly = new ArkId(NAAN, NAME);

    @Test
    @DisplayName("Ark Id can be constructed from Ark string")
    public void testArkConstructorFromArkString() {
        ArkId arkIdFromArkString = new ArkId(arkStringNaanNameVersion);
        assertEquals(new ArkId(NAAN, NAME, VERSION), arkIdFromArkString);
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
                () -> assertEquals(arkIdFromNaanNameOnly.getName(), NAME)
        );
    }

    @Test
    @DisplayName("Ark Id from a naan, name, and version")
    public void testArkConstructorFromNaanNameVersion() {
        assertAll(
                () -> assertEquals(arkIdFromNaanNameVersion.getNaan(), NAAN),
                () -> assertEquals(arkIdFromNaanNameVersion.getName(), NAME),
                () -> assertEquals(arkIdFromNaanNameVersion.getVersion(), VERSION)
        );
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
    @DisplayName("Get Full Dash Ark with version returns correct string")
    public void testGetFullDashArkWithVersion() {
        assertEquals(String.format("%s-%s-%s", NAAN, NAME, VERSION), arkIdFromNaanNameVersion.getFullDashArk());
    }

    @Test
    @DisplayName("Get Full Dash Ark with no version returns correct string")
    public void testGetFullDashArkWithNoVersion() {
        assertEquals(String.format("%s-%s", NAAN, NAME), arkIdFromNaanNameOnly.getFullDashArk());
    }
}

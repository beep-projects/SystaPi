package de.freaklamarsch.systarest.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import de.freaklamarsch.systarest.FakeSTouchDisplay;
import de.freaklamarsch.systarest.STouchProtocol.Button;

class FakeSTouchDisplayTest {

    @Test
    void testAddAndFindButton() {
        FakeSTouchDisplay display = new FakeSTouchDisplay();
        display.addButton(new Button((byte) 1, 10, 10, 50, 50));
        display.addButton(new Button((byte) 2, 60, 60, 100, 100));

        assertNotNull(display.findButtonInObjectTree((byte) 1));
        assertNotNull(display.findButtonInObjectTree((byte) 2));
        assertNull(display.findButtonInObjectTree((byte) 3)); // Non-existent button
    }

    @Test
    void testAddOverlappingButtons() {
        FakeSTouchDisplay display = new FakeSTouchDisplay();
        display.addButton(new Button((byte) 1, 10, 10, 50, 50));
        display.addButton(new Button((byte) 2, 40, 40, 60, 60)); // Overlaps with button 1

        assertNotNull(display.findButtonInObjectTree((byte) 1));
        assertNotNull(display.findButtonInObjectTree((byte) 2));
    }

    @Test
    void testClearScreen() {
        FakeSTouchDisplay display = new FakeSTouchDisplay();
        display.addButton(new Button((byte) 1, 10, 10, 50, 50));
        display.clearScreen();

        assertNull(display.findButtonInObjectTree((byte) 1)); // All buttons cleared
    }
}
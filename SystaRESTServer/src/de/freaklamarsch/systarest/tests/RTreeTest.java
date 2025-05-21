/*
* Copyright (c) 2025, The beep-projects contributors
* this file originated from https://github.com/beep-projects
* Do not remove the lines above.
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see https://www.gnu.org/licenses/
*
*/
package de.freaklamarsch.systarest.tests;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.freaklamarsch.systarest.FakeSTouchDisplay;
import de.freaklamarsch.systarest.FakeSTouchDisplay.DisplayButton;
import de.freaklamarsch.systarest.RTree;
import de.freaklamarsch.systarest.RTreeNode;
import de.freaklamarsch.systarest.STouchProtocol.Button;

class RTreeTest {

    @Test
    void testAddAndFindNode() {
        RTree tree = new RTree();
        RTreeNode node = new RTreeNode(null, 10, 10, 50, 50, null, null, null);
        assertTrue(tree.add(node));
        assertEquals(node, tree.findNodeAtPos(10, 10));
        assertNull(tree.findNodeAtPos(100, 100)); // Outside bounds
    }

    @Test
    void testAddOverlappingNodes() {
        RTree tree = new RTree();
        RTreeNode node1 = new RTreeNode(null, 10, 10, 50, 50, null, null, null);
        RTreeNode node2 = new RTreeNode(null, 40, 40, 60, 60, null, null, null); // Overlaps with node1
        assertTrue(tree.add(node1));
        assertTrue(tree.add(node2));
    }

    @Test
    void testRemoveNode() {
        RTree tree = new RTree();
        RTreeNode node = new RTreeNode(null, 10, 10, 50, 50, null, null, null);
        tree.add(node);
        tree.remove(node);
        assertNull(tree.findNodeAtPos(10, 10)); // Node removed
    }

	@Test
	void nodeContainsAnotherNode() {
		
		RTreeNode mainNode = new RTreeNode(null, 10, 20, 110, 220, null, null, null);
		RTreeNode containedNode = new RTreeNode(null, 50, 70, 60, 80, null, null, null);
		RTreeNode notContainedNode = new RTreeNode(null, 500, 700, 600, 800, null, null, null);
		RTreeNode overlappingNode = new RTreeNode(null, 0, 0, 50, 50, null, null, null);
		RTreeNode sameSizedNode = new RTreeNode(null, 10, 20, 110, 220, null, null, null);
		assertTrue(mainNode.contains(containedNode));
		assertFalse(mainNode.contains(notContainedNode));
		assertTrue(mainNode.contains(sameSizedNode));
		assertFalse(mainNode.contains(overlappingNode));
		
	}

	@Test
	void buildTree() {
		FakeSTouchDisplay ftd = new FakeSTouchDisplay();
		RTree tree = new RTree();
		DisplayButton button1 = ftd.new DisplayButton(new Button((byte) 1, 10, 10, 20, 20));
		RTreeNode node1 = new RTreeNode(button1, 10, 10, 20, 20, null, null, null);
		DisplayButton button2 = ftd.new DisplayButton(new Button((byte) 2, 15, 15, 17, 17));
		RTreeNode node2 = new RTreeNode(button2, 15, 15, 17, 17, null, null, null);
		DisplayButton button3 = ftd.new DisplayButton(new Button((byte) 3, 50, 50, 100, 100));
		RTreeNode node3 = new RTreeNode(button3, 50, 50, 100, 100, null, null, null);
		DisplayButton button4 = ftd.new DisplayButton(new Button((byte) 4, 15, 15, 75, 75));
		RTreeNode node4 = new RTreeNode(button4, 15, 15, 75, 75, null, null, null);
		DisplayButton button5 = ftd.new DisplayButton(new Button((byte) 5, 18, 18, 52, 52));
		RTreeNode node5 = new RTreeNode(button5, 18, 18, 52, 52, null, null, null);
		DisplayButton button6 = ftd.new DisplayButton(new Button((byte) 6, 18, 18, 52, 52));
		RTreeNode node6 = new RTreeNode(button6, 20, 20, 30, 30, null, null, null);
		DisplayButton button7 = ftd.new DisplayButton(new Button((byte) 7, 18, 18, 52, 52));
		RTreeNode node7 = new RTreeNode(button7, 21, 21, 31, 31, null, null, null);
		assertTrue(tree.add(node1));
		assertTrue(tree.add(node1));
		assertTrue(tree.add(node2));
		assertTrue(tree.add(node3));
		assertTrue(tree.add(node4));
		assertTrue(tree.add(node5));
		assertTrue(tree.add(node6));
		assertTrue(tree.add(node7));
		String createdTreeString = tree.toString();
		String expectedTreeString = "none\n"
	                              + " Button ID: 1 (10, 10)/(20, 20) [null/null]\n"
                                  + "  Button ID: 2 (15, 15)/(17, 17) [null/null]\n"
                                  + " Button ID: 3 (50, 50)/(100, 100) [null/null]\n"
                                  + " Button ID: 4 (15, 15)/(75, 75) [null/null]\n"
                                  + "  Button ID: 5 (18, 18)/(52, 52) [null/null]\n"
                                  + "   Button ID: 6 (18, 18)/(52, 52) [null/null]\n"
                                  + "   Button ID: 7 (18, 18)/(52, 52) [null/null]";
		assertEquals(expectedTreeString, createdTreeString);
		// tree should be
		//                root
		//       /          |         \ 
		//    button1    button3     button4
		//      /                       \
		//	    button2                button5
		//	                            /    \
        //                        button6     button7
		//remove node5, but with parent == null, to test if equal is ignoring the parent
		tree.remove(new RTreeNode(button5, 18, 18, 52, 52, null, null, null));
		tree.remove(new RTreeNode(button1, 10, 10, 20, 20, null, null, null));
		System.out.println(tree);
	}


}

package de.freaklamarsch.systarest;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.freaklamarsch.systarest.FakeSTouchDisplay.DisplayButton;
import de.freaklamarsch.systarest.FakeSTouchDisplay.DisplayCircle;
import de.freaklamarsch.systarest.FakeSTouchDisplay.DisplayRectangle;
import de.freaklamarsch.systarest.FakeSTouchDisplay.DisplayText;

/**
 * A spatial data structure for efficiently storing and querying geometric
 * objects. This implementation uses an R-Tree to organize objects such as
 * rectangles, circles, and text elements.
 */
public class RTree {
	/** The root node of the R-Tree. */
	private RTreeNode root;
	/**
	 * A set of all nodes currently stored in the tree. This obkjct is used for
	 * direct access to the stored elements, without traversing the R-Tree
	 */
	private Set<RTreeNode> elements = Collections.newSetFromMap(new ConcurrentHashMap<>());

	/**
	 * Constructs an empty R-Tree with a root node that spans the entire coordinate
	 * space.
	 */
	public RTree() {
		this.root = new RTreeNode(null, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
				null, null, null);
	}

	/**
	 * Adds a new node to the R-Tree.
	 *
	 * @param newNode the node to add
	 * @return {@code true} if the node was added successfully; {@code false} if the
	 *         node already exists
	 */
	public synchronized boolean add(RTreeNode newNode) {
		if (elements.contains(newNode)) {
			// return false;
			return true; // TODO check how this case should be handled, should the tree have been cleared
							// before?
		} else {
			elements.add(newNode);
			return add(root, newNode);
		}
	}

	/**
	 * Removes a node from the R-Tree.
	 *
	 * @param node the node to remove
	 */
	public synchronized void remove(RTreeNode node) {
		RTreeNode storedNode = null;
		for (Iterator<RTreeNode> it = elements.iterator(); it.hasNext();) {
			storedNode = it.next();
			if (storedNode.equals(node)) {
				RTreeNode parent = storedNode.parent;
				parent.children.remove(storedNode);
				if (!storedNode.children.isEmpty()) {
					// readd the children, so they don't become orphans
					for (RTreeNode child : storedNode.children) {
						add(root, child);
					}
					storedNode.children.clear();
				}
				// each object can only exits once in the tree
				break;
			}
		}
		if (storedNode != null) {
			elements.remove(storedNode);
		}

	}

	public synchronized String toString() {
		return (root == null) ? "Empty RTree" : root.toString();
	}

	/**
	 * TODO what is this search for?
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public Object findContainingObject(int x, int y) {
		return findContainingObject(root, x, y);
	}

	/**
	 * Searches for a node at the specified geometric position.
	 *
	 * @param x the x-coordinate of the position
	 * @param y the y-coordinate of the position
	 * @return the node at the specified position, or {@code null} if no node exists
	 *         at that position
	 */
	public synchronized RTreeNode findNodeAtPos(int x, int y) {
		return findNodeAtPos(root, x, y);
	}

	private synchronized RTreeNode findNodeAtPos(RTreeNode node, int x, int y) {
		if (node == null) {
			return null;
		}
		if (x == node.minX && y == node.minY) {
			return node;
		}
		if (node.contains(x, y)) {
			if (node.children.isEmpty()) {
				return null;
			}
			for (RTreeNode child : node.children) {
				if (child.contains(x, y)) {
					return findNodeAtPos(child, x, y);
				}
			}
		}
		return null;
	}

	/**
	 * TODO what is this search for?
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public synchronized Object findContainingObjectWithType(int x, int y, Class<?> type) {
		return findContainingObjectWithType(root, x, y, type);
	}

	private synchronized boolean add(RTreeNode node, RTreeNode newNode) {
		if (node == null) {
			return false;
		}
		if (node.contains(newNode)) {
			if (!node.children.isEmpty()) {
				for (RTreeNode child : node.children) {
					if (child.contains(newNode)) {
						add(child, newNode);
						return true;
					}
				}
			}
			newNode.parent = node;
			node.children.add(newNode);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * TODO what is this search for?
	 * 
	 * @param node
	 * @param x
	 * @param y
	 * @return
	 */
	private synchronized Object findContainingObject(RTreeNode node, int x, int y) {
		if (node == null) {
			return null;
		}
		if (node.children.isEmpty()) {
			if (x >= node.minX && x <= node.maxX && y >= node.minY && y <= node.maxY) {
				return node.object;
			} else {
				return null;
			}
		}
		for (RTreeNode child : node.children) {
			if (x >= child.minX && x <= child.maxX && y >= child.minY && y <= child.maxY) {
				return findContainingObject(child, x, y);
			}
		}
		return false;
	}

	private synchronized Object findContainingObjectWithType(RTreeNode node, int x, int y, Class<?> type) {
		if (node == null) {
			return null;
		}
		// Check if (x, y) lies within the boundaries of the current node
		if (x >= node.minX && x <= node.maxX && y >= node.minY && y <= node.maxY) {
			// Check if the object matches the target class
			if (node.object != null && type.isInstance(node.object)) {
				return node.object; // Return the object if it matches
			}
		}
		// Recursively search in children nodes
		for (RTreeNode child : node.children) {
			Object result = findContainingObjectWithType(child, x, y, type);
			if (result != null) {
				return result; // Stop if we found a match in the subtree
			}
		}
		return null; // No match found
	}

	public synchronized String getExcalidrawJSON() {
		String header = "{\n" + "  \"type\": \"excalidraw\",\n" + "  \"version\": 2,\n"
				+ "  \"source\": \"https://github.com/beep-projects/SystaPi\",\n" + "  \"elements\": [\n";
		String footer = "  ],\n" + "  \"appState\": {\n" + "    \"gridSize\": null,\n"
				+ "    \"viewBackgroundColor\": \"#ffffff\"\n" + "  },\n" + "  \"files\": {}\n" + "}\n";
		String objects = "";
		for (RTreeNode node : elements) {
			String elementJSON = getElementAsExcalidrawJSON(node.object);
			if ("".equals(objects)) {
				objects = elementJSON;
			} else {
				objects = objects.substring(0, objects.lastIndexOf("}") + 1) + ",\n" + elementJSON;
			}
		}
		return header + objects + footer;
	}

	private synchronized String getElementAsExcalidrawJSON(Object o) {
		if (o == null) {
			return "{}";
		}
		if (o instanceof DisplayButton) {
			DisplayButton btn = (DisplayButton) o;
			String json = "    {\n" + "      \"type\": \"rectangle\",\n" + "      \"x\": " + btn.button.xMin + ",\n"
					+ "      \"y\": " + btn.button.yMin + ",\n" + "      \"width\": "
					+ (btn.button.xMax - btn.button.xMin) + ",\n" + "      \"height\": "
					+ (btn.button.yMax - btn.button.yMin) + ",\n" + "      \"strokeColor\": \"#e03131\"\n" + "    },\n"
					+ "    {\n" + "      \"type\": \"text\",\n" + "      \"x\": " + (btn.button.xMin + 2) + ",\n"
					+ "      \"y\": " + btn.button.yMin + ",\n" + "      \"width\": " + countDigits(btn.button.id) * 20
					+ ",\n" + "      \"height\": 20,\n" + "      \"text\": \"" + btn.button.id + "\",\n"
					+ "      \"fontSize\": 20,\n" + "      \"strokeColor\": \"#e03131\"\n" + "    }\n";
			return json;
		}
		if (o instanceof DisplayRectangle) {
			DisplayRectangle rect = (DisplayRectangle) o;
			String json = "    {\n" + "      \"type\": \"rectangle\",\n" + "      \"x\": " + rect.rectangle.xMin + ",\n"
					+ "      \"y\": " + rect.rectangle.yMin + ",\n" + "      \"width\": "
					+ (rect.rectangle.xMax - rect.rectangle.xMin) + ",\n" + "      \"height\": "
					+ (rect.rectangle.yMax - rect.rectangle.yMin) + ",\n" + "      \"strokeColor\": \"#313131\"\n"
					+ "    }\n";
			return json;
		}
		if (o instanceof DisplayText) {
			DisplayText txt = (DisplayText) o;
			String json = "    {\n" + "      \"type\": \"text\",\n" + "      \"x\": " + txt.textXY.x + ",\n"
					+ "      \"y\": " + (txt.textXY.y - 10) + ",\n" + "      \"width\": "
					+ (txt.textXY.text.length() * 20) + ",\n" + "      \"height\": 20,\n" + "      \"text\": \""
					+ txt.textXY.text + "\",\n" + "      \"fontSize\": 20\n" + "    }\n";
			return json;
		}
		if (o instanceof DisplayCircle) {
			DisplayCircle c = (DisplayCircle) o;
			String json = "    {\n" + "      \"type\": \"ellipse\",\n" + "      \"x\": "
					+ (c.circle.x - c.circle.radius / 2) + ",\n" + "      \"y\": " + (c.circle.y - c.circle.radius / 2)
					+ ",\n" + "      \"width\": " + c.circle.radius + ",\n" + "      \"height\": " + c.circle.radius
					+ ",\n" + "      \"strokeColor\": \"#2f9e44\",\n" + "      \"backgroundColor\": \"#b2f2bb\",\n"
					+ "      \"fillStyle\": \"solid\"\n" + "    }\n";
			return json;
		}
		return "{}";
	}

	private synchronized int countDigits(int n) {
		if (n == 0) {
			return 1;
		}
		int count = 0;
		if (n < 0) {
			count++;
		}
		while (n != 0) {
			n = n / 10;
			count++;
		}
		return count;
	}

	public synchronized BufferedImage getImage() {
		BufferedImage image = new BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, 320, 240);
		for (RTreeNode node : elements) {
			drawElement(g, node.object, node.foregroundColor, node.backgroundColor);
		}
		g.dispose();
		return image;
	}

	private synchronized void drawElement(Graphics2D g, Object o, Color fgColor, Color bgColor) {
		if (o == null) {
			return;
		}
		if (o instanceof DisplayButton) {
			DisplayButton btn = (DisplayButton) o;
			g.setColor(bgColor);
			g.draw3DRect(btn.button.xMin, btn.button.yMin, btn.button.xMax - btn.button.xMin,
					btn.button.yMax - btn.button.yMin, false);
			g.setColor(fgColor);
			g.drawString("" + btn.button.id, btn.button.xMin + 5, btn.button.yMin + 20);
		}
		if (o instanceof DisplayRectangle) {
			DisplayRectangle rect = (DisplayRectangle) o;
			g.setColor(bgColor);
			g.drawRect(rect.rectangle.xMin, rect.rectangle.yMin, rect.rectangle.xMax - rect.rectangle.xMin,
					rect.rectangle.yMax - rect.rectangle.yMin);
		}
		if (o instanceof DisplayText) {
			DisplayText txt = (DisplayText) o;
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D rect = fm.getStringBounds(txt.textXY.text, g);
			int textWidth = fm.stringWidth(txt.textXY.text);
			int offsetX = 0;
			int offsetBoundsX = 0;
			switch (txt.alignment) {
			case DisplayText.ALIGN_NONE:
				offsetX = 0;
				offsetBoundsX = 0;
				break;
			case DisplayText.ALIGN_RIGHT:
				offsetX = 0;
				offsetBoundsX = 0;
				break;
			case DisplayText.ALIGN_CENTER:
				offsetX = -textWidth / 2;
				offsetBoundsX = (int) (-rect.getWidth() / 2);
				break;
			case DisplayText.ALIGN_LEFT:
				offsetX = -textWidth;
				offsetBoundsX = (int) -rect.getWidth();
				break;
			}
			g.setColor(bgColor);
			g.fillRect(txt.textXY.x + offsetBoundsX, txt.textXY.y - fm.getAscent(), (int) rect.getWidth(),
					(int) rect.getHeight());
			g.setColor(fgColor);
			g.drawString(txt.textXY.text, txt.textXY.x + offsetX, txt.textXY.y);
		}
		if (o instanceof DisplayCircle) {
			DisplayCircle c = (DisplayCircle) o;
			g.setColor(fgColor);
			g.fillOval(c.circle.x - c.circle.radius, c.circle.y - c.circle.radius, c.circle.radius, c.circle.radius);
		}
	}
}

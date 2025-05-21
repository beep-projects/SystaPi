package de.freaklamarsch.systarest;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a node in an R-Tree, which stores geometric objects such as
 * rectangles, circles, and text elements.
 */
public class RTreeNode {

	/**
	 * The object stored in this node (e.g., a rectangle, circle, or text element).
	 */
	Object object;
	/** The x-coordinate of the top-left corner of the node's bounding box. */
	int minX;
	/** The x-coordinate of the bottom-right corner of the node's bounding box. */
	int maxX;
	/** The y-coordinate of the top-left corner of the node's bounding box. */
	int minY;
	/** The y-coordinate of the bottom-right corner of the node's bounding box. */
	int maxY;
	/** The color of the foreground elements in this node. */
	Color foregroundColor;
	/** The color of the background elements in this node. */
	Color backgroundColor;
	/** The parent node of this node. */
	RTreeNode parent;
	/** The list of child nodes. */
	List<RTreeNode> children;

	/**
	 * Constructs an RTreeNode with the specified properties.
	 *
	 * @param parent          the parent node
	 * @param minX            the x-coordinate of the top-left corner of the
	 *                        bounding box
	 * @param minY            the y-coordinate of the top-left corner of the
	 *                        bounding box
	 * @param maxX            the x-coordinate of the bottom-right corner of the
	 *                        bounding box
	 * @param maxY            the y-coordinate of the bottom-right corner of the
	 *                        bounding box
	 * @param foregroundColor the foreground color
	 * @param backgroundColor the background color
	 * @param object          the object stored in the node
	 */
	public RTreeNode(Object object, int minX, int minY, int maxX, int maxY, Color foregroundColor,
			Color backgroundColor, RTreeNode parent) {
		this.object = object;
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		this.foregroundColor = foregroundColor;
		this.backgroundColor = backgroundColor;
		this.parent = parent;
		this.children = new ArrayList<RTreeNode>();
	}

	public boolean contains(RTreeNode otherNode) {
		return (this.minX <= otherNode.minX && this.maxX >= otherNode.maxX && this.minY <= otherNode.minY
				&& this.maxY >= otherNode.maxY);
	}

	public boolean contains(int x, int y) {
		return (this.minX <= x && this.maxX >= x && this.minY <= y && this.maxY >= y);
	}

	public String toString() {
		String string = (object == null) ? "none"
				: object.toString() + " [" + backgroundColor + "/" + foregroundColor + "]";
		int i = 1;
		RTreeNode aParent = parent;
		while (aParent != null) {
			i++;
			aParent = aParent.parent;
		}
		for (RTreeNode child : children) {
			string += "\n" + " ".repeat(i) + child.toString();
		}
		return string;
	}

	@Override
	public int hashCode() {
		return Objects.hash(minX, minY, maxX, maxY, object);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RTreeNode other = (RTreeNode) obj;
		// System.out.println(maxX + " == " + other.maxX + " && " + maxY + " == " +
		// other.maxY + " && " + minX + " == " + other.minX + " && " + minY + " == " +
		// other.minY
		// + " && OBJECTEQUALS:" + Objects.equals(object, other.object));

		return maxX == other.maxX && maxY == other.maxY && minX == other.minX && minY == other.minY
				&& Objects.equals(object, other.object);
	}

}

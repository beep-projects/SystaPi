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
package de.freaklamarsch.systarest;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import de.freaklamarsch.systarest.STouchProtocol.Button;
import de.freaklamarsch.systarest.STouchProtocol.Circle;
import de.freaklamarsch.systarest.STouchProtocol.Rectangle;
import de.freaklamarsch.systarest.STouchProtocol.TextXY;

/**
 * A mock implementation of a S-Touch display used, for interaction with a
 * S-Touch capable device.
 */
public class FakeSTouchDisplay {
	// most of these values are derived from reverse engineering the protocol
	// communication
	// they might only be correct for the software and hardware versions used for
	// this.
	byte[] checksumByte = { 0x56, 0x72, 0x41, (byte) (0xB5 & 0xFF) };
	int checksum = 0;
	int config = 0;
	int FONTS_AVAILABLE = 7;
	public int FONTS_USED = 3;
	public int RESSOURCE_ID = 0;
	int SYMBOLS_AVAILABLE = 17;
	public int SYMBOLS_USED = 3;

	// state variables
	private int x = -1;
	private int y = -1;
	private int button = -1;
	private ArrayList<DisplayText> texts = new ArrayList<>();
	private ArrayList<DisplayRectangle> rectangles = new ArrayList<>();
	private HashMap<Integer, DisplayButton> buttons = new HashMap<>();
	private RTree objectTree = new RTree();
	private Color foregroundColor = Color.BLACK;
	private Color backgroundColor = Color.WHITE;
	private int style = -1;

	public class DisplayButton {
		Button button;

		public DisplayButton(Button button) {
			this.button = button;
		}

		@Override
		public String toString() {
			return "Button ID: " + this.button.id + " (" + this.button.xMin + ", " + this.button.yMin + ")/("
					+ this.button.xMax + ", " + this.button.yMax + ")";
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.button.id, this.button.xMin, this.button.yMin, this.button.xMax, this.button.yMax);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if ((obj == null) || (getClass() != obj.getClass()))
				return false;
			DisplayButton other = (DisplayButton) obj;
			return this.button.id == other.button.id && this.button.xMin == other.button.xMin
					&& this.button.yMin == other.button.yMin && this.button.xMax == other.button.xMax
					&& this.button.yMax == other.button.yMax;
		}
	}

	public class DisplayCircle {
		Circle circle;

		public DisplayCircle(Circle circle) {
			this.circle = circle;
		}

		@Override
		public String toString() {
			return "Circle: (" + this.circle.x + ", " + this.circle.y + ") - " + this.circle.radius;
		}

		@Override
		public int hashCode() {
			return Objects.hash("circle", this.circle.x, this.circle.y, this.circle.radius);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if ((obj == null) || (getClass() != obj.getClass()))
				return false;
			DisplayCircle other = (DisplayCircle) obj;
			return this.circle.x == other.circle.x && this.circle.y == other.circle.y
					&& this.circle.radius == other.circle.radius;
		}
	}

	public class DisplayRectangle {
		Rectangle rectangle;

		public DisplayRectangle(Rectangle rectangle) {
			this.rectangle = rectangle;
		}

		@Override
		public String toString() {
			return "Rectangle (" + this.rectangle.xMin + ", " + this.rectangle.yMin + ")/(" + this.rectangle.xMax + ", "
					+ this.rectangle.yMax + ")";
		}

		@Override
		public int hashCode() {
			return Objects.hash("rectangle", this.rectangle.xMin, this.rectangle.yMin, this.rectangle.xMax,
					this.rectangle.yMax);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if ((obj == null) || (getClass() != obj.getClass()))
				return false;
			DisplayRectangle other = (DisplayRectangle) obj;
			return this.rectangle.xMin == other.rectangle.xMin && this.rectangle.yMin == other.rectangle.yMin
					&& this.rectangle.xMax == other.rectangle.xMax && this.rectangle.yMax == other.rectangle.yMax;
		}
	}

	public class DisplayText {

		public static final int ALIGN_NONE = 0;
		public static final int ALIGN_LEFT = 1;
		public static final int ALIGN_CENTER = 2;
		public static final int ALIGN_RIGHT = 3;

		TextXY textXY;
		int style;
		int alignment = 0;

		public DisplayText(TextXY text) {
			this.textXY = text;
		}

		public DisplayText(TextXY text, int style) {
			this.textXY = text;
			this.setStyle(style);
		}

		public void setStyle(int style) {
			this.style = style;
			switch (style) {
			case 131:
				alignment = ALIGN_RIGHT;
				break;
			case 147:
				alignment = ALIGN_LEFT;
				break;
			case 97:
				alignment = ALIGN_CENTER;
				break;
			default:
				alignment = ALIGN_NONE;
				break;
			}
		}

		public String getALignmentString() {
			switch (this.alignment) {
			case ALIGN_RIGHT:
				return "ALIGN_RIGHT";
			case ALIGN_LEFT:
				return "ALIGN_LEFT";
			case ALIGN_CENTER:
				return "ALIGN_CENTER";
			case ALIGN_NONE:
				return "ALIGN_NONE";
			default:
				return "unknown";
			}
		}

		@Override
		public String toString() {
			return "Text: " + textXY.text + " (" + textXY.x + ", " + textXY.y + ")" + "[" + getALignmentString() + "]";
		}

		@Override
		public int hashCode() {
			return Objects.hash(textXY.text, textXY.x, textXY.y, style, alignment);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if ((obj == null) || (getClass() != obj.getClass())) {
				return false;
			}
			DisplayText other = (DisplayText) obj;
			return this.textXY.text.equals(other.textXY.text) && this.textXY.x == other.textXY.x
					&& this.textXY.y == other.textXY.y && this.style == other.style
					&& this.alignment == other.alignment;
		}
	}

	public synchronized boolean delButton(int id) {
		if (id == -1 || id == 255) {
			// -1 is used as clear all flag
			clearScreen(); // TODO is it really delete all? Or only Buttons?
		} else {
			DisplayButton b = buttons.remove(id);
			if (b != null) {
				getObjectTree().remove(
						new RTreeNode(b, b.button.xMin, b.button.yMin, b.button.xMax, b.button.yMax, null, null, null));
			}
		}
		return true;
	}

	public synchronized void clearScreen() {
		clearTexts();
		clearButtons();
		clearRectangles();
		objectTree = new RTree();
	}

	public synchronized void clearButtons() {
		for (DisplayButton b : buttons.values()) {
			getObjectTree().remove(
					new RTreeNode(b, b.button.xMin, b.button.yMin, b.button.xMax, b.button.yMax, null, null, null));
		}
		buttons.clear();
	}

	public synchronized void clearTexts() {
		for (DisplayText t : texts) {
			getObjectTree().remove(new RTreeNode(t, t.textXY.x, t.textXY.y, t.textXY.x, t.textXY.y, null, null, null));
		}
		texts.clear();
	}

	public synchronized void clearRectangles() {
		for (DisplayRectangle r : rectangles) {
			getObjectTree().remove(new RTreeNode(r, r.rectangle.xMin, r.rectangle.yMin, r.rectangle.xMax,
					r.rectangle.yMax, null, null, null));
		}
		buttons.clear();
	}

	public int[] display_GetFrameBuffer() {
		return new int[] {};
	}

	public synchronized void setTouch(int id, int x, int y) {
		// remove existing touch marker
		getObjectTree().remove(new RTreeNode(new DisplayCircle(new Circle(this.x, this.y, 10)), this.x, this.y, this.x,
				this.y, null, null, null));
		// set new touch marker
		getObjectTree().add(
				new RTreeNode(new DisplayCircle(new Circle(x, y, 10)), x, y, x, y, Color.GREEN, Color.WHITE, null));
		this.x = x;
		this.y = y;
		selectButton(id);
	}

	public void setTouch(int x, int y) {
		setTouch(findButtonPressed(x, y), x, y);
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	/**
	 * @return the button
	 */
	public int getButton() {
		return button;
	}

	/**
	 * @return the button
	 */
	public synchronized int findButtonPressed(int x, int y) {
		if (x == -1 || y == -1) {
			return -1;
		}

		DisplayButton btn = (DisplayButton) getObjectTree().findContainingObjectWithType(x, y, DisplayButton.class);
		if (btn != null) {
			return btn.button.id;
		}
		return -1;
	}

	/**
	 * @param btn the button to set
	 */
	public void selectButton(int btn) {
		this.button = btn;
	}

	public synchronized boolean addButton(int id, int xMin, int yMin, int xMax, int yMax) {
		return addButton(new Button(id, xMin, yMin, xMax, yMax));
	}

	public synchronized boolean addButton(Button b) {
		DisplayButton button = new DisplayButton(b);
		buttons.put(b.id, button);
		return getObjectTree()
				.add(new RTreeNode(button, b.xMin, b.yMin, b.xMax, b.yMax, foregroundColor, backgroundColor, null));
	}

	public synchronized boolean setClick(int id) {
		return pushButton(id);
	}

	public synchronized boolean pushButton(int id) {
		DisplayButton button = buttons.get(id);
		if (button == null) {
			return false;
		}
		int x = (int) ((Math.random() * (button.button.xMax - button.button.xMin)) + button.button.xMin);
		int y = (int) ((Math.random() * (button.button.yMax - button.button.yMin)) + button.button.yMin);
		setTouch(id, x, y);
		return true;
	}

	public synchronized boolean touchText(String searchText) {
		DisplayText foundText = findTextInObjectTree(searchText);
		if (foundText != null) {
			// Generate a touch event at the upper-left corner of the text
			setTouch(foundText.textXY.x, foundText.textXY.y);
			return true;
		}
		return false; // Text not found
	}

	public FakeSTouchDisplay() {
		check();
	}

	public boolean setChecksum(int checksum) {
		this.checksumByte = ByteBuffer.allocate(4).putInt(checksum).array();
		if (!check()) {
			// checksum is not correct
			return false;
		}
		return true;
	}

	public boolean setChecksum(byte[] bArr) {
		this.checksumByte[0] = bArr[6];
		this.checksumByte[1] = bArr[7];
		this.checksumByte[2] = bArr[8];
		this.checksumByte[3] = bArr[9];
		if (!check()) {
			// checksum is not correct
			return false;
		}
		return true;
	}

	public boolean check() {
		this.checksum = 0;
		try {
			long l1 = checksumByte[3];
			long l2 = checksumByte[2];
			long l3 = checksumByte[1];
			long l4 = checksumByte[0];
			long l5 = 0L;
			byte b = 0;
			while (true) {
				int i = checksumByte.length;
				if (b < i) {
					long l6 = checksumByte[b + 3];
					long l7 = checksumByte[b + 2];
					long l8 = checksumByte[b + 1];
					i = checksumByte[b + 0];
					long l9 = i;
					b += 4;
					l5 = ((l6 & 0xFFL) << 24L | (l7 & 0xFFL) << 16L | (l8 & 0xFFL) << 8L | (l9 & 0xFFL) << 0L)
							+ (0xFFFFFFFFL & l5);
					continue;
				}
				if (l5 != ((l1 & 0xFFL) << 24L | (l2 & 0xFFL) << 16L | (l3 & 0xFFL) << 8L | (l4 & 0xFFL) << 0L)) {
					return false;
				}
				this.checksum = (int) l5;
				return true;
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			return false;
		}
	}

	public int getChecksum() {
		return this.checksum;
	}

	public boolean setConfig(int config) {
		this.config = config;
		this.config &= 1;
		return true;
	}

	// TODO remove this old method
	public boolean setConfig(byte[] bArr, int i2) {
		this.config = (int) (((((long) bArr[i2 + 6]) & 255) << 0) | ((((long) bArr[i2 + 7]) & 255) << 8)
				| ((((long) bArr[i2 + 8]) & 255) << 16) | ((255 & ((long) bArr[i2 + 9])) << 24));
		this.config &= 1;
		return true;
	}

	// TODO remove this old method
	/*
	 * public synchronized boolean addText(byte[] bytes, int cmdStartIdx, int
	 * cmdEndIdx) { ByteBuffer rcvBuf = ByteBuffer.wrap(bytes);
	 * rcvBuf.order(ByteOrder.LITTLE_ENDIAN); rcvBuf.position(cmdStartIdx); int x =
	 * rcvBuf.getShort(); int y = rcvBuf.getShort(); StringBuilder builder = new
	 * StringBuilder(); while (rcvBuf.hasRemaining()) { byte b = rcvBuf.get(); if (b
	 * == 0) { break; } builder.append((char) b); } String text = new
	 * String(builder.toString().getBytes(),
	 * Charset.forName("windows-1252")).trim(); return addText(new TextXY(x, y,
	 * text)); }
	 */

	public synchronized boolean addText(TextXY textXY) {
		int x = textXY.x;
		int y = textXY.y;
		RTreeNode nodeAtPos = this.objectTree.findNodeAtPos(x, y);
		if (nodeAtPos != null && nodeAtPos.object instanceof DisplayText) {
			texts.remove(nodeAtPos.object);
			this.objectTree.remove(nodeAtPos);
		}
		DisplayText text = new DisplayText(textXY);
		text.setStyle(style);
		getObjectTree().add(new RTreeNode(text, x, y, x, y, foregroundColor, backgroundColor, null));
		return texts.add(text);
	}

	public synchronized DisplayText findTextInObjectTree(String searchText) {
		for (DisplayText text : texts) {
			if (text.textXY.text.equals(searchText)) {
				return text;
			}
		}
		return null;
	}

	public synchronized DisplayButton findButtonInObjectTree(int id) {
		return buttons.get(id);
	}

	public int getFreeCommandSpace() {
		// fake some value
		// 1411
		// 8191
		return 8191;
	}

	public boolean setStyle(int style) {
		this.style = style;
		return true;
	}

	public boolean setForeColor(int colorCode16bit) {
		foregroundColor = getColorFrom16BitValue(colorCode16bit);
		return true;
	}

	public boolean setBackColor(int colorCode16bit) {
		backgroundColor = getColorFrom16BitValue(colorCode16bit);
		return true;
	}

	public boolean setForeColor(Color foreColor) {
		foregroundColor = foreColor;
		return true;
	}

	public boolean setBackColor(Color backColor) {
		backgroundColor = backColor;
		return true;
	}

	/**
	 * @param colorCode16bit
	 */
	private Color getColorFrom16BitValue(int colorCode16bit) {
		// colors are 16 bit values
		// these need to be converted to the 24-bit colors used by Java
		int red = (colorCode16bit >> 11) & 0x1F;
		int green = (colorCode16bit >> 5) & 0x3F;
		int blue = colorCode16bit & 0x1F;
		return new Color((red << 3) | (red >> 2), (green << 2) | (green >> 4), (blue << 3) | (blue >> 2));
	}

	public synchronized boolean drawRect(int xMin, int yMin, int xMax, int yMax) {
		return drawRect(new Rectangle(xMin, yMin, xMax, yMax));
	}

	public synchronized boolean drawRect(Rectangle rectangle) {
		DisplayRectangle rect = new DisplayRectangle(rectangle);
		// Rectangles are drawn on top of the current screen
		// This is like removing them from the display
		var iterator = texts.iterator();
		while (iterator.hasNext()) {
			DisplayText text = iterator.next();
			if (text.textXY.x >= rectangle.xMin && text.textXY.x <= rectangle.xMax && text.textXY.y >= rectangle.yMin
					&& text.textXY.y <= rectangle.yMax) {
				// Remove from objectTree
				objectTree.remove(new RTreeNode(text, text.textXY.x, text.textXY.y, text.textXY.x, text.textXY.y, null,
						null, null));
				// Remove from texts
				iterator.remove();
			}
		}

		getObjectTree().add(new RTreeNode(rect, rectangle.xMin, rectangle.yMin, rectangle.xMax, rectangle.yMax,
				foregroundColor, backgroundColor, null));
		return rectangles.add(rect);
	}

	public int getFonts() {
		return FONTS_AVAILABLE;
	}

	public int getSymbs() {
		return SYMBOLS_AVAILABLE;
	}

	public void printContent() {
		System.out.println(getObjectTree());
	}

	/**
	 * @return the objectTree
	 */
	public RTree getObjectTree() {
		return objectTree;
	}

	public synchronized String getContentAsExcalidrawJSON() {
		return getObjectTree().getExcalidrawJSON();
	}

	public synchronized BufferedImage getContentAsImage() {
		return getObjectTree().getImage();
	}

}

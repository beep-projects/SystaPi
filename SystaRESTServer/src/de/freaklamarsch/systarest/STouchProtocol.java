package de.freaklamarsch.systarest;

import java.awt.Color;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Contains all parsers for handling commands in the communication protocol used
 * by the S-Touch App. Each parser implements the {@link STouchCommandParser}
 * interface and is responsible for parsing and processing a specific command.
 * This class also provides a centralized map of parsers, the
 * {@link STouchCommand} enum, and utility methods like {@code getCmdLen}.
 */
public class STouchProtocol {

	/**
	 * Interface for parsing and processing commands.
	 */
	public interface ObjectReaderWriter<T> {
		/**
		 * Parses the command from the received buffer and returns a parsed object.
		 *
		 * @param buffer the buffer containing the data to read
		 * @return the parsed object
		 */
		T readFromBuffer(ByteBuffer buffer);

		/**
		 * Converts the parsed object into the reply buffer.
		 *
		 * @param buffer the buffer to write to
		 * @param object the object to convert
		 * @return the length of the bytes written to the buffer
		 */
		int writeToBuffer(ByteBuffer buffer, T object);
	}

	/**
	 * Enum used to map the command ids used by the S-Touch protocol to display
	 * functions
	 */
	public static enum STouchCommand {
		// commands with their corresponding IDs and lengths, -1 indicates variable
		// length that gets calculated by getCmdLen
		// S-Touch protocol commands, the ids must match the ones used in the
		// protocol for correct message parsing
		DISPLAY_SWITCHON(0, 0), DISPLAY_SWITCHOFF(1, 0), DISPLAY_SETSTYLE(2, 1), DISPLAY_SETINVERS(3, 1),
		DISPLAY_SETFORECOLOR(4, 2), DISPLAY_SETBACKCOLOR(5, 2), DISPLAY_SETFONTTYPE(6, 1), DISPLAY_SETPIXEL(7, 0),
		DISPLAY_MOVETO(8, 4), DISPLAY_LINETO(9, 4), DISPLAY_DRAWRECT(10, 8), DISPLAY_DRAWARC(11, 6),
		DISPLAY_DRAWROUNDRECT(12, 10), DISPLAY_DRAWSYMBOL(13, 6), DISPLAY_DELETESYMBOL(14, 6), DISPLAY_SETXY(15, 4),
		DISPLAY_PUTC(16, 1), DISPLAY_PRINT(17, -1), // Variable length
		DISPLAY_PRINTXY(18, -1), // Variable length
		DISPLAY_PUTCROT(19, 3), DISPLAY_PRINTROT(20, -1), // Variable length
		DISPLAY_CALIBRATETOUCH(21, 0), DISPLAY_SYNCNOW(22, 2), DISPLAY_SETBACKLIGHT(128, 1), DISPLAY_SETBUZZER(129, 2),
		DISPLAY_SETCLICK(130, 1), DISPLAY_SETBUTTON(144, 9), DISPLAY_DELBUTTON(145, 1), DISPLAY_SETTEMPOFFSETS(146, 4),
		SYSTEM_GETSYSTEM(240, 0), SYSTEM_GOSYSTEM(241, 0), SYSTEM_CLEARID(242, 0), SYSTEM_GETRESOURCEINFO(243, 0),
		SYSTEM_ERASERESOURCE(244, 0), SYSTEM_FLASHRESOURCE(245, 0), SYSTEM_ACTIVATERESOURCE(246, 0),
		SYSTEM_SETCONFIG(247, 0), SYSTEM_CLEARAPP(250, 0), SYSTEM_FLASHAPP(251, 0), SYSTEM_ACTIVATEAPP(252, 0),
		// internal commands added for parsing the received messages. The ids are
		// selected randomly
		TYPE_BYTE(1337, 1), TYPE_SHORT_INTEGER(1338, 2), TYPE_INTEGER(1339, 4), TYPE_MAC_ADDRESS(1340, 6),
		TYPE_COMMAND(1341, 1);

		private final int id;
		private final int length; // -1 indicates variable length

		private STouchCommand(final int id, final int length) {
			this.id = id;
			this.length = length;
		}

		public static STouchCommand getCmd(final int id) {
			for (STouchCommand cmd : values()) {
				if (cmd.id == id) {
					return cmd;
				}
			}
			return null;
		}

		/**
		 * Calculates the length of an STouchCommand, based on the bytes at the current
		 * position of the ByteBuffer. This is done without modifying the current
		 * position of the ByteBuffer outside this call.
		 * 
		 * @param buffer the received message buffer from the S-touch device
		 * @return the length of the command
		 */
		public int length(ByteBuffer buffer) {
			if (length == 0) {
				return 0;
			} else if (length > 0) {
				if (buffer != null && buffer.remaining() >= length) {
					return length;
				} else {
					return -1;
				}
			} else if (length == -1) {
				buffer.mark();
				try {
					switch (this) {
					case DISPLAY_PRINT:
						return findNext0(buffer, 0);
					case DISPLAY_PRINTXY:
						return findNext0(buffer, 4);
					case DISPLAY_PRINTROT:
						return findNext0(buffer, 2);
					default:
						return -1;
					}
				} finally {
					buffer.reset();
				}
			}
			return -1;
		}

		private static int findNext0(ByteBuffer buffer, int offset) {
			buffer.position(buffer.position() + offset);
			try {
				int distance = 0;
				while (buffer.position() < buffer.limit()) {
					if (buffer.get() == 0) {
						return distance + offset + 1;
					}
					distance++;
				}
				return -1;
			} finally {
				buffer.reset();
			}
		}
	}

	/**
	 * A map of command parsers, where the key is the command code and the value is
	 * the corresponding parser.
	 */
	private static final Map<STouchProtocol.STouchCommand, ObjectReaderWriter<?>> readerWriters = new HashMap<>();

	static {
		// DISPLAY_SWITCHON(0, 0)
		// DISPLAY_SWITCHOFF(1, 0)
		readerWriters.put(STouchCommand.DISPLAY_SETSTYLE, new ByteReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_SETINVERS, new ByteReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_SETFORECOLOR, new ColorReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_SETBACKCOLOR, new ColorReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_SETFONTTYPE, new ByteReaderWriter());
		// DISPLAY_SETPIXEL(7, 0)
		readerWriters.put(STouchCommand.DISPLAY_MOVETO, new CoordinatesReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_LINETO, new CoordinatesReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_DRAWRECT, new RectangleReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_DRAWARC, new CircleReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_DRAWROUNDRECT, new RoundRectangleReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_DRAWSYMBOL, new SymbolReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_DELETESYMBOL, new SymbolReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_SETXY, new CoordinatesReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_PUTC, new ByteReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_PRINT, new TextReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_PRINTXY, new TextXYReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_PUTCROT, new CharacterRotateReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_PRINTROT, new TextRotateReaderWriter());
		// DISPLAY_CALIBRATETOUCH(21, 0)
		readerWriters.put(STouchCommand.DISPLAY_SYNCNOW, new ShortIntegerReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_SETBACKLIGHT, new ByteReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_SETBUZZER, new ShortIntegerReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_SETCLICK, new ByteReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_SETBUTTON, new ButtonReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_DELBUTTON, new ByteReaderWriter());
		readerWriters.put(STouchCommand.DISPLAY_SETTEMPOFFSETS, new CoordinatesReaderWriter());
		// SYSTEM_GETSYSTEM(240, 0),
		// SYSTEM_GOSYSTEM(241, 0)
		// SYSTEM_CLEARID(242, 0)
		// SYSTEM_GETRESOURCEINFO(243, 0),
		// SYSTEM_ERASERESOURCE(244, 0)
		// SYSTEM_FLASHRESOURCE(245, 0)
		// SYSTEM_ACTIVATERESOURCE(246, 0),
		// SYSTEM_SETCONFIG(247, 0)
		// SYSTEM_CLEARAPP(250, 0)
		// SYSTEM_FLASHAPP(251, 0),
		// SYSTEM_ACTIVATEAPP(252, 0),
		readerWriters.put(STouchCommand.TYPE_BYTE, new ByteReaderWriter());
		readerWriters.put(STouchCommand.TYPE_SHORT_INTEGER, new ShortIntegerReaderWriter());
		readerWriters.put(STouchCommand.TYPE_INTEGER, new IntegerReaderWriter());
		readerWriters.put(STouchCommand.TYPE_MAC_ADDRESS, new MacAddressReaderWriter());
		readerWriters.put(STouchCommand.TYPE_COMMAND, new CommandReaderWriter());
	}

	/**
	 * Retrieves the parser for the given command code.
	 *
	 * @param cmd the STouchProtocol.STouchCommand
	 * @return the corresponding {@link ObjectReaderWriter}, or {@code null} if no
	 *         parser exists for the command
	 */
	public static ObjectReaderWriter<?> getParser(STouchCommand cmd) {
		return readerWriters.get(cmd);
	}

	public static Object read(STouchCommand cmd, ByteBuffer buffer) {
		if (cmd == null) {
			throw new IllegalArgumentException("STouchCommand cmd must not be null");
		}
		if (buffer == null) {
			throw new IllegalArgumentException("ByteBuffer buffer must not be null");
		}
		ObjectReaderWriter<?> parser = readerWriters.get(cmd);
		if (parser == null) {
			return null;
		}
		if (!buffer.hasRemaining()) {
			throw new BufferUnderflowException();
		}
		return parser.readFromBuffer(buffer);
	}

	public static <T> int write(STouchCommand cmd, ByteBuffer buffer, T object) {
		if (cmd == null) {
			throw new IllegalArgumentException("STouchCommand cmd must not be null");
		}
		if (buffer == null) {
			throw new IllegalArgumentException("ByteBuffer buffer must not be null");
		}
		if (object == null) {
			throw new IllegalArgumentException("Object must not be null");
		}
		if (!buffer.hasRemaining()) {
			throw new BufferOverflowException();
		}
		@SuppressWarnings("unchecked")
		ObjectReaderWriter<T> parser = (ObjectReaderWriter<T>) readerWriters.get(cmd);
		if (parser == null) {
			throw new IllegalArgumentException("No writer registered for command: " + cmd);
		}
		return parser.writeToBuffer(buffer, object);
	}

	/********************************************************
	 ********************************************************
	 * PUT OBJECT PARSERS INTO THIS SECTION *****************
	 ********************************************************
	 * parsers should be private **************************** custom objects should
	 * be defined in the next section *
	 ********************************************************
	 *******************************************************/

	private static class ByteReaderWriter implements ObjectReaderWriter<Integer> {
		@Override
		public Integer readFromBuffer(ByteBuffer buffer) {
			if (buffer.remaining() < 1) {
				throw new BufferUnderflowException();
			}
			return buffer.get() & 0xFF; // Ensures the byte is treated as an unsigned value
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, Integer object) {
			object = object & 0xFF;
			if (object < 0 || object > 255) {
				throw new IllegalArgumentException(
						"Object value must be within the range of a single byte (0-255): " + object);
			}
			if (buffer.remaining() < 1) {
				throw new BufferOverflowException();
			}
			buffer.put(object.byteValue());
			return 1; // Number of bytes written
		}
	}

	private static class ShortIntegerReaderWriter implements ObjectReaderWriter<Integer> {
		@Override
		public Integer readFromBuffer(ByteBuffer buffer) {
			if (buffer.remaining() < 2) {
				throw new BufferUnderflowException();
			}
			return (int) buffer.getShort() & 0xFFFF; // Ensures the short is treated as an unsigned value
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, Integer object) {
			object = object & 0xFFFF;
			// Validate that the integer fits in 16 bits (range: 0 to 65,535)
			if (object < 0 || object > 0xFFFF) {
				throw new IllegalArgumentException("Object value must be within 16-bit range (0-65,535): " + object);
			}
			if (buffer.remaining() < 2) {
				throw new BufferOverflowException();
			}
			buffer.putShort(object.shortValue());
			return 2; // Length of the reply
		}
	}

	private static class IntegerReaderWriter implements ObjectReaderWriter<Integer> {
		@Override
		public Integer readFromBuffer(ByteBuffer buffer) {
			if (buffer.remaining() < 4) {
				throw new BufferUnderflowException();
			}
			/*
			 * TODO check if there is an endianess issue int highByte = (buffer.get() &
			 * 0xff) << 8; int lowByte = (buffer.get() & 0xff) << 0; return (highByte |
			 * lowByte);
			 */
			return (int) buffer.getInt();
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, Integer object) {
			// Validate that the integer fits in 32 bits (range: -32,768 to 32,767)
			if (object < Integer.MIN_VALUE || object > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Object value must be within 32-bit range: " + object);
			}
			if (buffer.remaining() < 4) {
				throw new BufferOverflowException();
			}

			buffer.putInt(object.intValue());
			return 4; // Length of the reply
		}
	}

	public static class MacAddressReaderWriter implements ObjectReaderWriter<MacAddress> {

		@Override
		public MacAddress readFromBuffer(ByteBuffer buffer) {
			if (buffer.remaining() < 6) {
				throw new BufferUnderflowException();
			}
			byte[] address = new byte[6];
			buffer.get(address);
			return new MacAddress(address);
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, MacAddress macAddress) {
			if (macAddress == null || macAddress.getAddress().length != 6) {
				throw new IllegalArgumentException("MAC address must be exactly 6 bytes long.");
			}
			if (buffer.remaining() < 6) {
				throw new BufferOverflowException();
			}
			buffer.put(macAddress.getAddress());
			return 6;
		}
	}

	private static class CommandReaderWriter implements ObjectReaderWriter<STouchCommand> {
		private final ByteReaderWriter byteReaderWriter = new ByteReaderWriter();

		@Override
		public STouchCommand readFromBuffer(ByteBuffer buffer) {
			Integer id = byteReaderWriter.readFromBuffer(buffer);
			STouchCommand command = STouchCommand.getCmd(id);
			if (command == null) {
				// TODO decide how this case should be handled
				// throw new IllegalArgumentException("Invalid command id read from buffer: " +
				// id);
			}
			return command;
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, STouchCommand command) {
			// Use ByteReaderWriter to write the command id as a single byte
			byteReaderWriter.writeToBuffer(buffer, command.id);
			return 1;
		}
	}

	private static class ColorReaderWriter implements ObjectReaderWriter<Color> {
		private final ShortIntegerReaderWriter shortIntegerReaderWriter = new ShortIntegerReaderWriter();

		@Override
		public Color readFromBuffer(ByteBuffer buffer) {
			return getColorFrom16BitValue(shortIntegerReaderWriter.readFromBuffer(buffer));
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, Color color) {
			// Use ByteReaderWriter to write the command id as a single byte
			shortIntegerReaderWriter.writeToBuffer(buffer, get16BitValueFromColor(color));
			return 2;
		}

		private Color getColorFrom16BitValue(int colorCode16bit) {
			// colors are 16 bit values
			// these need to be converted to the 24-bit colors used by Java
			int red = (colorCode16bit >> 11) & 0x1F;
			int green = (colorCode16bit >> 5) & 0x3F;
			int blue = colorCode16bit & 0x1F;
			return new Color((red << 3) | (red >> 2), (green << 2) | (green >> 4), (blue << 3) | (blue >> 2));
		}

		private int get16BitValueFromColor(Color color) {
			// Extract RGB components
			int red = color.getRed(); // 0-255
			int green = color.getGreen(); // 0-255
			int blue = color.getBlue(); // 0-255
			// Scale each component to fit into 5 bits (0-31) or 6 bits (0-63)
			int red5 = red >> 3; // Scale to 0-31
			int green5 = green >> 2; // Scale to 0-63
			int blue5 = blue >> 3; // Scale to 0-31
			return (red5 << 10) | (green5 << 5) | blue5;
		}
	}

	private static class CoordinatesReaderWriter implements ObjectReaderWriter<Coordinates> {
		private final ShortIntegerReaderWriter shortIntegerReaderWriter = new ShortIntegerReaderWriter();

		@Override
		public Coordinates readFromBuffer(ByteBuffer buffer) {
			int x = shortIntegerReaderWriter.readFromBuffer(buffer);
			int y = shortIntegerReaderWriter.readFromBuffer(buffer);
			return new Coordinates(x, y);
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, Coordinates coordinates) {
			// Use ByteReaderWriter to write the command id as a single byte
			shortIntegerReaderWriter.writeToBuffer(buffer, coordinates.x);
			shortIntegerReaderWriter.writeToBuffer(buffer, coordinates.y);
			return 4;
		}
	}

	private static class RectangleReaderWriter implements ObjectReaderWriter<Rectangle> {
		private final CoordinatesReaderWriter coordinatesReaderWriter = new CoordinatesReaderWriter();

		@Override
		public Rectangle readFromBuffer(ByteBuffer buffer) {
			Coordinates upperLeft = coordinatesReaderWriter.readFromBuffer(buffer);
			Coordinates lowerRight = coordinatesReaderWriter.readFromBuffer(buffer);
			return new Rectangle(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, Rectangle rectangle) {
			// Use ByteReaderWriter to write the command id as a single byte
			coordinatesReaderWriter.writeToBuffer(buffer, new Coordinates(rectangle.xMin, rectangle.yMin));
			coordinatesReaderWriter.writeToBuffer(buffer, new Coordinates(rectangle.xMax, rectangle.yMax));
			return 8;
		}
	}

	private static class RoundRectangleReaderWriter implements ObjectReaderWriter<RoundRectangle> {
		private final CoordinatesReaderWriter coordinatesReaderWriter = new CoordinatesReaderWriter();
		private final ShortIntegerReaderWriter shortIntegerReaderWriter = new ShortIntegerReaderWriter();

		@Override
		public RoundRectangle readFromBuffer(ByteBuffer buffer) {
			Coordinates upperLeft = coordinatesReaderWriter.readFromBuffer(buffer);
			Coordinates lowerRight = coordinatesReaderWriter.readFromBuffer(buffer);
			int curvature = shortIntegerReaderWriter.readFromBuffer(buffer);
			return new RoundRectangle(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y, curvature);
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, RoundRectangle roundRectangle) {
			// Use ByteReaderWriter to write the command id as a single byte
			coordinatesReaderWriter.writeToBuffer(buffer, new Coordinates(roundRectangle.xMin, roundRectangle.yMin));
			coordinatesReaderWriter.writeToBuffer(buffer, new Coordinates(roundRectangle.xMax, roundRectangle.yMax));
			shortIntegerReaderWriter.writeToBuffer(buffer, roundRectangle.curvature);
			return 10;
		}
	}

	private static class CircleReaderWriter implements ObjectReaderWriter<Circle> {
		private final CoordinatesReaderWriter coordinatesReaderWriter = new CoordinatesReaderWriter();
		private final ShortIntegerReaderWriter shortIntegerReaderWriter = new ShortIntegerReaderWriter();

		@Override
		public Circle readFromBuffer(ByteBuffer buffer) {
			Coordinates center = coordinatesReaderWriter.readFromBuffer(buffer);
			int radius = shortIntegerReaderWriter.readFromBuffer(buffer);
			return new Circle(center.x, center.y, radius);
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, Circle circle) {
			coordinatesReaderWriter.writeToBuffer(buffer, new Coordinates(circle.x, circle.y));
			shortIntegerReaderWriter.writeToBuffer(buffer, circle.radius);
			return 6;
		}
	}

	private static class SymbolReaderWriter implements ObjectReaderWriter<Symbol> {
		private final CoordinatesReaderWriter coordinatesReaderWriter = new CoordinatesReaderWriter();
		private final ShortIntegerReaderWriter shortIntegerReaderWriter = new ShortIntegerReaderWriter();

		@Override
		public Symbol readFromBuffer(ByteBuffer buffer) {
			Coordinates upperLeft = coordinatesReaderWriter.readFromBuffer(buffer);
			int id = shortIntegerReaderWriter.readFromBuffer(buffer);
			return new Symbol(upperLeft.x, upperLeft.y, id);
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, Symbol symbol) {
			coordinatesReaderWriter.writeToBuffer(buffer, new Coordinates(symbol.x, symbol.y));
			shortIntegerReaderWriter.writeToBuffer(buffer, symbol.id);
			return 6;
		}
	}

	private static class TextReaderWriter implements ObjectReaderWriter<String> {

		@Override
		public String readFromBuffer(ByteBuffer buffer) {
			buffer.mark();
			int length = 0;
			while (buffer.hasRemaining()) {
				byte b = buffer.get();
				if (b == 0) {
					break;
				}
				length++;
			}
			buffer.reset();
			byte[] bytes = new byte[length];
			buffer.get(bytes, 0, length);
			buffer.get();
			return new String(bytes, Charset.forName("windows-1252")).trim();
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, String text) {
			byte[] stringBytes = text.getBytes(Charset.forName("windows-1252"));
			buffer.put(stringBytes);
			buffer.put((byte) 0);
			return text.length() + 1;
		}
	}

	private static class TextXYReaderWriter implements ObjectReaderWriter<TextXY> {
		CoordinatesReaderWriter coordinatesReaderWriter = new CoordinatesReaderWriter();
		TextReaderWriter textReaderWriter = new TextReaderWriter();

		@Override
		public TextXY readFromBuffer(ByteBuffer buffer) {
			Coordinates coordinates = coordinatesReaderWriter.readFromBuffer(buffer);
			String text = textReaderWriter.readFromBuffer(buffer);
			return new TextXY(coordinates.x, coordinates.y, text);
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, TextXY textXY) {
			coordinatesReaderWriter.writeToBuffer(buffer, new Coordinates(textXY.x, textXY.y));
			textReaderWriter.writeToBuffer(buffer, textXY.text);
			return 4 + textXY.text.length() + 1;
		}
	}

	private static class TextRotateReaderWriter implements ObjectReaderWriter<TextRotate> {
		private final ShortIntegerReaderWriter shortIntegerReaderWriter = new ShortIntegerReaderWriter();
		TextReaderWriter textReaderWriter = new TextReaderWriter();

		@Override
		public TextRotate readFromBuffer(ByteBuffer buffer) {
			int rotationAngle = shortIntegerReaderWriter.readFromBuffer(buffer);
			String text = textReaderWriter.readFromBuffer(buffer);
			return new TextRotate(rotationAngle, text);
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, TextRotate texRotate) {
			shortIntegerReaderWriter.writeToBuffer(buffer, texRotate.rotationAngle);
			textReaderWriter.writeToBuffer(buffer, texRotate.text);
			return 2 + texRotate.text.length() + 1;
		}
	}

	private static class CharacterRotateReaderWriter implements ObjectReaderWriter<TextRotate> {
		private final ShortIntegerReaderWriter shortIntegerReaderWriter = new ShortIntegerReaderWriter();

		@Override
		public TextRotate readFromBuffer(ByteBuffer buffer) {
			int rotationAngle = shortIntegerReaderWriter.readFromBuffer(buffer);
			StringBuilder builder = new StringBuilder();
			byte b = buffer.get();
			builder.append((char) b);
			String text = new String(builder.toString().getBytes(), Charset.forName("windows-1252")).trim();
			return new TextRotate(rotationAngle, text);
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, TextRotate texRotate) {
			shortIntegerReaderWriter.writeToBuffer(buffer, texRotate.rotationAngle);
			byte[] stringBytes = texRotate.text.getBytes(Charset.forName("windows-1252"));
			buffer.put(stringBytes[0]);
			return 3;
		}
	}

	private static class ButtonReaderWriter implements ObjectReaderWriter<Button> {
		private final ByteReaderWriter byteReaderWriter = new ByteReaderWriter();
		private final CoordinatesReaderWriter coordinatesReaderWriter = new CoordinatesReaderWriter();

		@Override
		public Button readFromBuffer(ByteBuffer buffer) {
			int id = byteReaderWriter.readFromBuffer(buffer);
			Coordinates upperLeft = coordinatesReaderWriter.readFromBuffer(buffer);
			Coordinates lowerRight = coordinatesReaderWriter.readFromBuffer(buffer);
			return new Button(id, upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);
		}

		@Override
		public int writeToBuffer(ByteBuffer buffer, Button button) {
			byteReaderWriter.writeToBuffer(buffer, button.id);
			coordinatesReaderWriter.writeToBuffer(buffer, new Coordinates(button.xMin, button.yMin));
			coordinatesReaderWriter.writeToBuffer(buffer, new Coordinates(button.xMax, button.yMax));
			return 9;
		}
	}

	/***********************************************
	 ***********************************************
	 * PUT COMMUNICATION OBJECTS INTO THIS SECTION *
	 ***********************************************
	 **********************************************/

	public static class MacAddress {
		private final byte[] address;

		public MacAddress(byte[] address) {
			if (address == null || address.length != 6) {
				throw new IllegalArgumentException("MAC address must be exactly 6 bytes long.");
			}
			this.address = address.clone();
		}

		public byte[] getAddress() {
			return address.clone();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < address.length; i++) {
				if (i > 0) {
					sb.append(':');
				}
				sb.append(String.format("%02x", address[i]));
			}
			return sb.toString();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof MacAddress) {
				return Objects.deepEquals(this.address, ((MacAddress) obj).address);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(address);
		}
	}

	public static class Coordinates {
		public final int x;
		public final int y;

		public Coordinates(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return "Coordinates { \"x\": " + x + ", \"y\": " + y + " }";
		}
	}

	public static class Rectangle {
		public final int xMin;
		public final int yMin;
		public final int xMax;
		public final int yMax;

		public Rectangle(int xMin, int yMin, int xMax, int yMax) {
			this.xMin = xMin;
			this.yMin = yMin;
			this.xMax = xMax;
			this.yMax = yMax;
		}

		@Override
		public String toString() {
			return "Rectangle { \"xMin\": " + xMin + ", \"yMin\": " + yMin + ", \"xMax\": " + xMax + ", \"yMax\": "
					+ yMax + " }";
		}
	}

	public static class RoundRectangle {
		public final int xMin;
		public final int yMin;
		public final int xMax;
		public final int yMax;
		public final int curvature;

		public RoundRectangle(int xMin, int yMin, int xMax, int yMax, int curvature) {
			this.xMin = xMin;
			this.yMin = yMin;
			this.xMax = xMax;
			this.yMax = yMax;
			this.curvature = curvature;
		}

		@Override
		public String toString() {
			return "RoundRectangle { \"xMin\": " + xMin + ", \"yMin\": " + yMin + ", \"xMax\": " + xMax + ", \"yMax\": "
					+ yMax + ", \"curvature\": " + curvature + " }";
		}
	}

	public static class Circle {
		public final int x;
		public final int y;
		public final int radius;

		public Circle(int x, int y, int radius) {
			this.x = x;
			this.y = y;
			this.radius = radius;
		}

		@Override
		public String toString() {
			return "Circle { \"x\": " + x + ", \"y\": " + y + ", \"radius\": " + radius + " }";
		}
	}

	public static class Symbol {
		public final int x;
		public final int y;
		public final int id;

		public Symbol(int x, int y, int id) {
			this.x = x;
			this.y = y;
			this.id = id;
		}

		@Override
		public String toString() {
			return "Symbol { \"x\": " + x + ", \"y\": " + y + ", \"id\": " + id + " }";
		}
	}

	public static class TextXY {
		public final int x;
		public final int y;
		public final String text;

		public TextXY(int x, int y, String text) {
			this.x = x;
			this.y = y;
			this.text = text;
		}

		@Override
		public String toString() {
			return "TextXY { \"x\": " + x + ", \"y\": " + y + ", \"text\": \"" + text + "\" }";
		}
	}

	public static class TextRotate {
		public final int rotationAngle;
		public final String text;

		public TextRotate(int rotationAngle, String text) {
			this.rotationAngle = rotationAngle;
			this.text = text;
		}

		@Override
		public String toString() {
			return "TextRotate { \"rotationAngle\": " + rotationAngle + ", \"text\": \"" + text + "\" }";
		}
	}

	public static class Button {
		public final int id;
		public final int xMin;
		public final int yMin;
		public final int xMax;
		public final int yMax;

		public Button(int id, int xMin, int yMin, int xMax, int yMax) {
			this.id = id;
			this.xMin = xMin;
			this.yMin = yMin;
			this.xMax = xMax;
			this.yMax = yMax;
		}

		@Override
		public String toString() {
			return "Button { \"id\": " + id + ", \"xMin\": " + xMin + ", \"yMin\": " + yMin + ", \"xMax\": " + xMax
					+ ", \"yMax\": " + yMax + " }";
		}
	}
}
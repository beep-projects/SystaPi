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
 * Defines the S-Touch communication protocol, including command structures,
 * data types, and parsers for reading and writing protocol messages.
 * <p>
 * This class serves as a central registry for S-Touch commands ({@link STouchCommand})
 * and their associated reader/writer implementations ({@link ObjectReaderWriter}).
 * It also provides static utility methods for reading and writing command data
 * from/to {@link ByteBuffer} instances, respecting the protocol's data formats
 * (e.g., byte order, string encoding).
 * </p>
 * <p>
 * The S-Touch protocol involves various commands for controlling a display,
 * handling touch input, and managing system-level operations. Each command can have
 * different parameters and data structures, which are encapsulated by the inner
 * data classes (e.g., {@link Coordinates}, {@link Rectangle}, {@link Button}) and
 * handled by specific {@code ObjectReaderWriter} implementations.
 * </p>
 * <p>
 * Note: The protocol uses Little Endian byte order for multi-byte numerical values,
 * and "windows-1252" character encoding for strings. This class and its components
 * aim to adhere to these specifications.
 * </p>
 */
public class STouchProtocol {

    /**
     * Defines an interface for reading an object of type {@code T} from a ByteBuffer
     * and writing an object of type {@code T} to a ByteBuffer, according to the
     * S-Touch protocol specifications for a particular data type or command structure.
     *
     * @param <T> The type of the object to be read or written.
     */
    public interface ObjectReaderWriter<T> {
        /**
         * Reads data from the given {@link ByteBuffer} at its current position and
         * constructs an object of type {@code T}.
         * The buffer's position is advanced by the number of bytes read.
         *
         * @param buffer The buffer containing the S-Touch protocol data to read.
         *               The buffer is assumed to be in Little Endian byte order.
         * @return The parsed object of type {@code T}.
         * @throws BufferUnderflowException if there are not enough bytes remaining in the buffer
         *                                  to read the expected data structure.
         * @throws IllegalArgumentException if the data in the buffer is malformed or invalid
         *                                  for the expected type {@code T}.
         */
        T readFromBuffer(ByteBuffer buffer);

        /**
         * Writes the given object of type {@code T} to the provided {@link ByteBuffer}
         * at its current position, formatted according to the S-Touch protocol.
         * The buffer's position is advanced by the number of bytes written.
         *
         * @param buffer The buffer to write the S-Touch protocol data to.
         *               The buffer should be configured for Little Endian byte order by the caller.
         * @param object The object of type {@code T} to write.
         * @return The number of bytes written to the buffer.
         * @throws BufferOverflowException if there is not enough space remaining in the buffer
         *                                 to write the object's data.
         * @throws IllegalArgumentException if the provided object is null or invalid for serialization.
         */
        int writeToBuffer(ByteBuffer buffer, T object);
    }

    /**
     * Enumerates all known S-Touch protocol commands, including their numeric ID
     * and fixed data length. For commands with variable data length (e.g., strings),
     * the length is specified as -1, and actual length is determined dynamically.
     * <p>
     * This enum also includes internal "type" commands (e.g., {@code TYPE_BYTE},
     * {@code TYPE_SHORT_INTEGER}) used by the protocol parsing/writing utilities
     * to handle primitive data types.
     * </p>
     */
    public static enum STouchCommand {
        // S-Touch protocol display and system commands.
        // The format is (commandId, fixedDataLength).
        // A length of -1 indicates variable length, determined by content (e.g., null-terminated string).
        // A length of 0 indicates no parameters.

        /** Switches the display on. No parameters. */
        DISPLAY_SWITCHON(0, 0),
        /** Switches the display off. No parameters. */
        DISPLAY_SWITCHOFF(1, 0),
        /** Sets the display style. Parameter: 1 byte (style_id). */
        DISPLAY_SETSTYLE(2, 1),
        /** Sets inverse display mode. Parameter: 1 byte (0=off, 1=on). */
        DISPLAY_SETINVERS(3, 1),
        /** Sets the foreground color. Parameter: 2 bytes (16-bit color value). */
        DISPLAY_SETFORECOLOR(4, 2),
        /** Sets the background color. Parameter: 2 bytes (16-bit color value). */
        DISPLAY_SETBACKCOLOR(5, 2),
        /** Sets the font type. Parameter: 1 byte (font_id). */
        DISPLAY_SETFONTTYPE(6, 1),
        /** Sets a single pixel (not fully implemented in display simulation). No parameters, assumes coordinates set by SETXY. */
        DISPLAY_SETPIXEL(7, 0),
        /** Moves the drawing cursor to specified coordinates. Parameters: 2 x 2 bytes (x, y). */
        DISPLAY_MOVETO(8, 4),
        /** Draws a line from current cursor position to specified coordinates. Parameters: 2 x 2 bytes (x, y). */
        DISPLAY_LINETO(9, 4),
        /** Draws a rectangle. Parameters: 4 x 2 bytes (x1, y1, x2, y2). */
        DISPLAY_DRAWRECT(10, 8),
        /** Draws an arc/circle. Parameters: 3 x 2 bytes (centerX, centerY, radius). */
        DISPLAY_DRAWARC(11, 6),
        /** Draws a rounded rectangle. Parameters: 5 x 2 bytes (x1, y1, x2, y2, curvature). */
        DISPLAY_DRAWROUNDRECT(12, 10),
        /** Draws a predefined symbol. Parameters: 3 x 2 bytes (x, y, symbol_id). */
        DISPLAY_DRAWSYMBOL(13, 6),
        /** Deletes/clears a symbol area (not fully implemented in display simulation). Parameters: 3 x 2 bytes (x, y, symbol_id). */
        DISPLAY_DELETESYMBOL(14, 6),
        /** Sets the current X, Y coordinates for subsequent text/pixel operations. Parameters: 2 x 2 bytes (x, y). */
        DISPLAY_SETXY(15, 4),
        /** Puts a single character at the current cursor position. Parameter: 1 byte (char). */
        DISPLAY_PUTC(16, 1),
        /** Prints a null-terminated string at the current cursor position. Parameter: variable length string. */
        DISPLAY_PRINT(17, -1),
        /** Prints a null-terminated string at specified X, Y coordinates. Parameters: 2 x 2 bytes (x, y) + variable length string. */
        DISPLAY_PRINTXY(18, -1),
        /** Puts a single character with rotation. Parameters: 2 bytes (rotation_angle) + 1 byte (char). */
        DISPLAY_PUTCROT(19, 3),
        /** Prints a null-terminated string with rotation. Parameters: 2 bytes (rotation_angle) + variable length string. */
        DISPLAY_PRINTROT(20, -1),
        /** Initiates touch calibration sequence (not relevant for fake display). No parameters. */
        DISPLAY_CALIBRATETOUCH(21, 0),
        /** Synchronizes command counters. Parameter: 2 bytes (sync_id). */
        DISPLAY_SYNCNOW(22, 2),
        /** Sets display backlight intensity. Parameter: 1 byte (intensity). */
        DISPLAY_SETBACKLIGHT(128, 1),
        /** Controls the buzzer. Parameters: 2 bytes (duration/frequency). */
        DISPLAY_SETBUZZER(129, 2),
        /** Sets click/touch feedback behavior. Parameter: 1 byte (mode). */
        DISPLAY_SETCLICK(130, 1),
        /** Defines a touch button area. Parameters: 1 byte (id) + 4 x 2 bytes (x1, y1, x2, y2). */
        DISPLAY_SETBUTTON(144, 9),
        /** Deletes a defined touch button. Parameter: 1 byte (id). */
        DISPLAY_DELBUTTON(145, 1),
        /** Sets temperature offsets (not fully implemented in display simulation). Parameters: 2 x 2 bytes (offset1, offset2). */
        DISPLAY_SETTEMPOFFSETS(146, 4),

        // System commands
        /** Requests system information. No parameters. */
        SYSTEM_GETSYSTEM(240, 0),
        /** Triggers a system reset/go (specific behavior depends on device). No parameters. */
        SYSTEM_GOSYSTEM(241, 0),
        /** Clears system ID (specific behavior depends on device). No parameters. */
        SYSTEM_CLEARID(242, 0),
        /** Requests resource information (e.g., fonts, symbols). No parameters. */
        SYSTEM_GETRESOURCEINFO(243, 0),
        /** Erases a resource from flash. No parameters (resource ID might be implicit or set prior). */
        SYSTEM_ERASERESOURCE(244, 0),
        /** Flashes/writes a resource. No parameters (data usually follows in subsequent packets). */
        SYSTEM_FLASHRESOURCE(245, 0),
        /** Activates a flashed resource. No parameters (resource ID might be implicit). */
        SYSTEM_ACTIVATERESOURCE(246, 0), // Parameter (checksum) handled in processCommands
        /** Sets system configuration flags. No parameters (config data usually follows). */
        SYSTEM_SETCONFIG(247, 0),      // Parameter (config flags) handled in processCommands
        /** Clears application from flash. No parameters. */
        SYSTEM_CLEARAPP(250, 0),
        /** Flashes/writes application. No parameters (data usually follows). */
        SYSTEM_FLASHAPP(251, 0),
        /** Activates flashed application. No parameters. */
        SYSTEM_ACTIVATEAPP(252, 0),

        // Internal pseudo-commands used for type-safe reading/writing of primitive data types.
        // These do not correspond to actual S-Touch protocol command IDs sent over the wire as primary commands,
        // but represent the data types of parameters for other commands.
        /** Represents a single byte data type. */
        TYPE_BYTE(1337, 1),
        /** Represents a 2-byte short integer data type (Little Endian). */
        TYPE_SHORT_INTEGER(1338, 2),
        /** Represents a 4-byte integer data type (Little Endian). */
        TYPE_INTEGER(1339, 4),
        /** Represents a 6-byte MAC address data type. */
        TYPE_MAC_ADDRESS(1340, 6),
        /** Represents a 1-byte command code data type. */
        TYPE_COMMAND(1341, 1);

        private final int id;       // Numeric ID of the command in the S-Touch protocol.
        private final int length;   // Fixed length of the command's parameters in bytes.
                                    // -1 indicates variable length (e.g., for strings).

        STouchCommand(final int id, final int length) {
            this.id = id;
            this.length = length;
        }

        /**
         * Retrieves an {@code STouchCommand} enum constant by its numeric ID.
         *
         * @param id The numeric ID of the command.
         * @return The corresponding {@code STouchCommand}, or {@code null} if no command matches the ID.
         */
        public static STouchCommand getCmd(final int id) {
            for (STouchCommand cmd : values()) {
                if (cmd.id == id) {
                    return cmd;
                }
            }
            return null; // No command found for the given ID
        }

        /**
         * Calculates the length of this command's parameters based on its definition
         * and, for variable-length commands, the content of the provided {@link ByteBuffer}.
         * <p>
         * For fixed-length commands ({@code length >= 0}), it returns the defined length if the
         * buffer has enough remaining bytes, otherwise -1.
         * For variable-length commands ({@code length == -1}), it inspects the buffer
         * (e.g., searching for a null terminator for strings) to determine the actual length.
         * The buffer's position is preserved after this call (mark/reset is used internally).
         * </p>
         *
         * @param buffer The {@link ByteBuffer} containing the command parameters, positioned
         *               at the start of the parameters for this command.
         * @return The length of the command's parameters in bytes. Returns -1 if the length
         *         cannot be determined (e.g., buffer underflow for fixed length, or malformed
         *         variable-length data like a missing null terminator).
         */
        public int length(ByteBuffer buffer) {
            if (length == 0) { // Command has no parameters
                return 0;
            } else if (length > 0) { // Command has fixed-length parameters
                // Check if buffer has enough data for this fixed-length command
                return (buffer != null && buffer.remaining() >= length) ? length : -1;
            } else { // Command has variable-length parameters (length == -1)
                if (buffer == null) return -1;
                buffer.mark(); // Mark current position to restore it later
                try {
                    switch (this) {
                        case DISPLAY_PRINT:
                            // Length is determined by finding the null terminator, starting from current position (offset 0).
                            return findNextNullTerminator(buffer, 0);
                        case DISPLAY_PRINTXY:
                            // Parameters: X(2 bytes), Y(2 bytes), then null-terminated string.
                            // Length = 4 (for X,Y) + length of string (including null terminator).
                            return findNextNullTerminator(buffer, 4); // Offset 4 for X and Y coordinates
                        case DISPLAY_PRINTROT:
                            // Parameters: RotationAngle(2 bytes), then null-terminated string.
                            // Length = 2 (for angle) + length of string (including null terminator).
                            return findNextNullTerminator(buffer, 2); // Offset 2 for rotation angle
                        default:
                            // This specific variable-length command is not recognized or its length calculation is not implemented.
                            return -1;
                    }
                } finally {
                    buffer.reset(); // Restore buffer's original position
                }
            }
        }

        /**
         * Helper method to find the length of a null-terminated string within a ByteBuffer.
         * It calculates the length including the null terminator itself.
         * The buffer's position is advanced by {@code baseOffset} before searching for the
         * null terminator. The buffer's original position (before calling this method)
         * must be managed by the caller (e.g., using mark/reset if preservation is needed
         * beyond this method's internal mark/reset for the search part).
         *
         * @param buffer     The ByteBuffer to search within.
         * @param baseOffset The number of initial bytes to skip (e.g., for fixed-size parameters
         *                   preceding the string) before starting the search for the null terminator.
         * @return The total length from the buffer's position *after* skipping {@code baseOffset}
         *         up to and including the null terminator. Returns -1 if a null terminator
         *         is not found before the buffer limit is reached, or if the buffer doesn't
         *         have enough bytes for the baseOffset.
         */
        private static int findNextNullTerminator(ByteBuffer buffer, int baseOffset) {
            // This method assumes buffer.mark() was called by its invoker (STouchCommand.length())
            // and buffer.reset() will be called by its invoker.
            if (buffer.remaining() < baseOffset) {
                return -1; // Not enough data for the base offset itself
            }
            // Advance buffer position by baseOffset to start search after fixed parameters
            buffer.position(buffer.position() + baseOffset);
            int stringLength = 0;
            boolean foundTerminator = false;
            while (buffer.hasRemaining()) {
                stringLength++; // Count current byte
                if (buffer.get() == 0) { // Found null terminator
                    foundTerminator = true;
                    break;
                }
            }
            return foundTerminator ? (baseOffset + stringLength) : -1; // Total length including baseOffset and null terminator
        }
    }

    /**
     * A map of command reader/writer instances, where the key is the {@link STouchCommand}
     * enum constant and the value is the corresponding {@link ObjectReaderWriter} implementation.
     * This map is used by {@link #getParser(STouchCommand)}, {@link #read(STouchCommand, ByteBuffer)},
     * and {@link #write(STouchCommand, ByteBuffer, Object)} methods.
     */
    private static final Map<STouchProtocol.STouchCommand, ObjectReaderWriter<?>> readerWriters = new HashMap<>();

    static {
        // Initialize the map with reader/writer instances for each command/data type.
        // Commands with no parameters (e.g., DISPLAY_SWITCHON) do not need an entry here
        // as their length is 0 and they don't read/write additional data.

        // Reader/writers for command parameters
        readerWriters.put(STouchCommand.DISPLAY_SETSTYLE, new ByteReaderWriter());
        readerWriters.put(STouchCommand.DISPLAY_SETINVERS, new ByteReaderWriter());
        readerWriters.put(STouchCommand.DISPLAY_SETFORECOLOR, new ColorReaderWriter());
        readerWriters.put(STouchCommand.DISPLAY_SETBACKCOLOR, new ColorReaderWriter());
        readerWriters.put(STouchCommand.DISPLAY_SETFONTTYPE, new ByteReaderWriter());
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
        readerWriters.put(STouchCommand.DISPLAY_SYNCNOW, new ShortIntegerReaderWriter());
        readerWriters.put(STouchCommand.DISPLAY_SETBACKLIGHT, new ByteReaderWriter());
        readerWriters.put(STouchCommand.DISPLAY_SETBUZZER, new ShortIntegerReaderWriter());
        readerWriters.put(STouchCommand.DISPLAY_SETCLICK, new ByteReaderWriter());
        readerWriters.put(STouchCommand.DISPLAY_SETBUTTON, new ButtonReaderWriter());
        readerWriters.put(STouchCommand.DISPLAY_DELBUTTON, new ByteReaderWriter()); // Parameter is button ID (byte)
        readerWriters.put(STouchCommand.DISPLAY_SETTEMPOFFSETS, new CoordinatesReaderWriter()); // Assuming two short offsets, fits Coordinates

        // System commands that might have parameters (though many are 0-length and handled by direct logic)
        // Example: SYSTEM_ACTIVATERESOURCE expects an integer checksum.
        // Example: SYSTEM_SETCONFIG expects an integer config value.
        // These are often handled directly in FakeSTouch.processCommands due to specific reply logic,
        // but if they were to use this generic read/write, they'd need entries.
        // For now, assuming their parameters are read directly if needed.

        // Reader/writers for internal pseudo-types (used for parameters of other commands)
        readerWriters.put(STouchCommand.TYPE_BYTE, new ByteReaderWriter());
        readerWriters.put(STouchCommand.TYPE_SHORT_INTEGER, new ShortIntegerReaderWriter());
        readerWriters.put(STouchCommand.TYPE_INTEGER, new IntegerReaderWriter());
        readerWriters.put(STouchCommand.TYPE_MAC_ADDRESS, new MacAddressReaderWriter());
        readerWriters.put(STouchCommand.TYPE_COMMAND, new CommandReaderWriter()); // For reading a command code itself
    }

    /**
     * Retrieves the {@link ObjectReaderWriter} for the given {@link STouchCommand}.
     * This parser can then be used to read or write the parameters associated with that command.
     *
     * @param cmd The {@link STouchCommand} for which to get the parser.
     * @return The corresponding {@link ObjectReaderWriter}, or {@code null} if no specific
     *         parser is registered for this command (e.g., for commands with no parameters
     *         or those handled by special logic).
     */
    public static ObjectReaderWriter<?> getParser(STouchCommand cmd) {
        return readerWriters.get(cmd);
    }

    /**
     * Reads an object from the given {@link ByteBuffer} based on the specified {@link STouchCommand}'s
     * expected data type and format. This method uses the registered {@link ObjectReaderWriter} for the command.
     *
     * @param cmd    The {@link STouchCommand} defining the type of object to read.
     *               This command must have a corresponding parser registered.
     * @param buffer The {@link ByteBuffer} to read from, positioned at the start of the object's data.
     *               The buffer's byte order should be Little Endian.
     * @return The parsed object, or {@code null} if no parser is registered for the command
     *         or if the command definition implies no data to read (though this case might
     *         better be handled by checking command length first).
     * @throws IllegalArgumentException if {@code cmd} or {@code buffer} is null.
     * @throws BufferUnderflowException if the buffer does not contain enough data for the command.
     * @throws IllegalStateException if no parser is found for a command that is expected to have one.
     */
    public static Object read(STouchCommand cmd, ByteBuffer buffer) {
        Objects.requireNonNull(cmd, "STouchCommand cmd must not be null");
        Objects.requireNonNull(buffer, "ByteBuffer buffer must not be null");

        ObjectReaderWriter<?> parser = readerWriters.get(cmd);
        if (parser == null) {
            // This case typically means the command has no parameters or is handled by specific logic elsewhere.
            // If cmd.length is > 0, this would be an issue.
            // For now, returning null if no parser, assuming commands with 0 length don't call read.
            if (cmd.length != 0) { // If command is defined to have parameters but no parser.
                 throw new IllegalStateException("No ObjectReaderWriter registered for command: " + cmd + " which expects parameters.");
            }
            return null;
        }
        if (!buffer.hasRemaining() && cmd.length != 0) { // Check remaining only if command expects data
            throw new BufferUnderflowException();
        }
        return parser.readFromBuffer(buffer);
    }

    /**
     * Writes the given object to the {@link ByteBuffer} according to the format specified
     * by the {@link STouchCommand}. This method uses the registered {@link ObjectReaderWriter} for the command.
     *
     * @param <T>    The type of the object to write.
     * @param cmd    The {@link STouchCommand} defining the format for writing the object.
     *               This command must have a corresponding writer registered.
     * @param buffer The {@link ByteBuffer} to write to, positioned where the object's data should start.
     *               The buffer's byte order should be set to Little Endian by the caller.
     * @param object The object to write.
     * @return The number of bytes written to the buffer.
     * @throws IllegalArgumentException if {@code cmd}, {@code buffer}, or {@code object} is null,
     *                                  or if no writer is registered for the command.
     * @throws BufferOverflowException if the buffer does not have enough space for the object's data.
     */
    public static <T> int write(STouchCommand cmd, ByteBuffer buffer, T object) {
        Objects.requireNonNull(cmd, "STouchCommand cmd must not be null");
        Objects.requireNonNull(buffer, "ByteBuffer buffer must not be null");
        Objects.requireNonNull(object, "Object to write must not be null");

        if (!buffer.hasRemaining() && cmd.length !=0) { // Check remaining only if command expects data.
            throw new BufferOverflowException();
        }

        @SuppressWarnings("unchecked")
        ObjectReaderWriter<T> writer = (ObjectReaderWriter<T>) readerWriters.get(cmd);
        if (writer == null) {
            if (cmd.length != 0) { // If command is defined to have parameters but no writer.
                throw new IllegalArgumentException("No ObjectReaderWriter registered for command: " + cmd + " which expects parameters.");
            }
            return 0; // No data to write for a 0-length command
        }
        return writer.writeToBuffer(buffer, object);
    }

    /***************************************************************************
     *                 ObjectReaderWriter Implementations                      *
     *-------------------------------------------------------------------------*
     * These private static inner classes implement ObjectReaderWriter for     *
     * various data types used in the S-Touch protocol. Each class handles     *
     * reading from and writing to a ByteBuffer for a specific data structure  *
     * or primitive type, adhering to Little Endian byte order and specific    *
     * S-Touch protocol formatting (e.g., string encoding, color representation). *
     ***************************************************************************/


    /**
     * Reads/writes a single byte (represented as an Integer 0-255).
     */
    private static class ByteReaderWriter implements ObjectReaderWriter<Integer> {
        @Override
        public Integer readFromBuffer(ByteBuffer buffer) {
            if (buffer.remaining() < 1) {
                throw new BufferUnderflowException();
            }
            return buffer.get() & 0xFF; // Read byte and treat as unsigned (0-255)
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, Integer object) {
            Objects.requireNonNull(object, "Byte object to write cannot be null.");
            if (object < 0 || object > 255) {
                throw new IllegalArgumentException(
                        "Object value for byte must be within 0-255, got: " + object);
            }
            if (buffer.remaining() < 1) {
                throw new BufferOverflowException();
            }
            buffer.put(object.byteValue());
            return 1; // Bytes written
        }
    }

    /**
     * Reads/writes a 2-byte short integer (represented as an Integer 0-65535, Little Endian).
     */
    private static class ShortIntegerReaderWriter implements ObjectReaderWriter<Integer> {
        @Override
        public Integer readFromBuffer(ByteBuffer buffer) {
            if (buffer.remaining() < 2) {
                throw new BufferUnderflowException();
            }
            // ByteBuffer.getShort() respects the buffer's byte order (should be LITTLE_ENDIAN)
            return (int) buffer.getShort() & 0xFFFF; // Treat as unsigned short
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, Integer object) {
            Objects.requireNonNull(object, "ShortInteger object to write cannot be null.");
            if (object < 0 || object > 0xFFFF) { // Range of an unsigned short
                throw new IllegalArgumentException(
                        "Object value for short integer must be within 0-65535, got: " + object);
            }
            if (buffer.remaining() < 2) {
                throw new BufferOverflowException();
            }
            // ByteBuffer.putShort() respects the buffer's byte order
            buffer.putShort(object.shortValue());
            return 2; // Bytes written
        }
    }

    /**
     * Reads/writes a 4-byte integer (Little Endian).
     */
    private static class IntegerReaderWriter implements ObjectReaderWriter<Integer> {
        @Override
        public Integer readFromBuffer(ByteBuffer buffer) {
            if (buffer.remaining() < 4) {
                throw new BufferUnderflowException();
            }
            // TODO: Confirm Endianness Check.
            // ByteBuffer by default uses BIG_ENDIAN. If the S-Touch protocol strictly uses
            // LITTLE_ENDIAN for all multi-byte types, the ByteBuffer instance passed to this
            // method must have its order set to ByteOrder.LITTLE_ENDIAN by the caller.
            // For example, in FakeSTouch.processCommands:
            //   receiveBuffer.order(ByteOrder.LITTLE_ENDIAN);
            //   replyBuffer.order(ByteOrder.LITTLE_ENDIAN);
            // This comment serves as a reminder for consistent byte order handling.
            return buffer.getInt(); // ByteBuffer.getInt() respects the buffer's byte order
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, Integer object) {
            Objects.requireNonNull(object, "Integer object to write cannot be null.");
            // No range check needed for standard Java int, as it's already 32-bit.
            if (buffer.remaining() < 4) {
                throw new BufferOverflowException();
            }
            buffer.putInt(object); // ByteBuffer.putInt() respects the buffer's byte order
            return 4; // Bytes written
        }
    }

    /**
     * Reads/writes a 6-byte MAC address.
     */
    public static class MacAddressReaderWriter implements ObjectReaderWriter<MacAddress> {
        @Override
        public MacAddress readFromBuffer(ByteBuffer buffer) {
            if (buffer.remaining() < 6) {
                throw new BufferUnderflowException();
            }
            byte[] addressBytes = new byte[6];
            buffer.get(addressBytes);
            return new MacAddress(addressBytes);
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, MacAddress macAddress) {
            Objects.requireNonNull(macAddress, "MacAddress object to write cannot be null.");
            byte[] addressBytes = macAddress.getAddress(); // getAddress() should already return a 6-byte array
            if (addressBytes.length != 6) { // Defensive check
                throw new IllegalArgumentException("MAC address byte array must be 6 bytes long.");
            }
            if (buffer.remaining() < 6) {
                throw new BufferOverflowException();
            }
            buffer.put(addressBytes);
            return 6; // Bytes written
        }
    }

    /**
     * Reads/writes an STouchCommand ID (1 byte).
     */
    private static class CommandReaderWriter implements ObjectReaderWriter<STouchCommand> {
        private final ByteReaderWriter byteReaderWriter = new ByteReaderWriter();

        @Override
        public STouchCommand readFromBuffer(ByteBuffer buffer) {
            Integer id = byteReaderWriter.readFromBuffer(buffer);
            STouchCommand command = STouchCommand.getCmd(id);
            if (command == null) {
                // TODO: Decide how to handle unknown command IDs.
                // Options: throw IllegalArgumentException, return a special UNKNOWN command, or return null.
                // Current behavior (from STouchCommand.getCmd) is to return null.
                // For robustness in parsing, throwing an exception might be better if an unknown command is critical.
                System.err.println("[STouchProtocol] Encountered unknown command ID: " + id); // Or log
            }
            return command;
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, STouchCommand command) {
            Objects.requireNonNull(command, "STouchCommand object to write cannot be null.");
            // Use ByteReaderWriter to write the command ID as a single byte
            return byteReaderWriter.writeToBuffer(buffer, command.id);
        }
    }

    /**
     * Reads/writes a 16-bit color value (RGB565 format).
     */
    private static class ColorReaderWriter implements ObjectReaderWriter<Color> {
        private final ShortIntegerReaderWriter shortIntegerReaderWriter = new ShortIntegerReaderWriter();

        @Override
        public Color readFromBuffer(ByteBuffer buffer) {
            int colorCode16bit = shortIntegerReaderWriter.readFromBuffer(buffer);
            return getColorFrom16BitValue(colorCode16bit);
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, Color color) {
            Objects.requireNonNull(color, "Color object to write cannot be null.");
            int colorCode16bit = get16BitValueFromColor(color);
            return shortIntegerReaderWriter.writeToBuffer(buffer, colorCode16bit);
        }

        /** Converts a 16-bit RGB565 color value to a Java AWT Color object. */
        private Color getColorFrom16BitValue(int colorCode16bit) {
            // RGB565 format: RRRRR GGGGGG BBBBB
            int red5 = (colorCode16bit >> 11) & 0x1F;  // Extract 5 bits for red
            int green6 = (colorCode16bit >> 5) & 0x3F; // Extract 6 bits for green
            int blue5 = colorCode16bit & 0x1F;    // Extract 5 bits for blue

            // Scale to 8-bit values (0-255)
            // Simple scaling: (val * 255) / max_5_bit_val or (val * 255) / max_6_bit_val
            // Or bit-shifting approximation: red8 = (red5 << 3) | (red5 >> 2) for 5->8 bits
            int red8 = (red5 * 255) / 31;
            int green8 = (green6 * 255) / 63;
            int blue8 = (blue5 * 255) / 31;

            return new Color(red8, green8, blue8);
        }

        /** Converts a Java AWT Color object to a 16-bit RGB565 color value. */
        private int get16BitValueFromColor(Color color) {
            int r = color.getRed() >> 3;   // Convert 8-bit red to 5-bit
            int g = color.getGreen() >> 2; // Convert 8-bit green to 6-bit
            int b = color.getBlue() >> 3;  // Convert 8-bit blue to 5-bit
            return (r << 11) | (g << 5) | b;
        }
    }

    /**
     * Reads/writes Coordinates (2 shorts: x, y).
     */
    private static class CoordinatesReaderWriter implements ObjectReaderWriter<Coordinates> {
        private final ShortIntegerReaderWriter shortReader = new ShortIntegerReaderWriter();

        @Override
        public Coordinates readFromBuffer(ByteBuffer buffer) {
            int x = shortReader.readFromBuffer(buffer);
            int y = shortReader.readFromBuffer(buffer);
            return new Coordinates(x, y);
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, Coordinates coordinates) {
            Objects.requireNonNull(coordinates, "Coordinates object cannot be null.");
            int bytesWritten = shortReader.writeToBuffer(buffer, coordinates.x);
            bytesWritten += shortReader.writeToBuffer(buffer, coordinates.y);
            return bytesWritten; // Should be 4
        }
    }

    /**
     * Reads/writes a Rectangle (4 shorts: xMin, yMin, xMax, yMax).
     */
    private static class RectangleReaderWriter implements ObjectReaderWriter<Rectangle> {
        private final CoordinatesReaderWriter coordsReader = new CoordinatesReaderWriter();

        @Override
        public Rectangle readFromBuffer(ByteBuffer buffer) {
            Coordinates upperLeft = coordsReader.readFromBuffer(buffer);
            Coordinates lowerRight = coordsReader.readFromBuffer(buffer);
            return new Rectangle(upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, Rectangle rectangle) {
            Objects.requireNonNull(rectangle, "Rectangle object cannot be null.");
            int bytesWritten = coordsReader.writeToBuffer(buffer, new Coordinates(rectangle.xMin, rectangle.yMin));
            bytesWritten += coordsReader.writeToBuffer(buffer, new Coordinates(rectangle.xMax, rectangle.yMax));
            return bytesWritten; // Should be 8
        }
    }

    /**
     * Reads/writes a RoundRectangle (4 shorts for rect, 1 short for curvature).
     */
    private static class RoundRectangleReaderWriter implements ObjectReaderWriter<RoundRectangle> {
        private final RectangleReaderWriter rectReader = new RectangleReaderWriter();
        private final ShortIntegerReaderWriter shortReader = new ShortIntegerReaderWriter();

        @Override
        public RoundRectangle readFromBuffer(ByteBuffer buffer) {
            Rectangle rect = rectReader.readFromBuffer(buffer);
            int curvature = shortReader.readFromBuffer(buffer);
            return new RoundRectangle(rect.xMin, rect.yMin, rect.xMax, rect.yMax, curvature);
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, RoundRectangle roundRect) {
            Objects.requireNonNull(roundRect, "RoundRectangle object cannot be null.");
            int bytesWritten = rectReader.writeToBuffer(buffer, new Rectangle(roundRect.xMin, roundRect.yMin, roundRect.xMax, roundRect.yMax));
            bytesWritten += shortReader.writeToBuffer(buffer, roundRect.curvature);
            return bytesWritten; // Should be 10
        }
    }

    /**
     * Reads/writes a Circle (2 shorts for center, 1 short for radius).
     */
    private static class CircleReaderWriter implements ObjectReaderWriter<Circle> {
        private final CoordinatesReaderWriter coordsReader = new CoordinatesReaderWriter();
        private final ShortIntegerReaderWriter shortReader = new ShortIntegerReaderWriter();

        @Override
        public Circle readFromBuffer(ByteBuffer buffer) {
            Coordinates center = coordsReader.readFromBuffer(buffer);
            int radius = shortReader.readFromBuffer(buffer);
            return new Circle(center.x, center.y, radius);
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, Circle circle) {
            Objects.requireNonNull(circle, "Circle object cannot be null.");
            int bytesWritten = coordsReader.writeToBuffer(buffer, new Coordinates(circle.x, circle.y));
            bytesWritten += shortReader.writeToBuffer(buffer, circle.radius);
            return bytesWritten; // Should be 6
        }
    }

    /**
     * Reads/writes a Symbol (2 shorts for coords, 1 short for ID).
     */
    private static class SymbolReaderWriter implements ObjectReaderWriter<Symbol> {
        private final CoordinatesReaderWriter coordsReader = new CoordinatesReaderWriter();
        private final ShortIntegerReaderWriter shortReader = new ShortIntegerReaderWriter();

        @Override
        public Symbol readFromBuffer(ByteBuffer buffer) {
            Coordinates upperLeft = coordsReader.readFromBuffer(buffer);
            int id = shortReader.readFromBuffer(buffer);
            return new Symbol(upperLeft.x, upperLeft.y, id);
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, Symbol symbol) {
            Objects.requireNonNull(symbol, "Symbol object cannot be null.");
            int bytesWritten = coordsReader.writeToBuffer(buffer, new Coordinates(symbol.x, symbol.y));
            bytesWritten += shortReader.writeToBuffer(buffer, symbol.id);
            return bytesWritten; // Should be 6
        }
    }

    /**
     * Reads/writes a null-terminated string (windows-1252 encoding).
     */
    private static class TextReaderWriter implements ObjectReaderWriter<String> {
        private static final Charset CHARSET = Charset.forName("windows-1252");

        @Override
        public String readFromBuffer(ByteBuffer buffer) {
            buffer.mark();
            int length = 0;
            while (buffer.hasRemaining()) {
                if (buffer.get() == 0) { // Found null terminator
                    break;
                }
                length++;
                if (length > buffer.limit() - buffer.position() + length ) { // Safety break if no null terminator found within reasonable bounds
                    buffer.reset();
                    throw new IllegalArgumentException("String not null-terminated or exceeds buffer limit.");
                }
            }
            buffer.reset(); // Reset to position before length calculation

            if (length == 0 && (!buffer.hasRemaining() || buffer.get(buffer.position()) != 0)) {
                 // Empty string not followed by null terminator (unless at end of buffer)
                 // Or string is truly empty and next byte is not null (malformed)
                 // This case might indicate an issue or an empty string.
                 // If it was just end of buffer, that's fine. If not null terminated, it's an issue.
                 // For now, if length is 0, we check if it's properly terminated or if buffer simply ended.
                 if (buffer.hasRemaining() && buffer.get(buffer.position()) == 0) buffer.get(); // Consume null if present
                 return "";
            }

            byte[] stringBytes = new byte[length];
            buffer.get(stringBytes);
            if (buffer.hasRemaining() && buffer.get() != 0) { // Consume the null terminator
                 buffer.reset(); // Critical: if this wasn't the terminator, reset and error
                 throw new IllegalArgumentException("String not null-terminated as expected after reading " + length + " bytes.");
            }
            return new String(stringBytes, CHARSET); // No trim(), protocol implies exact string.
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, String text) {
            Objects.requireNonNull(text, "Text string cannot be null.");
            byte[] stringBytes = text.getBytes(CHARSET);
            if (buffer.remaining() < stringBytes.length + 1) { // +1 for null terminator
                throw new BufferOverflowException();
            }
            buffer.put(stringBytes);
            buffer.put((byte) 0); // Null terminator
            return stringBytes.length + 1;
        }
    }

    /**
     * Reads/writes TextXY (Coordinates + null-terminated String).
     */
    private static class TextXYReaderWriter implements ObjectReaderWriter<TextXY> {
        private final CoordinatesReaderWriter coordsReader = new CoordinatesReaderWriter();
        private final TextReaderWriter textReader = new TextReaderWriter();

        @Override
        public TextXY readFromBuffer(ByteBuffer buffer) {
            Coordinates coordinates = coordsReader.readFromBuffer(buffer);
            String text = textReader.readFromBuffer(buffer);
            return new TextXY(coordinates.x, coordinates.y, text);
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, TextXY textXY) {
            Objects.requireNonNull(textXY, "TextXY object cannot be null.");
            int bytesWritten = coordsReader.writeToBuffer(buffer, new Coordinates(textXY.x, textXY.y));
            bytesWritten += textReader.writeToBuffer(buffer, textXY.text);
            return bytesWritten;
        }
    }

    /**
     * Reads/writes TextRotate (short angle + null-terminated String).
     */
    private static class TextRotateReaderWriter implements ObjectReaderWriter<TextRotate> {
        private final ShortIntegerReaderWriter shortReader = new ShortIntegerReaderWriter();
        private final TextReaderWriter textReader = new TextReaderWriter();

        @Override
        public TextRotate readFromBuffer(ByteBuffer buffer) {
            int rotationAngle = shortReader.readFromBuffer(buffer);
            String text = textReader.readFromBuffer(buffer);
            return new TextRotate(rotationAngle, text);
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, TextRotate textRotate) {
            Objects.requireNonNull(textRotate, "TextRotate object cannot be null.");
            int bytesWritten = shortReader.writeToBuffer(buffer, textRotate.rotationAngle);
            bytesWritten += textReader.writeToBuffer(buffer, textRotate.text);
            return bytesWritten;
        }
    }

    /**
     * Reads/writes CharacterRotate (short angle + 1 byte char).
     */
    private static class CharacterRotateReaderWriter implements ObjectReaderWriter<TextRotate> {
        private final ShortIntegerReaderWriter shortReader = new ShortIntegerReaderWriter();
        private final ByteReaderWriter byteReader = new ByteReaderWriter();
        private static final Charset CHARSET = Charset.forName("windows-1252");


        @Override
        public TextRotate readFromBuffer(ByteBuffer buffer) {
            int rotationAngle = shortReader.readFromBuffer(buffer);
            byte charByte = byteReader.readFromBuffer(buffer).byteValue(); // Get the byte
            return new TextRotate(rotationAngle, new String(new byte[]{charByte}, CHARSET));
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, TextRotate textRotate) {
            Objects.requireNonNull(textRotate, "CharacterRotate (TextRotate) object cannot be null.");
            if (textRotate.text == null || textRotate.text.length() != 1) {
                throw new IllegalArgumentException("Text for CharacterRotate must be a single character.");
            }
            int bytesWritten = shortReader.writeToBuffer(buffer, textRotate.rotationAngle);
            byte charByte = textRotate.text.getBytes(CHARSET)[0];
            bytesWritten += byteReader.writeToBuffer(buffer, (int) charByte & 0xFF);
            return bytesWritten; // Should be 3
        }
    }

    /**
     * Reads/writes a Button (byte ID + 2x Coordinates).
     */
    private static class ButtonReaderWriter implements ObjectReaderWriter<Button> {
        private final ByteReaderWriter byteReader = new ByteReaderWriter();
        private final CoordinatesReaderWriter coordsReader = new CoordinatesReaderWriter();

        @Override
        public Button readFromBuffer(ByteBuffer buffer) {
            int id = byteReader.readFromBuffer(buffer);
            Coordinates upperLeft = coordsReader.readFromBuffer(buffer);
            Coordinates lowerRight = coordsReader.readFromBuffer(buffer);
            return new Button(id, upperLeft.x, upperLeft.y, lowerRight.x, lowerRight.y);
        }

        @Override
        public int writeToBuffer(ByteBuffer buffer, Button button) {
            Objects.requireNonNull(button, "Button object cannot be null.");
            int bytesWritten = byteReader.writeToBuffer(buffer, button.id);
            bytesWritten += coordsReader.writeToBuffer(buffer, new Coordinates(button.xMin, button.yMin));
            bytesWritten += coordsReader.writeToBuffer(buffer, new Coordinates(button.xMax, button.yMax));
            return bytesWritten; // Should be 1 + 4 + 4 = 9
        }
    }

    /***************************************************************************
     *              S-Touch Protocol Data Structure Classes                    *
     *-------------------------------------------------------------------------*
     * These public static inner classes define the data structures used by    *
     * S-Touch commands. They are primarily simple value objects (VOs) or data *
     * transfer objects (DTOs) with public final fields for immutability       *
     * where appropriate, and toString() methods for debugging.                *
     ***************************************************************************/

    /**
     * Represents a 6-byte MAC address.
     */
    public static class MacAddress {
        private final byte[] address;

        /**
         * Constructs a MacAddress from a 6-byte array.
         * @param address The byte array representing the MAC address. Must be 6 bytes long.
         * @throws IllegalArgumentException if the address is null or not 6 bytes long.
         */
        public MacAddress(byte[] address) {
            Objects.requireNonNull(address, "MAC address byte array cannot be null.");
            if (address.length != 6) {
                throw new IllegalArgumentException("MAC address must be exactly 6 bytes long.");
            }
            this.address = address.clone(); // Defensive copy
        }

        /**
         * Gets a copy of the byte array representing the MAC address.
         * @return A 6-byte array.
         */
        public byte[] getAddress() {
            return address.clone(); // Defensive copy
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(17); // XX:XX:XX:XX:XX:XX
            for (int i = 0; i < address.length; i++) {
                if (i > 0) {
                    sb.append(':');
                }
                sb.append(String.format("%02X", address[i])); // Use uppercase hex
            }
            return sb.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            MacAddress that = (MacAddress) obj;
            return Arrays.equals(address, that.address);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(address);
        }
    }

    /**
     * Represents a 2D coordinate (x, y) using integer values.
     * Typically used for screen positions or dimensions.
     */
    public static class Coordinates {
        public final int x;
        public final int y;

        /**
         * Constructs a Coordinates object.
         * @param x The x-coordinate.
         * @param y The y-coordinate.
         */
        public Coordinates(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Coordinates { \"x\": " + x + ", \"y\": " + y + " }";
        }
    }

    /**
     * Represents a rectangle defined by its minimum (top-left) and maximum
     * (bottom-right) x and y coordinates.
     */
    public static class Rectangle {
        public final int xMin;
        public final int yMin;
        public final int xMax;
        public final int yMax;

        /**
         * Constructs a Rectangle object.
         * @param xMin The minimum x-coordinate (left edge).
         * @param yMin The minimum y-coordinate (top edge).
         * @param xMax The maximum x-coordinate (right edge).
         * @param yMax The maximum y-coordinate (bottom edge).
         */
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

    /**
     * Represents a rounded rectangle, defined by its bounding box coordinates
     * and a curvature value for its corners.
     */
    public static class RoundRectangle {
        public final int xMin;
        public final int yMin;
        public final int xMax;
        public final int yMax;
        public final int curvature;

        /**
         * Constructs a RoundRectangle object.
         * @param xMin The minimum x-coordinate.
         * @param yMin The minimum y-coordinate.
         * @param xMax The maximum x-coordinate.
         * @param yMax The maximum y-coordinate.
         * @param curvature The curvature of the corners.
         */
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

    /**
     * Represents a circle defined by its center coordinates (x, y) and radius.
     */
    public static class Circle {
        public final int x; // Center x-coordinate
        public final int y; // Center y-coordinate
        public final int radius;

        /**
         * Constructs a Circle object.
         * @param x The x-coordinate of the circle's center.
         * @param y The y-coordinate of the circle's center.
         * @param radius The radius of the circle.
         */
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

    /**
     * Represents a predefined symbol to be drawn at specified (x, y) coordinates,
     * identified by a numeric ID.
     */
    public static class Symbol {
        public final int x; // Top-left x-coordinate to draw the symbol
        public final int y; // Top-left y-coordinate to draw the symbol
        public final int id; // Numeric ID of the symbol

        /**
         * Constructs a Symbol object.
         * @param x The x-coordinate for the symbol's top-left corner.
         * @param y The y-coordinate for the symbol's top-left corner.
         * @param id The ID of the symbol.
         */
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

    /**
     * Represents a text string to be drawn at specified (x, y) coordinates.
     */
    public static class TextXY {
        public final int x; // Starting x-coordinate for the text
        public final int y; // Starting y-coordinate for the text
        public final String text; // The text string

        /**
         * Constructs a TextXY object.
         * @param x The x-coordinate where the text begins.
         * @param y The y-coordinate where the text begins.
         * @param text The string to be displayed.
         */
        public TextXY(int x, int y, String text) {
            this.x = x;
            this.y = y;
            this.text = Objects.requireNonNull(text, "Text cannot be null");
        }

        @Override
        public String toString() {
            return "TextXY { \"x\": " + x + ", \"y\": " + y + ", \"text\": \"" + text + "\" }";
        }
    }

    /**
     * Represents a text string to be drawn with a specified rotation angle.
     * The coordinates for drawing are typically set by a preceding {@code DISPLAY_SETXY} command.
     */
    public static class TextRotate {
        public final int rotationAngle; // Angle of rotation for the text
        public final String text;        // The text string

        /**
         * Constructs a TextRotate object.
         * @param rotationAngle The angle (e.g., in degrees) to rotate the text.
         * @param text The string to be displayed.
         */
        public TextRotate(int rotationAngle, String text) {
            this.rotationAngle = rotationAngle;
            this.text = Objects.requireNonNull(text, "Text cannot be null");
        }

        @Override
        public String toString() {
            return "TextRotate { \"rotationAngle\": " + rotationAngle + ", \"text\": \"" + text + "\" }";
        }
    }

    /**
     * Represents a touch-sensitive button area on the display, defined by an ID
     * and bounding box coordinates.
     */
    public static class Button {
        public final int id;   // Unique ID for the button
        public final int xMin; // Minimum x-coordinate of the button area
        public final int yMin; // Minimum y-coordinate of the button area
        public final int xMax; // Maximum x-coordinate of the button area
        public final int yMax; // Maximum y-coordinate of the button area

        /**
         * Constructs a Button object.
         * @param id The ID of the button.
         * @param xMin The minimum x-coordinate (left edge).
         * @param yMin The minimum y-coordinate (top edge).
         * @param xMax The maximum x-coordinate (right edge).
         * @param yMax The maximum y-coordinate (bottom edge).
         */
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
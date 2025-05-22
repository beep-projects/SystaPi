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
import java.io.IOException;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import de.freaklamarsch.systarest.DeviceTouchSearch.DeviceTouchDeviceInfo;
import de.freaklamarsch.systarest.STouchProtocol.Button;
    public DatagramPacket processCommands(DatagramPacket incomingPacket) {
        // This method is now a wrapper. The main logic is in handleIncomingPacket.
        return handleIncomingPacket(incomingPacket);
    }

    /**
     * Handles an incoming datagram packet, processing the S-Touch commands within it.
     *
     * @param incomingPacket The packet received from the Systa controller.
     * @return A {@link DatagramPacket} to be sent as a reply, or {@code null} if no reply is needed or an error occurs.
     */
    private DatagramPacket handleIncomingPacket(DatagramPacket incomingPacket) {
        byte[] receivedBytes = incomingPacket.getData();
        int receivedLength = incomingPacket.getLength();

        ByteBuffer receiveBuffer = ByteBuffer.wrap(receivedBytes, 0, receivedLength).asReadOnlyBuffer();
        receiveBuffer.order(ByteOrder.LITTLE_ENDIAN);

        printDebugInfo("Handling incoming packet, length = " + receivedLength);
        if (this.debug) {
            printByteArrayAsHex(receivedBytes, receivedLength);
        }

        if (receiveBuffer.remaining() < 1) {
            printDebugInfo("Received empty or malformed packet.");
            return null; // Not enough data for packet type
        }
        byte packetType = receiveBuffer.get(); // First byte is packet type
        printDebugInfo("Received Packet Type = " + packetType);

        if (packetType == PACKET_TYPE_COMMAND || packetType == PACKET_TYPE_COMMAND_BATCH) {
            return processType1Or9Packet(packetType, receiveBuffer);
        } else {
            printDebugInfo("Unknown or unsupported packet type (" + packetType + ") received. No reply will be sent.");
            return null; // Do not send a reply for unknown packet types
        }
    }

    /**
     * Processes command packets of type 1 (single command, deprecated by some sources) or 9 (batch of commands).
     *
     * @param packetType    The type of the packet (1 or 9).
     * @param receiveBuffer The buffer containing the packet data (after the type byte).
     * @return A reply packet, or null if a fundamental error occurs.
     */
    private DatagramPacket processType1Or9Packet(byte packetType, ByteBuffer receiveBuffer) {
        if (receiveBuffer.remaining() < 4) { // PktCmd (2) + PktId (2)
            printDebugInfo("Packet too short for PktCmd and PktId, type: " + packetType);
            return null; // Malformed
        }
        this.PktCmd = STouchProtocol.readShort(receiveBuffer); // Read PktCmd
        int pktId = STouchProtocol.readShort(receiveBuffer);   // Read PktId
        printDebugInfo("Received PktCmd = " + this.PktCmd + ", PktId = " + pktId);

        byte[] replyBytes = new byte[MAX_DATAGRAM_LENGTH];
        ByteBuffer replyBuffer = ByteBuffer.wrap(replyBytes);
        replyBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Echo header back
        replyBuffer.put(packetType);
        STouchProtocol.writeShort(replyBuffer, (int) this.PktCmd);
        STouchProtocol.writeShort(replyBuffer, pktId);

        CommandProcessingResult result = processCommandLoop(receiveBuffer, replyBuffer, packetType);

        // Finalize reply based on processing result
        if (result.getReplyType() == ReplyType.ERROR) {
            buildErrorReply(replyBuffer, packetType, result.getProcessedCommandsCount(), result.getIgnoredCommandsCount());
        } else if (result.getReplyType() == ReplyType.OK) {
            buildOkReply(replyBuffer, packetType, result.getProcessedCommandsCount(), result.getIgnoredCommandsCount());
        }
        // For ReplyType.NONE, replyBuffer is assumed to be already populated by a specific command handler.

        DatagramPacket replyPacket = new DatagramPacket(replyBytes, replyBuffer.position(), this.inetAddress, this.port);

        if (this.debug) {
            printDebugInfo("Final reply packet content (length " + replyPacket.getLength() + "):");
            printByteArrayAsHex(replyPacket.getData(), replyPacket.getLength());
        }
        return replyPacket;
    }


    /**
     * Inner class to hold the result of command loop processing.
     */
    private static class CommandProcessingResult {
        private final ReplyType replyType;
        private final int processedCommandsCount;
        private final int ignoredCommandsCount;

        public CommandProcessingResult(ReplyType replyType, int processedCommandsCount, int ignoredCommandsCount) {
            this.replyType = replyType;
            this.processedCommandsCount = processedCommandsCount;
            this.ignoredCommandsCount = ignoredCommandsCount;
        }

        public ReplyType getReplyType() { return replyType; }
        public int getProcessedCommandsCount() { return processedCommandsCount; }
        public int getIgnoredCommandsCount() { return ignoredCommandsCount; }
    }


    /**
     * Loops through commands in the receive buffer, processes them, and builds the reply.
     *
     * @param receiveBuffer The buffer to read commands from.
     * @param replyBuffer   The buffer to write command-specific replies to (for type NONE).
     * @param packetType    The type of the overall packet (1 or 9).
     * @return A CommandProcessingResult summarizing the outcome.
     */
    private CommandProcessingResult processCommandLoop(ByteBuffer receiveBuffer, ByteBuffer replyBuffer, byte packetType) {
        int processedCommandsCount = 0;
        int ignoredCommandsCount = 0;
        ReplyType overallReplyType = ReplyType.OK; // Assume OK unless an error occurs

        while (receiveBuffer.hasRemaining()) {
            printDebugInfo("Command loop: receiveBuffer position @ " + receiveBuffer.position());
            processedCommandsCount++;

            if (packetType == PACKET_TYPE_COMMAND && processedCommandsCount > MAX_COMMANDS_PACKET_TYPE_1) {
                printDebugInfo("Exceeded max commands for packet type " + PACKET_TYPE_COMMAND + ".");
                overallReplyType = ReplyType.ERROR;
                break;
            }

            STouchCommand cmd = (STouchCommand) STouchProtocol.read(STouchCommand.TYPE_COMMAND, receiveBuffer);
            if (cmd == null) {
                printDebugInfo("Invalid or unknown command code encountered in loop.");
                overallReplyType = ReplyType.ERROR;
                break;
            }

            int cmdLen = cmd.length(receiveBuffer);
            if (cmdLen < 0 || receiveBuffer.remaining() < cmdLen && cmdLen > 0 ) { // cmdLen can be 0 for parameter-less commands
                 printDebugInfo("Invalid command length or buffer underflow for " + cmd.name() +
                               ". Expected len=" + cmdLen + ", remaining=" + receiveBuffer.remaining());
                overallReplyType = ReplyType.ERROR;
                break;
            }

            if (isCommandEnabled(cmd)) {
                Object parameters = null;
                if (cmdLen > 0) {
                    parameters = STouchProtocol.read(cmd, receiveBuffer);
                    if (debug) printDebugInfo("Parameters for " + cmd.name() + ": " + parameters);
                }
                printDebugInfo("Processing enabled command: " + cmd.name());
                ReplyType singleCmdReplyType = processSingleCommand(cmd, parameters, replyBuffer);
                if (singleCmdReplyType == ReplyType.ERROR) {
                    overallReplyType = ReplyType.ERROR;
                    break; // Stop processing on first error
                } else if (singleCmdReplyType == ReplyType.NONE) {
                    // This means the command has built its own specific reply.
                    // The overall reply type for the packet might still be OK or effectively NONE.
                    // If multiple commands return NONE, the last one's reply might be what's sent, or they aggregate.
                    // For simplicity, if any command has a specific reply (NONE), the overall packet reply type is also NONE.
                    overallReplyType = ReplyType.NONE;
                }
                // If singleCmdReplyType is OK, overallReplyType remains OK (or NONE if previously set).
            } else {
                printDebugInfo("Command " + cmd.name() + " is not enabled. Skipping " + cmdLen + " bytes.");
                if (receiveBuffer.remaining() >= cmdLen) {
                    receiveBuffer.position(receiveBuffer.position() + cmdLen);
                } else {
                     printDebugInfo("Buffer underflow while skipping disabled command " + cmd.name());
                     overallReplyType = ReplyType.ERROR; break;
                }
                ignoredCommandsCount++;
            }
        }
        return new CommandProcessingResult(overallReplyType, processedCommandsCount, ignoredCommandsCount);
    }

    /**
     * Processes a single S-Touch command.
     *
     * @param cmd         The command to process.
     * @param parameters  The parameters for the command (can be null).
     * @param replyBuffer The buffer to write specific replies into (for commands that don't use standard OK/Error).
     * @return The {@link ReplyType} for this specific command.
     */
    private ReplyType processSingleCommand(STouchCommand cmd, Object parameters, ByteBuffer replyBuffer) {
        Function<Object, Boolean> displayAction = displayMethods.get(cmd);
        if (displayAction != null) {
            try {
                if (!displayAction.apply(parameters)) {
                    printDebugInfo("Execution of display action for " + cmd.name() + " returned false.");
                    return ReplyType.ERROR;
                }
                return ReplyType.OK; // Assume display actions that return true are OK.
            } catch (Exception e) {
                printDebugInfo("Exception during display action for " + cmd.name() + ": " + e.getMessage());
                if (debug) e.printStackTrace(System.err);
                return ReplyType.ERROR;
            }
        }

        // Handle system commands or others not in displayMethods map
        switch (cmd) {
            case SYSTEM_GETSYSTEM:
                STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer, STouchCommand.SYSTEM_GETSYSTEM);
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, 1); // Status OK
                STouchProtocol.writeShort(replyBuffer, FakeSTouch.BASIS_SOFTWARE_VERSION / 100);
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) (FakeSTouch.BASIS_SOFTWARE_VERSION % 100));
                STouchProtocol.writeShort(replyBuffer, FakeSTouch.APP_MAJOR_VERSION);
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) FakeSTouch.APP_MINOR_VERSION);
                return ReplyType.NONE; // Specific reply format
            case SYSTEM_GETRESOURCEINFO:
                STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer, STouchCommand.SYSTEM_GETRESOURCEINFO);
                STouchProtocol.writeShort(replyBuffer, this.getDisplay().RESSOURCE_ID);
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) this.getDisplay().FONTS_USED);
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) this.getDisplay().SYMBOLS_USED);
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) (this.getDisplay().getFonts() - this.getDisplay().FONTS_USED));
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) (this.getDisplay().getSymbs() - this.getDisplay().SYMBOLS_USED));
                for (int i = 0; i < 4; i++) STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) -1);
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) -8);
                for (int i = 0; i < 3; i++) STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) 0);
                STouchProtocol.writeInt(replyBuffer, this.getDisplay().getChecksum());
                return ReplyType.NONE;
            case SYSTEM_ERASERESOURCE:
            case SYSTEM_FLASHRESOURCE:
                STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer, cmd);
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, REPLY_STATUS_OK);
                return ReplyType.NONE;
            case SYSTEM_ACTIVATERESOURCE:
                STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer, STouchCommand.SYSTEM_ACTIVATERESOURCE);
                int checksumToActivate = (parameters instanceof Integer) ? (Integer) parameters : 0; // Parameter should be Integer
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, this.getDisplay().setChecksum(checksumToActivate) ? REPLY_STATUS_OK : (byte) 2);
                return ReplyType.NONE;
            case SYSTEM_SETCONFIG:
                STouchProtocol.write(STouchCommand.TYPE_COMMAND, replyBuffer, STouchCommand.SYSTEM_SETCONFIG);
                int newConfigVal = (parameters instanceof Integer) ? (Integer) parameters : 0; // Parameter should be Integer
                this.getDisplay().setConfig(newConfigVal);
                if ((this.getDisplay().getConfig() & 0x01) == 0x01) {
                    this.cnCmd = 0;
                    printDebugInfo("SYSTEM_SETCONFIG: Config bit 0 set, cnCmd reset to 0.");
                }
                STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, REPLY_STATUS_OK);
                STouchProtocol.writeInt(replyBuffer, this.getDisplay().getConfig());
                return ReplyType.NONE;
            default:
                printDebugInfo("Unhandled command in processSingleCommand: " + cmd.name());
                return ReplyType.ERROR; // Unhandled command
        }
    }

    /**
     * Builds a standard OK reply in the provided buffer.
     * Assumes the header (type, PktCmd, PktId) is already written.
     */
    private void buildOkReply(ByteBuffer replyBuffer, byte packetType, int processedCommandsCount, int ignoredCommandsCount) {
        replyBuffer.position(PACKET_HEADER_SIZE); // Start after the echoed header
        STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, REPLY_STATUS_OK);
        if (packetType == PACKET_TYPE_COMMAND_BATCH) {
            STouchProtocol.writeShort(replyBuffer, processedCommandsCount);
        } else {
            STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) processedCommandsCount);
        }
        STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) this.display.getButton());
        STouchProtocol.writeShort(replyBuffer, this.display.getX());
        STouchProtocol.writeShort(replyBuffer, this.display.getY());
        STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) 0); // Reserved
        STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) 0); // Reserved
        STouchProtocol.writeShort(replyBuffer, this.getDisplay().getFreeCommandSpace());
        STouchProtocol.writeShort(replyBuffer, ignoredCommandsCount);
    }

    /**
     * Builds a standard Error reply in the provided buffer.
     * Assumes the header (type, PktCmd, PktId) is already written.
     */
    private void buildErrorReply(ByteBuffer replyBuffer, byte packetType, int processedCommandsCount, int ignoredCommandsCount) {
        replyBuffer.position(PACKET_HEADER_SIZE); // Start after the echoed header
        STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, REPLY_STATUS_ERROR);
        if (packetType == PACKET_TYPE_COMMAND_BATCH) {
            STouchProtocol.writeShort(replyBuffer, processedCommandsCount);
        } else {
            STouchProtocol.write(STouchCommand.TYPE_BYTE, replyBuffer, (byte) processedCommandsCount);
        }
        STouchProtocol.writeShort(replyBuffer, this.getDisplay().getFreeCommandSpace());
        STouchProtocol.writeShort(replyBuffer, ignoredCommandsCount);
    }

    private void printByteArrayAsHex(byte[] bytes, int len) {
		printByteArrayAsHex(Arrays.copyOfRange(bytes, 0, len));
	}

	private void printByteArrayAsHex(byte[] bytes) {
		BigInteger bigInteger = new BigInteger(1, bytes);
		System.out.println(String.format("%0" + (bytes.length << 1) + "X", bigInteger));
	}
}

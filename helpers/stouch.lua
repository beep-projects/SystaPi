-- Copyright (c) 2021, The beep-projects contributors
-- this file originated from https://github.com/beep-projects
-- Do not remove the lines above.
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as published by
-- the Free Software Foundation, either version 3 of the License, or
-- (at your option) any later version.
--
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
--
-- You should have received a copy of the GNU General Public License
-- along with this program.  If not, see https://www.gnu.org/licenses/

-- Wireshark dissector fot the device touce protocol used by the S-Touch app
-- should be placed in 
-- ~/.local/lib/wireshark/plugins (Linux)
-- %APPDATA%\Wireshark\plugins or WIRESHARK\plugins (Windows)
-- %APPDIR%/Contents/PlugIns/wireshark or INSTALLDIR/lib/wireshark/plugins (macOS)

stouch_protocol = Proto("S-Touch",  "S-Touch Protocol")

-- Header fields
packet_type     = ProtoField.uint8 ("S-Touch.packet_type"      , "packetType"        , base.DEC)
packet_command  = ProtoField.uint16 ("S-Touch.packet_command"  , "packetCommand"     , base.HEX)
pktID           = ProtoField.uint16 ("S-Touch.pktID"           , "pktID"             , base.DEC)
packet_subtype  = ProtoField.int8   ("S-Touch.packet_subtype"  , "packetSubtype"     , base.DEC)

-- Payload fields
pressId            = ProtoField.int8  ("S-Touch.button_pressed.id"  , "id"     , base.DEC)
pressX             = ProtoField.int16 ("S-Touch.button_pressed.x"   , "x"             , base.DEC)
pressY             = ProtoField.int16 ("S-Touch.button_pressed.y"   , "y"             , base.DEC)
zero_bytes         = ProtoField.int16 ("S-Touch.zero_bytes"         , "zeroBytes"         , base.DEC)
int01                 = ProtoField.int16 ("S-Touch.int01"                 , "int01"                , base.DEC)
int02                 = ProtoField.int16 ("S-Touch.int02"                 , "int02"                , base.DEC)
free_command_space = ProtoField.int16 ("S-Touch.free_command_space" , "freeCommandSpace"  , base.DEC)

-- command parameters
style = ProtoField.uint8 ("S-Touch.SETSTYLE.style" , "style"  , base.DEC)
x = ProtoField.int16 ("S-Touch.PRINT.x" , "x"  , base.DEC)
y = ProtoField.int16 ("S-Touch.PRINT.y" , "y"  , base.DEC)
text = ProtoField.string ("S-Touch.PRINT.text" , "text"  , base.UNICODE)
unknown = ProtoField.uint32 ("S-Touch.unknown" , "unknown"  , base.HEX)
id = ProtoField.int8 ("S-Touch.PRINT.id" , "id"  , base.DEC)
width = ProtoField.int16 ("S-Touch.PRINT.width" , "width"  , base.DEC)
height = ProtoField.int16 ("S-Touch.PRINT.height" , "height"  , base.DEC)
btnId = ProtoField.int8 ("S-Touch.SETBUTTON.id" , "id"  , base.DEC)
btnX1 = ProtoField.int16 ("S-Touch.SETBUTTON.x1" , "x1"  , base.DEC)
btnX2 = ProtoField.int16 ("S-Touch.SETBUTTON.x2" , "x2"  , base.DEC)
btnY1 = ProtoField.int16 ("S-Touch.SETBUTTON.y1" , "y1"  , base.DEC)
btnY2 = ProtoField.int16 ("S-Touch.SETBUTTON.y2" , "y2"  , base.DEC)
rectX1 = ProtoField.int16 ("S-Touch.DRAWRECTANGLE.x1" , "x1"  , base.DEC)
rectX2 = ProtoField.int16 ("S-Touch.DRAWRECTANGLE.x2" , "x2"  , base.DEC)
rectY1 = ProtoField.int16 ("S-Touch.DRAWRECTANGLE.y1" , "y1"  , base.DEC)
rectY2 = ProtoField.int16 ("S-Touch.DRAWRECTANGLE.y2" , "y2"  , base.DEC)
fgcolor = ProtoField.uint16 ("S-Touch.SETFGCOLOR.color" , "color"  , base.HEX)
bgcolor = ProtoField.uint16 ("S-Touch.SETBGCOLOR.color" , "color"  , base.HEX)
mvToX = ProtoField.int16 ("S-Touch.MOVETO.x" , "x"  , base.DEC)
mvToY = ProtoField.int16 ("S-Touch.MOVETO.y" , "y"  , base.DEC)

stouch_protocol.fields = {
  packet_type, packet_command, pktID, packet_subtype,                     -- Header
  pressId, pressX, pressY, zero_bytes, int02, int01, free_command_space,     -- PKT_OK || PKT_ERR
  style, x, y, text, unknown, id, width, height,  -- command parameters
  btnId, btnX1, btnX2, btnY1, btnY2,
  rectX1, rectX2, rectY1, rectY2, fgcolor, bgcolor,
  mvToX, mvToY
}

function stouch_protocol.dissector(buffer, pinfo, tree)
    length = buffer:len()
    if length == 0 then return end

    pinfo.cols.protocol = stouch_protocol.name

    local subtree = tree:add(stouch_protocol, buffer(), "S-Touch Protocol Data")

    -- Header
    subtree:add(packet_type,    buffer(0,1))
    local packet_type = buffer(0,1):uint()
    local packet_subtype_name = "Unknown"
    subtree:add_le(packet_command, buffer(1,2))
    local i = 3
    if packet_type == 1 || packet_type == 9 then
      subtree:add_le(pktID, buffer(3,2))
      i = 5
    --  i = 4
    --elseif packet_type == 9 then
    --  subtree:add_le(pktID, buffer(3,2))
    --  i = 5
    end	

	local packet_subtype = buffer(i,1):uint()
    
    if packet_subtype == 0 then --ok
      dissect_OK(buffer, pinfo, subtree, i)
    elseif packet_subtype == 255 then --error
      dissect_ERR(buffer, pinfo, subtree, i)
    else  -- command packet
      dissect_CMD(buffer, pinfo, subtree, i)
    end
    
end

function get_subtype_name(subtype)
    local subtype_name = "Unknown"

    if subtype == 0 then
      subtype_name = "PKT_OK"
    elseif subtype == 255 then
      subtype_name = "PKT_ERR"
    else
      subtype_name = "PKT_CMD"
    end

    return subtype_name
end

function dissect_CMD(buffer, pinfo, subtree, i)
  local cmdSubtree = subtree:add(stouch_protocol, buffer(i, buffer:len() - i), "Commands")
  local cmdLen = 0
  --loop over the rest of the packet and extract the commands
  while i < buffer:len() - 1 do
    local cmd = buffer(i,1):uint()
    -- print("cmd: " .. cmd .. ", i: " .. i)
    if cmd == stouch_commands.SWITCHON then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SWITCHON")
      i = i+1 -- move the read index
    elseif cmd == stouch_commands.SWITCHOFF then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SWITCHOFF")
      i = i+1 -- move the read index
    elseif cmd == stouch_commands.SETSTYLE then
      cmdLen = 1
      local styleSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETSTYLE")
      i = i+1 -- move the read index
      styleSubtree:add(style, buffer(i, cmdLen))
      i = i+cmdLen
    elseif cmd == stouch_commands.SETINVERS then
      cmdLen = 1
      local inverseSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETINVERS")
      i = i+1 -- move the read index
      -- inverseSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SWITCHOFF")
      i = i+cmdLen
    elseif cmd == stouch_commands.SETFOREGROUNDCOLOR then
      cmdLen = 2
      local fgSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETFOREGROUNDCOLOR")
      i = i+1 -- move the read index
      fgSubtree:add_le(fgcolor, buffer(i, cmdLen))
      i = i+cmdLen
    elseif cmd == stouch_commands.SETBACKGROUNDCOLOR then
      cmdLen = 2
      local bgSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETBACKGROUNDCOLOR")
      i = i+1 -- move the read index
      bgSubtree:add_le(bgcolor, buffer(i, cmdLen))
      i = i+cmdLen
    elseif cmd == stouch_commands.SETFONTTYPE then
      cmdLen = 1
      local fontSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETFONTTYPE")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.SETPIXEL then
      cmdLen = 0
      local pixelSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETPIXEL")
      i = i+1 -- move the read index
      i = i+cmdLen      
    elseif cmd == stouch_commands.MOVETO then
      cmdLen = 4
      local movetoSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "MOVETO")
      i = i+1 -- move the read index
      movetoSubtree:add_le(mvToX, buffer(i, 2))
      movetoSubtree:add_le(mvToY, buffer(i+2, 2))
      i = i+cmdLen
    elseif cmd == stouch_commands.DRAWLINETO then
      cmdLen = 4
      local DRAWLINETOSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "DRAWLINETO")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.DRAWRECTANGLE then
      cmdLen = 8
      local rectSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "DRAWRECTANGLE")
      i = i+1 -- move the read index
      rectSubtree:add_le(rectX1, buffer(i, 2))
      rectSubtree:add_le(rectY1, buffer(i+2, 2))
      rectSubtree:add_le(rectX2, buffer(i+4, 2))
      rectSubtree:add_le(rectY2, buffer(i+6, 2))
      i = i+cmdLen
    elseif cmd == stouch_commands.DRAWARC then
      cmdLen = 6
      local arcSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "DRAWARC")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.DRAWROUNDRECTANGLE then
      cmdLen = 10
      local roundrectSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "DRAWROUNDRECTANGLE")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.DRAWSYMBOL then
      cmdLen = 6
      local symbolSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "DRAWSYMBOL")
      i = i+1 -- move the read index
      symbolSubtree:add_le(x, buffer(i, 2))
      symbolSubtree:add_le(y, buffer(i+2, 2))
      symbolSubtree:add_le(id, buffer(i+4, 2))
      i = i+cmdLen
    elseif cmd == stouch_commands.DELETESYMBOL then
      cmdLen = 6
      local delsymbolSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "DELETESYMBOL")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.SETXY then
      cmdLen = 4
      local xySubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETXY")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.PUTC then
      cmdLen = 1
      local putcSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "PUTC")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.PRINT then
      -- next 0 after i+1
      cmdLen = findNextZero(buffer, i+1) - i
      local printSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "PRINT")
      i = i+1 -- move the read index
      printSubtree:add(text, buffer(i, cmdLen-1))  --cut the zero delimiter
      i = i+cmdLen
    elseif cmd == stouch_commands.PRINTXY then
      -- next 0 after i+5
      cmdLen = findNextZero(buffer, i+5) - i
      local printxySubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "PRINTXY")
      i = i+1 -- move the read index
      printxySubtree:add_le(x, buffer(i, 2))
      printxySubtree:add_le(y, buffer(i+2, 2))
      printxySubtree:add(text, buffer(i+4, cmdLen-5)) --cut the zero delimiter
      i = i+cmdLen
    elseif cmd == stouch_commands.PUTCROTATE then
      cmdLen = 3
      local crotSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "PUTCROTATE")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.PRINTROTATE then
      -- next 0 after i+3
      cmdLen = findNextZero(buffer, i+3) - i
      local printrotateSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "PRINTROTATE")
      i = i+1 -- move the read index
      printrotateSubtree:add(unknown, buffer(i, 1))
      printrotateSubtree:add(text, buffer(i+1, cmdLen-2)) --cut the zero delimiter
      i = i+cmdLen
    elseif cmd == stouch_commands.CALIBRATETOUCH then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "CALIBRATETOUCH")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.SYNCNOW then
      cmdLen = 2
      local syncSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SYNCNOW")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.SETBACKLIGHT then
      cmdLen = 1
      local backlightSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETBACKLIGHT")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.SETBUZZER then
      cmdLen = 2
      local buzzerSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETBUZZER")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.SETCLICK then
      cmdLen = 1
      local clickSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETCLICK")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.SETBUTTON then
      cmdLen = 9
      local btnSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETBUTTON")
      i = i+1 -- move the read index
      btnSubtree:add(btnId, buffer(i, 1))
      btnSubtree:add_le(btnX1, buffer(i+1, 2))
      btnSubtree:add_le(btnY1, buffer(i+3, 2))
      btnSubtree:add_le(btnX2, buffer(i+5, 2))
      btnSubtree:add_le(btnY2, buffer(i+7, 2))
      i = i+cmdLen
    elseif cmd == stouch_commands.DELETEBUTTON then
      cmdLen = 1
      local delbtnSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "DELETEBUTTON")
      i = i+1 -- move the read index
      delbtnSubtree:add(id, buffer(i, 1))
      i = i+cmdLen
    elseif cmd == stouch_commands.SETTEMPOFFSETS then
      cmdLen = 4
      local tmpoffSubtree = cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETTEMPOFFSETS")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.GETSYSTEM then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "GETSYSTEM")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.GOSYSTEM then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "GOSYSTEM")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.CLEARID then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "CLEARID")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.GETRESOURCEINFO then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "GETRESOURCEINFO")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.ERASERESOURCE then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "ERASERESOURCE")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.FLASHRESOURCE then
      cmdLen = 0
      subtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "FLASHRESOURCE")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.ACTIVATERESOURCE then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "ACTIVATERESOURCE")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.SETCONFIG then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "SETCONFIG")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.CLEARAPP then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "CLEARAPP")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.FLASHAPP then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "FLASHAPP")
      i = i+1 -- move the read index
      i = i+cmdLen
    elseif cmd == stouch_commands.ACTIVATEAPP then
      cmdLen = 0
      cmdSubtree:add(stouch_protocol, buffer(i, 1 + cmdLen), "ACTIVATEAPP")
      i = i+1 -- move the read index
      i = i+cmdLen
    else
      i = i+1
    end
  end
end

function findNextZero(buffer, i)
  local byte = 0
  while i < buffer:len() - 1 do
    byte = buffer(i,1):uint()
    if byte == 0 then 
      break
    end
    i = i + 1
  end
  return i
end

function dissect_OK(buffer, pinfo, subtree, i)
  -- local packet_subtype_name = get_subtype_name(packet_subtype)
  subtree:add(packet_subtype, buffer(i,1)):append_text(" ( PKT_OK )")
  i = i+1
  subtree:add_le(int02, buffer(i,2))
  i = i+2
  subtree:add(pressId, buffer(i,1))
  i = i+1
  subtree:add_le(pressX, buffer(i,2))
  i = i+2
  subtree:add_le(pressY, buffer(i,2))
  i = i+2
  subtree:add_le(zero_bytes, buffer(i,2))
  i = i+2
  subtree:add_le(free_command_space, buffer(i,2))
  i = i+2
  subtree:add_le(int01, buffer(i,2))

end

function dissect_ERR(buffer, pinfo, subtree, i)
  -- local packet_subtype_name = get_subtype_name(packet_subtype)
  subtree:add(packet_subtype, buffer(i,1)):append_text(" ( PKT_ERR )")
  i = i+1
  subtree:add_le(int02, buffer(i,2))
  i = i+2
  subtree:add_le(free_command_space, buffer(i,2))
  i = i+2
  subtree:add_le(int01, buffer(i,2))

end


stouch_commands = {
  SWITCHON = 0,
  SWITCHOFF = 1,
  SETSTYLE = 2,
  SETINVERS = 3,
  SETFOREGROUNDCOLOR = 4,
  SETBACKGROUNDCOLOR = 5,
  SETFONTTYPE = 6,
  SETPIXEL = 7,
  MOVETO = 8,
  DRAWLINETO = 9,
  DRAWRECTANGLE = 10,
  DRAWARC = 11,
  DRAWROUNDRECTANGLE = 12,
  DRAWSYMBOL = 13,
  DELETESYMBOL = 14,
  SETXY = 15,
  PUTC = 16,
  PRINT = 17,
  PRINTXY = 18,
  PUTCROTATE = 19,
  PRINTROTATE = 20,
  CALIBRATETOUCH = 21,
  SYNCNOW = 22,
  SETBACKLIGHT = 128,
  SETBUZZER = 129,
  SETCLICK = 130,
  SETBUTTON = 144,
  DELETEBUTTON = 145,
  SETTEMPOFFSETS = 146,
  GETSYSTEM = 240,
  GOSYSTEM = 241,
  CLEARID = 242,
  GETRESOURCEINFO = 243,
  ERASERESOURCE = 244,
  FLASHRESOURCE = 245,
  ACTIVATERESOURCE = 246,
  SETCONFIG = 247,
  CLEARAPP = 250,
  FLASHAPP = 251,
  ACTIVATEAPP = 252
}


local udp_port = DissectorTable.get("udp.port")
udp_port:add(3477, stouch_protocol)

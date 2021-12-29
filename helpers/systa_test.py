# Copyright (c) 2021, The beep-projects contributors
# this file originated from https://github.com/beep-projects
# Do not remove the lines above.
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see https://www.gnu.org/licenses/

"""Tests for reverseengineering the communication with a paradigma systacomfort

"""
from __future__ import print_function
import socket
import os
from datetime import datetime
import random


class SystaComfort(object):
  """ class for communication with a Systa Comfort II unit """

  def __init__(self):
    self.systaweb_ip = None
    self.systaweb_port = 22460
    self.systa_bcast_ip = None
    self.systa_bcast_port = 8001
    self.sc_info_string = None
    self.unit_ip = None
    self.unit_stouch_port = 0
    self.unit_name = None
    self.unit_id = 0
    self.unit_app = 0
    self.unit_platform = 0
    self.unit_sc_version = 0
    self.unit_sc_minor = 0
    self.unit_password = None
    self.unit_base_version = None
    self.unit_mac = None

  def find_sc(self):
    """check if the SystaPi is connected to any Systa Comfort"""
    # TODO this search is reverse engineered with a single unit.
    # If someone has more device touch capable devices in the network
    # this part has to be adapted to support this

    # get all available interfaces
    interfaces = self.find_system_ips()
    # loop over all interfaces and try to find a Systa Comfort
    systa_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    # make sure the socket can be reused by other functions
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    # Enable broadcasting mode
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    # Set a timeout so the socket does not block
    # indefinitely when trying to receive data.
    # 2 second should be enough
    systa_socket.settimeout(2)
    # configure connection with SystaComfort
    message = b"0 1 A"
    for ip, bcastip in interfaces:
      systa_socket.sendto(message, (bcastip, self.systa_bcast_port))
      try:
        data, addr = systa_socket.recvfrom(1048)  # pylint: disable=unused-variable
        if "systa" in data.decode("iso-8859-1").lower():
          self.systaweb_ip = ip
          self.systa_bcast_ip = bcastip
          break
      except:  # pylint: disable=bare-except
        pass
    if not self.systaweb_ip:
      print("----- Search Result -----")
      print("No compatible device found on the available interfaces")
      print(f"Interfaces tested: {interfaces}")
      print("-------------------------")
      return False

    sc_info_string = data.decode("iso-8859-1").split(" ")

    self.unit_ip = sc_info_string[2]
    self.unit_name = sc_info_string[5].strip()[:-1]
    self.unit_id = sc_info_string[6]
    self.unit_app = int("0x" + self.unit_id[0:2], base=16)
    self.unit_platform = int("0x" + self.unit_id[2:4], base=16)
    self.unit_sc_version = int("0x" + self.unit_id[6:8] + self.unit_id[4:6], base=16)
    self.unit_sc_minor = int("0x" + self.unit_id[8:10], base=16)
    self.unit_base_version = sc_info_string[8]
    self.unit_mac = sc_info_string[10]

    # get communication port for S-Touch communication
    message = (self.unit_mac + " 6 A R DISP Port").encode("iso-8859-1")
    systa_socket.sendto(message, (self.systa_bcast_ip, self.systa_bcast_port))
    data, addr = systa_socket.recvfrom(1048)
    # replies seen so far:
    # 0 7 unknown value:Uremoteportalde
    # 0 7 3477\x00
    if "unknown value" in data.decode("iso-8859-1"):
      # this unit does not support S-Touch
      self.unit_stouch_port = None
      self.unit_password = None
    else:
      # this unit supports S-Touch
      self.unit_stouch_port = int(data.decode("iso-8859-1").split(" ")[2].strip()[:-1], 10)
      # get UDP password
      message = (self.unit_mac + " 6 R UDP Pass").encode("iso-8859-1")
      systa_socket.sendto(message, (self.systa_bcast_ip, self.systa_bcast_port))
      data, addr = systa_socket.recvfrom(1048)
      self.unit_password = data.decode("iso-8859-1").split(" ")[2].strip()[:-1]

    # search done, close socket
    systa_socket.close()

    print("----- Search Result -----")
    print(f"MAC: {self.unit_mac}")
    print(f"IP: {self.unit_ip}")
    print(f"Name: {self.unit_name}")
    print(f"ID: {self.unit_id}")
    print(f"App: {self.unit_app}")
    print(f"Platform: {self.unit_platform}")
    print(f"Version: {float(self.unit_sc_version) / 100.0}.{self.unit_sc_minor}")
    print(f"Base Version: {self.unit_base_version}")
    print(f"S-Touch Port: {self.unit_stouch_port}")
    print(f"UDP password: {self.unit_password}")
    print("-------------------------")

    if self.unit_platform != 9:
      # 9 = Paradigma
      # 10 = Wodtke
      print("The found system does not seem to be a unit from Paradigma")
    elif not self.unit_stouch_port:
      print("Your SystaComfort does not support the S-Touch app")
    print("-------------------------")
    return True

  def send_reply(self, data, addr, systa_socket):
    """ send the standard reply paket on packets received from Systa Comfort"""
    # send reply
    message = bytearray(data[0:16])
    for i in range(8, 16):
      message[i] = 0

    # checksum #1
    c_sum = self.mac_checksum(message)
    checksum = c_sum.to_bytes(2, "little")
    message[12] = checksum[0]
    message[13] = checksum[1]

    # checksum #2
    counter_int = int.from_bytes(data[6:8], "little")
    cnt_int = 0x3FBF
    c_sum = (counter_int + cnt_int) & 0xFFFF
    checksum = c_sum.to_bytes(2, "little")
    message[14] = checksum[0]
    message[15] = checksum[1]

    systa_socket.sendto(message, addr)
    #hex_str = "".join(format(x, "02x") for x in message)
    #print("sent reply: %s, to %s" % (hex_str, addr))

  def listen_for_device(self):
    """ listen for data packets from SystaComfort to SystaWeb.
        For this test, the SystaComfort has to be DNS faked to use the device running this script
        as destination for the data packets
    """
    print("---- Connection test ----")

    # listen for messages sent to SystaWeb
    messages_received = 0
    interfaces = []
    if self.systaweb_ip:
      interfaces.append((self.systaweb_ip, None))
    else:
      # get all available interfaces
      interfaces = self.find_system_ips()
    for ip, bcastip in interfaces:
      # bind to socket
      print(f"Listening for 60s on {ip}:{self.systaweb_port}")
      systa_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
      # make sure the socket can be reused by other functions
      systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
      systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
      # disable broadcasting mode
      systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 0)
      # Set a timeout so the socket does not block indefinitely when trying to receive data.
      # 62 second should be enough, as the SystaComfort has a send interval of 60s
      systa_socket.settimeout(61)
      systa_socket.bind((ip, self.systaweb_port))

      try:
        # listen for a new data message coming from the SystaComfort
        data, addr = systa_socket.recvfrom(1048)
        messages_received += 1

        # reply to the received message for evaluating if communication works
        # Each data update of the SystaComfort consists of 3-4 messages that have to be
        # replied to keep the communication flow alive
        # lets see if we can get two more messages, to confirm that we are connected to
        # a compatible device
        for x in range(0, 2):  # pylint: disable=unused-variable
          self.send_reply(data, addr, systa_socket)
          # listen for the next message coming from the SystaComfort
          data, addr = systa_socket.recvfrom(1048)
          messages_received += 1

        if messages_received > 0:
          # there was at least one message received on this network interface
          # TODO because we currently only support one SystaComfort, this is ok for us
          # stop checking the other interfaces
          self.systaweb_ip = ip
          self.systa_bcast_ip = bcastip
          break
      except:  # pylint: disable=bare-except
        # nothing to do
        # rx timeouts from the socket are expected to happen if the
        # connected system is not compatible or fully configured
        pass

    # function done, close socket
    systa_socket.close()

    if messages_received == 3:
      # all three messages received, communication is working fine
      print("3 / 3 messages received")
      print("your system seems to be properly configured")
    elif messages_received == 1:
      # SystaComfort is sending messages to the faked SystaWeb
      # but the reply to this message seems to be wrong, so the SystaComfort
      # does not send the next data packets.
      print("1 / 3 messages received")
      print("your system seems not to be supported at the moment")
      print("please contribute to the SystaPi project!")
    else:
      # Nothing received, so the SystaComfort is most likely
      # not configured to send the data packets to this SystaPi
      print("0 / 3 messages received")
      print("your system is not properly configured")

    print("-------------------------")

    if messages_received == 3:
      # all three messages received, communication is working fine
      return True
    # communication is not working
    return False

  def try_and_error_offset_serach(self):
    """ tries offset values for the pwd offset until a suitable value is found"""
    # listen for messages sent to SystaWeb
    systa_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
    # disable broadcasting mode
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 0)
    # bind to socket
    print(f"{datetime.now().time()} Offset search on: {(self.systaweb_ip, self.systaweb_port)}")
    systa_socket.bind((self.systaweb_ip, self.systaweb_port))
    cnt_offset = 0x10F9

    while True:
      # listen for the next message coming from the SystaComfort
      # the first message might take a while
      systa_socket.settimeout(61)
      data, addr = systa_socket.recvfrom(1048)

      # loop for waiting a number of messages before
      # actually trying to set the operation mode
      for x in range(0, 3):  # pylint: disable=unused-variable
        packet_type = data[8]
        packet_subtype = data[16]
        print(f"skip {x}: {packet_type}:{packet_subtype}")
        self.send_reply(data, addr, systa_socket)
        # listen for the next message coming from the SystaComfort
        data, addr = systa_socket.recvfrom(1048)

      packet_type = data[8]
      packet_subtype = data[16]
      print(f"reply to: {packet_type}:{packet_subtype}")
      # announce parameter change
      message = bytearray(data[0:28])
      for i in range(8, 28):
        message[i] = 0
      message[8] = 0x01
      message[12] = 0x02
      message[14] = 0x08
      message[19] = 0x31
      message[20] = 0x32
      message[21] = 0x31
      message[22] = 0x32

      # checksum #1
      c_sum = self.mac_checksum(message)
      checksum = c_sum.to_bytes(2, "little")
      message[24] = checksum[0]
      message[25] = checksum[1]

      # checksum #2
      counter_int = int.from_bytes(data[6:8], "little")
      c_sum = (counter_int + cnt_offset) & 0xFFFF
      checksum = c_sum.to_bytes(2, "little")
      message[26] = checksum[0]
      message[27] = checksum[1]

      systa_socket.sendto(message, addr)
      try:
        # to be successful an answer within one second is expected
        systa_socket.settimeout(1)
        data, addr = systa_socket.recvfrom(1048)
        hex_str = "".join(format(x, "02x") for x in data)
        print(f"received message: {hex_str}, from {addr}")
        print(f"{datetime.now().time()} Offset was correct: 0x" + (format(cnt_offset, "02x")))
        break
      except:  # pylint: disable=bare-except
        print(f"{datetime.now().time()} Offset was not correct: 0x" + (format(cnt_offset, "02x")))
        # test next offset
        #cnt_offset = (cnt_offset + 0x0100*random.randint(0,255)) & 0xFFFF #+256
        # test another random offset
        cnt_offset = (cnt_offset + 0x0100 * random.randint(0, 256)) & 0xFFFF  #+256

  def set_operation_mode(self, mode):  # pylint: disable=unused-argument
    """ set the Systa Comfort to the given operation mode"""
    # listen for messages sent to SystaWeb
    systa_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
    # disable broadcasting mode
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 0)
    # bind to socket
    print((self.systaweb_ip, self.systaweb_port))
    systa_socket.bind((self.systaweb_ip, self.systaweb_port))

    # listen for the next message coming from the SystaComfort
    data, addr = systa_socket.recvfrom(1048)
    hex_str = "".join(format(x, "02x") for x in data)
    print(f"received message: {hex_str}, from {addr}")

    # loop for waiting a number of messages before
    # actually trying to set the operation mode
    for x in range(0, 1):  # pylint: disable=unused-variable
      self.send_reply(data, addr, systa_socket)
      # listen for the next message coming from the SystaComfort
      data, addr = systa_socket.recvfrom(1048)
      hex_str = "".join(format(x, "02x") for x in data)
      print(f"received message: {hex_str}, from {addr}")

    # announce parameter change
    message = bytearray(data[0:28])
    for i in range(8, 28):
      message[i] = 0
    message[8] = 0x01
    message[12] = 0x02
    message[14] = 0x08
    message[19] = 0x31
    message[20] = 0x32
    message[21] = 0x31
    message[22] = 0x32

    # checksum #1
    c_sum = self.mac_checksum(message)
    checksum = c_sum.to_bytes(2, "little")
    message[24] = checksum[0]
    message[25] = checksum[1]

    # checksum #2
    counter_int = int.from_bytes(data[6:8], "little")
    cnt_int = 0x10F9
    c_sum = (counter_int + cnt_int) & 0xFFFF
    checksum = c_sum.to_bytes(2, "little")
    message[26] = checksum[0]
    message[27] = checksum[1]

    systa_socket.sendto(message, addr)
    hex_str = "".join(format(x, "02x") for x in message)
    print(f"sent announcment: {hex_str}, from {addr}")

    data, addr = systa_socket.recvfrom(1048)
    hex_str = "".join(format(x, "02x") for x in data)
    print(f"received message: {hex_str}, from {addr}")

  def mac_checksum(self, data):
    mac = int.from_bytes(data[4:6], "little")  # last 2 byte of MAC
    offset = 0x8e82
    if data[12] == 0x02 and data[14] == 0x08:
      offset = 0xbfb5
    elif data[12] == 0x02 and data[14] == 0x11:
      if data[16] == 0x1f:  # Room temp heat (Raum Temp Heizen)
        offset = 0x8e7e
      elif data[16] == 0x1c:  # Operating Mode (Betriebsart) ... 0=Auto1 1=Auto2 ...
        offset = 0x8e7f
      elif data[16] == 0x20:  # Room temp comf (Raum Temp Komf)
        offset = 0x8ea3
      elif data[16] == 0x21:  # Room temp low. (Raum Temp Offset)
        offset = 0x8eae
      offset += data[20]
    return (mac + offset) & 0xffff

  def find_system_ips(self):
    """checks the configured network interfaces of the host system
       Returns:
           tuple list of IP and BROADCAST IP of each configured interface
    """
    interface_list = []
    interfaces = os.popen("ifconfig -a | grep \"inet \" | grep broadcast")
    for interface in interfaces:
      interface_info = interface.strip().split(" ")
      ip = interface_info[1]
      bcastip = interface_info[7]
      interface_list.append((ip, bcastip))
    return interface_list

  def check_if_is_up(self):
    """checks if the interface configured for SystaWeb is available"""
    interface = (
        os.popen("ip -4 -o a | grep " + self.systaweb_ip + " | cut -d ' ' -f 2").read().strip())
    if not interface:
      # if the IP is not found, the interface is not up
      return False

    operstate = os.popen("cat /sys/class/net/" + interface + "/operstate").read().strip()
    if operstate == "up":
      # the interface is up
      return True
    return False


def find_pwd_offset():
  sc = SystaComfort()
  if sc.find_sc() and sc.listen_for_device():
    sc.try_and_error_offset_serach()


def test_system_configuration():
  sc = SystaComfort()
  sc.find_sc()
  sc.listen_for_device()


# -------------MAIN-----------------
if __name__ == "__main__":
  #sc.set_operation_mode(2)
  #test_system_configuration()
  find_pwd_offset()

# Copyright (c) 2024, The beep-projects contributors
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

"""Tests for reverseengineering the communication with a paradigma systacomfort"""
from __future__ import print_function
import socket
import os
from datetime import datetime
import argparse
import sys
import time


class SystaComfort(object):
  """class for communication with a Systa Comfort II unit"""

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
    self.mac_offset = 0x8E82
    self.cnt_offset = 0x3FBF
    self.packet_subtype_max = -1

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
      except KeyboardInterrupt:
        # User interrupt the program with ctrl+c
        systa_socket.close()
        sys.exit()
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

    systa_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    # make sure the socket can be reused by other functions
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    # disable broadcasting mode
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 0)
    systa_socket.settimeout(62)
    systa_socket.bind((ip, self.systaweb_port))
    try:
      packetSubType = 0
      messages_received = 0
      while messages_received < 4 and packetSubType >= self.packet_subtype_max:
        # listen for a new data message coming from the SystaComfort
        data, addr = systa_socket.recvfrom(1048)
        messages_received += 1
        packetType = data[8]
        packetSubType = data[16]
        if packetSubType > self.packet_subtype_max :
          self.packet_subtype_max = packetSubType
      print("Your SystaComfort sends " + str(self.packet_subtype_max+1) + " data packets without being triggered")
      print("-------------------------")
    except KeyboardInterrupt:
      # User interrupt the program with ctrl+c
      systa_socket.close()
      sys.exit()
    except:  # pylint: disable=bare-except
      # nothing to do
      # rx timeouts from the socket are expected to happen if the
      # connected system is not compatible or fully configured
      pass
    return True

  def send_reply(self, data, addr, systa_socket):
    """send the standard reply packet on packets received from Systa Comfort"""
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
    c_sum = self.cnt_checksum(message)
    checksum = c_sum.to_bytes(2, "little")
    message[14] = checksum[0]
    message[15] = checksum[1]

    systa_socket.sendto(message, addr)

  def listen_for_device(self):
    """listen for data packets from SystaComfort to SystaWeb.
    For this test, the SystaComfort has to be DNS faked to use the device running this script
    as destination for the data packets
    """

    # listen for messages sent to SystaWeb
    messages_received = 0
    # bind to socket
    systa_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    # make sure the socket can be reused by other functions
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    # disable broadcasting mode
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 0)
    # Set a timeout so the socket does not block indefinitely when trying to receive data.
    # 62 second should be enough, as the SystaComfort has a send interval of 60s
    systa_socket.settimeout(61)
    systa_socket.bind((self.systaweb_ip, self.systaweb_port))

    try:
      # listen for a new data message coming from the SystaComfort
      data, addr = systa_socket.recvfrom(1048)
      packetType = data[8]
      packetSubType = data[16]

      if self.packet_subtype_max > 0 and packetSubType == self.packet_subtype_max:
        # after the last sub packet, the systa comfort does not respond on triggers
        # therefore wait for the next message
        data, addr = systa_socket.recvfrom(1048)
        packetType = data[8]
        packetSubType = data[16]
          
      # reply to the received message for evaluating if communication works
      # Each data update of the SystaComfort consists of 3-4 messages that have to be
      # replied to keep the communication flow alive
      # lets see if we can get two more messages, to confirm that we are connected to
      # a compatible device
      # to be successful an answer within one second is expected
      systa_socket.settimeout(3)
      for x in range(0, 2):  # pylint: disable=unused-variable
        self.send_reply(data, addr, systa_socket)
        # listen for the next message coming from the SystaComfort
        data, addr = systa_socket.recvfrom(1048)
        messages_received += 1
        packetType = data[8]
        if data[16] > packetSubType:
          # we triggered a new message
          break
    except KeyboardInterrupt:
      # User interrupt the program with ctrl+c
      systa_socket.close()
      sys.exit()
    except:  # pylint: disable=bare-except
      # nothing to do
      # rx timeouts from the socket are expected to happen if the
      # connected system is not compatible or fully configured
      pass

    # function done, close socket
    systa_socket.close()

    if messages_received > 0:
      # we received at least one reply, communication should work fine
      return True
    # communication is not working
    return False

  def mac_checksum(self, data):
    mac = int.from_bytes(data[4:6], "little")  # last 2 byte of MAC
    return (mac + self.mac_offset) & 0xFFFF

  def cnt_checksum(self, data):
    counter = int.from_bytes(data[6:8], "little")
    return (counter + self.cnt_offset) & 0xFFFF

  def find_system_ips(self):
    """checks the configured network interfaces of the host system
    Returns:
        tuple list of IP and BROADCAST IP of each configured interface
    """
    interface_list = []
    interfaces = os.popen('ifconfig -a | grep "inet " | grep broadcast')
    for interface in interfaces:
      interface_info = interface.strip().split(" ")
      ip = interface_info[1]
      bcastip = interface_info[7]
      interface_list.append((ip, bcastip))
    return interface_list

  def check_if_is_up(self):
    """checks if the interface configured for SystaWeb is available"""
    interface = (
        os.popen("ip -4 -o a | grep " + self.systaweb_ip + " | cut -d ' ' -f 2").read().strip()
    )
    if not interface:
      # if the IP is not found, the interface is not up
      return False

    operstate = os.popen("cat /sys/class/net/" + interface + "/operstate").read().strip()
    if operstate == "up":
      # the interface is up
      return True
    return False


def find_offsets(cnt_offset_start, mac_offset_start):
  sc = SystaComfort()
  if sc.find_sc():
    with open("systapi_offset_search.txt", "a+", encoding="utf-8") as f:
      for mac_offset_add in range(0, 100):
        for cnt_offset_add in range(0, 100):
          sc.cnt_offset = (cnt_offset_start + cnt_offset_add) % 0xFFFF
          sc.mac_offset = (mac_offset_start + mac_offset_add) % 0xFFFF
          start = time.perf_counter()
          if sc.listen_for_device():
            end = time.perf_counter()
            output = (
                f"{datetime.now().time()} Offsets found: CNT = 0x"
                + (format(sc.cnt_offset, "02x"))
                + ", MAC = 0x"
                + (format(sc.mac_offset, "02x"))
            )
            print(output)
            f.write(output + "\n")
            f.close()
            return
          else:
            end = time.perf_counter()
            if end - start > 50:
              output = (
                  f"{datetime.now().time()} Offsets candidates: CNT = 0x"
                  + (format(sc.cnt_offset, "02x"))
                  + ", MAC = 0x"
                  + (format(sc.mac_offset, "02x"))
              )
            else:
              output = (
                  f"{datetime.now().time()} Offsets were not correct: CNT = 0x"
                  + (format(sc.cnt_offset, "02x"))
                  + ", MAC = 0x"
                  + (format(sc.mac_offset, "02x"))
              )
            print(output)
            f.write(output + "\n")
    output = "find_offsets finished without success"
    print(output)
    f.write(output + "\n")
    f.close()


# -------------MAIN-----------------
if __name__ == "__main__":
  parser = argparse.ArgumentParser()
  parser.add_argument("-m", "--mac", type=str)
  parser.add_argument("-c", "--cnt", type=str)
  args = parser.parse_args()

  # default start values
  mac_offset = 0x8E80
  cnt_offset = 0x3FBA

  if args.mac:
    mac_offset = int(args.mac, 16)
  if args.cnt:
    cnt_offset = int(args.cnt, 16)
  find_offsets(cnt_offset, mac_offset)

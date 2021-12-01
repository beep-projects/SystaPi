# Copyright (c) 2021, The beep-projects contributors
# this file originated from https://github.com/beep-projects
# Do not remove the lines above.
# The rest of this source code is subject to the terms of the Mozilla Public License.
# You can obtain a copy of the MPL at <https://www.mozilla.org/MPL/2.0/>.
"""Tests for reverseengineering the communication with a paradigma systacomfort

"""
from __future__ import print_function
import socket
import os

class SystaComfort(object):
  """ class for communication with a Systa Comfort II unit """

  def __init__(self):
    self.systaweb_ip = None
    self.systaweb_port = 22460
    self.systa_bcast_ip = None
    self.systa_bcast_port = 8001
    self.sc_info_string = "NULL"
    self.unit_ip = "0.0.0.0"
    self.unit_stouch_port = 0
    self.unit_name = "NULL"
    self.unit_id = 0
    self.unit_app = 0
    self.unit_platform = 0
    self.unit_sc_version = 0
    self.unit_sc_minor = 0
    self.unit_password = "NULL"
    self.unit_base_version = "NULL"
    self.unit_mac = "NULL"

  def find_sc(self):
    """check if the SystaPi is connected to any Systa Comfort"""
    # get all available interfaces
    interfaces = self.find_system_ips()
    # loop over all interfaces and try to find a Systa Comfort
    systa_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
    # Enable broadcasting mode
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    # Set a timeout so the socket does not block
    # indefinitely when trying to receive data.
    # 2 second should be enough
    systa_socket.settimeout(2)
    # configure connection with SystaComfort
    message = b"0 1 A"
    connected = False
    for ip, bcastip in interfaces:
      systa_socket.sendto(message, (bcastip, self.systa_bcast_port))
      try:
        data, addr = systa_socket.recvfrom(1048)  # pylint: disable=unused-variable
        if "systa" in data.decode("iso-8859-1").lower():
          self.systaweb_ip = ip
          self.systa_bcast_ip = bcastip
          connected = True
          break
      except: # pylint: disable=bare-except
        pass
    if not connected:
      return False

    sc_info_string = data.decode("iso-8859-1").split(" ")

    self.unit_ip = sc_info_string[2]
    self.unit_name = sc_info_string[5].strip()[:-1]
    self.unit_id = sc_info_string[6]
    self.unit_app = int("0x" + self.unit_id[0:2], base=16)
    self.unit_platform = int("0x" + self.unit_id[2:4], base=16)
    self.unit_sc_version = int(
        "0x" + self.unit_id[6:8] + self.unit_id[4:6], base=16)
    self.unit_sc_minor = int("0x" + self.unit_id[8:10], base=16)
    self.unit_base_version = sc_info_string[8]
    self.unit_mac = sc_info_string[10]

    # get communication port for S-Touch communication
    message = (self.unit_mac + " 6 A R DISP Port").encode("iso-8859-1")
    systa_socket.sendto(
        message, (self.systa_bcast_ip, self.systa_bcast_port))
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

    # init done, close socket
    systa_socket.close()

    print("---- Find SC Result ----")
    print("MAC:", self.unit_mac)
    print("IP:", self.unit_ip)
    print("S-Touch Port: %s" % self.unit_stouch_port)
    print("Name:", self.unit_name)
    print("ID:", self.unit_id)
    print("App:", self.unit_app)
    print("Platform:", self.unit_platform)
    print("SystaComfort Version: %s.%s" % (float(self.unit_sc_version) / 100.0, self.unit_sc_minor))
    print("Base Version: %s" % self.unit_base_version)
    print("UDP password: %s" % self.unit_password)
    print("---------------------")
    return True

  def send_reply(self, data, addr, systa_socket):
    """ send the standard reply paket on pakets receive from Systa Comfort"""
    # send reply
    message = bytearray(data[0:16])
    for i in range(8, 16):
      message[i] = 0

    # checksum #1
    mac_int = int.from_bytes(data[4:6], "little")
    op_int = 0x8E82
    c_sum = (mac_int + op_int) & 0xFFFF
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
    hex_str = "".join(format(x, "02x") for x in message)
    print("sent reply: %s, to %s" % (hex_str, addr))

  def set_operation_mode(self, mode):   # pylint: disable=unused-argument
    """ set the Systa Comfort to the given operation mode"""
    # listen for messages sent to SystaWeb
    systa_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEPORT, 1)
    # disable broadcasting mode
    systa_socket.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 0)
    # bind to socket
    systa_socket.bind((self.systaweb_ip, self.systaweb_port))

    # listen for the next message coming from the SystaComfort
    data, addr = systa_socket.recvfrom(1048)
    hex_str = "".join(format(x, "02x") for x in data)
    print("received message: %s, from %s" % (hex_str, addr))

    # loop for waiting a number of messages before
    # actually trying to set the operation mode
    for x in range(0, 1):   # pylint: disable=unused-variable
      self.send_reply(data, addr, systa_socket)
      # listen for the next message coming from the SystaComfort
      data, addr = systa_socket.recvfrom(1048)
      hex_str = "".join(format(x, "02x") for x in data)
      print("received message: %s, from %s" % (hex_str, addr))

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
    mac_int = int.from_bytes(data[4:6], "little")
    op_int = 0xBFB5
    c_sum = (mac_int + op_int) & 0xFFFF
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
    print("sent announcment: %s, to %s" % (hex_str, addr))

    data, addr = systa_socket.recvfrom(1048)
    hex_str = "".join(format(x, "02x") for x in data)
    print("received message: %s, from %s" % (hex_str, addr))

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


# -------------MAIN-----------------
sc = SystaComfort()
sc.find_sc()
sc.set_operation_mode(2)

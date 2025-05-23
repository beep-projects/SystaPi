#!/bin/bash
#
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
#
# This file is inspired by the firstrun.sh, generated by the Raspberry Pi Imager https://www.raspberrypi.org/software/
#
# This file will be called after the network has been configured by firstrun.sh

#######################################
# Checks if any user is holding one of the various lock files used by apt
# and waits until they become available. 
# Warning, you might get stuck forever in here
# Globals:
#   None
# Arguments:
#   None
# Outputs:
#   None
#######################################
function waitForApt() {
  while sudo fuser /var/{lib/{dpkg,apt/lists},cache/apt/archives}/lock >/dev/null 2>&1; do
   echo ["$(date +%T)"] waiting for access to apt lock files ...
   sleep 1
  done
}

#######################################
# Checks if internet cann be accessed
# and waits until they become available. 
# Warning, you might get stuck forever in here
# Globals:
#   None
# Arguments:
#   None
# Outputs:
#   None
#######################################
function waitForInternet() {
  #until nc -zw1 google.com 443 >/dev/null 2>&1;  do
  #newer Raspberry Pi OS versions do not have nc preinstalled, but wget is still there
  while ! wget -q --spider http://google.com; do
    echo ["$(date +%T)"] waiting for internet access ...
    sleep 1
  done
}

# redirect output to 'secondrun.log':
exec 3>&1 4>&2
trap 'exec 2>&4 1>&3' 0 1 2 3
exec 1>/boot/secondrun.log 2>&1

echo "START secondrun.sh"
echo "This script is running as user: $( whoami )"
#the following variables should be set by firstrun.sh
IP_PREFIX="192.168.1"
#configured user name
USERNAME=beep

#enable ipv4 forwarding, which is required by ~/helpers/enable_stouch.nft
sudo sed -i "s/^#net\.ipv4\.ip_forward=1/net\.ipv4\.ip_forward=1/" /etc/sysctl.conf

#internet connectivity is required for installing required packages and updating the system
waitForInternet

#network should be up, update the system
echo "updating the system"
waitForApt
sudo apt update --allow-releaseinfo-change # bookworn introduced an issue with the release files being not valid
waitForApt
sudo apt full-upgrade -y
# do it again, because it seems to fix the bookworm release file issues
sudo apt update --allow-releaseinfo-change # bookworn introduced an issue with the release files being not valid
waitForApt
sudo apt full-upgrade -y
# do it again, because it seems to fix the bookworm release file issues
sudo apt update --allow-releaseinfo-change # bookworn introduced an issue with the release files being not valid
waitForApt
sudo apt full-upgrade -y

echo ""
echo "configure eth0 to use ${IP_PREFIX}.1 as static IP address"
#echo "configuring /etc/dhcpcd.conf"
##sudo echo "" >> /etc/dhcpcd.conf
##sudo echo "#configuring a static IP for connecting to the Paradigma Systa Comfort II" >> /etc/dhcpcd.conf
##sudo echo "interface eth0" >> /etc/dhcpcd.conf
##sudo echo "static ip_address=${IP_PREFIX}.1/24" >> /etc/dhcpcd.conf
#echo "" | sudo tee -a /etc/dhcpcd.conf > /dev/null
#echo "#configuring a static IP for connecting to the Paradigma Systa Comfort II" | sudo tee -a /etc/dhcpcd.conf > /dev/null
#echo "interface eth0" | sudo tee -a /etc/dhcpcd.conf > /dev/null
#echo "static ip_address=${IP_PREFIX}.1/24" | sudo tee -a /etc/dhcpcd.conf > /dev/null
# bookworm uses network manager
#nmcli connection add type ethernet id "SystaConnect" interface-name eth0 autoconnect yes autoconnect-priority 1337 ipv4.method manual ipv4.addresses ${IP_PREFIX}.1/24 ipv6.method disabled
sudo nmcli connection add type ethernet con-name "SystaConnect" ifname eth0 autoconnect yes ipv4.method manual ipv4.addresses 192.168.11.1/24 ipv6.method disabled
sudo nmcli connection up "SystaConnect"


#sudo systemctl daemon-reload
#install dnsmasq for DNS spoofing
echo "installing dnsmasq"
waitForApt
sudo apt install -y dnsmasq

echo "configuring /etc/dnsmasq.conf"
#sudo echo "" >> /etc/dnsmasq.conf
#sudo echo "#configuration to fake the remote portal for the Paradigma Systa Comfort II" >> /etc/dnsmasq.conf
#sudo echo "dhcp-range=${IP_PREFIX}.10,${IP_PREFIX}.25,12h  ## 12h is the lease time" >> /etc/dnsmasq.conf
#sudo echo "interface=eth0" >> /etc/dnsmasq.conf
#sudo echo "listen-address=${IP_PREFIX}.1" >> /etc/dnsmasq.conf
#sudo echo "no-dhcp-interface=wlan0" >> /etc/dnsmasq.conf
#sudo echo "address=/paradigma.remoteportal.de/${IP_PREFIX}.1" >> /etc/dnsmasq.conf 
echo "" | sudo tee -a /etc/dnsmasq.conf > /dev/null
echo "#configuration to fake the remote portal for the Paradigma Systa Comfort II" | sudo tee -a /etc/dnsmasq.conf > /dev/null
echo "dhcp-range=${IP_PREFIX}.10,${IP_PREFIX}.25,12h  ## 12h is the lease time" | sudo tee -a /etc/dnsmasq.conf > /dev/null
echo "interface=eth0" | sudo tee -a /etc/dnsmasq.conf > /dev/null
echo "listen-address=${IP_PREFIX}.1" | sudo tee -a /etc/dnsmasq.conf > /dev/null
echo "no-dhcp-interface=wlan0" | sudo tee -a /etc/dnsmasq.conf > /dev/null
echo "address=/paradigma.remoteportal.de/${IP_PREFIX}.1" | sudo tee -a /etc/dnsmasq.conf > /dev/null
echo "local-ttl=3600" | sudo tee -a /etc/dnsmasq.conf > /dev/null

echo "configuring /etc/dnsmasq.hosts"
#sudo echo "" >> /etc/dnsmasq.hosts
#sudo echo "#configure DNS spoofing for paradigma.remoteportal.de" >> /etc/dnsmasq.hosts
#sudo echo "${IP_PREFIX}.1 paradigma.remoteportal.de" >> /etc/dnsmasq.hosts
echo "" | sudo tee -a /etc/dnsmasq.hosts > /dev/null
echo "#configure DNS spoofing for paradigma.remoteportal.de" | sudo tee -a /etc/dnsmasq.hosts > /dev/null
echo "${IP_PREFIX}.1 paradigma.remoteportal.de" | sudo tee -a /etc/dnsmasq.hosts > /dev/null

echo "restart dnsmasq"
sudo sudo systemctl restart dnsmasq.service

#see https://www.azul.com/downloads/?architecture=arm-32-bit-hf&package=jdk for available versions
if [ ! -d /usr/lib/jvm ]; then
  sudo mkdir /usr/lib/jvm
fi
cd /usr/lib/jvm || exit 1

#AZUL_URL="https://cdn.azul.com/zulu-embedded/bin/zulu11.48.21-ca-jdk11.0.11-linux_aarch32hf.tar.gz"
#AZUL_URL="https://cdn.azul.com/zulu-embedded/bin/zulu11.50.19-ca-jdk11.0.12-linux_aarch32hf.tar.gz"
#AZUL_URL="https://cdn.azul.com/zulu-embedded/bin/zulu11.66.19-ca-jdk11.0.20.1-linux_aarch32hf.tar.gz"
#AZUL_URL="https://cdn.azul.com/zulu-embedded/bin/zulu11.70.15-ca-hl-jdk11.0.22-linux_aarch32hf.tar.gz"
#AZUL_URL="https://cdn.azul.com/zulu-embedded/bin/zulu11.78.15-ca-jdk11.0.26-linux_aarch32hf.tar.gz"
AZUL_URL="https://cdn.azul.com/zulu-embedded/bin/zulu11.80.21-ca-jdk11.0.27-linux_aarch32hf.tar.gz"
ARCH=$(arch)
echo "install OpenJDK build from Azul for detected architecture: ${ARCH}"
if [[ "$ARCH" == "armv8l" || "$ARCH" == "aarch64" ]]; then
    AZUL_URL="https://cdn.azul.com/zulu/bin/zulu11.80.21-ca-jdk11.0.27-linux_aarch64.tar.gz"
fi
AZUL_FILE_NAME=${AZUL_URL##*/}
AZUL_BUILD_NAME=${AZUL_FILE_NAME%.tar.gz}
sudo wget "${AZUL_URL}"
sudo tar -xzvf "${AZUL_FILE_NAME}"
sudo rm "${AZUL_FILE_NAME}"
sudo update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/${AZUL_BUILD_NAME}/bin/java" 1
sudo update-alternatives --install "/usr/bin/javac" "javac" "/usr/lib/jvm/${AZUL_BUILD_NAME}/bin/javac" 1

#copy SystaRESTServer to the ${USERNAME} home folder for easy access
echo "copy SystaRESTServer from /boot to /home/${USERNAME}"
cp -R /boot/SystaRESTServer /home/${USERNAME}/

echo "build SystaRESTServer from source files"
(cd /home/${USERNAME}/SystaRESTServer || exit 1 ; ./build.sh)

#make sure all the files belong to the user ${USERNAME}
sudo chown -R ${USERNAME}:${USERNAME} /home/${USERNAME}/SystaRESTServer

#copy helpers to the ${USERNAME} home folder for easy access
echo "copy helpers from /boot to /home/${USERNAME}"
cp -R /boot/helpers /home/${USERNAME}/

#make sure all the files belong to the user ${USERNAME}
sudo chown -R ${USERNAME}:${USERNAME} /home/${USERNAME}/helpers

#create service file
echo "create service file /etc/systemd/system/SystaRESTServer.service"
cat <<EOF >/etc/systemd/system/SystaRESTServer.service
[Unit]
Description=Systa REST API Server
Wants=network-online.target
After=network-online.target

[Service]
Type=simple
User=${USERNAME}
WorkingDirectory=/home/${USERNAME}/SystaRESTServer
ExecStart=/usr/bin/java -Dfile.encoding=UTF-8 -classpath /home/${USERNAME}/SystaRESTServer/bin:/home/${USERNAME}/SystaRESTServer/lib/* de.freaklamarsch.systarest.SystaRESTServer
Restart=on-failure
RestartSec=30s

[Install]
WantedBy=multi-user.target

EOF

#reload systemd to make the daemon aware of the new configuration
echo "reload services and enable SystaRESTServer.service"
sudo systemctl --system daemon-reload
#enable service
sudo systemctl enable SystaRESTServer

#clean up
echo "remove autoinstalled packages" 
waitForApt
echo "sudo apt -y autoremove"
sudo apt -y autoremove

echo "add run /boot/thirdrun.sh command to cmdline.txt file for next reboot"
sudo sed -i '$s|$| systemd.run=/boot/thirdrun.sh systemd.run_success_action=reboot systemd.unit=kernel-command-line.target\n|' /boot/cmdline.txt

#disable the service that started this script
sudo systemctl disable secondrun.service
echo "DONE secondrun.sh, rebooting the system"

sleep 2
sudo reboot

#!/bin/bash

set +e

echo "START secondrun.sh"

#network should be up, update the system
echo "updating the system"
sudo apt update
sudo apt full-upgrade -y

#install dnsmasq for DNS spoofing
echo "installing dnsmasq"
sudo apt install -y dnsmasq

echo ""
echo "configure eth0 to use 192.168.1.1 as static IP address"
echo "configuring /etc/dhcpcd.conf"
sudo echo "" >> /etc/dhcpcd.conf
sudo echo "#configuring a static IP for connecting to the Paradigma Systa Comfort II" >> /etc/dhcpcd.conf
sudo echo "interface eth0" >> /etc/dhcpcd.conf
sudo echo "static ip_address=192.168.1.1/24" >> /etc/dhcpcd.conf

systemctl daemon-reload

echo "configuring /etc/dnsmasq.conf"
sudo echo "" >> /etc/dnsmasq.conf
sudo echo "#configuration to fake the remote portal for the Paradigma Systa Comfort II" >> /etc/dnsmasq.conf
sudo echo "dhcp-range=192.168.1.10,192.168.1.25,12h  ## 12h is the lease time" >> /etc/dnsmasq.conf
sudo echo "interface=eth0" >> /etc/dnsmasq.conf
sudo echo "listen-address=192.168.1.1" >> /etc/dnsmasq.conf
sudo echo "no-dhcp-interface=wlan0" >> /etc/dnsmasq.conf
sudo echo "address=/paradigma.remoteportal.de/192.168.1.1" >> /etc/dnsmasq.conf 

echo "configuring /etc/dnsmasq.hosts"
sudo echo "" >> /etc/dnsmasq.hosts
sudo echo "#configure DNS spoofing for paradigma.remoteportal.de" >> /etc/dnsmasq.hosts
sudo echo "192.168.1.1 paradigma.remoteportal.de" >> /etc/dnsmasq.hosts

echo "restart dnsmasq"
sudo /etc/init.d/dnsmasq restart 

#see https://www.azul.com/downloads/?package=jdk#download-openjdk for available versions
echo "install OpenJDK build from Azul for Pi Zero (ARM 32-bit HF v6)"
if [ ! -d /usr/lib/jvm ]; then
  sudo mkdir /usr/lib/jvm
fi
cd /usr/lib/jvm
sudo wget https://cdn.azul.com/zulu-embedded/bin/zulu11.48.21-ca-jdk11.0.11-linux_aarch32hf.tar.gz
sudo tar -xzvf zulu11.48.21-ca-jdk11.0.11-linux_aarch32hf.tar.gz 
sudo rm zulu11.48.21-ca-jdk11.0.11-linux_aarch32hf.tar.gz
sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/zulu11.48.21-ca-jdk11.0.11-linux_aarch32hf/bin/java 1
sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/zulu11.48.21-ca-jdk11.0.11-linux_aarch32hf/bin/javac 1


#copy SystaRESTServer to the pi homefolder for easy access
echo "move SystaRESTServer from /boot to /home/pi"
mv /boot/SystaRESTServer /home/pi/
sudo chown -R pi:pi /home/pi/SystaRESTServer

#create service file
echo "create service file /etc/systemd/system/SystaRESTServer.service"
cat <<EOF >/etc/systemd/system/SystaRESTServer.service
[Unit]
Description=Systa REST API Server
Wants=network-online.target
After=network-online.target

[Service]
Type=simple
User=pi
WorkingDirectory=/home/pi/SystaRESTServer
ExecStart=/usr/bin/java -Dfile.encoding=UTF-8 -classpath /home/pi/SystaRESTServer/bin:/home/pi/SystaRESTServer/lib/* de.freaklamarsch.systarest.SystaRESTServer
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
sudo apt -y autoremove

echo "add run /boot/thirdrun.sh command to cmdline.txt file for next reboot"
sudo sed -i '$s|$| systemd.run=/boot/thirdrun.sh systemd.run_success_action=reboot systemd.unit=kernel-command-line.target\n|' /boot/cmdline.txt

#disable the service that started this script
sudo systemctl disable secondrun.service
echo "DONE secondrun.sh, rebooting the system"
sudo reboot
exit 0
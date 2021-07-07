#!/bin/bash

#this script is run as root, no need for sudo

set +e
echo "START firstrun.sh"

#-------------------------------------------------------------------------------
#----------------------- START OF CONFIGURATION --------------------------------
#-------------------------------------------------------------------------------

# which hostname do you want to give your raspberry pi?
HOSTNAME=systapi
# configure the wifi connection
# the example WPA_PASSPHRASE is generated via
#     wpa_passphrase MY_WIFI passphrase
# but you also can enter your passphrase as plain text, if you accept the potential insecurity of that approach
SSID=MY_WIFI
WPA_PASSPHRASE=3755b1112a687d1d37973547f94d218e6673f99f73346967a6a11f4ce386e41e
# configure your timezone and key board settings
TIMEZONE="Europe/Berlin"
COUNTRY="DE"
XKBMODEL="pc105"
XKBLAYOUT=$COUNTRY
XKBVARIANT=""
XKBOPTIONS=""
# if you want to use an ENC28J60 Ethernet HAT, enable it here
ENABLE_ENC28J60=true

#-------------------------------------------------------------------------------
#------------------------ END OF CONFIGURATION ---------------------------------
#-------------------------------------------------------------------------------

echo "setting hostname"

CURRENT_HOSTNAME=`cat /etc/hostname | tr -d " \t\n\r"`
echo $HOSTNAME >/etc/hostname
sed -i "s/127.0.1.1.*$CURRENT_HOSTNAME/127.0.1.1\t$HOSTNAME/g" /etc/hosts

echo "setting network options"

systemctl enable ssh
cat >/etc/wpa_supplicant/wpa_supplicant.conf <<WPAEOF
ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
country=$COUNTRY
#ap_scan=1
update_config=1
network={
	ssid="$SSID"
	psk=$WPA_PASSPHRASE
}

WPAEOF
chmod 600 /etc/wpa_supplicant/wpa_supplicant.conf
rfkill unblock wifi
for filename in /var/lib/systemd/rfkill/*:wlan ; do
  echo 0 > $filename
done
rm -f /etc/xdg/autostart/piwiz.desktop
rm -f /etc/localtime

echo "setting timezone and keyboard layout"
echo $TIMEZONE >/etc/timezone
dpkg-reconfigure -f noninteractive tzdata
cat >/etc/default/keyboard <<KBEOF
XKBMODEL=$XKBMODEL
XKBLAYOUT=$XKBLAYOUT
XKBVARIANT=$XKBVARIANT
XKBOPTIONS=$XKBOPTIONS
KBEOF
dpkg-reconfigure -f noninteractive keyboard-configuration

if $ENABLE_ENC28J60; then
  echo "enable enc28j60 overlay"
  LINE='dtoverlay=enc28j60'
  FILE='/boot/config.txt'
  grep -qxF -- "$LINE" "$FILE" || echo "$LINE" >> "$FILE"
fi

#clean up
echo "removing firstrun.sh from the system"
rm -f /boot/firstrun.sh
sed -i 's| systemd.run.*||g' /boot/cmdline.txt

echo "installing secondrun.service"
# make sure secondrun.sh is executed at next boot. 
# we will need network up and running, so we install the script as a service that depends on network
cat <<EOF >/etc/systemd/system/secondrun.service
[Unit]
Description=SecondRun
After=network.target
Before=rc-local.service
ConditionFileNotEmpty=/boot/secondrun.sh

[Service]
ExecStart=/boot/secondrun.sh
Type=oneshot
RemainAfterExit=no

[Install]
WantedBy=multi-user.target

EOF
#reload systemd to make the daemon aware of the new configuration
systemctl --system daemon-reload
#enable service
systemctl enable secondrun.service
echo "DONE firstrun.sh"

exit 0

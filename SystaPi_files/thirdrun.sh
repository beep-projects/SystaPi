#!/bin/bash

#this script is run as root, no need for sudo

set +e
echo "START thirdrun.sh"

#disable service
systemctl stop secondrun.service
systemctl disable secondrun.service
#remove service file
rm /etc/systemd/system/secondrun.service
#reload systemd to make the daemon aware of the new configuration
systemctl --system daemon-reload
systemctl reset-failed
#clean up
rm -f /boot/secondrun.sh
rm -f /boot/thirdrun.sh
sed -i 's| systemd.run.*||g' /boot/cmdline.txt

echo "DONE thirdrun.sh"

exit 0

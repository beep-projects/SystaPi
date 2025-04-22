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
# this script is run as root, no need for sudo

echo "START thirdrun.sh"
echo "This script is running as user: $( whoami )"

#disable service
systemctl stop secondrun.service
systemctl disable secondrun.service
#remove service file
rm /etc/systemd/system/secondrun.service
#reload systemd to make the daemon aware of the new configuration
systemctl --system daemon-reload
systemctl reset-failed
#clean up
#rm -rf /boot/install
#rm -f /boot/firstrun.log
#rm -f /boot/secondrun.log
#rm -f /boot/secondrun.sh
#rm -f /boot/thirdrun.sh
sed -i 's| systemd.run.*||g' /boot/cmdline.txt

echo "DONE thirdrun.sh"

exit 0

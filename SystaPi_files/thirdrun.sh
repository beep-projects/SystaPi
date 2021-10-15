#!/bin/bash
#
# Copyright (c) 2021, The beep-projects contributors
# this file originated from https://github.com/beep-projects
# Do not remove the lines above.
# The rest of this source code is subject to the terms of the Mozilla Public License.
# You can obtain a copy of the MPL at <https://www.mozilla.org/MPL/2.0/>.
#
# this script is run as root, no need for sudo

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
#rm -f /boot/secondrun.sh
#rm -f /boot/thirdrun.sh
sed -i 's| systemd.run.*||g' /boot/cmdline.txt

echo "DONE thirdrun.sh"

exit 0

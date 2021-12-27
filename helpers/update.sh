#!/bin/bash

# Copyright (c) 2021, The beep-projects contributors
# this file originated from https://github.com/beep-projects
# Do not remove the lines above.
# The rest of this source code is subject to the terms of the Mozilla Public License.
# You can obtain a copy of the MPL at <https://www.mozilla.org/MPL/2.0/>.

# make sure we are in the home directory
cd ~ || exit

# remove existing downloads
rm main.zip
rm -r SystaPi-main

# get lates version from github
wget https://github.com/beep-projects/SystaPi/archive/refs/heads/main.zip
unzip main.zip 

# build new version
cd SystaPi-main/SystaRESTServer/ || exit
./build.sh 
cd ~ || exit

# stop the SystaRESTServer.service
sudo systemctl stop SystaRESTServer.service 

# remove old binaries and install new ones
rm -r /home/pi/SystaRESTServer/
cp -r /home/pi/SystaPi-main/SystaRESTServer/ /home/pi/

# restart the SystaRESTServer.service
sudo systemctl start SystaRESTServer.service 

exit 0

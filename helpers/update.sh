#!/bin/bash

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

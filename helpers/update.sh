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
#
#
# Poor man's update of the SystaRESTServer to the version of a specific branch
# Downloads the zip file, replaces the local SystaRESTServer with the one from the zip and restarts SystaRESTServer.service 
#

# Initialize all the option variables.
# This ensures we are not contaminated by variables from the environment.
# default branch to update from is main
branch="main"

#######################################
# Show help.
# Globals:
#   None
# Arguments:
#   None
# Outputs:
#   Prints usage information to stdout
#######################################
help() {
cat <<ENDE
  Usage: ${0##*/} [-b BRANCHNAME]
  Download the branch BRANCHNAME from beep-projects repository and install the SystaRESTServer

    -h/-?/--help              display this help and exit
    -b/--branch BRANCHNAME    name of branch to install SystaRESTServer from

ENDE
}

while :; do
  case $1 in
    -h|-\?|--help)
      help # show help for this script
      exit
      ;;
    -b|--branch) # Takes an option argument; ensure it has been specified.
      if [ "$2" ]; then
        branch=$2
        shift
      else
        echo 'ERROR: "--branch" requires a non-empty option argument.'
        exit
      fi
      ;;
    --) # End of all options.
      shift
      break
      ;;
    -?*)
      echo 'WARN: Unknown option (ignored): %s\n' "$1" >&2
      ;;
    *) # Default case: No more options, so break out of the loop.
      break
  esac
  shift
done

# make sure we are in the home directory
cd ~ || exit

# remove existing downloads
if [[ -f "${branch}.zip" ]]; then
  rm "${branch}.zip"
fi
if [[ -d "SystaPi-${branch}" ]]; then
  rm -r "SystaPi-${branch}"
fi

# get lates version from github
wget "https://github.com/beep-projects/SystaPi/archive/refs/heads/${branch}.zip"
unzip "${branch}.zip" 

# build new version
cd "SystaPi-${branch}/SystaRESTServer/" || exit
./build.sh 
cd ~ || exit

# stop the SystaRESTServer.service
sudo systemctl stop SystaRESTServer.service 

# remove old binaries and install new ones
rm -r /home/pi/SystaRESTServer/
cp -r "/home/pi/SystaPi-${branch}/SystaRESTServer/" /home/pi/
# remove created files
rm "${branch}.zip"
rm -r "SystaPi-${branch}"

# restart the SystaRESTServer.service
sudo systemctl start SystaRESTServer.service 

exit 0

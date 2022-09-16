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

function error {
    printf "%s\n" "${1}" >&2 ## Send message to stderr.
    exit "${2-1}" ## Return a code specified by $2, or 1 by default.
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
        printf 'ERROR: "--branch" requires a non-empty option argument.'
        exit
      fi
      ;;
    --) # End of all options.
      shift
      break
      ;;
    -?*)
      printf 'WARN: Unknown option (ignored): %s\n' "$1" >&2
      ;;
    *) # Default case: No more options, so break out of the loop.
      break
  esac
  shift
done

# make sure we are in the home directory
cd "${HOME}" || error "Cannot cd into ${HOME}"

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

# replace the helpers folder
rm -rf "${HOME}/helpers/"
cp -r "SystaPi-${branch}/helpers" "${HOME}"
chmod 755 "${HOME}/helpers/update.sh"

# build new version
cd "SystaPi-${branch}/SystaRESTServer/" || error "Cannot cd into SystaPi-${branch}/SystaRESTServer/"
sudo chmod 755 ./build.sh
./build.sh 2>&1
cd "${HOME}" || error "Cannot cd into ${HOME}"

# stop the SystaRESTServer.service
sudo systemctl stop SystaRESTServer.service 

# remove old binaries and install new ones
rm -r "${HOME}/SystaRESTServer"
cp -r "${HOME}/SystaPi-${branch}/SystaRESTServer" "${HOME}"
# remove created files
rm "${branch}.zip"
rm -r "SystaPi-${branch}"

# clear systemd logs 
sudo journalctl --rotate
sudo journalctl --vacuum-time=1s

# restart the SystaRESTServer.service
sudo systemctl start SystaRESTServer.service 

exit 0

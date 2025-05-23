#!/usr/bin/bash
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

rm -rf ./bin/*
cp ./src/SystaREST.properties ./bin/
cp ./src/rawdatamonitor.html ./bin/
cp ./src/fakeremoteportal.html ./bin/
cp ./src/systapidashboard.html ./bin/

# Construct classpath
CP="./bin"
for jarfile in ./lib/*.jar; do
  # Exclude test-specific JARs for the main build script
  if [[ "$jarfile" != *"jersey-test-framework-core"* && \
        "$jarfile" != *"jersey-test-framework-provider-grizzly2"* && \
        "$jarfile" != *"hamcrest-core"* ]]; then
    CP="$CP:$jarfile"
  fi
done

javac -encoding utf8 -d ./bin/ -classpath "$CP" ./src/de/freaklamarsch/systarest/*.java

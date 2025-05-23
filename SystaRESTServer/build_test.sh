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

# Construct classpath
CP_TEST="./bin"
for jarfile in ./lib/*.jar; do
  CP_TEST="$CP_TEST:$jarfile"
done
CP_TEST="$CP_TEST:../.github/workflows/junit-platform-console-standalone-1.8.1.jar"

javac -encoding utf8 -d ./bin/ -classpath "$CP_TEST" ./src/de/freaklamarsch/systarest/*.java ./src/de/freaklamarsch/systarest/tests/*.java
cp ./src/SystaREST.properties ./bin/
cp ./src/rawdatamonitor.html ./bin/
cp ./src/fakeremoteportal.html ./bin/
cp ./src/systapidashboard.html ./bin/
cp ./src/de/freaklamarsch/systarest/tests/*.txt ./bin/de/freaklamarsch/systarest/tests/ 
cp ./src/de/freaklamarsch/systarest/tests/*.h ./bin/de/freaklamarsch/systarest/tests/

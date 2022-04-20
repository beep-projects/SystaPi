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
cp ./src/systapidashboard_flat.html ./bin/systapidashboard.html
javac -encoding utf8 -d ./bin/ -classpath ./bin:./lib/aopalliance-repackaged-3.0.1.jar:./lib/hk2-api-3.0.1.jar:./lib/hk2-locator-3.0.1.jar:./lib/hk2-utils-3.0.1.jar:./lib/jakarta.inject-api-2.0.0.jar:./lib/jakarta.json.bind-api-2.0.0.jar:./lib/jakarta.json-2.0.0-module.jar:./lib/jakarta.json-api-2.0.0.jar:./lib/jakarta.persistence-api-3.0.0.jar:./lib/jakarta.servlet-api-5.0.0.jar:./lib/jakarta.validation-api-3.0.0.jar:./lib/jakarta.ws.rs-api-3.0.0.jar:./lib/jakarta.ws.rs-api-3.0.0-sources.jar:./lib/javassist-3.25.0-GA.jar:./lib/jersey-client-3.0.2.jar:./lib/jersey-common-3.0.2.jar:./lib/jersey-container-jdk-http-3.0.2.jar:./lib/jersey-container-servlet-3.0.2.jar:./lib/jersey-container-servlet-core-3.0.2.jar:./lib/jersey-hk2-3.0.2.jar:./lib/jersey-media-jaxb-3.0.2.jar:./lib/jersey-media-json-binding-3.0.2.jar:./lib/jersey-media-sse-3.0.2.jar:./lib/jersey-server-3.0.2.jar:./lib/org.osgi.core-6.0.0.jar:./lib/osgi-resource-locator-1.0.3.jar:./lib/yasson-2.0.1.jar:./lib/jersey-test-framework-core-3.0.2.jar:./lib/jersey-test-framework-provider-grizzly2-3.0.2.jar:../.github/workflows/junit-platform-console-standalone-1.8.1.jar ./src/de/freaklamarsch/systarest/*.java ./src/de/freaklamarsch/systarest/tests/*

#!/usr/bin/bash
#
# Copyright (c) 2021, The beep-projects contributors
# this file originated from https://github.com/beep-projects
# Do not remove the lines above.
# The rest of this source code is subject to the terms of the Mozilla Public License.
# You can obtain a copy of the MPL at <https://www.mozilla.org/MPL/2.0/>.
#

rm -rf ./bin/*
cp ./src/SystaREST.properties ./bin/SystaREST.properties
javac -d ./bin/ -classpath ./bin:./lib/aopalliance-repackaged-3.0.1.jar:./lib/hk2-api-3.0.1.jar:./lib/hk2-locator-3.0.1.jar:./lib/hk2-utils-3.0.1.jar:./lib/jakarta.inject-api-2.0.0.jar:./lib/jakarta.json.bind-api-2.0.0.jar:./lib/jakarta.json-2.0.0-module.jar:./lib/jakarta.json-api-2.0.0.jar:./lib/jakarta.persistence-api-3.0.0.jar:./lib/jakarta.servlet-api-5.0.0.jar:./lib/jakarta.validation-api-3.0.0.jar:./lib/jakarta.ws.rs-api-3.0.0.jar:./lib/jakarta.ws.rs-api-3.0.0-sources.jar:./lib/javassist-3.25.0-GA.jar:./lib/jersey-client-3.0.2.jar:./lib/jersey-common-3.0.2.jar:./lib/jersey-container-jdk-http-3.0.2.jar:./lib/jersey-container-servlet-3.0.2.jar:./lib/jersey-container-servlet-core-3.0.2.jar:./lib/jersey-hk2-3.0.2.jar:./lib/jersey-media-jaxb-3.0.2.jar:./lib/jersey-media-json-binding-3.0.2.jar:./lib/jersey-media-sse-3.0.2.jar:./lib/jersey-server-3.0.2.jar:./lib/org.osgi.core-6.0.0.jar:./lib/osgi-resource-locator-1.0.3.jar:./lib/yasson-2.0.1.jar:./lib/jersey-test-framework-core-3.0.2.jar:./lib/jersey-test-framework-provider-grizzly2-3.0.2.jar:../.github/workflows/junit-platform-console-standalone-1.8.1.jar ./src/de/freaklamarsch/systarest/*.java ./src/de/freaklamarsch/systarest/tests/*
#!/bin/bash
set -e

# Script to update JAR files in the SystaRESTServer/lib directory.
# This script is based on manual research to identify the latest compatible versions
# and their download URLs.

echo "Starting JAR update process for SystaRESTServer/lib..."

JAR_DIR="SystaRESTServer/lib"

# Ensure the target directory exists
if [ ! -d "$JAR_DIR" ]; then
  echo "Error: Target directory $JAR_DIR does not exist. Please create it first."
  exit 1
fi

echo "Target directory: $JAR_DIR"

# --- JAR Update Definitions ---
# Structure:
# declare -A jar_updates
# jar_updates[OLD_FILENAME]="NEW_FILENAME|URL"
# jar_updates[OLD_FILENAME_TO_REMOVE_ONLY]="REMOVE_ONLY"

declare -A jar_updates
# Old Filename -> New Filename|New URL
jar_updates["aopalliance-repackaged-3.0.1.jar"]="aopalliance-repackaged-3.0.4.jar|https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-aopalliance-repackaged/3.0.4/hk2-aopalliance-repackaged-3.0.4.jar"
jar_updates["grizzly-framework-3.0.0.jar"]="grizzly-framework-3.0.1.jar|https://repo1.maven.org/maven2/org/glassfish/grizzly/grizzly-framework/3.0.1/grizzly-framework-3.0.1.jar"
jar_updates["grizzly-http-3.0.0.jar"]="grizzly-http-3.0.1.jar|https://repo1.maven.org/maven2/org/glassfish/grizzly/grizzly-http/3.0.1/grizzly-http-3.0.1.jar"
jar_updates["grizzly-http-server-3.0.0.jar"]="grizzly-http-server-3.0.1.jar|https://repo1.maven.org/maven2/org/glassfish/grizzly/grizzly-http-server/3.0.1/grizzly-http-server-3.0.1.jar"
jar_updates["grizzly-http-servlet-3.0.0.jar"]="grizzly-http-servlet-3.0.1.jar|https://repo1.maven.org/maven2/org/glassfish/grizzly/grizzly-http-servlet/3.0.1/grizzly-http-servlet-3.0.1.jar"
jar_updates["hamcrest-core-1.3.jar"]="hamcrest-2.2.jar|https://repo1.maven.org/maven2/org/hamcrest/hamcrest/2.2/hamcrest-2.2.jar"
jar_updates["hk2-api-3.0.1.jar"]="hk2-api-3.0.4.jar|https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-api/3.0.4/hk2-api-3.0.4.jar"
jar_updates["hk2-locator-3.0.1.jar"]="hk2-locator-3.0.4.jar|https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-locator/3.0.4/hk2-locator-3.0.4.jar"
jar_updates["hk2-utils-3.0.1.jar"]="hk2-utils-3.0.4.jar|https://repo1.maven.org/maven2/org/glassfish/hk2/hk2-utils/3.0.4/hk2-utils-3.0.4.jar"
jar_updates["istack-commons-runtime-4.0.1.jar"]="istack-commons-runtime-4.1.2.jar|https://repo1.maven.org/maven2/com/sun/istack/istack-commons-runtime/4.1.2/istack-commons-runtime-4.1.2.jar"
jar_updates["jakarta.activation-2.0.1.jar"]="jakarta.activation-2.0.1.jar|https://repo1.maven.org/maven2/com/sun/activation/jakarta.activation/2.0.1/jakarta.activation-2.0.1.jar" # No version change
jar_updates["jakarta.annotation-api-2.0.0.jar"]="jakarta.annotation-api-2.1.1.jar|https://repo1.maven.org/maven2/jakarta/annotation/jakarta.annotation-api/2.1.1/jakarta.annotation-api-2.1.1.jar"
jar_updates["jakarta.inject-api-2.0.0.jar"]="jakarta.inject-api-2.0.1.jar|https://repo1.maven.org/maven2/jakarta/inject/jakarta.inject-api/2.0.1/jakarta.inject-api-2.0.1.jar"
jar_updates["jakarta.json-2.0.0-module.jar"]="REMOVE_ONLY" # Will be replaced by separate jakarta.json and jakarta.json-api updates
jar_updates["jakarta.json-api-2.0.0.jar"]="jakarta.json-api-2.1.1.jar|https://repo1.maven.org/maven2/jakarta/json/jakarta.json-api/2.1.1/jakarta.json-api-2.1.1.jar"
jar_updates["jakarta.json.bind-api-2.0.0.jar"]="jakarta.json.bind-api-2.0.0.jar|https://repo1.maven.org/maven2/jakarta/jsonb-api/jakarta.json.bind-api/2.0.0/jakarta.json.bind-api-2.0.0.jar" # No version change
jar_updates["jakarta.persistence-api-3.0.0.jar"]="jakarta.persistence-api-3.1.0.jar|https://repo1.maven.org/maven2/jakarta/persistence/jakarta.persistence-api/3.1.0/jakarta.persistence-api-3.1.0.jar"
jar_updates["jakarta.servlet-api-5.0.0.jar"]="jakarta.servlet-api-5.0.0.jar|https://repo1.maven.org/maven2/jakarta/servlet/jakarta.servlet-api/5.0.0/jakarta.servlet-api-5.0.0.jar" # No version change
jar_updates["jakarta.validation-api-3.0.0.jar"]="jakarta.validation-api-3.0.2.jar|https://repo1.maven.org/maven2/jakarta/validation/jakarta.validation-api/3.0.2/jakarta.validation-api-3.0.2.jar"
jar_updates["jakarta.ws.rs-api-3.0.0-sources.jar"]="REMOVE_ONLY" # Sources JAR not needed in lib
jar_updates["jakarta.ws.rs-api-3.0.0.jar"]="jakarta.ws.rs-api-3.1.0.jar|https://repo1.maven.org/maven2/jakarta/ws/rs/jakarta.ws.rs-api/3.1.0/jakarta.ws.rs-api-3.1.0.jar"
jar_updates["jakarta.xml.bind-api-3.0.1.jar"]="jakarta.xml.bind-api-3.0.1.jar|https://repo1.maven.org/maven2/jakarta/xml/bind/jakarta.xml.bind-api/3.0.1/jakarta.xml.bind-api-3.0.1.jar" # No version change
jar_updates["javassist-3.25.0-GA.jar"]="javassist-3.29.2-GA.jar|https://repo1.maven.org/maven2/org/javassist/javassist/3.29.2-GA/javassist-3.29.2-GA.jar"
jar_updates["jaxb-core-3.0.1.jar"]="jaxb-core-3.0.2.jar|https://repo1.maven.org/maven2/com/sun/xml/bind/jaxb-core/3.0.2/jaxb-core-3.0.2.jar"
jar_updates["jaxb-runtime-3.0.1.jar"]="jaxb-runtime-3.0.2.jar|https://repo1.maven.org/maven2/org/glassfish/jaxb/jaxb-runtime/3.0.2/jaxb-runtime-3.0.2.jar"
jar_updates["jersey-client-3.0.2.jar"]="jersey-client-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/core/jersey-client/3.1.3/jersey-client-3.1.3.jar"
jar_updates["jersey-common-3.0.2.jar"]="jersey-common-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/core/jersey-common/3.1.3/jersey-common-3.1.3.jar"
jar_updates["jersey-container-grizzly2-http-3.0.2.jar"]="jersey-container-grizzly2-http-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/containers/jersey-container-grizzly2-http/3.1.3/jersey-container-grizzly2-http-3.1.3.jar"
jar_updates["jersey-container-grizzly2-servlet-3.0.2.jar"]="jersey-container-grizzly2-servlet-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/containers/jersey-container-grizzly2-servlet/3.1.3/jersey-container-grizzly2-servlet-3.1.3.jar"
jar_updates["jersey-container-jdk-http-3.0.2.jar"]="jersey-container-jdk-http-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/containers/jersey-container-jdk-http/3.1.3/jersey-container-jdk-http-3.1.3.jar"
jar_updates["jersey-container-servlet-3.0.2.jar"]="jersey-container-servlet-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/containers/jersey-container-servlet/3.1.3/jersey-container-servlet-3.1.3.jar"
jar_updates["jersey-container-servlet-core-3.0.2.jar"]="jersey-container-servlet-core-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/containers/jersey-container-servlet-core/3.1.3/jersey-container-servlet-core-3.1.3.jar"
jar_updates["jersey-hk2-3.0.2.jar"]="jersey-hk2-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/inject/jersey-hk2/3.1.3/jersey-hk2-3.1.3.jar"
jar_updates["jersey-media-jaxb-3.0.2.jar"]="jersey-media-jaxb-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/media/jersey-media-jaxb/3.1.3/jersey-media-jaxb-3.1.3.jar"
jar_updates["jersey-media-json-binding-3.0.2.jar"]="jersey-media-json-binding-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/media/jersey-media-json-binding/3.1.3/jersey-media-json-binding-3.1.3.jar"
jar_updates["jersey-media-sse-3.0.2.jar"]="jersey-media-sse-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/media/jersey-media-sse/3.1.3/jersey-media-sse-3.1.3.jar"
jar_updates["jersey-server-3.0.2.jar"]="jersey-server-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/core/jersey-server/3.1.3/jersey-server-3.1.3.jar"
jar_updates["jersey-test-framework-core-3.0.2.jar"]="jersey-test-framework-core-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/test-framework/jersey-test-framework-core/3.1.3/jersey-test-framework-core-3.1.3.jar"
jar_updates["jersey-test-framework-provider-grizzly2-3.0.2.jar"]="jersey-test-framework-provider-grizzly2-3.1.3.jar|https://repo1.maven.org/maven2/org/glassfish/jersey/test-framework/providers/jersey-test-framework-provider-grizzly2/3.1.3/jersey-test-framework-provider-grizzly2-3.1.3.jar"
jar_updates["org.osgi.core-6.0.0.jar"]="osgi.core-8.0.0.jar|https://repo1.maven.org/maven2/org/osgi/osgi.core/8.0.0/osgi.core-8.0.0.jar"
jar_updates["osgi-resource-locator-1.0.3.jar"]="osgi-resource-locator-1.0.3.jar|https://repo1.maven.org/maven2/org/glassfish/hk2/osgi-resource-locator/1.0.3/osgi-resource-locator-1.0.3.jar" # No version change
jar_updates["txw2-3.0.1.jar"]="txw2-3.0.2.jar|https://repo1.maven.org/maven2/com/sun/xml/bind/jaxb-txw2/3.0.2/jaxb-txw2-3.0.2.jar"
jar_updates["yasson-2.0.1.jar"]="yasson-2.0.4.jar|https://repo1.maven.org/maven2/org/eclipse/yasson/yasson/2.0.4/yasson-2.0.4.jar"

# --- JARs to Add ---
# Structure:
# declare -A jars_to_add
# jars_to_add[NEW_FILENAME]="URL"
declare -A jars_to_add
jars_to_add["jakarta.json-2.0.1.jar"]="https://repo1.maven.org/maven2/org/glassfish/jakarta.json/2.0.1/jakarta.json-2.0.1.jar" # Jakarta JSON implementation
jars_to_add["junit-jupiter-api-5.9.3.jar"]="https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.9.3/junit-jupiter-api-5.9.3.jar"
jars_to_add["junit-jupiter-engine-5.9.3.jar"]="https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.9.3/junit-jupiter-engine-5.9.3.jar"
jars_to_add["junit-platform-launcher-1.9.3.jar"]="https://repo1.maven.org/maven2/org/junit/platform/junit-platform-launcher/1.9.3/junit-platform-launcher-1.9.3.jar"
jars_to_add["apiguardian-api-1.1.2.jar"]="https://repo1.maven.org/maven2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"
jars_to_add["opentest4j-1.2.0.jar"]="https://repo1.maven.org/maven2/org/opentest4j/opentest4j/1.2.0/opentest4j-1.2.0.jar"


echo "Processing JAR updates..."
for old_jar in "${!jar_updates[@]}"; do
    old_jar_path="$JAR_DIR/$old_jar"
    update_info="${jar_updates[$old_jar]}"

    if [ -f "$old_jar_path" ]; then
        echo "Removing old JAR: $old_jar_path"
        rm "$old_jar_path"
    else
        echo "Warning: Old JAR not found, skipping removal: $old_jar_path"
    fi

    if [ "$update_info" == "REMOVE_ONLY" ]; then
        echo "Note: $old_jar is marked for removal only."
        continue
    fi

    # Parse NEW_FILENAME and URL
    new_jar=$(echo "$update_info" | cut -d'|' -f1)
    jar_url=$(echo "$update_info" | cut -d'|' -f2)
    new_jar_path="$JAR_DIR/$new_jar"

    echo "Downloading $new_jar to $new_jar_path from $jar_url"
    curl -L -o "$new_jar_path" "$jar_url"
    if [ $? -ne 0 ]; then
        echo "Error downloading $new_jar. Please check URL or network."
        # set -e will handle exit
    fi
done

echo "Processing additional JARs to add..."
for new_jar in "${!jars_to_add[@]}"; do
    jar_url="${jars_to_add[$new_jar]}"
    new_jar_path="$JAR_DIR/$new_jar"

    if [ -f "$new_jar_path" ]; then
        echo "Warning: JAR $new_jar already exists. Skipping download."
    else
        echo "Downloading $new_jar to $new_jar_path from $jar_url"
        curl -L -o "$new_jar_path" "$jar_url"
        if [ $? -ne 0 ]; then
            echo "Error downloading $new_jar. Please check URL or network."
            # set -e will handle exit
        fi
    fi
done

echo "JAR update process completed."
exit 0
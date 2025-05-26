#!/bin/bash

# Script to check for latest versions of JAR files in the SystaPi repository

echo "Ensuring repository is up-to-date and listing current JARs..."

# Navigate to the repository root (if not already there)
# This is a simple check; a more robust check might involve `git rev-parse --show-toplevel`
repo_root=$(git rev-parse --show-toplevel)
current_dir=$(pwd)
repo_name=$(basename "$repo_root")
if [[ "$repo_root" != "$current_dir" || "$repo_name" != "SystaPi" ]]; then
    echo "Error: This script must be run from the root of the SystaPi Git repository."
    exit 1
fi

# Switch to the main branch
git checkout main
if [ $? -ne 0 ]; then
  echo "Error: Failed to checkout main branch."
  # In a CI/testing environment, this might not be a fatal error if already on main or a specific commit.
  # For this script's purpose, we'll report it but not necessarily exit if pull can still work.
fi

# Pull the latest changes
git pull origin main
if [ $? -ne 0 ]; then
  echo "Error: Failed to pull latest changes from origin main."
  # Depending on policy, one might choose to exit here.
  # For now, we'll try to list JARs anyway if the directory exists.
fi

# Define the target directory
JAR_DIR="SystaRESTServer/lib"
OUTPUT_FILE="current_jars.txt"

# Check if the target directory exists
if [ ! -d "$JAR_DIR" ]; then
  echo "Error: Directory $JAR_DIR does not exist."
  exit 1
fi

# List JAR files and save to current_jars.txt
echo "Listing JAR files in $JAR_DIR..."
ls "$JAR_DIR" > "$OUTPUT_FILE"
if [ $? -ne 0 ]; then
  echo "Error: Failed to list JAR files in $JAR_DIR."
  exit 1
fi

echo "Current JAR list saved to $OUTPUT_FILE"

exit 0

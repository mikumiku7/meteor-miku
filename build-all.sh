#!/bin/bash

echo "Building Meteor Miku addon for all versions..."

echo ""
echo "Building for Minecraft 1.21.1..."
./gradlew clean build -x test --no-configuration-cache -Pminecraft_version=1.21.1

echo ""
echo "Building for Minecraft 1.21.4..."
./gradlew build -x test --no-configuration-cache -Pminecraft_version=1.21.4

echo ""
echo "Build completed! Check build/libs/ for the generated jars."
ls -la build/libs/*.jar

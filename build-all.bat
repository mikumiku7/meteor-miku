@echo off
echo Building Meteor Miku addon for all versions...

echo.
echo Building for Minecraft 1.21.1...
call gradlew.bat clean build -x test --no-configuration-cache -Pminecraft_version="1.21.1"

echo.
echo Building for Minecraft 1.21.4...
call gradlew.bat build -x test --no-configuration-cache -Pminecraft_version="1.21.4"

echo.
echo Build completed! Check build/libs/ for the generated jars.
dir build\libs\*.jar
pause

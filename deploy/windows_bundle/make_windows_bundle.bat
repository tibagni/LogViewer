@echo off
if exist "LogViewer.jar" (
    echo Found LogViewer.jar
) else (
    echo LogViewer.jar not found
    echo Run this script in the same folder as LogViewer.jar
    pause
    exit 1
)

jdeps.exe Logviewer.jar
echo Creating jre...
jlink.exe --add-modules java.base --add-modules java.datatransfer --add-modules java.desktop --add-modules java.logging --add-modules java.prefs --strip-debug --no-man-pages --no-header-files --compress=2 --output jre
echo Creating run script...
echo START "jre\bin\javaw -jar" LogViewer.jar > Run.bat
echo Zipping...
"C:\Program Files\7-Zip\7z.exe" a -tzip LogViewer-jre-bundled-Windows.zip LogViewer.jar jre Run.bat
echo Clening up...
rmdir /s /q jre
del Run.bat
echo Done!

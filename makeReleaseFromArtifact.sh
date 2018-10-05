VERSION=$(cat app.properties | grep version | cut -d'=' -f2)
OUT_JAR="out/release/LogViewer_$VERSION.jar"
OUT_APP="out/release/LogViewer_$VERSION.App"
SPLASH="src/main/resources/Images/splash.gif"

mkdir temp
cp $1 temp/LogViewer.jar
cd temp
unzip LogViewer.jar
rm LogViewer.jar
cp ../src/main/java/META-INF/MANIFEST.MF META-INF/
zip -r LogViewer.jar *
mkdir -p ../out/release
mv LogViewer.jar ../$OUT_JAR
cd ..
rm -rf temp

if which jar2app >/dev/null; then
    echo "Making Mac OS app bundle..."
    jar2app $OUT_JAR $OUT_APP -i resources/log_viewer.icns -n "Log Viewer" -v $VERSION -s $VERSION -o -p $SPLASH
    echo "Mac OS APP Release is ready on $OUT_APP..."
else
    echo "jar2app not found. Will not create a Mac Os App Bundle." 
fi

echo "JAR Release is ready on $OUT_JAR..."
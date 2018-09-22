VERSION=$(cat app.properties | grep version | cut -d'=' -f2)

mkdir temp
cp $1 temp/LogViewer.jar
cd temp
unzip LogViewer.jar
rm LogViewer.jar
cp ../src/main/java/META-INF/MANIFEST.MF META-INF/
zip -r LogViewer.jar *
mkdir -p ../out/release
mv LogViewer.jar ../out/release/LogViewer_$VERSION.jar
cd ..
rm -rf temp
echo "Release is ready on out/release/LogViewer_$VERSION.jar..."
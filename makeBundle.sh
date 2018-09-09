VERSION=$(cat app.properties | grep version | cut -d'=' -f2)

javapackager -deploy \
  -title "Log Viewer" \
  -name "Log Viewer" \
  -appclass com.tibagni.logviewer.LogViewerApplication \
  -native dmg \
  -outdir out/native \
  -outfile LogViewer \
  -vendor tibagni \
  -srcdir "out/artifacts/LogViewer_jar" \
  -Bicon="resources/log_viewer.icns" \
  -BappVersion=$VERSION
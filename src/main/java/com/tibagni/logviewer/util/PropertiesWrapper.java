package com.tibagni.logviewer.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertiesWrapper {
  private final ClassLoader objClassLoader;
  private Properties appProperties = new Properties();

  public PropertiesWrapper(String propertiesFilename) throws IOException {
    objClassLoader = getClass().getClassLoader();

    if (StringUtils.isEmpty(propertiesFilename)) {
      throw new IOException("Invalid properties file provided");
    }

    FileInputStream objFileInputStream =
        new FileInputStream(objClassLoader.getResource(propertiesFilename).getFile());
    appProperties.load(objFileInputStream);
  }

  public String get(String key) {
    return String.valueOf(appProperties.get(key));
  }
}

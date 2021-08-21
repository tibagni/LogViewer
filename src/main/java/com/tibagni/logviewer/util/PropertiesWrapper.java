package com.tibagni.logviewer.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesWrapper {
  private final Properties appProperties = new Properties();

  public PropertiesWrapper(String propertiesFilename) throws IOException {
    ClassLoader objClassLoader = getClass().getClassLoader();

    if (StringUtils.isEmpty(propertiesFilename)) {
      throw new IOException("Invalid properties file provided");
    }

    InputStream inputStream = objClassLoader.getResourceAsStream(propertiesFilename);
    appProperties.load(inputStream);
  }

  public String get(String key) {
    return String.valueOf(appProperties.get(key));
  }
}

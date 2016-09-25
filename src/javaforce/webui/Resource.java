package javaforce.webui;

import java.io.InputStream;
import java.util.HashMap;
import javaforce.JF;

/**
 *
 * @author pquiring
 */

public class Resource {
  public byte data[];
  public String id;
  public String mime;

  public static final String PNG = "image/png";
  public static final String JPG = "image/jpeg";
  public static final String JPEG = "image/jpeg";
  public static final String GIF = "image/gif";

  private static int nextID;
  private static HashMap<String, Resource> resList = new HashMap<String, Resource>();
  public static synchronized Resource registerResource(byte data[], String mime) {
    Resource res = new Resource();
    res.data = data;
    res.id = "r" + nextID++;
    res.mime = "";
    resList.put(res.id, res);
    return res;
  }
  public static Resource readResource(String name, String mime) {
    InputStream is = Resource.class.getClassLoader().getResourceAsStream(name);
    if (is == null) {
      return null;
    }
    byte data[] = JF.readAll(is);
    try {is.close();} catch (Exception e) {}
    return registerResource(data, mime);
  }
  public static Resource getResource(String id) {
    return resList.get(id);
  }
}

package javaforce.service.servlet;

/** SecureKeyMgr
 *
 * @author peter.quiring
 */

import javaforce.awt.security.*;

public class SecureKeyMgr {
  public static void main(String[] args) {
    Paths.init();
    ServletsService.initSecureWebKeys();
    KeyMgr.main(new String[] {"open", Paths.dataPath + "/jfservlets.key", "password"});
  }
}

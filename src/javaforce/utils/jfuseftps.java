package javaforce.utils;

import javaforce.FTP;

/**
 *
 * @author pquiring
 *
 * Created : Nov 5, 2013
 */

public class jfuseftps extends jfuseftp {
  public void connect(FTP ftp) throws Exception {
    ftp.connectSSL(server, 989);
  }
}

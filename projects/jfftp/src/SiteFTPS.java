/** FTP over SSL (explicit only) (implicit not supported)
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.awt.*;

public class SiteFTPS extends SiteFTP {
  @Override
  public boolean connect(SiteDetails sd) {
    try {
      ftp = new FTP();
      ftp.setLogging(true);
      ftp.addProgressListener(this);
      setStatus("Connecting...");
      if (!ftp.connectSSL(sd.host, Integer.valueOf(sd.port))) {
        throw new Exception("Connection failed");
      }
      setStatus("Login...");
      if (!ftp.login(sd.username, sd.password)) {
        throw new Exception("Login denied");
      }
      if (sd.remoteDir.length() > 0) {
        ftp.cd(sd.remoteDir);
      }
      remote_ls();
      if (!ftp.setBinary()) {
        throw new Exception("Binary mode not supported");
      }
      setStatus(null);
    } catch (Exception e) {
      JFAWT.showMessage("Error", "Error:" + e);
      JFLog.log(e);
      closeSite();
      return false;
    }
    return true;
  }
}

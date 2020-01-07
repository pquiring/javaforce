/** CameraInstance
 *
 * @author pquiring
 */

import java.io.*;

public class CameraInstance extends Thread {
  public Process process;
  public Camera camera;

  private boolean active;

  private static int nextPort = 5000;
  private static synchronized int getLocalPort(int range) {
    if (nextPort + range > 10000) nextPort = 5000;
    int port = nextPort;
    nextPort += range;
    return port;
  }

  public CameraInstance(Camera camera) {
    this.camera = camera;
  }

  public void cancel() {
    active = false;
    Process p = process;
    if (p != null) {
      p.destroy();
    }
  }

  private void delete_hs_logs() {
    try {
      File logs[] = new File(".").listFiles();
      if (logs == null) return;
      for(File log : logs) {
        if (log.isDirectory()) continue;
        if (log.getName().startsWith("hs") && log.getName().endsWith(".log")) {
          log.delete();
        }
      }
    } catch (Exception e) {}
  }

  public void run() {
    active = true;
    while (active) {
      delete_hs_logs();
      ProcessBuilder pb = new ProcessBuilder();
      try {
        int localPort = getLocalPort(15);
        pb.command(new String[] {"CameraWorker.exe", camera.name, Integer.toString(localPort), Integer.toString(localPort+5), Integer.toString(localPort+10)});
        process = pb.start();
        process.waitFor();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}

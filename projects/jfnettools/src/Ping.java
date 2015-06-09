/**
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

public class Ping extends Thread {
  private NetApp win;
  private String host;
  private boolean active, done;
  private int timeout = 2500;  //in ms

  public Ping(NetApp win, String host) {
    this.win = win;
    this.host = host;
  }
  public void run() {
    if (false)
      runJava();
    else
      runOS();
  }
  /** Uses InetAddress.isReachable() which is garbage. */
  private void runJava() {
    active = true;
    int ok = 0;
    int fail = 0;
    int total = 0;
    try {
      win.setPingStatus("Resolving host...");
      InetAddress ia = InetAddress.getByName(host);
      win.setPingStatus("Pinging host : " + ia.getHostAddress().toString());
      while (active) {
        if (ia.isReachable(timeout)) {
          ok++;
        } else {
          fail++;
        }
        total++;
        win.setPingStatus("Success=" + ok + ",fail=" + fail);
        try {Thread.sleep(1000);} catch (Exception e) {}
      }
      done = true;
    } catch (Exception e) {
      e.printStackTrace();
      win.setPingStatus(e.toString());
      active = false;
    }
  }
  /** Uses OS ping command. */
  private void runOS() {
    int ok = 0;
    int fail = 0;
    int total = 0;
    active = true;
    try {
      win.setPingStatus("Resolving host...");
      InetAddress ia = InetAddress.getByName(host);
      String ip = ia.getHostAddress().toString();
      win.setPingStatus("Pinging host : " + ip);
      while (active) {
        Process p;
        if (isWindows()) {
          p = Runtime.getRuntime().exec(new String[] {"ping", "-n", "1", ip});
        } else {
          p = Runtime.getRuntime().exec(new String[] {"ping", "-c", "1", ip});
        }
        InputStream is = p.getInputStream();
        p.waitFor();
        StringBuilder sb = new StringBuilder();
        while (is.available() > 0) {
          int len = is.available();
          byte data[] = new byte[len];
          int read = is.read(data);
          sb.append(new String(data, 0, read));
        }
        String out = sb.toString();
        if (isWindows()) {
          //process Windows output
          if (out.indexOf("Request timed out") != -1 || out.indexOf("Destination host unreachable") != -1) {
            fail++;
          } else {
            ok++;
          }
        } else {
          //process Linux output
          if (out.indexOf("0 received") != -1) {
            fail++;
          } else {
            ok++;
          }
        }
        total++;
        win.setPingStatus("Success=" + ok + ",fail=" + fail);
        try {Thread.sleep(1000);} catch (Exception e) {}
      }
    } catch (Exception e) {
      e.printStackTrace();
      win.setPingStatus(e.toString());
      active = false;
    }
    done = true;
  }
  private boolean isWindows() {
    return File.pathSeparatorChar == ';';
  }
  public void close() {
    active = false;
    while (!done) {
      try {Thread.sleep(100);} catch (Exception e) {}
    }
  }
}

/*

Windows success:
Pinging 10.1.1.1 with 32 bytes of data:
Reply from 10.1.1.1: bytes=32 time=1ms TTL=64

Ping statistics for 10.1.1.1:
    Packets: Sent = 1, Received = 1, Lost = 0 (0% loss),
Approximate round trip times in milli-seconds:
    Minimum = 1ms, Maximum = 1ms, Average = 1ms

Windows failure (internet):
Pinging 11.1.1.1 with 32 bytes of data:
Request timed out.

Ping statistics for 11.1.1.1:
    Packets: Sent = 1, Received = 0, Lost = 1 (100% loss),

Windows failure (intranet):
Pinging 10.1.1.99 with 32 bytes of data:
Reply from 10.1.1.2: Destination host unreachable.

Ping statistics for 10.1.1.99:
    Packets: Sent = 1, Received = 1, Lost = 0 (0% loss),

Linux success:
PING 10.1.1.1 (10.1.1.1) 56(84) bytes of data.
64 bytes from 10.1.1.1: icmp_seq=1 ttl=64 time=1.37 ms

--- 10.1.1.1 ping statistics ---
1 packets transmitted, 1 received, 0% packet loss, time 0ms
rtt min/avg/max/mdev = 1.372/1.372/1.372/0.000 ms

Linux failure (internet):
PING 11.1.1.1 (11.1.1.1) 56(84) bytes of data.

--- 11.1.1.1 ping statistics ---
1 packets transmitted, 0 received, 100% packet loss, time 0ms

Linux failure (intranet):
PING 10.1.1.99 (10.1.1.99) 56(84) bytes of data.
From 10.1.1.112 icmp_seq=1 Destination Host Unreachable

--- 10.1.1.99 ping statistics ---
1 packets transmitted, 0 received, +1 errors, 100% packet loss, time 0ms

*/

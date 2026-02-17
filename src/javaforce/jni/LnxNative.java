package javaforce.jni;

/** Linux Native API
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

import javaforce.*;

public class LnxNative {
  private static void load() {
    Library[] libs = {new Library("X11"), new Library("GL"), new Library("v4l2"), new Library("pam"), new Library("ncurses")};
    JFNative.findLibraries(new File[] {new File("/usr/lib"), new File(getArchLibFolder())}, libs, ".so");
    lnxInit(libs[0].path, libs[1].path, libs[2].path, libs[3].path, libs[4].path);
  }

  /** Returns CPU arch lib folder. */
  public static String getArchLibFolder() {
    if (new File("/usr/lib/x86_64-linux-gnu").exists()) {
      return "/usr/lib/x86_64-linux-gnu";
    }
    if (new File("/usr/lib/aarch64-linux-gnu").exists()) {
      return "/usr/lib/aarch64-linux-gnu";
    }
    if (new File("/usr/lib64").exists()) {
      //Fedora
      return "/usr/lib64";
    }
    JFLog.log("Warning:Arch Lib folder not found!");
    return "/usr/lib";
  }

  private static String readSocketMessage(SocketChannel channel) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    int bytesRead = channel.read(buffer);
    if (bytesRead < 0) return null;
    byte[] bytes = new byte[bytesRead];
    buffer.flip();
    buffer.get(bytes);
    String message = new String(bytes);
    return message;
  }

  private static String getServiceSocket() {
    try {
      String path = Path.of("/proc/self/exe").toRealPath().toString();
      int idx = path.lastIndexOf('/');
      String app = path.substring(idx+1);
      String sockpath = "/usr/lib/systemd/system/" + app + ".socket";
      return sockpath;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private static native boolean lnxInit(String libX11, String libGL, String libv4l2, String pam, String ncurses);
  /** Creates a unix socket which commands to the service can be issued.
   * Only supported command is "stop".  (see lnxServiceRequestStop())
   * Socket file is stored at /usr/lib/systemd/system/{service}.socket
   *
   * Note : unix sockets requires Java 16 (JEP 380).
   */
  private static void lnxServiceInit() {
    new Thread() {
      public void run() {
        try {
          String socketPath = getServiceSocket();
          UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(socketPath);
          ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
          new File(socketPath).delete();
          serverChannel.bind(socketAddress);
          boolean active = true;
          while (active) {
            SocketChannel channel = serverChannel.accept();
            while (active) {
              String msg = readSocketMessage(channel);
              if (msg == null) break;
              switch (msg) {
                case "stop": lnxServiceStop(); active = false; break;
              }
            }
            channel.close();
            new File(socketPath).delete();
          }
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }.start();
  }
  /** Invokes the services serviceStop() method.
   */
  private static native boolean lnxServiceStop();
  /** Sends a "stop" command to the service's unix socket.
   */
  private static void lnxServiceRequestStop() {
    //connect to unix socket and send stop command
    try {
      SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
      UnixDomainSocketAddress socketAddress = UnixDomainSocketAddress.of(getServiceSocket());
      channel.connect(socketAddress);
      ByteBuffer buffer = ByteBuffer.allocate(1024);
      buffer.clear();
      buffer.put("stop".getBytes());
      buffer.flip();
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  //pty
  public static native long ptyAlloc();
  public static native void ptyFree(long ctx);  //free resources on parent side
  public static native String ptyOpen(long ctx);  //creates a pty and returns the slaveName (one use per ctx)
  public static native void ptyClose(long ctx);  //close pty
  public static native int ptyRead(long ctx, byte[] data);  //read child output on parent side
  public static native void ptyWrite(long ctx, byte[] data);  //write to child on parent side
  public static native void ptySetSize(long ctx, int x, int y);  //set child term size
  public static native long ptyChildExec(String slaveName, String cmd, String[] args, String[] env);  //spawn child process

  //PAM (Pluggable Authentication Modules)
  public static native boolean authUser(String user, String pass, String backend);

  //setenv
  public static native void setenv(String name, String value);

  //console
  public static native void enableConsoleMode();
  public static native void disableConsoleMode();
  public static native int[] getConsoleSize();
  public static native int[] getConsolePos();
  public static native char readConsole();
  public static native boolean peekConsole();
  public static native void writeConsole(int ch);
  public static native void writeConsoleArray(byte[] ch, int off, int len);

  //file
  public static native int fileGetMode(String path);
  public static native void fileSetMode(String path, int mode);
  public static native void fileSetAccessTime(String path, long ts);
  public static native void fileSetModifiedTime(String path, long ts);
  public static native long fileGetID(String path);
}

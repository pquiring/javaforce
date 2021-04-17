package jfnetboot;

/** Client File Handle
 *
 * @author pquiring
 */

public class CHandle {
  public int serial;  //client serial
  public int arch;  //client arch (1=arm 2=x86)
  public long handle;  //file system handle

  public Client client;
  public FileSystem fs;

  public CHandle(int serial, int arch, long handle) {
    this.serial = serial;
    this.arch = arch;
    this.handle = handle;

    client = Clients.getClient(serial, Arch.toString(arch));
    fs = client.getFileSystem();
  }

  public String toString() {
    return String.format("%08x:%s:%016x", serial, Arch.toString(arch), handle);
  }
}

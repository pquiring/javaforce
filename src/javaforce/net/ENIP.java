package javaforce.net;

/** EtherNet/IP (Industrial Protocol).
 *
 * The ill named packet:
 *   EtherNet/IP is a sub packet of the Ethernet/IP/TCP packets.
 *   Namer deserves the Darwin award.
 *
 * @author pquiring
 */

public class ENIP implements SubPacket {
  //header (24 bytes)
  public short cmd;  //cmd type
  public short len;  //following command data below
  public int session;  //random ID for connection
  public int status;  //0 = success
  public long context;  //increments per packet
  public int options;  //0
  //CMD_RR_DATA (16 bytes)
  public int ihandle;  //0
  public short timeout;  //0
  public short count;
  public short type_1;
  public short len_1;
  public short type_2;
  public short len_2;  //length of CIP packet in bytes
  //CMD_GET_SESSION (4 bytes)
  public short protocol;
  public short flags;


  public static final short CMD_REG_SESSION = 0x65;
  public static final short CMD_UNREG_SESSION = 0x66;
  public static final short CMD_UNCONNECTED_SEND = 0x6f;
  public static final short CMD_CONNECTED_SEND = 0x70;

  public ENIP() {
    init();
  }

  public ENIP(short _cmd) {
    init();
    cmd = _cmd;
    if (cmd == CMD_CONNECTED_SEND) {
      timeout = 1;
    }
  }

  private void init() {
    count = 2;
    type_2 = 0x00b2;
    protocol = 0x0001;
  }

  public int getSize() {
    switch (cmd) {
      case CMD_UNCONNECTED_SEND: return 24 + 16;  //40
      case CMD_REG_SESSION: return 24 + 4;  //28
      case CMD_CONNECTED_SEND: return 24 + 22 - 4;  //46
    }
    return -1;
  }

  public int getDataSize() {
    return -1;
  }

  public void read(Packet packet) throws Exception {
    cmd = packet.readShort();
    len = packet.readShort();
    session = packet.readInt();
    status = packet.readInt();
    context = packet.readLong();
    options = packet.readInt();
    switch (cmd) {
      case CMD_UNCONNECTED_SEND:
        ihandle = packet.readInt();
        timeout = packet.readShort();
        count = packet.readShort();
        if (count != 2) throw new Exception("ab:bad ip packet");
        type_1 = packet.readShort();
        len_1 = packet.readShort();
        type_2 = packet.readShort();
        len_2 = packet.readShort();
        break;
      case CMD_REG_SESSION:
        protocol = packet.readShort();
        flags = packet.readShort();
        break;
      case CMD_CONNECTED_SEND:
        //TODO
        break;
    }
  }

  public void setSizes(int size) {
    switch (cmd) {
      case CMD_UNCONNECTED_SEND:
        len = (short)(16 + size);
        len_2 = (short)size;
        break;
      case CMD_REG_SESSION:
        len = 4;
        break;
      case CMD_CONNECTED_SEND:
        len = (short)(22 + size - 4);
        len_2 = (short)(2 + size);
        break;
    }
  }

  public void write(Packet packet) throws Exception {
    write(packet, null);
  }

  public void write(Packet packet, ENIPContext enip_context) throws Exception {
    if (enip_context != null) {
      session = enip_context.session;
      context = enip_context.context;
    }
    //24 bytes
    packet.writeShort(cmd);
    packet.writeShort(len);
    packet.writeInt(session);
    packet.writeInt(status);
    packet.writeLong(context);
    packet.writeInt(options);
    switch (cmd) {
      case CMD_UNCONNECTED_SEND:
        //16 bytes
        packet.writeInt(ihandle);
        packet.writeShort(timeout);
        packet.writeShort(count);
        packet.writeShort(type_1);
        packet.writeShort(len_1);
        packet.writeShort(type_2);
        packet.writeShort(len_2);
        if (enip_context != null) {
          enip_context.increment();
        }
        break;
      case CMD_REG_SESSION:
        //4
        packet.writeShort(protocol);
        packet.writeShort(flags);
        break;
      case CMD_CONNECTED_SEND:
        //22 bytes
        packet.writeInt(ihandle);
        packet.writeShort(timeout);
        packet.writeShort(count);
        packet.writeShort(type_1);
        packet.writeShort(len_1);
        packet.writeShort(type_2);
        packet.writeShort(len_2);
        break;
    }
  }
}

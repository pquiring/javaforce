package javaforce.net;

/** COTP - Connection-Oriented Transport Protocol (ISO 8073/X.224)
 *
 * @author pquiring
 */

public class COTP implements SubPacket {
  public byte length;
  public byte PDU_type;
  public byte[] pdata;  //convert to Packet

  private short src_ref;

  private static short next_id = 0x0f00;

  public static final byte TYPE_DATA = (byte)0xf0;
  public static final byte TYPE_CONNECT = (byte)0xe0;
  public static final byte TYPE_CONNECT_ACK = (byte)0xd0;

  public COTP() {
    length = 1;  //PDU_type
  }
  private synchronized static short get_next_id() {
    return next_id++;
  }
  public int getSize() {
    return length + 1;
  }
  public int getDataSize() {
    if (pdata != null) {
      return pdata.length;
    }
    return 0;
  }
  public void write(Packet packet) throws Exception {
    packet.writeByte(length);
    packet.writeByte(PDU_type);
    if (pdata != null) {
      packet.write(pdata);
    }
  }
  public void read(Packet packet) throws Exception {
    length = packet.readByte();  //excluding length itself
    PDU_type = packet.readByte();
    pdata = new byte[length - 1];  //-1 for PDU_type already read
    packet.read(pdata);
  }

  /** Create packet.
   * @param pdu_type = type_...
   * @param length = data length (including 1 byte for pdu_type)
   */
  public void create(byte pdu_type, byte length) {
    PDU_type = pdu_type;
    src_ref = get_next_id();
    this.length = length;
  }

  /** Create packet.
   * @param pdu_type = type_...
   * @param pdata = payload data.
   */
  public void create(byte pdu_type, byte[] pdata) {
    PDU_type = pdu_type;
    src_ref = get_next_id();
    this.pdata = pdata;
    this.length = (byte)(pdata.length + 1);
  }

  /** Create S7 process data. */
  public void createS7Data() {
    PDU_type = TYPE_DATA;
    src_ref = get_next_id();
    pdata = new byte[] {(byte)0x80};
    length = (byte)(pdata.length + 1);
  }

  /** Create S7 connect data. */
  public void createS7Connect() {
    PDU_type = TYPE_CONNECT;
    src_ref = get_next_id();
    pdata = new byte[] {
      (byte)0x00,
      (byte)0x00,  //dest ref
      (byte)(src_ref >>> 8),
      (byte)(src_ref & 0xff),  //src ref
      (byte)0x00,  //flags
      (byte)0xc0,  //param code : TPDU size
      (byte)1,  //param length
      (byte)0x0a,  //0x09=512 , 0x0a=1024
      (byte)0xc1,  //param code : src-tsap
      (byte)2,  //param length
      (byte)0x01,
      (byte)0x00,
      (byte)0xc2,  //param code : dst-tsap
      (byte)2,  //param length
      (byte)0x01,
      (byte)0x02,
    };
    length = (byte)(pdata.length + 1);
  }

  public byte getPDUType() {
    return PDU_type;
  }

  public Packet getDataAsPacket() {
    return new Packet(pdata);
  }
}

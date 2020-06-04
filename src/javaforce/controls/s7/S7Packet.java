package javaforce.controls.s7;

/** S7 Data Packet
 *
 * Reference : snap7.sf.net
 *
 * @author pquiring
 */

public class S7Packet {

  /** Creates a packet to connect at COTP level (connect step1). */
  public static byte[] makeConnectPacket1() {
    TPKT tpkt = new TPKT();
    COTP cotp = new COTP(COTP.type_connect);
    byte data[];

    int size = tpkt.size() + cotp.size();
    data = new byte[size];
    int dataoff = 0;
    tpkt.write(data, dataoff, (short)size);
    dataoff += tpkt.size();
    cotp.write(data, dataoff);
    return data;
  }

  /** Creates a packet to connect at S7 level (connect step2). */
  public static byte[] makeConnectPacket2() {
    TPKT tpkt = new TPKT();
    COTP cotp = new COTP(COTP.type_data);
    S7Header header = new S7Header();
    S7Params params = new S7Params();
    byte[] data;

    params.makeConnect();
    int size = tpkt.size() + cotp.size() + header.size() + params.size();
    data = new byte[size];
    int dataoff = 0;
    tpkt.write(data, dataoff, (short)size);
    dataoff += tpkt.size();
    cotp.write(data, dataoff);
    dataoff += cotp.size();
    header.write(data, dataoff, (short)params.size(), (short)0);
    dataoff += header.size();
    params.write(data, dataoff);
    return data;
  }

  /** Creates a packet to read data from S7. */
  public static byte[] makeReadPacket(S7Data s7) {
    TPKT tpkt = new TPKT();
    COTP cotp = new COTP(COTP.type_data);
    S7Header header = new S7Header();
    S7Params params = new S7Params();
    byte data[];

    params.makeRead(s7);
    int size = tpkt.size() + cotp.size() + header.size() + params.size();
    data = new byte[size];
    int dataoff = 0;
    tpkt.write(data, dataoff, (short)size);
    dataoff += tpkt.size();
    cotp.write(data, dataoff);
    dataoff += cotp.size();
    header.write(data, dataoff, (short)params.size(), (short)0);
    dataoff += header.size();
    params.write(data, dataoff);
    return data;
  }

  /** Creates a packet to read data from S7. */
  public static byte[] makeReadPacket(S7Data[] s7) {
    TPKT tpkt = new TPKT();
    COTP cotp = new COTP(COTP.type_data);
    S7Header header = new S7Header();
    S7Params params = new S7Params();
    byte[] data;

    params.makeRead(s7);
    int size = tpkt.size() + cotp.size() + header.size() + params.size();
    data = new byte[size];
    int dataoff = 0;
    tpkt.write(data, dataoff, (short)size);
    dataoff += tpkt.size();
    cotp.write(data, dataoff);
    dataoff += cotp.size();
    header.write(data, dataoff, (short)params.size(), (short)0);
    dataoff += header.size();
    params.write(data, dataoff);
    return data;
  }

  /** Creates a packet to write data to S7. */
  public static byte[] makeWritePacket(S7Data type) {
    TPKT tpkt = new TPKT();
    COTP cotp = new COTP(COTP.type_data);
    S7Header header = new S7Header();
    S7Params params = new S7Params();
    byte[] data;

    params.makeWrite(type.block_type, type.block_number, type.data_type, type.offset, type.length, type.data);
    int size = tpkt.size() + cotp.size() + header.size() + params.size();
    data = new byte[size];
    int dataoff = 0;
    tpkt.write(data, dataoff, (short)size);
    dataoff += tpkt.size();
    cotp.write(data, dataoff);
    dataoff += cotp.size();
    header.write(data, dataoff, (short)(params.size() - 4 - type.data.length), (short)(4 + type.data.length));
    dataoff += header.size();
    params.write(data, dataoff);
    return data;
  }

  /** Decodes S7 Address.
   *
   * Supports: DB,M,I,Q
   *
   * Does not support ranges yet.
   */
  public static S7Data decodeAddress(String addr) {
    //DB##.DB?##[.#]
    //M[?]##[.#]
    //I[?]##[.#]
    //Q[?]##[.#]
    S7Data data = new S7Data();
    if (addr.startsWith("DB")) {
      data.block_type = S7Types.DB;
      int idx = addr.indexOf('.');  //.DB?##[.#]
      data.block_number = Short.valueOf(addr.substring(2, idx));
      addr = addr.substring(idx+2);  //B?##[.#]
    } else if (addr.startsWith("M")) {
      data.block_type = S7Types.M;
    } else if (addr.startsWith("I")) {
      data.block_type = S7Types.I;
    } else if (addr.startsWith("Q")) {
      data.block_type = S7Types.Q;
    } else {
      return null;
    }
    data.data_type = S7Types.getType(addr.charAt(1));
    int offset;
    int idx = addr.indexOf('.');
    if (idx == -1) idx = addr.indexOf(' ');
    if (idx == -1) idx = addr.length();
    if (data.data_type == 0) {
      //no type present (assume bit)
      offset = Integer.valueOf(addr.substring(1, idx));
      data.data_type = S7Types.BIT;
    } else {
      offset = Integer.valueOf(addr.substring(2, idx));
    }
    data.offset = offset << 3;
    if (data.data_type == S7Types.BIT) {
      int idx2 = addr.indexOf(' ');
      if (idx2 == -1) idx2 = addr.length();
      byte bit = Byte.valueOf(addr.substring(idx+1, idx2));
      data.offset += bit;
    }
    data.length = 1;  //# of elements (not bytes)
    idx = addr.indexOf(" BIT ");
    if (idx != -1) {
      data.length = Short.valueOf(addr.substring(idx+5));
    }
    idx = addr.indexOf(" BYTE ");
    if (idx != -1) {
      data.data_type = S7Types.BYTE;
      data.length = Short.valueOf(addr.substring(idx+6));
    }
    idx = addr.indexOf(" CHAR ");
    if (idx != -1) {
      data.data_type = S7Types.CHAR;
      data.length = Short.valueOf(addr.substring(idx+6));
    }
    idx = addr.indexOf(" WORD ");
    if (idx != -1) {
      data.data_type = S7Types.WORD;
      data.length = Short.valueOf(addr.substring(idx+6));
    }
    idx = addr.indexOf(" INT ");
    if (idx != -1) {
      data.data_type = S7Types.INT;
      data.length = Short.valueOf(addr.substring(idx+5));
    }
    idx = addr.indexOf(" DWORD ");
    if (idx != -1) {
      data.data_type = S7Types.DWORD;
      data.length = Short.valueOf(addr.substring(idx+7));
    }
    idx = addr.indexOf(" DINT ");
    if (idx != -1) {
      data.data_type = S7Types.DINT;
      data.length = Short.valueOf(addr.substring(idx+6));
    }
    idx = addr.indexOf(" REAL ");
    if (idx != -1) {
      data.data_type = S7Types.REAL;
      data.length = Short.valueOf(addr.substring(idx+6));
    }
    return data;
  }

  /** Decodes a packet and returns any data returned. */
  public static S7Data decodePacket(byte packet[]) {
    try {
      S7Data data = new S7Data();
      int offset = 0;
      TPKT tpkt = new TPKT();
      tpkt.read(packet, offset);
      offset += tpkt.size();
      COTP cotp = new COTP();
      cotp.read(packet, offset);
      if (cotp.PDU_type == COTP.type_connect) return data;
      if (cotp.PDU_type == COTP.type_connect_ack) return data;
      offset += cotp.size();
      S7Header header = new S7Header();
      header.read(packet, offset);
      offset += header.size();
      S7Params params = new S7Params();
      params.read(packet, offset, data);
      return data;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /** Decodes a packet and returns any data returned. */
  public static S7Data[] decodeMultiPacket(byte packet[], int count) {
    try {
      S7Data data[] = new S7Data[count];
      for(int a=0;a<count;a++) {
        data[a] = new S7Data();
      }
      int offset = 0;
      TPKT tpkt = new TPKT();
      tpkt.read(packet, offset);
      offset += tpkt.size();
      COTP cotp = new COTP();
      cotp.read(packet, offset);
      if (cotp.PDU_type == COTP.type_connect) return data;
      if (cotp.PDU_type == COTP.type_connect_ack) return data;
      offset += cotp.size();
      S7Header header = new S7Header();
      header.read(packet, offset);
      offset += header.size();
      S7Params params = new S7Params();
      params.read(packet, offset, data);
      return data;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static boolean isPacketComplete(byte packet[]) {
    return decodePacket(packet) != null;
  }
}

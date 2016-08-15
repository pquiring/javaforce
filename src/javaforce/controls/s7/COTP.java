package javaforce.controls.s7;

/** COTP - Connection-Oriented Transport Protocol (ISO 8073/X.224)
 *
 * @author pquiring
 */

public class COTP {
  public byte length;
  public byte PDU_type;
  public byte pdata[];

  public static final byte type_data = (byte)0xf0;
  public static final byte type_connect = (byte)0xe0;
  public static final byte type_connect_ack = (byte)0xd0;

  public COTP() {}
  public COTP(byte type) {
    PDU_type = type;
    switch (type) {
      case type_data:
        length = 2;
        break;
      case type_connect:
        length = 17;
        break;
      default:
        System.out.println("Error:Unknown COTP type!!!");
        break;
    }
  }
  public int size() {
    return length + 1;
  }
  public void write(byte data[], int offset) {
    data[offset++] = length;
    data[offset++] = PDU_type;
    switch (PDU_type) {
      case type_data:
        data[offset++] = (byte)0x80;  //dest ref
        break;
      case type_connect:
        data[offset++] = 0x00; data[offset++] = 0x00;  //dest ref
        data[offset++] = 0x00; data[offset++] = 0x01;  //src ref
        data[offset++] = 0x00;  //flags
        data[offset++] = (byte)0xc0;  //param code : TPDU size
        data[offset++] = 1;  //param length
        data[offset++] = 0x0a;  //1024
        data[offset++] = (byte)0xc1;  //param code : src-tsap
        data[offset++] = 2;  //param length
        data[offset++] = 0x01;
        data[offset++] = 0x00;
        data[offset++] = (byte)0xc2;  //param code : dst-tsap
        data[offset++] = 2;  //param length
        data[offset++] = 0x01;
        data[offset++] = 0x02;
        break;
    }
  }
  public void read(byte data[], int offset) throws Exception {
    length = data[offset++];
    PDU_type = data[offset++];
    pdata = new byte[length - 1];
    for(int a=0;a<length-1;a++) {
      pdata[a] = data[offset++];
    }
  }
}

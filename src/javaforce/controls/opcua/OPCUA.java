package javaforce.controls.opcua;

import java.io.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.MGF1ParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

import javaforce.*;
import javaforce.net.*;

/** OPCUA.
 *
 * Common methods.
 *
 * @author pquiring
 */

public class OPCUA {
  public void write_header(Packet packet, String cmd) throws Exception {
    if (cmd.length() != 3) throw new Exception("OPCUA.write_header:invalid cmd");
    packet.writeString(cmd);
    packet.writeByte((byte)'F');
    packet.writeInt(packet.offset);  //need to patch later
  }

  public void write_header(Packet packet, String cmd, int channel_id, int token_id, int seq_num, int req_id) throws Exception {
    if (cmd.length() != 3) throw new Exception("OPCUA.write_header:invalid cmd");
    packet.writeString(cmd);
    packet.writeByte((byte)'F');
    packet.writeInt(packet.length);
    packet.writeInt(channel_id);
    packet.writeInt(token_id);
    packet.writeInt(seq_num);
    packet.writeInt(req_id);
  }

  public void patch_header_length(Packet packet) {
    LE.setuint32(packet.data, 4, packet.offset);
  }

  public String read_reply;
  public char read_chunkType;

  private void read_packet(InputStream is, Packet packet, int offset, int length) throws Exception {
    int total_read = 0;
    while (total_read < length) {
      int read = is.read(packet.data, offset + total_read, length - total_read);
      if (read == -1) throw new Exception("read error");
      if (read > 0) {
        total_read += read;
      }
    }
  }

  public Packet read_packet(InputStream is) throws Exception {
    Packet packet = new Packet(new byte[8], Endian.L);
    read_packet(is, packet, 0, 8);
    read_reply = packet.readString(3);
    read_chunkType = (char)packet.readByte();
    int len = packet.readInt();
    packet.setLength(len);
    len -= 8;  //exclude header
    read_packet(is, packet, 8, len);
    packet.offset = 8;  //start read after header
    return packet;
  }

  public static final byte NODE_ID_BYTE = 0x00;  //two byte encoding
  public static final byte NODE_ID_SHORT = 0x01;  //four byte encoding
  public static final byte NODE_ID_INT = 0x02;  //numeric encoding
  public static final byte NODE_ID_STRING = 0x03;  //string encoding
  public static final byte NODE_ID_GUID = 0x04;  //guid encoding
  public static final byte NODE_ID_OPAQUE = 0x05;  //opaque encoding

  public static final short INDEX_0 = 0;
  public static final short INDEX_1 = 1;

  public static final short OPEN_SECURE_CHANNEL_REQ = 0x01be;
  public static final short OPEN_SECURE_CHANNEL_RES = 0x01c1;
  public static final short CREATE_SESSION_REQ = 0x01cd;
  public static final short CREATE_SESSION_RES = 0x01d0;
  public static final short ACTIVATE_SESSION_REQ = 0x01d3;
  public static final short ACTIVATE_SESSION_RES = 0x01d6;
  public static final short CLOSE_SESSION_REQ = 0x01d9;
  public static final short CLOSE_SESSION_RES = 0x01dc;
  public static final short READ_TAG_REQ = 0x0277;
  public static final short READ_TAG_RES = 0x027a;
  public static final short WRITE_TAG_REQ = 0x02a1;
  public static final short WRITE_TAG_RES = 0x02a4;

  //standard pre-defined tag numerics
  public static final int MAX_READ_TAGS = 10705;
  public static final int MAX_WRITE_TAGS = 10707;

  public static final short USER_TOKEN = 0x144;

  public static final int ATTR_VALUE = 0x0d;

  //data type : see https://reference.opcfoundation.org/specs/OPC-10000-6/5.1.2
  public static final byte TYPE_BOOLEAN = 0x01;
  public static final byte TYPE_INT8 = 0x02;
  public static final byte TYPE_UINT8 = 0x03;
  public static final byte TYPE_INT16 = 0x04;
  public static final byte TYPE_UINT16 = 0x05;
  public static final byte TYPE_INT32 = 0x06;
  public static final byte TYPE_UINT32 = 0x07;
  public static final byte TYPE_INT64 = 0x08;
  public static final byte TYPE_UINT64 = 0x09;
  public static final byte TYPE_FLOAT32 = 0x0a;
  public static final byte TYPE_FLOAT64 = 0x0b;
  public static final byte TYPE_STRING = 0x0c;
  public static final byte TYPE_DATETIME = 0x0d;
  public static final byte TYPE_GUID = 0x0e;
  public static final byte TYPE_BYTE_ARRAY = 0x0f;
  public static final byte TYPE_XML = 0x10;
  public static final byte TYPE_NODE_ID = 0x11;
  public static final byte TYPE_EX_NODE_ID = 0x12;
  public static final byte TYPE_STATUS_CODE = 0x13;
  public static final byte TYPE_QUALIFIED_NAME = 0x14;
  public static final byte TYPE_LOCAL_TEXT = 0x15;
  public static final byte TYPE_EX_OBJECT = 0x16;
  public static final byte TYPE_DATA_VALUE = 0x17;
  public static final byte TYPE_VARIANT = 0x18;
  public static final byte TYPE_DIAG = 0x19;


  public Object read_node_id(Packet packet) throws Exception {
    //see https://reference.opcfoundation.org/specs/OPC-10000-6/5.2.2.9
    byte type = packet.readByte();
    switch (type) {
      case NODE_ID_BYTE:  //byte (two byte)
        return packet.readByte();  //node id
      case NODE_ID_SHORT:  //short (four byte)
        packet.readByte();  //index8
        return packet.readShort();  //node id
      case NODE_ID_INT:  //numeric
        packet.readShort();  //index16
        return packet.readInt();  //node id
      case NODE_ID_STRING:  //string
        packet.readShort();  //index16
        int strlen = packet.readInt();  //strlen
        return packet.readString(strlen);  //string
      case NODE_ID_GUID:  //guid
        GUID guid = new GUID();
        packet.readShort();  //index16
        guid.data1 = packet.readInt();
        guid.data2 = packet.readShort();
        guid.data3 = packet.readShort();
        guid.data4 = packet.readLong();
        return guid;  //GUID
      case NODE_ID_OPAQUE:  //opaque
        packet.readShort();  //index16
        int bytelen = packet.readInt();  //bytelen
        byte[] id = new byte[bytelen];
        packet.read(id);
        return id;  //byte[]
    }
    return null;
  }

  public void write_node_id(Packet packet, byte type, Object node_id) throws Exception {
    write_node_id(packet, type, (short)0, node_id);
  }

  public void write_node_id(Packet packet, byte type, short index, Object node_id) throws Exception {
    packet.writeByte(type);
    switch (type) {
      case NODE_ID_BYTE:  //byte (two byte)
        packet.writeByte((byte)node_id);  //node id
        break;
      case NODE_ID_SHORT:  //short (four byte)
        packet.writeByte((byte)index);  //index8
        packet.writeShort((short)node_id);  //node id
        break;
      case NODE_ID_INT:  //numeric
        packet.writeShort((short)index);  //index16
        packet.writeInt((int)node_id);  //node id
        break;
      case NODE_ID_STRING:  //string
        String str = (String)node_id;
        packet.writeShort((short)index);  //index16
        packet.writeInt(str.length());  //strlen
        packet.writeString(str);  //string
        break;
      case NODE_ID_GUID:  //guid
        GUID guid = (GUID)node_id;
        packet.writeShort((short)index);  //index16
        packet.writeInt(guid.data1);
        packet.writeShort(guid.data2);
        packet.writeShort(guid.data3);
        packet.writeLong(guid.data4);
        break;
      case NODE_ID_OPAQUE:  //opaque
        byte[] opaque = (byte[])node_id;
        packet.writeShort(index);  //index16
        packet.writeInt(opaque.length);
        packet.write(opaque);
        break;
    }
  }

  private static final long EPOCH_DIFF_100NS = 116_444_736_000_000_000L;

  /** Converts OPC timestamp to Unix timestamp. */
  public static long toUnix(long opc_timestamp) {
    return (opc_timestamp - EPOCH_DIFF_100NS) / 10_000L;
  }

  /** Converts Unix timestamp to OPC timestamp. */
  public static long toOPC(long unix_timestamp) {
    return (unix_timestamp * 10_000L) + EPOCH_DIFF_100NS;
  }

  //password

  public X509Certificate parseDerCertificate(byte[] derBytes) throws Exception {
    CertificateFactory factory = CertificateFactory.getInstance("X.509");
    try (ByteArrayInputStream in = new ByteArrayInputStream(derBytes)) {
      return (X509Certificate) factory.generateCertificate(in);
    }
  }

  public static final String XMLENC_RSA_OAEP_URI = "http://www.w3.org/2001/04/xmlenc#rsa-oaep";

  public static byte[] encode_password(String securityPolicyUri, String password, byte[] serverNonce, X509Certificate serverCertificate) throws Exception {
    byte[] passwordBytes = password.getBytes();
    byte[] nonceBytes = serverNonce;

    // 1. OPC UA Legacy RSA-OAEP format packs:
    //    [4-byte Little-Endian Length] + [Password UTF-8 Bytes] + [Server Nonce Bytes]
    int totalPlaintextLength = passwordBytes.length + nonceBytes.length;
    ByteBuffer buffer = ByteBuffer.allocate(4 + totalPlaintextLength);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(totalPlaintextLength);
    buffer.put(passwordBytes);
    buffer.put(nonceBytes);

    byte[] dataToEncrypt = buffer.array();

    // 2. Initialize RSA-OAEP Cipher (SHA-1 + MGF1 is standard for xmlenc#rsa-oaep)
    PublicKey serverPublicKey = serverCertificate.getPublicKey();
    Cipher cipher;

    if (securityPolicyUri.endsWith("Aes256_Sha256_RsaPss")) {
      cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
      OAEPParameterSpec oaepParams = new OAEPParameterSpec(
        "SHA-256",
        "MGF1",
        MGF1ParameterSpec.SHA256,
        PSource.PSpecified.DEFAULT
      );
      cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey, oaepParams);
    } else if (securityPolicyUri.endsWith("Basic256Sha256") || securityPolicyUri.endsWith("Aes128_Sha256_RsaOaep") || securityPolicyUri.endsWith("Basic256")) {
      // Standard RSA-OAEP uses SHA-1 for both digest and MGF1
      cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");
      cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
    } else if (securityPolicyUri.endsWith("Basic128Rsa15")) {
      cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
      cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
    } else {
      throw new IllegalArgumentException("Unsupported SecurityPolicy: " + securityPolicyUri);
    }

    byte[] encryptedPassword = cipher.doFinal(dataToEncrypt);

    return encryptedPassword;
  }
}

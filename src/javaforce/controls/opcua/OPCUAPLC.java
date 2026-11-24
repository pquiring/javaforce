package javaforce.controls.opcua;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.cert.X509Certificate;

import javaforce.*;
import javaforce.net.*;
import javaforce.controls.*;

/** OPC-UA PLC.
 *
 * Default Port : 4840
 * Ignition Port : 62541
 *
 * @author pquiring
 */

public class OPCUAPLC extends OPCUA implements PLC {
  private Socket socket;
  private InputStream is;
  private OutputStream os;
  private boolean connected;
  private String host;
  private String url;

  private static Object connect_lock = new Object();

  public static boolean debug = true;
  public static int port = 4840;
  public Exception lastException;

  //known ports
  public static final int PORT_DEFAULT = 4840;
  public static final int PORT_IGNITION = 62541;

  //messages
  private static final String HEL = "HEL";  //hello
  private static final String ACK = "ACK";  //ack
  private static final String OPN = "OPN";  //open secure channel
  private static final String MSG = "MSG";  //message

  //context
  private int channel_id;
  private int token_id;
  private int seq_num;
  private int req_id;

  public static void setPort(int port) {
    OPCUAPLC.port = port;
  }

  public boolean connect(String host) {
    String user = null;
    String pass = null;
    int idx = host.indexOf("@");
    if (idx != -1) {
      String user_pass = host.substring(0, idx);
      host = host.substring(idx + 1);
      idx = user_pass.indexOf(":");
      if (idx != -1) {
        user = user_pass.substring(0, idx);
        pass = user_pass.substring(idx + 1);
      }
    }
    synchronized(connect_lock) {
      try {
        socket = PLCFactory.connect(host, port);
        os = socket.getOutputStream();
        is = socket.getInputStream();
        this.host = host;
        hello(host);
        open_secure_channel();
        create_session_request();
        if (user != null && pass != null) {
          if (!login(user, pass)) {
            throw new Exception("login failed");
          }
        }
        connected = true;
        return true;
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
    }
  }

  public boolean disconnect() {
    try {
      if (socket != null) {
        close_session_request();
        socket.close();
        socket = null;
      }
      connected = false;
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean login(String user, String pass) {
    try {
      activate_session_request(user, pass);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean isConnected() {
    if (socket == null) return false;
    return socket.isConnected();
  }

  public boolean isBE() {
    return false;  //varies
  }

  public boolean keepAlive() {
    return false;
  }

  public byte[] readTag(String tag) {
    try {
      return read_request(new String[] {tag})[0];
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public byte[][] readTags(String[] tags) {
    return null;
  }

  public boolean writeTag(String tag, byte[] data, int tagType) {
    try {
      write_request(tag, data, tagType);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public Calendar getClock() {
    return null;
  }

  public boolean setClock(Calendar clock) {
    return false;
  }

  public Exception getException() {
    return lastException;
  }

  private int max_msg_size;
  private int max_chunk_count;

  private void hello(String host) throws Exception {
    url = "opc.tcp://" + host + ":" + port;
    Packet packet = new Packet(Endian.L);
    write_header(packet, HEL);
    packet.writeInt(0);  //version
    packet.writeInt(64 * 1024 - 1);  //rec buf size
    packet.writeInt(64 * 1024 - 1);  //send buf size
    packet.writeInt(0);  //max msg size
    packet.writeInt(0);  //max chunk size
    packet.writeInt(url.length());  //strlen
    packet.writeString(url);
    patch_header_length(packet);
    os.write(packet.toByteArray());
    packet = read_packet(is);
    if (!read_reply.equals(ACK)) throw new Exception("OPCUA.hello:expecting ACK");
    int version = packet.readInt();
    int rec_buf_size = packet.readInt();
    int snd_buf_size = packet.readInt();
    max_msg_size = packet.readInt();
    max_chunk_count = packet.readInt();
  }

  private void open_secure_channel() throws Exception {
    seq_num = 1;
    req_id = 1;
    String uri = "http://opcfoundation.org/UA/SecurityPolicy#None";
    Packet packet = new Packet(Endian.L);
    write_header(packet, OPN);
    packet.writeInt(0);  //secure channel id
    packet.writeInt(uri.length());  //strlen
    packet.writeString(uri);  //secure channel uri
    packet.writeInt(-1);  //sender cert
    packet.writeInt(-1);  //receiver cert
    packet.writeInt(seq_num);  //seq #
    packet.writeInt(req_id);  //req id
    //type_id {
      write_node_id(packet, NODE_ID_SHORT, INDEX_0, OPEN_SECURE_CHANNEL_REQ);
    //}
    //OpenSecureChannelRequest {
      //RequestHeader {
        //type_id {
          write_node_id(packet, NODE_ID_BYTE, (byte)0);
        //}
        packet.writeLong(toOPC(System.currentTimeMillis()));  //timestamp
        packet.writeInt(req_id);  //req handle
        packet.writeInt(0);  //diag
        packet.writeInt(-1);  //audit id
        packet.writeInt(10000);  //timehint
        //ExtensionObject {
          //type_id {
            write_node_id(packet, NODE_ID_BYTE, (byte)0);
          //}
          packet.writeByte((byte)0x00);  //encoding mask bits
        //}
      //}
      packet.writeInt(0);  //client protocol version (0)
      packet.writeInt(0);  //sec token req type : issue
      packet.writeInt(1);  //msg sec mode
      packet.writeInt(-1);  //client nonce
      packet.writeInt(0x36ee80);  //req lifetime (3600000)
    //}
    patch_header_length(packet);
    os.write(packet.toByteArray());
    packet = read_packet(is);
    if (!read_reply.equals(OPN)) throw new Exception("OPCUA.open_secure_channel:expecting OPN");
    packet.readInt();  //secure channel id
    int strlen = packet.readInt();  //strlen
    packet.readString(strlen);  //secure channel uri
    packet.readInt();  //sender cert
    packet.readInt();  //receiver cert
    packet.readInt();  //seq #
    packet.readInt();  //req id
    //type_id {
      short msg = (short)read_node_id(packet);  //node id : open secure channel response
      if (msg != OPEN_SECURE_CHANNEL_RES) throw new Exception("OPCUA.open_secure_channel:expecting open_secure_channel_response");
    //}
    //OpenSecureChannelResponse {
      toUnix(packet.readLong());  //timestamp
      packet.readInt();  //request handle
      int res = packet.readInt();  //service result (0=success)
      if (res != 0) {
        throw new Exception("OPCUA.open_secure_channel:failed:result=0x" + Integer.toHexString(res));
      }
      packet.readByte();  //service diag
      packet.readInt();  //array size (-1)
      //ExtensionObject {
        //type_id {
          read_node_id(packet);
        //}
        packet.readByte();  //encoding mask
      //}
      packet.readInt();  //service protocol version (0)
      //secureToken {
        channel_id = packet.readInt();  //channel id
        token_id = packet.readInt();  //token id
        toUnix(packet.readLong());  //token created timestamp
        packet.readInt();  //token lifetime
      //}
      packet.readInt();  //service nonce
    //}
  }

  private static final String app_uri = "urn:opcua-client:client";
  private static final String app_name = "opcua-client";
  private static final String session_name = "opcua-client-session";

  private byte[] client_nonce;
  private byte[] server_nonce;

  private byte[] server_cert;
  private String server_sec_policy;

  private String session_id;
  private byte[] auth_token;

  private Random random = new Random();

  private void create_session_request() throws Exception {
    Packet packet = new Packet(Endian.L);
    seq_num++;
    req_id = 10;  //why???
    write_header(packet, MSG, channel_id, token_id, seq_num, req_id);
    //type_id {
      write_node_id(packet, NODE_ID_SHORT, INDEX_0, CREATE_SESSION_REQ);
    //}
    //CreateSessionRequest {
      //RequestHeader {
        //type_id {
          write_node_id(packet, NODE_ID_BYTE, (byte)0);
        //}
        packet.writeLong(toOPC(System.currentTimeMillis()));  //timestamp
        packet.writeInt(req_id);  //req handle
        packet.writeInt(0);  //diag
        packet.writeInt(-1);  //audit id
        packet.writeInt(10000);  //timehint
        //ExtensionObject {
          //type_id {
            write_node_id(packet, NODE_ID_BYTE, (byte)0);
          //}
          packet.writeByte((byte)0x00);  //encoding mask bits
        //}
      //}
      //ClientDescription {
        packet.writeInt(app_uri.length());  //strlen
        packet.writeString(app_uri);  //app uri
        packet.writeInt(-1);  //prod uri (null)
        //AppName {
          packet.writeByte((byte)2);  //encoding (2) TEXT
          packet.writeInt(app_name.length());
          packet.writeString(app_name);  //app name text
        //}
        packet.writeInt(1);  //apptype : 1=client
        packet.writeInt(-1);  //gateway server uri (null)
        packet.writeInt(-1);  //discovery profile uri (null)
        packet.writeInt(0);  //discovery urls (none)
        packet.writeInt(-1);  //server uri (null)
        packet.writeInt(url.length());  //strlen
        packet.writeString(url);  //endpoint url
        packet.writeInt(session_name.length());  //strlen
        packet.writeString(session_name);  //session name
        client_nonce = new byte[32];
        random.nextBytes(client_nonce);
        packet.writeInt(32);
        packet.write(client_nonce);
        packet.writeInt(-1);  //client cert
        packet.writeLong(120000);  //session timeout (120 secs)
        packet.writeInt(0);  //max response msg size
      //}
    //}
    patch_header_length(packet);
    os.write(packet.toByteArray());
    packet = read_packet(is);
    if (!read_reply.equals(MSG)) throw new Exception("OPCUA.create_session_request:expecting MSG");
    packet.readInt();  //channel id
    packet.readInt();  //token id
    packet.readInt();  //seq #
    packet.readInt();  //req id
    //type_id {
      short msg = (short)read_node_id(packet);  //node id : create session response
      if (msg != CREATE_SESSION_RES) throw new Exception("OPCUA.create_session_request:expecting create_session_response");
    //}
    //CreateSessionResponse {
      //ResponseHeader {
        toUnix(packet.readLong());  //timestamp
        packet.readInt();  //req handle
        int res = packet.readInt();  //service result (0=success)
        if (res != 0) {
          throw new Exception("OPCUA.create_session_request:failed:result=0x" + Integer.toHexString(res));
        }
        packet.readByte();  //service diag
        packet.readInt();  //StringTable : length = -1 (null)
        //ExtensionObject {
          //type_id {
            read_node_id(packet);
          //}
          packet.readByte();  //encoding mask
        //}
        //type_id {
          session_id = (String)read_node_id(packet);  //session id string
        //}
        //type_id {
          auth_token = (byte[])read_node_id(packet);  //authentication token
        //}
      //}
      packet.readLong();  //session timeout
      int nlen = packet.readInt();  //nonce length
      server_nonce = new byte[nlen];
      packet.read(server_nonce);  //server nonce

      int certlen = packet.readInt();  //server cert length
      server_cert = new byte[certlen];
      packet.read(server_cert);  //server certificate
      //ServerEndPoints {
        int numendpoints = packet.readInt();
        for(int a=0;a<numendpoints;a++) {
          int urllen = packet.readInt();
          String url = packet.readString(urllen);
          int applen = packet.readInt();
          String app = packet.readString(applen);
          int prodlen = packet.readInt();  //-1
          //AppName {
            packet.readByte();  //encoding (0)
            packet.readInt();  //apptype (0=server)
            packet.readInt();  //gateway uri (-1=null)
            packet.readInt();  //discovery uri (-1=null)
          //}
          packet.readInt();  //discovery urls (-1=none)
          packet.readInt();  //server cert (-1=null)
          packet.readInt();  //msg security mode (0=none)
          int pollen = packet.readInt();
          String ep_sec_policy = packet.readString(pollen);
          int user_id_tokens = packet.readInt();
          for(int b=0;b<user_id_tokens;b++) {
            int policy_len = packet.readInt();
            String policy_id = packet.readString(policy_len);  //policy id ("username")
            packet.readInt();  //user token type (1=username)
            packet.readInt();  //issued token type (-1)
            packet.readInt();  //endpoint url (-1)
            int sec_policy_len = packet.readInt();
            String user_id_sec_policy = packet.readString(sec_policy_len);  //security policy uri : http://opcfoundation.org/UA/SecurityPolicy#Basic256 #Basic128Rsa15 #Basic256Sha256
            if (ep_sec_policy.endsWith("None")) {
              server_sec_policy = user_id_sec_policy;
            }
          }
          int profile_len = packet.readInt();
          String profile = packet.readString(profile_len);  //transpport profile uri
          packet.readByte();  //sec level (varies)
        }
      //}
      packet.readInt();  //server software certs (0)
      //ServerSignData {
        packet.readInt();  //algo (-1)
        packet.readInt();  //sign (-1)
      //}
      packet.readInt();  //max msg size
    //}
  }

  private void activate_session_request(String user, String pass) throws Exception {
    Packet packet = new Packet(Endian.L);
    seq_num++;
    req_id++;
    write_header(packet, MSG, channel_id, token_id, seq_num, req_id);
    //type_id {
      write_node_id(packet, NODE_ID_SHORT, INDEX_0, ACTIVATE_SESSION_REQ);
    //}
    //ActivateSessionRequest {
      //RequestHeader {
        //type_id {
          write_node_id(packet, NODE_ID_OPAQUE, INDEX_0, auth_token);
        //}
        packet.writeLong(toOPC(System.currentTimeMillis()));  //timestamp
        packet.writeInt(req_id);  //req handle
        packet.writeInt(0);  //diag
        packet.writeInt(-1);  //audit id
        packet.writeInt(10000);  //timehint
        //ExtensionObject {
          //type_id {
            write_node_id(packet, NODE_ID_BYTE, (byte)0);
          //}
          packet.writeByte((byte)0x00);  //encoding mask bits
        //}
      //}
      //ClientSignature {
        packet.writeInt(-1);  //algo
        packet.writeInt(-1);  //sign
      //}
      packet.writeInt(0);  //client software certs (0)
      packet.writeInt(0);  //local ids (0)
      //UserIdentityToken {
        write_node_id(packet, NODE_ID_SHORT, USER_TOKEN);
        packet.writeByte((byte)0x01);  //encoding mask
        //UserNameIdentityToken {
          int struct_offset = packet.offset;
          packet.writeInt(0);  //struct length (varies) (patch later)
          packet.writeInt(8);  //strlen
          packet.writeString("username");  //policyid
          packet.writeInt(user.length());
          packet.writeString(user);  //user name
          X509Certificate cert = (X509Certificate)parseDerCertificate(server_cert);
          if (debug) {
            JFLog.log("cert=" + cert);
            JFLog.log("password=" + pass);
            JFLog.log("policy=" + server_sec_policy);
          }
          byte[] epass = encode_password(server_sec_policy, pass, server_nonce, cert);
          packet.writeInt(epass.length);
          packet.write(epass);
          packet.writeInt(XMLENC_RSA_OAEP_URI.length());
          packet.writeString(XMLENC_RSA_OAEP_URI);
          LE.setuint32(packet.data, struct_offset, packet.offset - struct_offset - 4);  //patch struct length
        //}
      //}
      //UserTokenSignature {
        packet.writeInt(-1);  //algo
        packet.writeInt(-1);  //sign
      //}
    //}
    patch_header_length(packet);
    os.write(packet.toByteArray());
    packet = read_packet(is);
    if (!read_reply.equals(MSG)) throw new Exception("OPCUA.activate_session_request:expecting MSG");
    packet.readInt();  //channel id
    packet.readInt();  //token id
    packet.readInt();  //seq #
    packet.readInt();  //req id
    //type_id {
      short msg = (short)read_node_id(packet);  //node id : activate session response
      if (msg != ACTIVATE_SESSION_RES) throw new Exception("OPCUA.activate_session_request:expecting activate_session_response");
    //}
    //ActivateSessionResponse {
      //ResponseHeader {
        toUnix(packet.readLong());  //timestamp
        packet.readInt();  //req handle
        int res = packet.readInt();  //service result (0=success)
        if (res != 0) {
          throw new Exception("OPCUA.activate_session_request:failed:result=0x" + Integer.toHexString(res));
        }
        packet.readByte();  //service diag
        packet.readInt();  //StringTable : length = -1 (null)
        //ExtensionObject {
          //type_id {
            read_node_id(packet);
          //}
          packet.readByte();  //encoding mask
        //}
      //}
      int nlen = packet.readInt();  //nonce length
      server_nonce = new byte[nlen];
      packet.read(server_nonce);  //server nonce
      packet.readInt();  //Array of Status code
      packet.readInt();  //diag info
    //}
  }

  private void close_session_request() throws Exception {
    Packet packet = new Packet(Endian.L);
    seq_num++;
    req_id++;
    write_header(packet, MSG, channel_id, token_id, seq_num, req_id);
    //type_id {
      write_node_id(packet, NODE_ID_SHORT, INDEX_0, CLOSE_SESSION_REQ);
    //}
    //CloseSessionRequest {
      //RequestHeader {
        //type_id {
          write_node_id(packet, NODE_ID_OPAQUE, INDEX_0, auth_token);
        //}
        packet.writeLong(toOPC(System.currentTimeMillis()));  //timestamp
        packet.writeInt(req_id);  //req handle
        packet.writeInt(0);  //diag
        packet.writeInt(-1);  //audit id
        packet.writeInt(10000);  //timehint
        //ExtensionObject {
          //type_id {
            write_node_id(packet, NODE_ID_BYTE, (byte)0);
          //}
          packet.writeByte((byte)0x00);  //encoding mask bits
        //}
      //}
      packet.writeByte((byte)1);  //delete subscriptions
    //}
    patch_header_length(packet);
    os.write(packet.toByteArray());
    packet = read_packet(is);
    if (!read_reply.equals(MSG)) throw new Exception("OPCUA.close_session_request:expecting MSG");
    packet.readInt();  //channel id
    packet.readInt();  //token id
    packet.readInt();  //seq #
    packet.readInt();  //req id
    //type_id {
      short msg = (short)read_node_id(packet);  //node id : close session response
      if (msg != CLOSE_SESSION_RES) throw new Exception("OPCUA.close_session_request:expecting close_session_response");
    //}
    //CloseSessionResponse {
      //ResponseHeader {
        toUnix(packet.readLong());  //timestamp
        packet.readInt();  //req handle
        int res = packet.readInt();  //service result (0=success)
        if (res != 0) {
          throw new Exception("OPCUA.close_session_request:failed:result=0x" + Integer.toHexString(res));
        }
        packet.readByte();  //service diag
        packet.readInt();  //StringTable : length = -1 (null)
        //ExtensionObject {
          //type_id {
            read_node_id(packet);
          //}
          packet.readByte();  //encoding mask
        //}
      //}
    //}
  }

  private byte[][] read_request(String[] tags) throws Exception {
    Packet packet = new Packet(Endian.L);
    seq_num++;
    req_id++;
    write_header(packet, MSG, channel_id, token_id, seq_num, req_id);
    //type_id {
      write_node_id(packet, NODE_ID_SHORT, INDEX_0, READ_TAG_REQ);
    //}
    //ReadRequest {
      //RequestHeader {
        //type_id {
          write_node_id(packet, NODE_ID_OPAQUE, INDEX_0, auth_token);
        //}
        packet.writeLong(toOPC(System.currentTimeMillis()));  //timestamp
        packet.writeInt(req_id);  //req handle
        packet.writeInt(0);  //diag
        packet.writeInt(-1);  //audit id
        packet.writeInt(10000);  //timehint
        //ExtensionObject {
          //type_id {
            write_node_id(packet, NODE_ID_BYTE, (byte)0);
          //}
          packet.writeByte((byte)0x00);  //encoding mask bits
        //}
      //}
      packet.writeLong(0);  //max age
      packet.writeInt(2);  //timestamps to return (2=both)
      //NodesToRead {
        packet.writeInt(tags.length);  //array length
        for (String tag : tags) {
          //type_id {
            write_node_id(packet, NODE_ID_STRING, INDEX_1, tag);
          //}
          packet.writeInt(ATTR_VALUE);  //attr id : 0x0d = value
          packet.writeInt(-1);  //index range
          //QualifiedName {
            packet.writeShort((short)0);  //id
            packet.writeInt(0);  //name=null
          //}
        }
      //}
    //}
    patch_header_length(packet);
    os.write(packet.toByteArray());
    packet = read_packet(is);
    if (!read_reply.equals(MSG)) throw new Exception("OPCUA.read_request:expecting MSG");
    packet.readInt();  //channel id
    packet.readInt();  //token id
    packet.readInt();  //seq #
    packet.readInt();  //req id
    //type_id {
      short msg = (short)read_node_id(packet);  //node id : close session response
      if (msg != READ_TAG_RES) throw new Exception("OPCUA.read_request:expecting read_response");
    //}
    //ReadResponse {
      //ResponseHeader {
        toUnix(packet.readLong());  //timestamp
        packet.readInt();  //req handle
        int res = packet.readInt();  //service result (0=success)
        if (res != 0) {
          throw new Exception("OPCUA.read_request:failed:result=0x" + Integer.toHexString(res));
        }
        packet.readByte();  //service diag
        packet.readInt();  //StringTable : length = -1 (null)
        //ExtensionObject {
          //type_id {
            read_node_id(packet);
          //}
          packet.readByte();  //encoding mask
        //}
        //Results {
          int len = packet.readInt();
          byte[][] results = new byte[len][];
          for(int a=0;a<len;a++) {
            byte status = packet.readByte();  //encoding mask
            if ((status & 0x01) == 0x01) {
              //has value
              byte type = packet.readByte();  //tag type
              byte[] result = null;
              switch (type) {
                case TYPE_BOOLEAN:
                case TYPE_INT8:
                case TYPE_UINT8:
                  result = new byte[1];
                  break;
                case TYPE_INT16:
                case TYPE_UINT16:
                  result = new byte[2];
                  break;
                case TYPE_INT32:
                case TYPE_UINT32:
                case TYPE_FLOAT32:
                  result = new byte[4];
                  break;
                case TYPE_INT64:
                case TYPE_UINT64:
                case TYPE_FLOAT64:
                  result = new byte[8];
                  break;
              }
              if (result != null) {
                packet.read(result);
              }
              results[a] = result;
              toUnix(packet.readLong());  //source timestamp
              toUnix(packet.readLong());  //server timestamp
            } else if ((status & 0x02) == 0x02) {
              //has status code
              int error = packet.readInt();
              JFLog.log("read:error=0x" + Integer.toHexString(error));
            }
          }
        //}
        packet.readInt();  //diags array size (0)
      //}
    //}
    return results;
  }

  private void write_request(String tag, byte[] data, int tagType) throws Exception {
    Packet packet = new Packet(Endian.L);
    seq_num++;
    req_id++;
    write_header(packet, MSG, channel_id, token_id, seq_num, req_id);
    //type_id {
      write_node_id(packet, NODE_ID_SHORT, INDEX_0, WRITE_TAG_REQ);
    //}
    //ReadRequest {
      //RequestHeader {
        //type_id {
          write_node_id(packet, NODE_ID_OPAQUE, INDEX_0, auth_token);
        //}
        packet.writeLong(toOPC(System.currentTimeMillis()));  //timestamp
        packet.writeInt(req_id);  //req handle
        packet.writeInt(0);  //diag
        packet.writeInt(-1);  //audit id
        packet.writeInt(10000);  //timehint
        //ExtensionObject {
          //type_id {
            write_node_id(packet, NODE_ID_BYTE, (byte)0);
          //}
          packet.writeByte((byte)0x00);  //encoding mask bits
        //}
      //}
      //NodesToWrite {
        packet.writeInt(1);  //array length
        //for (String tag : tags) {
          //type_id {
            write_node_id(packet, NODE_ID_STRING, INDEX_1, tag);
          //}
          packet.writeInt(ATTR_VALUE);  //attr id : 0x0d = value
          packet.writeInt(-1);  //index range
          //DataValue {
            packet.writeByte((byte)0x01);  //encoding mask : 1 = has value
            switch (tagType) {
              case TagType.bit:
                packet.writeByte(TYPE_BOOLEAN);
                break;
              case TagType.int8:
                packet.writeByte(TYPE_INT8);
                break;
              case TagType.int16:
                packet.writeByte(TYPE_INT16);
                break;
              case TagType.int32:
                packet.writeByte(TYPE_INT32);
                break;
              case TagType.int64:
                packet.writeByte(TYPE_INT64);
                break;
              case TagType.float32:
                packet.writeByte(TYPE_FLOAT32);
                break;
              case TagType.float64:
                packet.writeByte(TYPE_FLOAT64);
                break;
            }
            packet.write(data);
          //}
        //}
      //}
    //}
    patch_header_length(packet);
    os.write(packet.toByteArray());
    packet = read_packet(is);
    if (!read_reply.equals(MSG)) throw new Exception("OPCUA.write_request:expecting MSG");
    packet.readInt();  //channel id
    packet.readInt();  //token id
    packet.readInt();  //seq #
    packet.readInt();  //req id
    //type_id {
      short msg = (short)read_node_id(packet);  //node id : write request response
      if (msg != WRITE_TAG_RES) throw new Exception("OPCUA.write_request:expecting write_response");
    //}
    //ReadResponse {
      //ResponseHeader {
        toUnix(packet.readLong());  //timestamp
        packet.readInt();  //req handle
        int res = packet.readInt();  //service result (0=success)
        if (res != 0) {
          throw new Exception("OPCUA.write_request:failed:result=0x" + Integer.toHexString(res));
        }
        packet.readByte();  //service diag
        packet.readInt();  //StringTable : length = -1 (null)
        //ExtensionObject {
          //type_id {
            read_node_id(packet);
          //}
          packet.readByte();  //encoding mask
        //}
        //Results {
          int len = packet.readInt();
          for(int a=0;a<len;a++) {
            int result = packet.readInt();  //result
            if (result != 0) {
              throw new Exception("write failed");
            }
          }
        //}
        packet.readInt();  //diags array size (0)
      //}
    //}
  }
}

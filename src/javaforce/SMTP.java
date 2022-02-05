package javaforce;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/**
 * SMTP client class.
 */
public class SMTP {

  public SMTP() {
  }
  private Socket s;
  private InputStream is;
  private OutputStream os;
  private BufferedReader br;
  private boolean passive = true;
  private String host;
  private ServerSocket active;  //active socket
  private boolean log = false;
  public boolean debug = false;
  /**
   * Holds the repsonse strings from the last executed command
   */
  public String response[];

  public static final int PORT = 25;  //default port
  public static final int SSLPORT = 465;  //default SSL port

  public boolean connect(String host, int port) throws Exception {
    s = new Socket(host, port);
    is = s.getInputStream();
    br = new BufferedReader(new InputStreamReader(is));
    os = s.getOutputStream();
    this.host = host;
    getResponse();
    if (response[response.length - 1].startsWith("220")) {
      return true;
    }
    disconnect();  //not valid SMTP site
    return false;
  }

  public boolean connectSSL(String host, int port) throws Exception {
    TrustManager[] trustAllCerts = new TrustManager[]{
      new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
      }
    };
    // Let us create the factory where we can set some parameters for the connection
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
//      SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();  //this method will only work with trusted certs
    SSLSocketFactory sslsocketfactory = (SSLSocketFactory) sc.getSocketFactory();  //this method will work with untrusted certs
    SSLSocket ssl = (SSLSocket) sslsocketfactory.createSocket(host, port);
    s = (Socket) ssl;
    is = s.getInputStream();
    br = new BufferedReader(new InputStreamReader(is));
    os = s.getOutputStream();
    this.host = host;
    getResponse();
    if (response[response.length - 1].startsWith("220")) {
      return true;
    }
    disconnect();  //not valid SMTP site
    return false;
  }

  public void disconnect() throws Exception {
    if (s != null) {
      s.close();
    }
    s = null;
    is = null;
    os = null;
  }

  public void setLogging(boolean state) {
    log = state;
  }

  public boolean login() throws Exception {
    cmd("HELO " + host);
    getResponse();
    if (!response[response.length - 1].startsWith("250")) {
      return false;
    }
    return true;
  }

  public static final String AUTH_LOGIN = "LOGIN";
  public static final String AUTH_NTLM = "NTLM";

  public boolean auth(String user, String pass, String type) throws Exception {
    switch (type) {
      case AUTH_LOGIN: cmd("AUTH LOGIN"); break;
      case AUTH_NTLM: cmd("AUTH NTLM"); break;
      default: JFLog.log("SMTP:Unknown auth type:" + type);
    }
    getResponse();
    if (response[response.length - 1].startsWith("504")) {
      if (log) {
        JFLog.log("AUTH " + type + " not supported!");
      }
      return false;
    }
    if (!response[response.length - 1].startsWith("334")) {
      return false;
    }
    cmd(encodeFirst(user, pass, type, response[response.length - 1].substring(4)));
    getResponse();
    if (!response[response.length - 1].startsWith("334")) {
      return false;
    }
    cmd(encodeSecond(user, pass, type, response[response.length - 1].substring(4)));
    getResponse();
    if (!response[response.length - 1].startsWith("235")) {
      return false;
    }
    return true;
  }

  public boolean auth(String user, String pass) throws Exception {
    return auth(user, pass, AUTH_LOGIN);
  }

  public void logout() throws Exception {
    cmd("quit");
    getResponse();  //should be "221" but ignored
  }

  public void cmd(String cmd) throws Exception {
    if ((s == null) || (s.isClosed())) {
      throw new Exception("not connected");
    }
    if (log) {
      if (cmd.startsWith("pass ")) {
        JFLog.log("pass ****");
      } else {
        JFLog.log(cmd);
      }
    }
    cmd += "\r\n";
    os.write(cmd.getBytes());
  }

  public void from(String email) throws Exception {
    cmd("MAIL FROM:<" + email + ">");
    getResponse();
  }

  public void to(String email) throws Exception {
    cmd("RCPT TO:<" + email + ">");
    getResponse();
  }

  public boolean data(String msg) throws Exception {
    /*
     sample data:
     From: "First Last" <bob@example.com>
     To: "First Last" <to@example.com>
     Cc: "First Last" <cc@example.com>
     Date: Tue, 15 Jan 2008 16:02:43 -0500
     Subject: Subject line

     Hello Bob,
     blah blah blah
     make sure you don't have . (period) on a line by itself since that ends the message.
     */
    cmd("DATA");
    getResponse();
    if (!response[response.length - 1].startsWith("354")) {
      return false;
    }
    os.write(msg.getBytes());
    os.write("\r\n.\r\n".getBytes());
    getResponse();
    if (!response[response.length - 1].startsWith("250")) {
      return false;
    }
    return true;
  }

  private void getResponse() throws Exception {
    ArrayList<String> tmp = new ArrayList<String>();
    String str;
    while (!s.isClosed()) {
      str = br.readLine();
      tmp.add(str);
      if (str.charAt(3) == ' ') {
        break;
      }
    }
    int size = tmp.size();
    response = new String[size];
    for (int a = 0; a < size; a++) {
      response[a] = tmp.get(a);
      if (debug) {
        System.out.println(response[a]);
      }
    }
  }

  public String getLastResponse() {
    if (response == null) return null;
    return response[response.length - 1];
  }

  private static String extractEmail(String in) {
    int i1 = in.indexOf('<');
    int i2 = in.indexOf('>');
    return in.substring(i1+1, i2);
  }

  public static void main(String args[]) {
    if (args.length < 2) {
      System.out.println("Usage:SMTP server[:port] msg.txt [user pass [type]]");
      System.out.println("  type=LOGIN(default) NTLM");
      System.out.println("msg.txt sample:\r\n");
      System.out.println("From: \"First Last\" <bob@example.com>");
      System.out.println("To: \"First Last\" <to@example.com>");
      System.out.println("Cc: \"First Last\" <cc@example.com>");
      System.out.println("Date: Tue, 15 Jan 2008 16:02:43 -0500");
      System.out.println("Subject: Subject line\r\n");
      System.out.println("Hello Bob, ...");
      return;
    }
    SMTP smtp = new SMTP();
    try {
      String txt = new String(JF.readAll(new FileInputStream(args[1])));
      txt = txt.replaceAll("\r", "");
      String lns[] = txt.split("\n");
      if (lns.length < 4) throw new Exception("file too short");
      smtp.setLogging(true);
      smtp.debug = true;
      int port = 25;
      String host = args[0];
      int idx = host.indexOf(':');
      if (idx != -1) {
        port = JF.atoi(host.substring(idx+1));
        host = host.substring(0, idx);
      }
      if (port == 25 || port == 587) {
        smtp.connect(host, port);
      } else {
        //465
        smtp.connectSSL(host, port);
      }
      smtp.login();
      if (args.length >= 4) {
        String auth_type = AUTH_LOGIN;
        if (args.length == 5) {
          switch (args[4]) {
            case "LOGIN": auth_type = AUTH_LOGIN; break;
            case "NTLM": auth_type = AUTH_NTLM; break;
          }
        }
        if (!smtp.auth(args[2], args[3], auth_type)) {
          throw new Exception("Login failed!");
        }
      }
      smtp.from(extractEmail(lns[0]));
      for(int a=1;a<lns.length;a++) {
        String ln = lns[a].toLowerCase();
        if (ln.startsWith("to:") || ln.startsWith("cc:") || ln.startsWith("bcc:")) {
          smtp.to(extractEmail(lns[a]));
        } else {
          break;
        }
      }
      smtp.data(txt);
      System.out.println("Reply=" + smtp.getLastResponse());
      smtp.disconnect();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  //https://github.com/eclipse-ee4j/mail/blob/master/mail/src/main/java/com/sun/mail/auth/Ntlm.java
  private static final int NTLMSSP_NEGOTIATE_UNICODE	= 0x00000001;
  private static final int NTLMSSP_NEGOTIATE_OEM	= 0x00000002;
  private static final int NTLMSSP_REQUEST_TARGET	= 0x00000004;
  private static final int NTLMSSP_NEGOTIATE_SIGN	= 0x00000010;
  private static final int NTLMSSP_NEGOTIATE_SEAL	= 0x00000020;
  private static final int NTLMSSP_NEGOTIATE_DATAGRAM	= 0x00000040;
  private static final int NTLMSSP_NEGOTIATE_LM_KEY	= 0x00000080;
  private static final int NTLMSSP_NEGOTIATE_NTLM	= 0x00000200;
  private static final int NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED	= 0x00001000;
  private static final int NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED	= 0x00002000;
  private static final int NTLMSSP_NEGOTIATE_ALWAYS_SIGN	= 0x00008000;
  private static final int NTLMSSP_TARGET_TYPE_DOMAIN	= 0x00010000;
  private static final int NTLMSSP_TARGET_TYPE_SERVER	= 0x00020000;
  private static final int NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY	= 0x00080000;
  private static final int NTLMSSP_NEGOTIATE_IDENTIFY	= 0x00100000;
  private static final int NTLMSSP_REQUEST_NON_NT_SESSION_KEY	= 0x00400000;
  private static final int NTLMSSP_NEGOTIATE_TARGET_INFO	= 0x00800000;
  private static final int NTLMSSP_NEGOTIATE_VERSION	= 0x02000000;
  private static final int NTLMSSP_NEGOTIATE_128	= 0x20000000;
  private static final int NTLMSSP_NEGOTIATE_KEY_EXCH	= 0x40000000;
  private static final int NTLMSSP_NEGOTIATE_56	= 0x80000000;
  private static final byte RESPONSERVERSION = 1;
  private static final byte HIRESPONSERVERSION = 1;
  private static final byte[] Z6 = new byte[] { 0, 0, 0, 0, 0, 0 };
  private static final byte[] Z4 = new byte[] { 0, 0, 0, 0 };
  private static byte[] NEGOTIATE_MESSAGE() {
    //NEGOTIATE_MESSAGE
    //https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-nlmp/b34032e5-3aae-4bc6-84c3-c6d80eadf7f2
    String domain_str = System.getenv("USERDOMAIN");
    if (domain_str == null) {
      JFLog.log("Error:USERDOMAIN==null");
      return null;
    }
    byte domain[];
    try {
      domain = domain_str.getBytes("iso-8859-1");
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
    int domain_len = domain.length;
    String workstation_str = System.getenv("COMPUTERNAME");
    if (workstation_str == null) {
      JFLog.log("Error:COMPUTERNAME==null");
      return null;
    }
    byte workstation[];
    try {
      workstation = workstation_str.getBytes("iso-8859-1");
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
    int workstation_len = workstation.length;
    byte[] type1 = new byte[32 + domain_len + workstation_len];
    int flags =
      NTLMSSP_NEGOTIATE_UNICODE |
      NTLMSSP_NEGOTIATE_OEM |
      NTLMSSP_NEGOTIATE_NTLM |
	    NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED |
      NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED |
      NTLMSSP_NEGOTIATE_ALWAYS_SIGN |
      NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY;
    int off = 0;
    LE.setString(type1, off, 7, "NTLMSSP");  //byte signature[8]
    off += 8;  //8
    LE.setuint32(type1, off, 0x00000001);  //int message_type
    off += 4;  //12
    LE.setuint32(type1, off, flags);  //int flags
    off += 4;  //16
    //DomainName : short Len, short MaxLen, int Offset
    LE.setuint16(type1, off, domain_len);  //Len
    off += 2;  //18
    LE.setuint16(type1, off, domain_len);  //MaxLen (ignored)
    off += 2;  //20
    LE.setuint32(type1, off, 32);  //Offset
    off += 4;  //24
    //WorkstationName : short Len, short MaxLen, int Offset
    LE.setuint16(type1, off, workstation_len);  //Len
    off += 2;  //26
    LE.setuint16(type1, off, workstation_len);  //MaxLen (ignored)
    off += 2;  //28
    LE.setuint32(type1, off, 32 + domain_len);  //Offset
    off += 4;  //32
    //Version omitted
    //Payload : Domain
    System.arraycopy(domain, 0, type1, off, domain.length);
    off += domain_len;
    //Payload : Workstation
    System.arraycopy(workstation, 0, type1, off, workstation.length);
    return type1;
  }
  public static String encodeFirst(String user, String pass, String type, String response) {
    switch (type) {
      case AUTH_LOGIN:
        return new String(Base64.encode(user.getBytes()));
      case AUTH_NTLM:
        return new String(Base64.encode(NEGOTIATE_MESSAGE()));
    }
    return null;
  }
  private static byte[] makeDesKey(byte[] input, int off) {
    int[] in = new int[input.length];
    for (int i = 0; i < in.length; i++) {
      in[i] = input[i] < 0 ? input[i] + 256: input[i];
    }
    byte[] out = new byte[8];
    out[0] = (byte)in[off+0];
    out[1] = (byte)(((in[off+0] << 7) & 0xff) | (in[off+1] >> 1));
    out[2] = (byte)(((in[off+1] << 6) & 0xff) | (in[off+2] >> 2));
    out[3] = (byte)(((in[off+2] << 5) & 0xff) | (in[off+3] >> 3));
    out[4] = (byte)(((in[off+3] << 4) & 0xff) | (in[off+4] >> 4));
    out[5] = (byte)(((in[off+4] << 3) & 0xff) | (in[off+5] >> 5));
    out[6] = (byte)(((in[off+5] << 2) & 0xff) | (in[off+6] >> 6));
    out[7] = (byte)((in[off+6] << 1) & 0xff);
    return out;
  }
  private static byte[] calcLMHash(String password, Cipher cipher, SecretKeyFactory fac) throws GeneralSecurityException {
    byte[] magic = {0x4b, 0x47, 0x53, 0x21, 0x40, 0x23, 0x24, 0x25};
    byte[] pwb = null;
    try {
      pwb = password.toUpperCase(Locale.ENGLISH).getBytes("iso-8859-1");
    } catch (UnsupportedEncodingException ex) {
      // should never happen
      assert false;
    }
    byte[] pwb1 = new byte[14];
    int len = password.length();
    if (len > 14) {
      len = 14;
    }
    System.arraycopy(pwb, 0, pwb1, 0, len); /* Zero padded */

    DESKeySpec dks1 = new DESKeySpec(makeDesKey(pwb1, 0));
    DESKeySpec dks2 = new DESKeySpec(makeDesKey(pwb1, 7));

    SecretKey key1 = fac.generateSecret(dks1);
    SecretKey key2 = fac.generateSecret(dks2);
    cipher.init(Cipher.ENCRYPT_MODE, key1);
    byte[] out1 = cipher.doFinal(magic, 0, 8);
    cipher.init(Cipher.ENCRYPT_MODE, key2);
    byte[] out2 = cipher.doFinal(magic, 0, 8);

    byte[] result = new byte [21];
    System.arraycopy(out1, 0, result, 0, 8);
    System.arraycopy(out2, 0, result, 8, 8);
    return result;
  }
  private static byte[] calcNTHash(String password) throws GeneralSecurityException {
    //nTOWFv1()
    byte[] pw = null;
    try {
      pw = password.getBytes("UnicodeLittleUnmarked");
    } catch (UnsupportedEncodingException e) {
      assert false;
    }
    MD4 md4 = new MD4();
    byte[] out = md4.digest(pw);
    byte[] result = new byte[21];
    System.arraycopy(out, 0, result, 0, 16);
    return result;
  }
  private static byte[] calcNTv2Hash(String domain, String username, String password) throws GeneralSecurityException {
    //nTOWFv2()
    try {
      MD4 md4 = new MD4();
      md4.update(password.getBytes("UnicodeLittleUnmarked"));
      HMACT64 hmac = new HMACT64(md4.digest());
      hmac.update(username.toUpperCase().getBytes("UnicodeLittleUnmarked"));
      hmac.update(domain.getBytes("UnicodeLittleUnmarked"));
      return hmac.digest();
    } catch (Exception uee) {
      return null;
    }
  }
  private static byte[] calcResponse(byte[] key, byte[] text, Cipher cipher, SecretKeyFactory fac) throws GeneralSecurityException {
    assert key.length == 21;
    DESKeySpec dks1 = new DESKeySpec(makeDesKey(key, 0));
    DESKeySpec dks2 = new DESKeySpec(makeDesKey(key, 7));
    DESKeySpec dks3 = new DESKeySpec(makeDesKey(key, 14));
    SecretKey key1 = fac.generateSecret(dks1);
    SecretKey key2 = fac.generateSecret(dks2);
    SecretKey key3 = fac.generateSecret(dks3);
    cipher.init(Cipher.ENCRYPT_MODE, key1);
    byte[] out1 = cipher.doFinal(text, 0, 8);
    cipher.init(Cipher.ENCRYPT_MODE, key2);
    byte[] out2 = cipher.doFinal(text, 0, 8);
    cipher.init(Cipher.ENCRYPT_MODE, key3);
    byte[] out3 = cipher.doFinal(text, 0, 8);
    byte[] result = new byte[24];
    System.arraycopy(out1, 0, result, 0, 8);
    System.arraycopy(out2, 0, result, 8, 8);
    System.arraycopy(out3, 0, result, 16, 8);
    return result;
  }
  private static byte[] hmacMD5(Mac hmac, byte[] key, byte[] text) {
    try {
      byte[] nk = new byte[16];
      System.arraycopy(key, 0, nk, 0, key.length > 16 ? 16 : key.length);
      SecretKeySpec skey = new SecretKeySpec(nk, "HmacMD5");
      hmac.init(skey);
      return hmac.doFinal(text);
    } catch (InvalidKeyException ex) {
      assert false;
    } catch (RuntimeException e) {
      assert false;
    }
    return null;
  }

  public static byte[] calcLMv2Response(String domain, String user,
    String password, byte[] server_nonce, byte[] client_nonce)
  {
    try {
      byte[] hash = new byte[16];
      byte[] response = new byte[24];
      MD4 md4 = new MD4();
      HMACT64 hmac = new HMACT64(md4.digest(password.getBytes("UnicodeLittleUnmarked")));
      hmac.update(user.toUpperCase().getBytes("UnicodeLittleUnmarked"));
      hmac.update(domain.toUpperCase().getBytes("UnicodeLittleUnmarked"));
      hmac = new HMACT64(hmac.digest());
      hmac.update(server_nonce);
      hmac.update(client_nonce);
      hmac.digest(response, 0, 16);
      System.arraycopy(client_nonce, 0, response, 16, 8);
      return response;
    } catch (Exception ex) {
      return null;
    }
  }

  private static byte[] calcNTv2Response(byte[] nthash, byte[] client_blob, byte[] server_nonce, String domain, String username)
    throws GeneralSecurityException
  {
    //this version is from Jakarta Mail API
    byte[] txt = null;
    try {
      txt = (username.toUpperCase(Locale.ENGLISH) + domain).getBytes("UnicodeLittleUnmarked");
    } catch (UnsupportedEncodingException ex) {
      // should never happen
      assert false;
    }
    Mac hmac = Mac.getInstance("HmacMD5");
    byte[] ntlmv2hash = hmacMD5(hmac, nthash, txt);
    byte[] cb = new byte[client_blob.length + 8];
    System.arraycopy(server_nonce, 0, cb, 0, 8);
    System.arraycopy(client_blob, 0, cb, 8, client_blob.length);
    byte[] result = new byte[client_blob.length + 16];
    System.arraycopy(hmacMD5(hmac, ntlmv2hash, cb), 0, result, 0, 16);
    System.arraycopy(client_blob, 0, result, 16, client_blob.length);
    return result;
  }

  private static byte[] calcNTv2ResponseVariant(byte[] nthash, byte[] client_blob, byte[] server_nonce)
    throws GeneralSecurityException
  {
    //this version is from jcifs
    HMACT64 hmac = new HMACT64(nthash);
    hmac.update(server_nonce);
    hmac.update(client_blob, 0, client_blob.length);
    byte[] mac = hmac.digest();
    byte[] ret = new byte[mac.length + client_blob.length];
    System.arraycopy(mac, 0, ret, 0, mac.length);
    System.arraycopy(client_blob, 0, ret, mac.length, client_blob.length);
    return ret;
  }

  private static byte[] AUTHENTICATE_MESSAGE(String user_str, String pass_str, String response) {
    //CHALLENGE_MESSAGE
    //https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-nlmp/801a4681-8809-4be9-ab0d-61dcfe762786
    byte[] challenge = null;
    try {
      challenge = response.getBytes("us-ascii");
    } catch (Exception e) {
      JFLog.log(e);
    }
    //TODO : validate challenge
    byte[] type2 = Base64.decode(challenge);
    byte[] server_nonce = Arrays.copyOfRange(type2, 24, 24+8);
    int server_flags = LE.getuint32(type2, 20);

    //AUTHENTICATE_MESSAGE
    //https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-nlmp/033d32cc-88f9-4483-9bf2-b273055038ce

    byte[] lmhash;
    byte[] lm_response;
    byte[] nthash;
    byte[] nt_response;

    int client_flags =
		  NTLMSSP_NEGOTIATE_UNICODE |
		  NTLMSSP_NEGOTIATE_NTLM |
		  NTLMSSP_NEGOTIATE_ALWAYS_SIGN;

    String domain_str = System.getenv("USERDOMAIN");
    String workstation_str = System.getenv("COMPUTERNAME");

    try {
      if ((server_flags & NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY) != 0) {
        JFLog.log("SMTP:AUTH:NTLMv2");
        client_flags |= NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY;
        byte[] client_nonce = new byte[8];
	      (new Random()).nextBytes(client_nonce);
        lm_response = calcLMv2Response(domain_str, user_str, pass_str, server_nonce, client_nonce);

        //just for debugging
        byte[] targetName = new byte[0];
        if ((server_flags & NTLMSSP_REQUEST_TARGET) != 0) {
          int tlen = LE.getuint16(type2, 12);
          int toff = LE.getuint32(type2, 16);
          targetName = new byte[tlen];
          System.arraycopy(type2, toff, targetName, 0, tlen);
        }

        byte[] targetInfo = new byte[0];
        if ((server_flags & NTLMSSP_NEGOTIATE_TARGET_INFO) != 0) {
          int tlen = LE.getuint16(type2, 40);
          int toff = LE.getuint32(type2, 44);
          targetInfo = new byte[tlen];
          System.arraycopy(type2, toff, targetInfo, 0, tlen);
          //https://docs.microsoft.com/en-us/openspecs/windows_protocols/ms-nlmp/83f5e789-660d-4781-8491-5f8c6641f75e
        }
        byte[] blob = new byte[28 + targetInfo.length + 4];
        blob[0] = RESPONSERVERSION;
        blob[1] = HIRESPONSERVERSION;
        System.arraycopy(Z6, 0, blob, 2, 6);
        // convert time to NT format
        long now = (System.currentTimeMillis() + 11644473600000L) * 10000L;
        for (int i = 0; i < 8; i++) {
          blob[8 + i] = (byte)(now & 0xff);
          now >>= 8;
        }
	      (new Random()).nextBytes(client_nonce);
        System.arraycopy(client_nonce, 0, blob, 16, 8);
        System.arraycopy(Z4, 0, blob, 24, 4);
        System.arraycopy(targetInfo, 0, blob, 28, targetInfo.length);
        System.arraycopy(Z4, 0, blob, 28 + targetInfo.length, 4);
        if (false) {  //both methods work
          //Jakarta mail method
          nthash = calcNTHash(pass_str);
          nt_response = calcNTv2Response(nthash, blob, server_nonce, domain_str, user_str);
        } else {
          //jcifs method
          nthash = calcNTv2Hash(domain_str, user_str, pass_str);
          nt_response = calcNTv2ResponseVariant(nthash, blob, server_nonce);
        }
        if ((server_flags & NTLMSSP_NEGOTIATE_SIGN) != 0) {
          JFLog.log("SMTP:NTML:ERROR:NEED SESSION KEYS");
          //https://github.com/codelibs/jcifs/blob/master/src/main/java/jcifs/smb1/ntlmssp/Type3Message.java#L247
          if ((server_flags & NTLMSSP_NEGOTIATE_KEY_EXCH) != 0) {
            JFLog.log("SMTP:NTML:ERROR:NEED EXCHANGE KEYS");
          }
        }
      } else {
        JFLog.log("SMTP:AUTH:NTLMv1");
        SecretKeyFactory fac = SecretKeyFactory.getInstance("DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        lmhash = calcLMHash(pass_str, cipher, fac);
        lm_response = calcResponse(lmhash, server_nonce, cipher, fac);
        nthash = calcNTHash(pass_str);
        nt_response = calcResponse(nthash, server_nonce, cipher, fac);
      }
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }

    int payload_off = 64;

    int lm_response_len = lm_response.length;
    int lm_response_off = payload_off;
    payload_off += lm_response_len;

    int nt_response_len = nt_response.length;
    int nt_response_off = payload_off;
    payload_off += nt_response_len;

    if (domain_str == null) return null;
    byte[] domain = null;
    try {
      domain = domain_str.getBytes("UnicodeLittleUnmarked");
    } catch (Exception e) {
      JFLog.log(e);
    }
    int domain_len = domain.length;
    int domain_off = payload_off;
    payload_off += domain_len;

    byte[] user = null;
    try {
      user = user_str.getBytes("UnicodeLittleUnmarked");
    } catch (Exception e) {
      JFLog.log(e);
    }
    int user_len = user.length;
    int user_off = payload_off;
    payload_off += user_len;

    if (workstation_str == null) return null;
    byte[] workstation = null;
    try {
      workstation = workstation_str.getBytes("UnicodeLittleUnmarked");
    } catch (Exception e) {
      JFLog.log(e);
    }
    int workstation_len = workstation.length;
    int workstation_off = payload_off;
    payload_off += workstation_len;

    byte[] session_key = new byte[0];
    int session_key_len = session_key.length;
    int session_key_off = payload_off;
    payload_off += session_key_len;

//    byte[] mic = new byte[16];
    byte[] type3 = new byte[64 + lm_response_len + nt_response_len + domain_len + user_len + workstation_len + session_key_len];

    int off = 0;
    LE.setString(type3, off, 7, "NTLMSSP");  //byte signature[8]
    off += 8;  //8
    LE.setuint32(type3, off, 0x00000003);  //int message_type
    off += 4;  //12
    //LmResponse : short Len, short MaxLen, int Offset
    LE.setuint16(type3, off, lm_response_len);  //Len
    off += 2;  //14
    LE.setuint16(type3, off, lm_response_len);  //MaxLen (ignored)
    off += 2;  //16
    LE.setuint32(type3, off, lm_response_off);  //Offset
    off += 4;  //20
    //NtResponse : short Len, short MaxLen, int Offset
    LE.setuint16(type3, off, nt_response_len);  //Len
    off += 2;  //22
    LE.setuint16(type3, off, nt_response_len);  //MaxLen (ignored)
    off += 2;  //24
    LE.setuint32(type3, off, nt_response_off);  //Offset
    off += 4;  //28
    //Domain : short Len, short MaxLen, int Offset
    LE.setuint16(type3, off, domain_len);  //Len
    off += 2;  //30
    LE.setuint16(type3, off, domain_len);  //MaxLen (ignored)
    off += 2;  //32
    LE.setuint32(type3, off, domain_off);  //Offset
    off += 4;  //36
    //Username : short Len, short MaxLen, int Offset
    LE.setuint16(type3, off, user_len);  //Len
    off += 2;  //38
    LE.setuint16(type3, off, user_len);  //MaxLen (ignored)
    off += 2;  //40
    LE.setuint32(type3, off, user_off);  //Offset
    off += 4;  //44
    //Workstation : short Len, short MaxLen, int Offset
    LE.setuint16(type3, off, workstation_len);  //Len
    off += 2;  //46
    LE.setuint16(type3, off, workstation_len);  //MaxLen (ignored)
    off += 2;  //48
    LE.setuint32(type3, off, workstation_off);  //Offset
    off += 4;  //52
    //SessionKeys : short Len, short MaxLen, int Offset
    LE.setuint16(type3, off, session_key_len);  //Len
    off += 2;  //54
    LE.setuint16(type3, off, session_key_len);  //MaxLen (ignored)
    off += 2;  //56
    LE.setuint32(type3, off, session_key_off);  //Offset
    off += 4;  //60
    LE.setuint32(type3, off, client_flags);  //int flags
    off += 4;  //64
    //MIC (16 bytes)
//    System.arraycopy(mic, 0, type3, off, mic.length);
//    off += mic.length;  //80
    //Payload
    System.arraycopy(lm_response, 0, type3, off, lm_response.length);
    off += lm_response.length;
    System.arraycopy(nt_response, 0, type3, off, nt_response.length);
    off += nt_response.length;
    System.arraycopy(domain, 0, type3, off, domain.length);
    off += domain.length;
    System.arraycopy(user, 0, type3, off, user.length);
    off += user.length;
    System.arraycopy(workstation, 0, type3, off, workstation.length);
    off += workstation.length;
    System.arraycopy(session_key, 0, type3, off, session_key.length);
    off += session_key.length;

    return type3;
  }
  public static String encodeSecond(String user, String pass, String type, String response) {
    switch (type) {
      case AUTH_LOGIN:
        return new String(Base64.encode(pass.getBytes()));
      case AUTH_NTLM:
        return new String(Base64.encode(AUTHENTICATE_MESSAGE(user, pass, response)));
    }
    return null;
  }

  private static char[] hex = { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };
  private static String toHex(byte[] b) {
    StringBuilder sb = new StringBuilder(b.length * 3);
    for (int i = 0; i < b.length; i++)
      sb.append(hex[(b[i]>>4)&0xF]).append(hex[b[i]&0xF]).append(' ');
    return sb.toString();
  }
}

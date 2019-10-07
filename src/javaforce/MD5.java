package javaforce;

/*
 RSA Data Security, Inc., MD5 message-digest algorithm
 Copyright c 1991-2, RSA Data Security, Inc. Created 1991. All
 rights reserved.

 License to copy and use this software is granted provided that it
 is identified as the "RSA Data Security, Inc. MD5 Message-Digest
 Algorithm" in all material mentioning or referencing this software
 or this function.

 License is also granted to make and use derivative works provided
 that such works are identified as "derived from the RSA Data
 Security, Inc. MD5 Message-Digest Algorithm" in all material
 mentioning or referencing the derived work.

 RSA Data Security, Inc. makes no representations concerning either
 the merchantability of this software or the suitability of this
 software for any particular purpose. It is provided "as is"
 without express or implied warranty of any kind.

 These notices must be retained in any copies of any part of this
 documentation and/or software.
 */
/**
 * In cryptography,
 * <code>MD5</code> (Message-Digest algorithm 5) is a widely used cryptographic
 * hash function with a 128-bit hash value.
 */
public class MD5 {

  public MD5() {
    init();
  }

  private static class Context {

    int state[] = new int[4];        /* state (ABCD) */

    long count;                      /* number of bits */

    byte buffer[] = new byte[64];    /* input buffer */

  };
  private Context context;

  /* Constants for MD5Transform routine. */
  private final int S11 = 7;
  private final int S12 = 12;
  private final int S13 = 17;
  private final int S14 = 22;
  private final int S21 = 5;
  private final int S22 = 9;
  private final int S23 = 14;
  private final int S24 = 20;
  private final int S31 = 4;
  private final int S32 = 11;
  private final int S33 = 16;
  private final int S34 = 23;
  private final int S41 = 6;
  private final int S42 = 10;
  private final int S43 = 15;
  private final int S44 = 21;
  private final byte[] PADDING = new byte[] {
    (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
  };

  /* F, G, H and I are basic MD5 functions. */
  private int F(int x, int y, int z) {
    return ((x & y) | ((~x) & z));
  }

  private int G(int x, int y, int z) {
    return ((x & z) | (y & (~z)));
  }

  private int H(int x, int y, int z) {
    return (x ^ y ^ z);
  }

  private int I(int x, int y, int z) {
    return (y ^ (x | (~z)));
  }

  /* ROTATE_LEFT rotates x left n bits. */
  private int ROTATE_LEFT(int x, int n) {
    return ((x << n) | (x >>> (32 - n)));
  }

  /* FF, GG, HH, and II transformations for rounds 1, 2, 3, and 4.
   Rotation is separate from addition to prevent recomputation. */
  private int FF(int a, int b, int c, int d, int x, int s, int ac) {
    a += F(b, c, d) + x + ac;
    a = ROTATE_LEFT(a, s);
    a += b;
    return a;
  }

  private int GG(int a, int b, int c, int d, int x, int s, int ac) {
    a += G(b, c, d) + x + ac;
    a = ROTATE_LEFT(a, s);
    a += b;
    return a;
  }

  private int HH(int a, int b, int c, int d, int x, int s, int ac) {
    a += H(b, c, d) + x + ac;
    a = ROTATE_LEFT(a, s);
    a += b;
    return a;
  }

  private int II(int a, int b, int c, int d, int x, int s, int ac) {
    a += I(b, c, d) + x + ac;
    a = ROTATE_LEFT(a, s);
    a += b;
    return a;
  }

  /**
   * MD5 initialization. Begins an MD5 operation.
   */
  public void init() {
    context = new Context();
    context.count = 0;
    /* Load magic initialization constants.*/
    context.state[0] = 0x67452301;
    context.state[1] = 0xefcdab89;
    context.state[2] = 0x98badcfe;
    context.state[3] = 0x10325476;
  }

  /**
   * MD5 block add operation. Continues an MD5 message-digest operation,
   * processing another message block, and updating the context.
   */
  public void add(byte[] input, int inputOffset, int inputLen) {
    int i, index, partLen;

    /* Compute number of bytes */
    index = (int) ((context.count >> 3) & 0x3f);

    /* add number of bits */
    context.count += ((long) inputLen) << 3;

    partLen = 64 - index;

    /* Transform as many times as possible. */
    if (inputLen >= partLen) {
      System.arraycopy(input, inputOffset, context.buffer, index, partLen);
      MD5Transform(context.state, context.buffer, 0);
      for (i = partLen; i + 63 < inputLen; i += 64) {
        MD5Transform(context.state, input, inputOffset + i);
      }
      index = 0;
    } else {
      i = 0;
    }

    /* Buffer remaining input */
    System.arraycopy(input, inputOffset + i, context.buffer, index, inputLen - i);
  }

  /**
   * Adds a string.
   */
  public void add(String str) {
    add(str.getBytes(), 0, str.length());
  }

  /**
   * MD5 finalization. Ends an MD5 message-digest operation, writing the the
   * message digest and zeroizing the context.
   */
  public byte[] done() {
    byte digest[] = new byte[16];
    byte bits[] = new byte[8];
    int index, padLen;

    /* Save number of bits */
    Encode(bits, context.count, 8);

    /* Pad out to 56 mod 64. */
    index = (int) ((context.count >> 3) & 0x3f);
    padLen = (index < 56) ? (56 - index) : (120 - index);
    add(PADDING, 0, padLen);

    /* Append length (before padding) */
    add(bits, 0, 8);

    /* Store state in digest */
    Encode(digest, context.state, 16);

    /* Zeroize sensitive information. */
    int z;
    for (z = 0; z < 4; z++) {
      context.state[z] = 0;
    }
    for (z = 0; z < 64; z++) {
      context.buffer[z] = 0;
    }
    context = null;

    return digest;
  }

  /**
   * Finalize MD5 and return as a string.
   */
  public String toString() {
    return new String(byte2char(done()));
  }

  /**
   * Converts binary output of done() to 64 chars
   */
  public char[] byte2char(byte[] b) {
    if (b.length != 16) {
      return null;
    }
    String ret = "", tmp;
    for (int a = 0; a < 16; a++) {
      tmp = Integer.toString(((int) b[a]) & 0xff, 16);
      if (tmp.length() == 1) {
        ret += "0";
      }
      ret += tmp;
    }
    return ret.toCharArray();
  }

  /* MD5 basic transformation. Transforms state based on block. */
  private void MD5Transform(int state[], byte block[], int blockOffset) {
    int a = state[0], b = state[1], c = state[2], d = state[3], x[] = new int[16];

    Decode(x, block, blockOffset, 64);

    /* Round 1 */
    a = FF(a, b, c, d, x[ 0], S11, 0xd76aa478); /* 1 */
    d = FF(d, a, b, c, x[ 1], S12, 0xe8c7b756); /* 2 */
    c = FF(c, d, a, b, x[ 2], S13, 0x242070db); /* 3 */
    b = FF(b, c, d, a, x[ 3], S14, 0xc1bdceee); /* 4 */
    a = FF(a, b, c, d, x[ 4], S11, 0xf57c0faf); /* 5 */
    d = FF(d, a, b, c, x[ 5], S12, 0x4787c62a); /* 6 */
    c = FF(c, d, a, b, x[ 6], S13, 0xa8304613); /* 7 */
    b = FF(b, c, d, a, x[ 7], S14, 0xfd469501); /* 8 */
    a = FF(a, b, c, d, x[ 8], S11, 0x698098d8); /* 9 */
    d = FF(d, a, b, c, x[ 9], S12, 0x8b44f7af); /* 10 */
    c = FF(c, d, a, b, x[10], S13, 0xffff5bb1); /* 11 */
    b = FF(b, c, d, a, x[11], S14, 0x895cd7be); /* 12 */
    a = FF(a, b, c, d, x[12], S11, 0x6b901122); /* 13 */
    d = FF(d, a, b, c, x[13], S12, 0xfd987193); /* 14 */
    c = FF(c, d, a, b, x[14], S13, 0xa679438e); /* 15 */
    b = FF(b, c, d, a, x[15], S14, 0x49b40821); /* 16 */

    /* Round 2 */
    a = GG(a, b, c, d, x[ 1], S21, 0xf61e2562); /* 17 */
    d = GG(d, a, b, c, x[ 6], S22, 0xc040b340); /* 18 */
    c = GG(c, d, a, b, x[11], S23, 0x265e5a51); /* 19 */
    b = GG(b, c, d, a, x[ 0], S24, 0xe9b6c7aa); /* 20 */
    a = GG(a, b, c, d, x[ 5], S21, 0xd62f105d); /* 21 */
    d = GG(d, a, b, c, x[10], S22, 0x2441453); /* 22 */
    c = GG(c, d, a, b, x[15], S23, 0xd8a1e681); /* 23 */
    b = GG(b, c, d, a, x[ 4], S24, 0xe7d3fbc8); /* 24 */
    a = GG(a, b, c, d, x[ 9], S21, 0x21e1cde6); /* 25 */
    d = GG(d, a, b, c, x[14], S22, 0xc33707d6); /* 26 */
    c = GG(c, d, a, b, x[ 3], S23, 0xf4d50d87); /* 27 */
    b = GG(b, c, d, a, x[ 8], S24, 0x455a14ed); /* 28 */
    a = GG(a, b, c, d, x[13], S21, 0xa9e3e905); /* 29 */
    d = GG(d, a, b, c, x[ 2], S22, 0xfcefa3f8); /* 30 */
    c = GG(c, d, a, b, x[ 7], S23, 0x676f02d9); /* 31 */
    b = GG(b, c, d, a, x[12], S24, 0x8d2a4c8a); /* 32 */

    /* Round 3 */
    a = HH(a, b, c, d, x[ 5], S31, 0xfffa3942); /* 33 */
    d = HH(d, a, b, c, x[ 8], S32, 0x8771f681); /* 34 */
    c = HH(c, d, a, b, x[11], S33, 0x6d9d6122); /* 35 */
    b = HH(b, c, d, a, x[14], S34, 0xfde5380c); /* 36 */
    a = HH(a, b, c, d, x[ 1], S31, 0xa4beea44); /* 37 */
    d = HH(d, a, b, c, x[ 4], S32, 0x4bdecfa9); /* 38 */
    c = HH(c, d, a, b, x[ 7], S33, 0xf6bb4b60); /* 39 */
    b = HH(b, c, d, a, x[10], S34, 0xbebfbc70); /* 40 */
    a = HH(a, b, c, d, x[13], S31, 0x289b7ec6); /* 41 */
    d = HH(d, a, b, c, x[ 0], S32, 0xeaa127fa); /* 42 */
    c = HH(c, d, a, b, x[ 3], S33, 0xd4ef3085); /* 43 */
    b = HH(b, c, d, a, x[ 6], S34, 0x4881d05); /* 44 */
    a = HH(a, b, c, d, x[ 9], S31, 0xd9d4d039); /* 45 */
    d = HH(d, a, b, c, x[12], S32, 0xe6db99e5); /* 46 */
    c = HH(c, d, a, b, x[15], S33, 0x1fa27cf8); /* 47 */
    b = HH(b, c, d, a, x[ 2], S34, 0xc4ac5665); /* 48 */

    /* Round 4 */
    a = II(a, b, c, d, x[ 0], S41, 0xf4292244); /* 49 */
    d = II(d, a, b, c, x[ 7], S42, 0x432aff97); /* 50 */
    c = II(c, d, a, b, x[14], S43, 0xab9423a7); /* 51 */
    b = II(b, c, d, a, x[ 5], S44, 0xfc93a039); /* 52 */
    a = II(a, b, c, d, x[12], S41, 0x655b59c3); /* 53 */
    d = II(d, a, b, c, x[ 3], S42, 0x8f0ccc92); /* 54 */
    c = II(c, d, a, b, x[10], S43, 0xffeff47d); /* 55 */
    b = II(b, c, d, a, x[ 1], S44, 0x85845dd1); /* 56 */
    a = II(a, b, c, d, x[ 8], S41, 0x6fa87e4f); /* 57 */
    d = II(d, a, b, c, x[15], S42, 0xfe2ce6e0); /* 58 */
    c = II(c, d, a, b, x[ 6], S43, 0xa3014314); /* 59 */
    b = II(b, c, d, a, x[13], S44, 0x4e0811a1); /* 60 */
    a = II(a, b, c, d, x[ 4], S41, 0xf7537e82); /* 61 */
    d = II(d, a, b, c, x[11], S42, 0xbd3af235); /* 62 */
    c = II(c, d, a, b, x[ 2], S43, 0x2ad7d2bb); /* 63 */
    b = II(b, c, d, a, x[ 9], S44, 0xeb86d391); /* 64 */

    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;

    /* Zeroize sensitive information.*/
    for (int i = 0; i < 16; i++) {
      x[i] = 0;
    }
  }

  /* Encodes input (int) into output (byte). Assumes len is a multiple of 4. */
  private void Encode(byte[] output, int[] input, int len) {
    int i, j;
    for (i = 0, j = 0; j < len; i++, j += 4) {
      output[j] = (byte) (input[i] & 0xff);
      output[j + 1] = (byte) ((input[i] >> 8) & 0xff);
      output[j + 2] = (byte) ((input[i] >> 16) & 0xff);
      output[j + 3] = (byte) ((input[i] >> 24) & 0xff);
    }
  }

  /* Encodes input (long) into output (byte). Assumes len is a multiple of 4. */
  private void Encode(byte[] output, long input, int len) {
    int i, j;
    for (i = 0, j = 0; j < len; i++, j += 4) {
      output[j] = (byte) ((input >> (0 + i * 32)) & 0xff);
      output[j + 1] = (byte) ((input >> (8 + i * 32)) & 0xff);
      output[j + 2] = (byte) ((input >> (16 + i * 32)) & 0xff);
      output[j + 3] = (byte) ((input >> (24 + i * 32)) & 0xff);
    }
  }

  /* Decodes input (byte) into output (int). Assumes len is a multiple of 4. */
  private void Decode(int[] output, byte[] input, int inputOffset, int len) {
    int i, j;
    for (i = 0, j = 0; j < len; i++, j += 4) {
      output[i] =
              (((int) input[inputOffset + j]) & 0xff)
              | ((((int) input[inputOffset + j + 1]) & 0xff) << 8)
              | ((((int) input[inputOffset + j + 2]) & 0xff) << 16)
              | ((((int) input[inputOffset + j + 3]) & 0xff) << 24);
    }
  }
}

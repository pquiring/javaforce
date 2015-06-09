/* G729aCode - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class G729aCode implements Constants
{
    private byte[] data;
    
    final short getL0() {
	return (short) ((data[0] & 0x80) >>> 7);
    }
    
    final void setL0(short i) {
	data[0] &= 0x7f;
	data[0] |= i << 7;
    }
    
    final short getL1() {
	return (short) (data[0] & 0x7f);
    }
    
    final void setL1(short i) {
	data[0] &= 0x80;
	data[0] |= i;
    }
    
    final short getL2() {
	return (short) ((data[1] & 0xf8) >>> 3);
    }
    
    final void setL2(short i) {
	data[1] &= 0x7;
	data[1] |= i << 3;
    }
    
    final short getL3() {
	short i = 0;
	i |= (short) ((data[1] & 0x7) << 2);
	i |= (short) ((data[2] & 0xc0) >>> 6);
	return i;
    }
    
    final void setL3(short i) {
	data[1] &= 0xf8;
	data[1] |= i >>> 2;
	data[2] &= 0x3f;
	data[2] |= (i & 0x3) << 6;
    }
    
    final short getP1() {
	return (short) ((data[2] & 0x3f) << 2 | (data[3] & 0xc0) >>> 6);
    }
    
    final void setP1(short i) {
	data[2] &= 0xc0;
	data[2] |= i >>> 2;
	data[3] &= 0x3f;
	data[3] |= i << 6;
    }
    
    final short getP0() {
	return (short) ((data[3] & 0x20) >>> 5);
    }
    
    final void setP0(short i) {
	data[3] &= 0xdf;
	data[3] |= i << 5;
    }
    
    final short getC1() {
	short i = 0;
	i |= (short) ((data[3] & 0x1f) << 8);
	i |= (short) (data[4] & 0xff);
	return i;
    }
    
    final void setC1(short i) {
	data[3] &= 0xe0;
	data[3] |= i >>> 8;
	data[4] = (byte) 0;
	data[4] |= i & 0xff;
    }
    
    final short getS1() {
	return (short) ((data[5] & 0xf0) >>> 4);
    }
    
    final void setS1(short i) {
	data[5] &= 0xf;
	data[5] |= i << 4;
    }
    
    final short getGA1() {
	return (short) ((data[5] & 0xe) >>> 1);
    }
    
    final void setGA1(short i) {
	data[5] &= 0xf1;
	data[5] |= i << 1;
    }
    
    final short getGB1() {
	short i = 0;
	i |= (short) ((data[5] & 0x1) << 3);
	i |= (short) ((data[6] & 0xe0) >>> 5);
	return i;
    }
    
    final void setGB1(short i) {
	data[5] &= 0xfe;
	data[5] |= i >>> 3;
	data[6] &= 0x1f;
	data[6] |= (i & 0x7) << 5;
    }
    
    final short getP2() {
	return (short) (data[6] & 0x1f);
    }
    
    final void setP2(short i) {
	data[6] &= 0xe0;
	data[6] |= i & 0x1f;
    }
    
    final short getC2() {
	short i = 0;
	i |= (short) ((data[7] & 0xff) << 5);
	i |= (short) ((data[8] & 0xf8) >>> 3);
	return i;
    }
    
    final void setC2(short i) {
	data[7] &= 0x0;
	data[7] |= i >>> 5;
	data[8] &= 0x7;
	data[8] |= (i & 0x1f) << 3;
    }
    
    final short getS2() {
	return (short) ((data[8] & 0x7) << 1 | (data[9] & 0x80) >>> 7);
    }
    
    final void setS2(short i) {
	data[8] &= 0xf8;
	data[8] |= i >> 1;
	data[9] &= 0x7f;
	data[9] |= i << 7;
    }
    
    final short getGA2() {
	return (short) ((data[9] & 0x70) >>> 4);
    }
    
    final void setGA2(short i) {
	data[9] &= 0x8f;
	data[9] |= i << 4;
    }
    
    final short getGB2() {
	return (short) (data[9] & 0xf);
    }
    
    final void setGB2(short i) {
	data[9] &= 0xf0;
	data[9] |= i;
    }
    
    G729aCode() {
	data = new byte[10];
    }
    
    G729aCode(byte[] is, int i) {
	data = new byte[10];
	System.arraycopy(is, i, data, 0, 10);
    }
    
    byte[] getData() {
	return data;
    }
    
    final void setData(byte[] is, int i) {
	System.arraycopy(is, i, data, 0, 10);
    }
    
    public final String toString() {
	StringBuffer stringbuffer = new StringBuffer();
	stringbuffer.append("L0=");
	stringbuffer.append((short) ((data[0] & 0x80) >>> 7));
	stringbuffer.append("\n");
	stringbuffer.append("L1=");
	stringbuffer.append((short) (data[0] & 0x7f));
	stringbuffer.append("\n");
	stringbuffer.append("L2=");
	stringbuffer.append((short) ((data[1] & 0xf8) >>> 3));
	stringbuffer.append("\n");
	stringbuffer.append("L3=");
	stringbuffer.append(getL3());
	stringbuffer.append("\n");
	stringbuffer.append("P1=");
	stringbuffer.append((short) ((data[2] & 0x3f) << 2
				     | (data[3] & 0xc0) >>> 6));
	stringbuffer.append("\n");
	stringbuffer.append("P0=");
	stringbuffer.append((short) ((data[3] & 0x20) >>> 5));
	stringbuffer.append("\n");
	stringbuffer.append("C1=");
	stringbuffer.append(getC1());
	stringbuffer.append("\n");
	stringbuffer.append("S1=");
	stringbuffer.append((short) ((data[5] & 0xf0) >>> 4));
	stringbuffer.append("\n");
	stringbuffer.append("GA1=");
	stringbuffer.append((short) ((data[5] & 0xe) >>> 1));
	stringbuffer.append("\n");
	stringbuffer.append("GB1=");
	stringbuffer.append(getGB1());
	stringbuffer.append("\n");
	stringbuffer.append("P2=");
	stringbuffer.append((short) (data[6] & 0x1f));
	stringbuffer.append("\n");
	stringbuffer.append("C2=");
	stringbuffer.append(getC2());
	stringbuffer.append("\n");
	stringbuffer.append("S2=");
	stringbuffer.append((short) ((data[8] & 0x7) << 1
				     | (data[9] & 0x80) >>> 7));
	stringbuffer.append("\n");
	stringbuffer.append("GA2=");
	stringbuffer.append((short) ((data[9] & 0x70) >>> 4));
	stringbuffer.append("\n");
	stringbuffer.append("GB2=");
	stringbuffer.append((short) (data[9] & 0xf));
	stringbuffer.append("\n");
	return stringbuffer.toString();
    }
}
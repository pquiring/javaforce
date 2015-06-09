/* g729a_decode_internal - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_decode_internal implements g729a_constants {

  float[] v = new float[40];
  float[] c = new float[40];
  float[] s = new float[40];
  float[] sf = new float[40];
  float[] a_quant_subframe1 = new float[10];
  float[] a_quant_subframe2 = new float[10];
  int T1;
  float gp_quan;
  float gc_quan;
  int C;
  int S;
  int GA;
  int GB;

  void print() {
    for (int i = 0; i < 40; i++) {
      System.err.println("inter.v[" + i + "]=" + g729a_utils.HF(v[i]));
    }
    for (int i = 0; i < 40; i++) {
      System.err.println("inter.c[" + i + "]=" + g729a_utils.HF(c[i]));
    }
    for (int i = 0; i < 40; i++) {
      System.err.println("inter.s[" + i + "]=" + g729a_utils.HF(s[i]));
    }
    for (int i = 0; i < 40; i++) {
      System.err.println("inter.sf[" + i + "]=" + g729a_utils.HF(sf[i]));
    }
    for (int i = 0; i < 10; i++) {
      System.err.println("inter.a_quant_subframe1[" + i + "]="
              + g729a_utils.HF(a_quant_subframe1[i]));
    }
    for (int i = 0; i < 10; i++) {
      System.err.println("inter.a_quant_subframe2[" + i + "]="
              + g729a_utils.HF(a_quant_subframe2[i]));
    }
    System.err.println("inter.T1=" + T1);
    System.err.println("inter.gp_quan=" + g729a_utils.HF(gp_quan));
    System.err.println("inter.gc_quan=" + g729a_utils.HF(gc_quan));
    System.err.println("inter.C=" + C);
    System.err.println("inter.S=" + S);
    System.err.println("inter.GA=" + GA);
    System.err.println("inter.GB=" + GB);
  }
}
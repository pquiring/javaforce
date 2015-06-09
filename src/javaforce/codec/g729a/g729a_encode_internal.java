/* g729a_encode_internal - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_encode_internal implements g729a_constants {

  float[] x = new float[40];
  float[] y = new float[40];
  float[] z = new float[40];
  float[] v;
  float[] c = new float[40];
  float[] h = new float[40];
  float[] r;
  float[] a_quant_subframe1 = new float[10];
  float[] a_quant_subframe2 = new float[10];
  float[] a_gamma = new float[10];
  int T_opt;
  int T1;
  int frac_T1;
  int T2;
  int frac_T2;
  float gp;
  float gp_quan;
  float gc_quan;
  short C;
  short S;
  short GA;
  short GB;
  int taming;
  float xy;
  float yy;
}
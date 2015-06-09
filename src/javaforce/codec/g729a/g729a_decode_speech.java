/* g729a_decode_speech - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_decode_speech implements g729a_constants {

  float[] excitation;
  float[] residual;
  float[] U = new float[4];
  float past_quan_gp;
  float past_quan_gc;
  float past_g;
  int seed;
  int periodic;
  int T2;

  void print() {
    for (int i = 0; i < 194; i++) {
      System.err.println("speech.excitation[" + i + "]="
              + g729a_utils.HF(excitation[i]));
    }
    for (int i = 0; i < 184; i++) {
      System.err.println("speech.residual[" + i + "]="
              + g729a_utils.HF(residual[i]));
    }
    for (int i = 0; i < 4; i++) {
      System.err.println("speech.U[" + i + "]=" + g729a_utils.HF(U[i]));
    }
    System.err
            .println("speech.past_quan_gp=" + g729a_utils.HF(past_quan_gp));
    System.err
            .println("speech.past_quan_gc=" + g729a_utils.HF(past_quan_gc));
    System.err.println("speech.past_g=" + g729a_utils.HF(past_g));
    System.err.println("speech.seed=" + seed);
    System.err.println("speech.periodic=" + periodic);
    System.err.println("speech.T2=" + T2);
  }
}
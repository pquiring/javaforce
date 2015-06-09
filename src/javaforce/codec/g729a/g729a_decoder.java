/* g729a_decoder - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_decoder implements g729a_constants {

  private g729a_preproc_filter postproc;
  private g729a_sinthesis_filter sinthesis;
  private g729a_sinthesis_filter residual_filter;
  private g729a_sinthesis_filter shortterm;
  private g729a_tilt_filter tilt;
  private g729a_decode_filters filters;
  private g729a_decode_speech speech;
  private g729a_decode_lp lp;
  private float[] residual = new float[184];
  private float[] excitation = new float[194];
  private float[] q_quant_past_subframe_2 = new float[10];
  private float[] q_quant_present_subframe_2 = new float[10];
  private float[] l_quant_past_frame_4 = new float[10];
  private float[] l_quant_past_frame_3 = new float[10];
  private float[] l_quant_past_frame_2 = new float[10];
  private float[] l_quant_past_frame_1 = new float[10];

  final void decode(short[] is, G729aCode g729acode, boolean bool,
          boolean bool_0_) {
    g729a_decode_internal var_g729a_decode_internal = new g729a_decode_internal();
    g729a_utils.validateData(is, 0);
    float[] fs = speech.U;
    if (!bool) {
      speech.periodic = 0;
      if (!bool_0_) {
        g729a_reconstruct.g729a_lspdec(lp, g729acode);
      } else {
        g729a_reconstruct.g729a_lspdec_erased(lp);
      }
      g729a_common.g729a_lsp_interpolation(lp.q_quant_present_subframe_1, lp.q_quant_past_subframe_2,
              lp.q_quant_present_subframe_2);
      g729a_common.g729a_LSP2LP((var_g729a_decode_internal.a_quant_subframe1),
              lp.q_quant_present_subframe_1);
      g729a_common.g729a_LSP2LP((var_g729a_decode_internal.a_quant_subframe2),
              lp.q_quant_present_subframe_2);
      float[] fs_1_ = lp.q_quant_past_subframe_2;
      lp.q_quant_past_subframe_2 = lp.q_quant_present_subframe_2;
      lp.q_quant_present_subframe_2 = fs_1_;
      var_g729a_decode_internal.C = g729acode.getC1();
      var_g729a_decode_internal.S = g729acode.getS1();
      var_g729a_decode_internal.GA = g729acode.getGA1();
      var_g729a_decode_internal.GB = g729acode.getGB1();
      g729a_reconstruct.g729a_dec_lag3_1(g729acode,
              var_g729a_decode_internal,
              speech);
      g729a_reconstruct.g729a_de_acelp(var_g729a_decode_internal,
              var_g729a_decode_internal.T1,
              speech.past_quan_gp);
      g729a_reconstruct.g729a_dec_gain(var_g729a_decode_internal,
              speech);
      g729a_reconstruct.g729a_reconstruct_speech(var_g729a_decode_internal, speech, filters,
              var_g729a_decode_internal.a_quant_subframe1);
      g729a_pst.g729a_pst(speech, var_g729a_decode_internal, filters,
              var_g729a_decode_internal.a_quant_subframe1,
              var_g729a_decode_internal.T1);
      g729a_post_pro.g729a_postproc_filter(is, 0,
              var_g729a_decode_internal.sf,
              filters);
      var_g729a_decode_internal.C = g729acode.getC2();
      var_g729a_decode_internal.S = g729acode.getS2();
      var_g729a_decode_internal.GA = g729acode.getGA2();
      var_g729a_decode_internal.GB = g729acode.getGB2();
      g729a_reconstruct.g729a_dec_lag3_2(g729acode,
              var_g729a_decode_internal,
              speech);
      g729a_reconstruct.g729a_de_acelp(var_g729a_decode_internal,
              speech.T2, speech.past_quan_gp);
      g729a_reconstruct.g729a_dec_gain(var_g729a_decode_internal,
              speech);
      g729a_reconstruct.g729a_reconstruct_speech(var_g729a_decode_internal, speech, filters,
              var_g729a_decode_internal.a_quant_subframe2);
      g729a_pst.g729a_pst(speech, var_g729a_decode_internal, filters,
              var_g729a_decode_internal.a_quant_subframe2,
              speech.T2);
      g729a_post_pro.g729a_postproc_filter(is, 40,
              var_g729a_decode_internal.sf,
              filters);
    } else {
      g729a_reconstruct.g729a_lspdec_erased(lp);
      g729a_common.g729a_lsp_interpolation(lp.q_quant_present_subframe_1, lp.q_quant_past_subframe_2,
              lp.q_quant_present_subframe_2);
      g729a_common.g729a_LSP2LP((var_g729a_decode_internal.a_quant_subframe1),
              lp.q_quant_present_subframe_1);
      g729a_common.g729a_LSP2LP((var_g729a_decode_internal.a_quant_subframe2),
              lp.q_quant_present_subframe_2);
      float[] fs_2_ = lp.q_quant_past_subframe_2;
      lp.q_quant_past_subframe_2 = lp.q_quant_present_subframe_2;
      lp.q_quant_present_subframe_2 = fs_2_;
      int i;
      speech.seed = i = speech.seed * 31821 + 13849;
      var_g729a_decode_internal.C = i & 0x1fff;
      speech.seed = i = speech.seed * 31821 + 13849;
      var_g729a_decode_internal.S = i & 0xf;
      if (speech.past_quan_gp > 0.9F) {
        var_g729a_decode_internal.gp_quan = 0.9F;
      } else {
        var_g729a_decode_internal.gp_quan = 0.9F * speech.past_quan_gp;
      }
      speech.past_quan_gp = var_g729a_decode_internal.gp_quan;
      speech.past_quan_gc = var_g729a_decode_internal.gc_quan = 0.98F * speech.past_quan_gc;
      float f = 0.25F * (fs[0] + fs[1] + fs[2] + fs[3]) - 4.0F;
      if (f < -14.0F) {
        f = -14.0F;
      }
      fs[3] = fs[2];
      fs[2] = fs[1];
      fs[1] = fs[0];
      fs[0] = f;
      if (speech.periodic == 1) {
        g729a_common.g729a_generation_adaptive_vector(var_g729a_decode_internal.v, speech.excitation, speech.T2,
                0);
        g729a_utils.g729a_set_0(var_g729a_decode_internal.c, 40);
      } else {
        g729a_utils.g729a_set_0(var_g729a_decode_internal.v, 40);
        g729a_reconstruct.g729a_de_acelp(var_g729a_decode_internal,
                speech.T2,
                speech.past_quan_gp);
      }
      g729a_reconstruct.g729a_reconstruct_speech(var_g729a_decode_internal, speech, filters,
              var_g729a_decode_internal.a_quant_subframe1);
      g729a_pst.g729a_pst(speech, var_g729a_decode_internal, filters,
              var_g729a_decode_internal.a_quant_subframe1,
              speech.T2);
      g729a_post_pro.g729a_postproc_filter(is, 0,
              var_g729a_decode_internal.sf,
              filters);
      if (speech.T2 < 143) {
        speech.T2++;
      }
      speech.seed = i = speech.seed * 31821 + 13849;
      var_g729a_decode_internal.C = i & 0x1fff;
      speech.seed = i = speech.seed * 31821 + 13849;
      var_g729a_decode_internal.S = i & 0xf;
      if (speech.past_quan_gp > 0.9F) {
        var_g729a_decode_internal.gp_quan = 0.9F;
      } else {
        var_g729a_decode_internal.gp_quan = 0.9F * speech.past_quan_gp;
      }
      speech.past_quan_gp = var_g729a_decode_internal.gp_quan;
      speech.past_quan_gc = var_g729a_decode_internal.gc_quan = 0.98F * speech.past_quan_gc;
      f = 0.25F * (fs[0] + fs[1] + fs[2] + fs[3]) - 4.0F;
      if (f < -14.0F) {
        f = -14.0F;
      }
      fs[3] = fs[2];
      fs[2] = fs[1];
      fs[1] = fs[0];
      fs[0] = f;
      if (speech.periodic == 1) {
        g729a_common.g729a_generation_adaptive_vector(var_g729a_decode_internal.v, speech.excitation, speech.T2,
                0);
        g729a_utils.g729a_set_0(var_g729a_decode_internal.c, 40);
      } else {
        g729a_utils.g729a_set_0(var_g729a_decode_internal.v, 40);
        g729a_reconstruct.g729a_de_acelp(var_g729a_decode_internal,
                speech.T2,
                speech.past_quan_gp);
      }
      g729a_reconstruct.g729a_reconstruct_speech(var_g729a_decode_internal, speech, filters,
              var_g729a_decode_internal.a_quant_subframe2);
      g729a_pst.g729a_pst(speech, var_g729a_decode_internal, filters,
              var_g729a_decode_internal.a_quant_subframe2,
              speech.T2);
      g729a_post_pro.g729a_postproc_filter(is, 40,
              var_g729a_decode_internal.sf,
              filters);
      if (speech.T2 < 143) {
        speech.T2++;
      }
    }
  }

  void decode(short[] is, int i, byte[] is_3_, int i_4_, int i_5_) {
    int i_6_ = i;
    int i_7_ = i_4_;
    for (int i_8_ = 0; i_8_ < i_5_; i_8_++) {
      decode(is, i_6_, is_3_, i_7_, false, false);
      i_6_ += 80;
      i_7_ += 10;
    }
  }

  void decode(short[] is, int i, G729aCode[] g729acodes, int i_9_,
          boolean bool, boolean bool_10_) {
    if (g729acodes.length < i_9_) {
      throw new G729aException(1);
    }
    int i_11_ = i;
    for (int i_12_ = 0; i_12_ < i_9_; i_12_++) {
      decode(is, i_11_, g729acodes[i_12_], bool, bool_10_);
      i_11_ += 80;
    }
  }

  void decode(short[] is, int i, byte[] is_13_, int i_14_, boolean bool,
          boolean bool_15_) {
    g729a_utils.validateCode(is_13_, i_14_);
    G729aCode g729acode = new G729aCode(is_13_, i_14_);
    decode(is, i, g729acode, bool, bool_15_);
  }

  void decode(short[] is, int i, G729aCode g729acode, boolean bool,
          boolean bool_16_) {
    g729a_utils.validateData(is, i);
    short[] is_17_ = new short[80];
    decode(is_17_, g729acode, bool, bool_16_);
    System.arraycopy(is_17_, 0, is, i, 80);
  }

  g729a_decoder() {
    postproc = new g729a_preproc_filter();
    sinthesis = new g729a_sinthesis_filter();
    residual_filter = new g729a_sinthesis_filter();
    shortterm = new g729a_sinthesis_filter();
    tilt = new g729a_tilt_filter();
    g729a_preproc_filter var_g729a_preproc_filter = postproc;
    g729a_sinthesis_filter var_g729a_sinthesis_filter = sinthesis;
    g729a_sinthesis_filter var_g729a_sinthesis_filter_18_ = residual_filter;
    g729a_sinthesis_filter var_g729a_sinthesis_filter_19_ = shortterm;
    g729a_tilt_filter var_g729a_tilt_filter = tilt;
    var_g729a_preproc_filter.a0 = 123182.23F;
    var_g729a_preproc_filter.a1 = -246360.75F;
    var_g729a_preproc_filter.a2 = 123182.23F;
    var_g729a_preproc_filter.b1 = 1.9330735F;
    var_g729a_preproc_filter.b2 = -0.93579197F;
    var_g729a_preproc_filter.x1 = 0.0F;
    var_g729a_preproc_filter.x2 = 0.0F;
    var_g729a_preproc_filter.y1 = 0.0F;
    var_g729a_preproc_filter.y2 = 0.0F;
    var_g729a_tilt_filter.x1 = 0.0F;
    speech = new g729a_decode_speech();
    speech.U[0] = speech.U[1] = speech.U[2] = speech.U[3] = -14.0F;
    speech.past_quan_gp = 0.2000122F;
    speech.past_g = 0.0F;
    speech.seed = 21845;
    speech.T2 = 60;
    float[] fs = q_quant_past_subframe_2;
    float f = 0.28559935F;
    for (int i = 0; i < 10; i++) {
      fs[i] = (float) Math.cos((double) (f * (float) (i + 1)));
    }
    lp = new g729a_decode_lp();
    for (int i = 0; i < 10; i++) {
      lp.past_omega[i] = f * (float) (i + 1);
    }
    float[] fs_20_ = l_quant_past_frame_4;
    float[] fs_21_ = l_quant_past_frame_3;
    float[] fs_22_ = l_quant_past_frame_2;
    float[] fs_23_ = l_quant_past_frame_1;
    for (int i = 0; i < 10; i++) {
      fs_20_[i] = fs_21_[i] = fs_22_[i] = fs_23_[i] = (float) (i + 1) * f;
    }
    for (int i = 0; i < 2; i++) {
      fs_23_ = lp.fg_l_quant_sum;
      int i_24_ = 10 * i;
      fs = g729a_tables.g729a_fg_sum;
      int i_25_ = 10 * i;
      for (int i_26_ = 0; i_26_ < 10; i_26_++) {
        fs_23_[i_26_ + i_24_] = (1.0F - fs[i_26_ + i_25_]) * f * (float) (i_26_ + 1);
      }
    }
    filters = new g729a_decode_filters();
    filters.postproc = var_g729a_preproc_filter;
    filters.sinthesis = var_g729a_sinthesis_filter;
    filters.residual = var_g729a_sinthesis_filter_18_;
    filters.shortterm = var_g729a_sinthesis_filter_19_;
    filters.tilt = var_g729a_tilt_filter;
    speech.residual = residual;
    speech.excitation = excitation;
    lp.q_quant_past_subframe_2 = q_quant_past_subframe_2;
    lp.q_quant_present_subframe_2 = q_quant_present_subframe_2;
    lp.l_quant_past_frame_4 = l_quant_past_frame_4;
    lp.l_quant_past_frame_3 = l_quant_past_frame_3;
    lp.l_quant_past_frame_2 = l_quant_past_frame_2;
    lp.l_quant_past_frame_1 = l_quant_past_frame_1;
  }

  void reset() {
    g729a_preproc_filter var_g729a_preproc_filter = postproc;
    g729a_sinthesis_filter var_g729a_sinthesis_filter = sinthesis;
    g729a_sinthesis_filter var_g729a_sinthesis_filter_27_ = residual_filter;
    g729a_sinthesis_filter var_g729a_sinthesis_filter_28_ = shortterm;
    g729a_tilt_filter var_g729a_tilt_filter = tilt;
    var_g729a_preproc_filter.a0 = 123182.23F;
    var_g729a_preproc_filter.a1 = -246360.75F;
    var_g729a_preproc_filter.a2 = 123182.23F;
    var_g729a_preproc_filter.b1 = 1.9330735F;
    var_g729a_preproc_filter.b2 = -0.93579197F;
    var_g729a_preproc_filter.x1 = 0.0F;
    var_g729a_preproc_filter.x2 = 0.0F;
    var_g729a_preproc_filter.y1 = 0.0F;
    var_g729a_preproc_filter.y2 = 0.0F;
    g729a_utils.g729a_set_0(var_g729a_sinthesis_filter.delay, 10);
    g729a_utils.g729a_set_0(var_g729a_sinthesis_filter_27_.delay, 10);
    g729a_utils.g729a_set_0(var_g729a_sinthesis_filter_28_.delay, 10);
    var_g729a_tilt_filter.x1 = 0.0F;
    g729a_utils.g729a_set_0(residual, 184);
    g729a_utils.g729a_set_0(excitation, 194);
    speech.U[0] = speech.U[1] = speech.U[2] = speech.U[3] = -14.0F;
    speech.past_quan_gp = 0.2000122F;
    speech.past_g = 0.0F;
    speech.seed = 21845;
    speech.T2 = 60;
    float[] fs = q_quant_past_subframe_2;
    float f = 0.28559935F;
    for (int i = 0; i < 10; i++) {
      fs[i] = (float) Math.cos((double) (f * (float) (i + 1)));
    }
    for (int i = 0; i < 10; i++) {
      lp.past_omega[i] = f * (float) (i + 1);
    }
    float[] fs_29_ = l_quant_past_frame_4;
    float[] fs_30_ = l_quant_past_frame_3;
    float[] fs_31_ = l_quant_past_frame_2;
    float[] fs_32_ = l_quant_past_frame_1;
    for (int i = 0; i < 10; i++) {
      fs_29_[i] = fs_30_[i] = fs_31_[i] = fs_32_[i] = (float) (i + 1) * f;
    }
    for (int i = 0; i < 2; i++) {
      fs_32_ = lp.fg_l_quant_sum;
      int i_33_ = 10 * i;
      fs = g729a_tables.g729a_fg_sum;
      int i_34_ = 10 * i;
      for (int i_35_ = 0; i_35_ < 10; i_35_++) {
        fs_32_[i_35_ + i_33_] = (1.0F - fs[i_35_ + i_34_]) * f * (float) (i_35_ + 1);
      }
    }
    filters.postproc = var_g729a_preproc_filter;
    filters.sinthesis = var_g729a_sinthesis_filter;
    filters.residual = var_g729a_sinthesis_filter_27_;
    filters.shortterm = var_g729a_sinthesis_filter_28_;
    filters.tilt = var_g729a_tilt_filter;
    speech.residual = residual;
    speech.excitation = excitation;
    lp.q_quant_past_subframe_2 = q_quant_past_subframe_2;
    lp.q_quant_present_subframe_2 = q_quant_present_subframe_2;
    lp.l_quant_past_frame_4 = l_quant_past_frame_4;
    lp.l_quant_past_frame_3 = l_quant_past_frame_3;
    lp.l_quant_past_frame_2 = l_quant_past_frame_2;
    lp.l_quant_past_frame_1 = l_quant_past_frame_1;
  }
}
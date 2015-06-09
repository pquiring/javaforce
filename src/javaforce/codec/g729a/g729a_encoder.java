/* g729a_encoder - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package javaforce.codec.g729a;

final class g729a_encoder implements g729a_constants {

  g729a_preproc_filter preproc;
  g729a_sinthesis_filter sinthesis;
  g729a_sinthesis_filter weghted_sinthesis;
  g729a_sinthesis_filter weghted;
  g729a_encode_filters filters;
  g729a_encode_speech speech;
  g729a_encode_lp lp;
  float[] past_subframe_3 = new float[40];
  float[] past_subframe_2 = new float[40];
  float[] past_subframe_1 = new float[40];
  float[] present_subframe_1 = new float[40];
  float[] present_subframe_2 = new float[40];
  float[] future_subframe = new float[40];
  float[] weighted_speech = new float[224];
  float[] excitation = new float[194];
  float[] q_quant_past_subframe_2 = new float[10];
  float[] q_quant_present_subframe_2 = new float[10];
  float[] l_quant_past_frame_4 = new float[10];
  float[] l_quant_past_frame_3 = new float[10];
  float[] l_quant_past_frame_2 = new float[10];
  float[] l_quant_past_frame_1 = new float[10];
  float[] exc_err = new float[4];

  void encode(G729aCode g729acode, short[] is) {
    g729a_encode_internal var_g729a_encode_internal = new g729a_encode_internal();
    float[] fs = new float[40];
    float[][] fs_0_ = new float[3][40];
    float[][] fs_1_ = new float[3][];
    g729a_utils.validateData(is, 0);
    var_g729a_encode_internal.r = fs;
    fs_1_[0] = fs_0_[0];
    fs_1_[1] = fs_0_[1];
    fs_1_[2] = fs_0_[2];
    g729a_pre_proc.g729a_preproc_filter(speech.present_subframe_2, is, 0,
            filters);
    g729a_pre_proc.g729a_preproc_filter(speech.future_subframe, is, 40,
            filters);
    g729a_lpc.g729a_lp_analysis(lp.q, speech);
    g729a_qua_lsp.g729a_qua_lsp_encode(lp, g729acode);
    g729a_common.g729a_lsp_interpolation(lp.q_quant_present_subframe_1,
            lp.q_quant_past_subframe_2,
            lp.q_quant_present_subframe_2);
    g729a_common.g729a_LSP2LP(var_g729a_encode_internal.a_quant_subframe1,
            lp.q_quant_present_subframe_1);
    g729a_common.g729a_LSP2LP(var_g729a_encode_internal.a_quant_subframe2,
            lp.q_quant_present_subframe_2);
    float[] fs_2_ = lp.q_quant_past_subframe_2;
    lp.q_quant_past_subframe_2 = lp.q_quant_present_subframe_2;
    lp.q_quant_present_subframe_2 = fs_2_;
    g729a_pwf.g729a_pwf(speech.weighted_speech, 0, speech.excitation, 154,
            speech.present_subframe_1,
            var_g729a_encode_internal.a_quant_subframe1,
            filters);
    g729a_pwf.g729a_pwf(speech.weighted_speech, 40,
            var_g729a_encode_internal.r, 0,
            speech.present_subframe_2,
            var_g729a_encode_internal.a_quant_subframe2,
            filters);
    var_g729a_encode_internal.T_opt = g729a_pitch.g729a_opla(speech.weighted_speech);
    g729a_pitch.g729a_impulse_response(var_g729a_encode_internal,
            (var_g729a_encode_internal.a_quant_subframe1));
    g729a_pitch.g729a_target(var_g729a_encode_internal, speech, filters);
    g729a_pitch.g729a_ad_cod_search_1(g729acode, var_g729a_encode_internal,
            speech, fs_1_);
    g729a_pitch.g729a_adcb_gain(var_g729a_encode_internal);
    var_g729a_encode_internal.taming = g729a_pitch.g729a_test_err(exc_err, var_g729a_encode_internal.T1,
            var_g729a_encode_internal.frac_T1);
    if (var_g729a_encode_internal.taming == 1
            && var_g729a_encode_internal.gp > 0.9499512F) {
      var_g729a_encode_internal.gp = 0.9499512F;
    }
    g729a_acelp_co.g729a_fc_search(var_g729a_encode_internal,
            var_g729a_encode_internal.T1,
            speech.past_quan_gp);
    g729a_qua_gain.g729a_qua_gain(var_g729a_encode_internal, speech);
    g729acode.setC1(var_g729a_encode_internal.C);
    g729acode.setS1(var_g729a_encode_internal.S);
    g729acode.setGA1(var_g729a_encode_internal.GA);
    g729acode.setGB1(var_g729a_encode_internal.GB);
    g729a_qua_gain.g729a_memory_update_1(speech, var_g729a_encode_internal,
            filters);
    g729a_pitch.g729a_update_exc_err(exc_err,
            var_g729a_encode_internal.gp_quan,
            var_g729a_encode_internal.T1);
    g729a_pitch.g729a_impulse_response(var_g729a_encode_internal,
            (var_g729a_encode_internal.a_quant_subframe2));
    g729a_pitch.g729a_target(var_g729a_encode_internal, speech, filters);
    g729a_pitch.g729a_ad_cod_search_2(g729acode, var_g729a_encode_internal,
            speech, fs_1_);
    g729a_pitch.g729a_adcb_gain(var_g729a_encode_internal);
    var_g729a_encode_internal.taming = g729a_pitch.g729a_test_err(exc_err, var_g729a_encode_internal.T2,
            var_g729a_encode_internal.frac_T2);
    if (var_g729a_encode_internal.taming == 1
            && var_g729a_encode_internal.gp > 0.9499512F) {
      var_g729a_encode_internal.gp = 0.9499512F;
    }
    g729a_acelp_co.g729a_fc_search(var_g729a_encode_internal,
            var_g729a_encode_internal.T2,
            speech.past_quan_gp);
    g729a_qua_gain.g729a_qua_gain(var_g729a_encode_internal, speech);
    g729acode.setC2(var_g729a_encode_internal.C);
    g729acode.setS2(var_g729a_encode_internal.S);
    g729acode.setGA2(var_g729a_encode_internal.GA);
    g729acode.setGB2(var_g729a_encode_internal.GB);
    g729a_qua_gain.g729a_memory_update_2(speech, var_g729a_encode_internal,
            filters);
    g729a_pitch.g729a_update_exc_err(exc_err,
            var_g729a_encode_internal.gp_quan,
            var_g729a_encode_internal.T2);
    float[] fs_3_ = speech.past_subframe_3;
    float[] fs_4_ = speech.past_subframe_2;
    speech.past_subframe_3 = speech.past_subframe_1;
    speech.past_subframe_2 = speech.present_subframe_1;
    speech.past_subframe_1 = speech.present_subframe_2;
    speech.present_subframe_1 = speech.future_subframe;
    speech.present_subframe_2 = fs_3_;
    speech.future_subframe = fs_4_;
  }

  public void encode(byte[] is, int i, short[] is_5_, int i_6_, int i_7_) {
    int i_8_ = i_6_;
    int i_9_ = i;
    for (int i_10_ = 0; i_10_ < i_7_; i_10_++) {
      encode(is, i_9_, is_5_, i_8_);
      i_8_ += 80;
      i_9_ += 10;
    }
  }

  public void encode(G729aCode[] g729acodes, short[] is, int i, int i_11_) {
    if (g729acodes.length < i_11_) {
      throw new G729aException(1);
    }
    int i_12_ = i;
    for (int i_13_ = 0; i_13_ < i_11_; i_13_++) {
      encode(g729acodes[i_13_], is, i_12_);
      i_12_ += 80;
    }
  }

  public void encode(byte[] is, int i, short[] is_14_, int i_15_) {
    G729aCode g729acode = new G729aCode();
    g729a_utils.validateCode(is, i);
    g729a_utils.validateData(is_14_, i_15_);
    encode(g729acode, is_14_, i_15_);
    byte[] is_16_ = g729acode.getData();
    System.arraycopy(is_16_, 0, is, i, 10);
  }

  public void encode(G729aCode g729acode, short[] is, int i) {
    short[] is_17_ = new short[80];
    g729a_utils.validateData(is, i);
    System.arraycopy(is, i, is_17_, 0, 80);
    encode(g729acode, is_17_);
  }

  public g729a_encoder() {
    preproc = new g729a_preproc_filter();
    sinthesis = new g729a_sinthesis_filter();
    weghted_sinthesis = new g729a_sinthesis_filter();
    weghted = new g729a_sinthesis_filter();
    g729a_preproc_filter var_g729a_preproc_filter = preproc;
    g729a_sinthesis_filter var_g729a_sinthesis_filter = sinthesis;
    g729a_sinthesis_filter var_g729a_sinthesis_filter_18_ = weghted_sinthesis;
    g729a_sinthesis_filter var_g729a_sinthesis_filter_19_ = weghted;
    var_g729a_preproc_filter.a0 = 0.46363717F;
    var_g729a_preproc_filter.a1 = -0.92724705F;
    var_g729a_preproc_filter.a2 = 0.46363717F;
    var_g729a_preproc_filter.b1 = 1.9059465F;
    var_g729a_preproc_filter.b2 = -0.9114024F;
    var_g729a_preproc_filter.x1 = 0.0F;
    var_g729a_preproc_filter.x2 = 0.0F;
    var_g729a_preproc_filter.y1 = 0.0F;
    var_g729a_preproc_filter.y2 = 0.0F;
    speech = new g729a_encode_speech();
    speech.past_quan_gp = 0.2000122F;
    lp = new g729a_encode_lp();
    float[] fs = q_quant_past_subframe_2;
    float[] fs_20_ = lp.q;
    float f = 0.28559935F;
    for (int i = 0; i < 10; i++) {
      fs[i] = (float) Math.cos((double) (f * (float) (i + 1)));
      fs_20_[i] = fs[i];
    }
    float[] fs_21_ = l_quant_past_frame_4;
    float[] fs_22_ = l_quant_past_frame_3;
    float[] fs_23_ = l_quant_past_frame_2;
    fs_20_ = l_quant_past_frame_1;
    for (int i = 0; i < 10; i++) {
      fs_21_[i] = fs_22_[i] = fs_23_[i] = fs_20_[i] = (float) (i + 1) * f;
    }
    for (int i = 0; i < 2; i++) {
      fs_20_ = lp.fg_l_quant_sum;
      fs = g729a_tables.g729a_fg_sum;
      for (int i_24_ = 0; i_24_ < 10; i_24_++) {
        fs_20_[i_24_ + 10 * i] = (1.0F - fs[i_24_ + 10 * i]) * fs_23_[i_24_];
      }
    }
    filters = new g729a_encode_filters();
    filters.preproc = var_g729a_preproc_filter;
    filters.sinthesis = var_g729a_sinthesis_filter;
    filters.weghted_sinthesis = var_g729a_sinthesis_filter_18_;
    filters.weghted = var_g729a_sinthesis_filter_19_;
    speech.past_subframe_3 = past_subframe_3;
    speech.past_subframe_2 = past_subframe_2;
    speech.past_subframe_1 = past_subframe_1;
    speech.present_subframe_1 = present_subframe_1;
    speech.present_subframe_2 = present_subframe_2;
    speech.future_subframe = future_subframe;
    speech.weighted_speech = weighted_speech;
    speech.excitation = excitation;
    lp.q_quant_past_subframe_2 = q_quant_past_subframe_2;
    lp.q_quant_present_subframe_2 = q_quant_present_subframe_2;
    lp.l_quant_past_frame_4 = l_quant_past_frame_4;
    lp.l_quant_past_frame_3 = l_quant_past_frame_3;
    lp.l_quant_past_frame_2 = l_quant_past_frame_2;
    lp.l_quant_past_frame_1 = l_quant_past_frame_1;
    exc_err[0] = exc_err[1] = exc_err[2] = exc_err[3] = 1.0F;
  }

  void reset() {
    g729a_preproc_filter var_g729a_preproc_filter = preproc;
    g729a_sinthesis_filter var_g729a_sinthesis_filter = sinthesis;
    g729a_sinthesis_filter var_g729a_sinthesis_filter_25_ = weghted_sinthesis;
    g729a_sinthesis_filter var_g729a_sinthesis_filter_26_ = weghted;
    var_g729a_preproc_filter.a0 = 0.46363717F;
    var_g729a_preproc_filter.a1 = -0.92724705F;
    var_g729a_preproc_filter.a2 = 0.46363717F;
    var_g729a_preproc_filter.b1 = 1.9059465F;
    var_g729a_preproc_filter.b2 = -0.9114024F;
    var_g729a_preproc_filter.x1 = 0.0F;
    var_g729a_preproc_filter.x2 = 0.0F;
    var_g729a_preproc_filter.y1 = 0.0F;
    var_g729a_preproc_filter.y2 = 0.0F;
    g729a_utils.g729a_set_0(var_g729a_sinthesis_filter.delay, 10);
    g729a_utils.g729a_set_0(var_g729a_sinthesis_filter_25_.delay, 10);
    g729a_utils.g729a_set_0(var_g729a_sinthesis_filter_26_.delay, 10);
    g729a_utils.g729a_set_0(past_subframe_3, 40);
    g729a_utils.g729a_set_0(past_subframe_2, 40);
    g729a_utils.g729a_set_0(past_subframe_1, 40);
    g729a_utils.g729a_set_0(present_subframe_1, 40);
    g729a_utils.g729a_set_0(weighted_speech, 144);
    g729a_utils.g729a_set_0(excitation, 154);
    speech.U[0] = speech.U[1] = speech.U[2] = speech.U[3] = 0;
    speech.past_quan_gp = 0.2000122F;
    float[] fs = q_quant_past_subframe_2;
    float[] fs_27_ = lp.q;
    float f = 0.28559935F;
    for (int i = 0; i < 10; i++) {
      fs[i] = (float) Math.cos((double) (f * (float) (i + 1)));
      fs_27_[i] = fs[i];
    }
    float[] fs_28_ = l_quant_past_frame_4;
    float[] fs_29_ = l_quant_past_frame_3;
    float[] fs_30_ = l_quant_past_frame_2;
    fs_27_ = l_quant_past_frame_1;
    for (int i = 0; i < 10; i++) {
      fs_28_[i] = fs_29_[i] = fs_30_[i] = fs_27_[i] = (float) (i + 1) * f;
    }
    for (int i = 0; i < 2; i++) {
      fs_27_ = lp.fg_l_quant_sum;
      fs = g729a_tables.g729a_fg_sum;
      for (int i_31_ = 0; i_31_ < 10; i_31_++) {
        fs_27_[i_31_ + 10 * i] = (1.0F - fs[i_31_ + 10 * i]) * fs_30_[i_31_];
      }
    }
    filters.preproc = var_g729a_preproc_filter;
    filters.sinthesis = var_g729a_sinthesis_filter;
    filters.weghted_sinthesis = var_g729a_sinthesis_filter_25_;
    filters.weghted = var_g729a_sinthesis_filter_26_;
    speech.past_subframe_3 = past_subframe_3;
    speech.past_subframe_2 = past_subframe_2;
    speech.past_subframe_1 = past_subframe_1;
    speech.present_subframe_1 = present_subframe_1;
    speech.present_subframe_2 = present_subframe_2;
    speech.future_subframe = future_subframe;
    speech.weighted_speech = weighted_speech;
    speech.excitation = excitation;
    q_quant_past_subframe_2 = q_quant_past_subframe_2;
    q_quant_present_subframe_2 = q_quant_present_subframe_2;
    l_quant_past_frame_4 = l_quant_past_frame_4;
    l_quant_past_frame_3 = l_quant_past_frame_3;
    l_quant_past_frame_2 = l_quant_past_frame_2;
    l_quant_past_frame_1 = l_quant_past_frame_1;
    exc_err[0] = exc_err[1] = exc_err[2] = exc_err[3] = 1.0F;
  }
}

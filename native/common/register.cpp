static JNINativeMethod javaforce_controls_ni_DAQmx[] = {
  {"daqInit", "()Z", (void *)&Java_javaforce_controls_ni_DAQmx_daqInit},
  {"createTask", "()J", (void *)&Java_javaforce_controls_ni_DAQmx_createTask},
  {"createChannelAnalog", "(JLjava/lang/String;DJDD)Z", (void *)&Java_javaforce_controls_ni_DAQmx_createChannelAnalog},
  {"createChannelDigital", "(JLjava/lang/String;DJ)Z", (void *)&Java_javaforce_controls_ni_DAQmx_createChannelDigital},
  {"createChannelCounter", "(JLjava/lang/String;DJDDLjava/lang/String;DI)Z", (void *)&Java_javaforce_controls_ni_DAQmx_createChannelCounter},
  {"startTask", "(J)Z", (void *)&Java_javaforce_controls_ni_DAQmx_startTask},
  {"readTaskAnalog", "(JI[D)I", (void *)&Java_javaforce_controls_ni_DAQmx_readTaskAnalog},
  {"readTaskBinary", "(JI[I)I", (void *)&Java_javaforce_controls_ni_DAQmx_readTaskBinary},
  {"readTaskDigital", "(JI[I)I", (void *)&Java_javaforce_controls_ni_DAQmx_readTaskDigital},
  {"readTaskCounter", "(JI[D)I", (void *)&Java_javaforce_controls_ni_DAQmx_readTaskCounter},
  {"stopTask", "(J)Z", (void *)&Java_javaforce_controls_ni_DAQmx_stopTask},
  {"clearTask", "(J)Z", (void *)&Java_javaforce_controls_ni_DAQmx_clearTask},
  {"printError", "()V", (void *)&Java_javaforce_controls_ni_DAQmx_printError},
};

static JNINativeMethod javaforce_gl_GL[] = {
  {"glInit", "()Z", (void *)&Java_javaforce_gl_GL_glInit},
  {"glActiveTexture", "(I)V", (void *)&Java_javaforce_gl_GL_glActiveTexture},
  {"glAttachShader", "(II)V", (void *)&Java_javaforce_gl_GL_glAttachShader},
  {"glBindBuffer", "(II)V", (void *)&Java_javaforce_gl_GL_glBindBuffer},
  {"glBindFramebuffer", "(II)V", (void *)&Java_javaforce_gl_GL_glBindFramebuffer},
  {"glBindRenderbuffer", "(II)V", (void *)&Java_javaforce_gl_GL_glBindRenderbuffer},
  {"glBindTexture", "(II)V", (void *)&Java_javaforce_gl_GL_glBindTexture},
  {"glBlendFunc", "(II)V", (void *)&Java_javaforce_gl_GL_glBlendFunc},
  {"glBufferData", "(II[FI)V", (void *)&Java_javaforce_gl_GL_glBufferData__II_3FI},
  {"glBufferData", "(II[SI)V", (void *)&Java_javaforce_gl_GL_glBufferData__II_3SI},
  {"glBufferData", "(II[II)V", (void *)&Java_javaforce_gl_GL_glBufferData__II_3II},
  {"glBufferData", "(II[BI)V", (void *)&Java_javaforce_gl_GL_glBufferData__II_3BI},
  {"glClear", "(I)V", (void *)&Java_javaforce_gl_GL_glClear},
  {"glClearColor", "(FFFF)V", (void *)&Java_javaforce_gl_GL_glClearColor},
  {"glColorMask", "(ZZZZ)V", (void *)&Java_javaforce_gl_GL_glColorMask},
  {"glCompileShader", "(I)V", (void *)&Java_javaforce_gl_GL_glCompileShader},
  {"glCreateProgram", "()I", (void *)&Java_javaforce_gl_GL_glCreateProgram},
  {"glCreateShader", "(I)I", (void *)&Java_javaforce_gl_GL_glCreateShader},
  {"glCullFace", "(I)V", (void *)&Java_javaforce_gl_GL_glCullFace},
  {"glDeleteBuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glDeleteBuffers},
  {"glDeleteFramebuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glDeleteFramebuffers},
  {"glDeleteRenderbuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glDeleteRenderbuffers},
  {"glDeleteTextures", "(I[I)V", (void *)&Java_javaforce_gl_GL_glDeleteTextures},
  {"glDrawElements", "(IIII)V", (void *)&Java_javaforce_gl_GL_glDrawElements},
  {"glDepthFunc", "(I)V", (void *)&Java_javaforce_gl_GL_glDepthFunc},
  {"glDisable", "(I)V", (void *)&Java_javaforce_gl_GL_glDisable},
  {"glDisableVertexAttribArray", "(I)V", (void *)&Java_javaforce_gl_GL_glDisableVertexAttribArray},
  {"glDepthMask", "(Z)V", (void *)&Java_javaforce_gl_GL_glDepthMask},
  {"glEnable", "(I)V", (void *)&Java_javaforce_gl_GL_glEnable},
  {"glEnableVertexAttribArray", "(I)V", (void *)&Java_javaforce_gl_GL_glEnableVertexAttribArray},
  {"glFlush", "()V", (void *)&Java_javaforce_gl_GL_glFlush},
  {"glFramebufferTexture2D", "(IIIII)V", (void *)&Java_javaforce_gl_GL_glFramebufferTexture2D},
  {"glFramebufferRenderbuffer", "(IIII)V", (void *)&Java_javaforce_gl_GL_glFramebufferRenderbuffer},
  {"glFrontFace", "(I)V", (void *)&Java_javaforce_gl_GL_glFrontFace},
  {"glGetAttribLocation", "(ILjava/lang/String;)I", (void *)&Java_javaforce_gl_GL_glGetAttribLocation},
  {"glGetError", "()I", (void *)&Java_javaforce_gl_GL_glGetError},
  {"glGetProgramInfoLog", "(I)Ljava/lang/String;", (void *)&Java_javaforce_gl_GL_glGetProgramInfoLog},
  {"glGetShaderInfoLog", "(I)Ljava/lang/String;", (void *)&Java_javaforce_gl_GL_glGetShaderInfoLog},
  {"glGetString", "(I)Ljava/lang/String;", (void *)&Java_javaforce_gl_GL_glGetString},
  {"glGetIntegerv", "(I[I)V", (void *)&Java_javaforce_gl_GL_glGetIntegerv},
  {"glGenBuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glGenBuffers},
  {"glGenFramebuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glGenFramebuffers},
  {"glGenRenderbuffers", "(I[I)V", (void *)&Java_javaforce_gl_GL_glGenRenderbuffers},
  {"glGenTextures", "(I[I)V", (void *)&Java_javaforce_gl_GL_glGenTextures},
  {"glGetUniformLocation", "(ILjava/lang/String;)I", (void *)&Java_javaforce_gl_GL_glGetUniformLocation},
  {"glLinkProgram", "(I)V", (void *)&Java_javaforce_gl_GL_glLinkProgram},
  {"glPixelStorei", "(II)V", (void *)&Java_javaforce_gl_GL_glPixelStorei},
  {"glReadPixels", "(IIIIII[I)V", (void *)&Java_javaforce_gl_GL_glReadPixels},
  {"glRenderbufferStorage", "(IIII)V", (void *)&Java_javaforce_gl_GL_glRenderbufferStorage},
  {"glShaderSource", "(II[Ljava/lang/String;[I)I", (void *)&Java_javaforce_gl_GL_glShaderSource},
  {"glStencilFunc", "(III)I", (void *)&Java_javaforce_gl_GL_glStencilFunc},
  {"glStencilMask", "(I)I", (void *)&Java_javaforce_gl_GL_glStencilMask},
  {"glStencilOp", "(III)I", (void *)&Java_javaforce_gl_GL_glStencilOp},
  {"glTexImage2D", "(IIIIIIII[I)V", (void *)&Java_javaforce_gl_GL_glTexImage2D},
  {"glTexSubImage2D", "(IIIIIIII[I)V", (void *)&Java_javaforce_gl_GL_glTexSubImage2D},
  {"glTexParameteri", "(III)V", (void *)&Java_javaforce_gl_GL_glTexParameteri},
  {"glUseProgram", "(I)V", (void *)&Java_javaforce_gl_GL_glUseProgram},
  {"glUniformMatrix4fv", "(III[F)V", (void *)&Java_javaforce_gl_GL_glUniformMatrix4fv},
  {"glUniform4fv", "(II[F)V", (void *)&Java_javaforce_gl_GL_glUniform4fv},
  {"glUniform3fv", "(II[F)V", (void *)&Java_javaforce_gl_GL_glUniform3fv},
  {"glUniform2fv", "(II[F)V", (void *)&Java_javaforce_gl_GL_glUniform2fv},
  {"glUniform1f", "(IF)V", (void *)&Java_javaforce_gl_GL_glUniform1f},
  {"glUniform4iv", "(II[I)V", (void *)&Java_javaforce_gl_GL_glUniform4iv},
  {"glUniform3iv", "(II[I)V", (void *)&Java_javaforce_gl_GL_glUniform3iv},
  {"glUniform2iv", "(II[I)V", (void *)&Java_javaforce_gl_GL_glUniform2iv},
  {"glUniform1i", "(II)V", (void *)&Java_javaforce_gl_GL_glUniform1i},
  {"glVertexAttribPointer", "(IIIIII)V", (void *)&Java_javaforce_gl_GL_glVertexAttribPointer},
  {"glViewport", "(IIII)V", (void *)&Java_javaforce_gl_GL_glViewport},
};

static JNINativeMethod javaforce_ui_Window[] = {
  {"ninit", "()Z", (void *)&Java_javaforce_ui_Window_ninit},
  {"ncreate", "(ILjava/lang/String;IILjavaforce/ui/Window;J)J", (void *)&Java_javaforce_ui_Window_ncreate},
  {"ndestroy", "(J)V", (void *)&Java_javaforce_ui_Window_ndestroy},
  {"nsetcurrent", "(J)V", (void *)&Java_javaforce_ui_Window_nsetcurrent},
  {"nseticon", "(JLjava/lang/String;II)V", (void *)&Java_javaforce_ui_Window_nseticon},
  {"pollEvents", "(I)V", (void *)&Java_javaforce_ui_Window_pollEvents},
  {"postEvent", "()V", (void *)&Java_javaforce_ui_Window_postEvent},
  {"nshow", "(J)V", (void *)&Java_javaforce_ui_Window_nshow},
  {"nhide", "(J)V", (void *)&Java_javaforce_ui_Window_nhide},
  {"nswap", "(J)V", (void *)&Java_javaforce_ui_Window_nswap},
  {"nhidecursor", "(J)V", (void *)&Java_javaforce_ui_Window_nhidecursor},
  {"nshowcursor", "(J)V", (void *)&Java_javaforce_ui_Window_nshowcursor},
  {"nlockcursor", "(J)V", (void *)&Java_javaforce_ui_Window_nlockcursor},
  {"ngetpos", "(J[I)V", (void *)&Java_javaforce_ui_Window_ngetpos},
  {"nsetpos", "(JII)V", (void *)&Java_javaforce_ui_Window_nsetpos},
};

static JNINativeMethod javaforce_ui_Font[] = {
  {"loadFont", "([BI[I[I[I[I[BII)I", (void *)&Java_javaforce_ui_Font_loadFont}
};

static JNINativeMethod javaforce_ui_Image[] = {
  {"nloadPNG", "([B[I)[I", (void *)&Java_javaforce_ui_Image_nloadPNG},
  {"nsavePNG", "([III)[B", (void *)&Java_javaforce_ui_Image_nsavePNG},
  {"nloadJPG", "([B[I)[I", (void *)&Java_javaforce_ui_Image_nloadJPG},
  {"nsaveJPG", "([IIII)[B", (void *)&Java_javaforce_ui_Image_nloadJPG}
};

static JNINativeMethod javaforce_media_Camera[] = {
  {"cameraInit", "()Z", (void *)&Java_javaforce_media_Camera_cameraInit},
  {"cameraUninit", "()Z", (void *)&Java_javaforce_media_Camera_cameraUninit},
  {"cameraListDevices", "()[Ljava/lang/String;", (void *)&Java_javaforce_media_Camera_cameraListDevices},
  {"cameraListModes", "(I)[Ljava/lang/String;", (void *)&Java_javaforce_media_Camera_cameraListModes},
  {"cameraStart", "(III)Z", (void *)&Java_javaforce_media_Camera_cameraStart},
  {"cameraStop", "()Z", (void *)&Java_javaforce_media_Camera_cameraStop},
  {"cameraGetFrame", "()[I", (void *)&Java_javaforce_media_Camera_cameraGetFrame},
  {"cameraGetWidth", "()I", (void *)&Java_javaforce_media_Camera_cameraGetWidth},
  {"cameraGetHeight", "()I", (void *)&Java_javaforce_media_Camera_cameraGetHeight},
};

static JNINativeMethod javaforce_media_MediaCoder[] = {
  {"ninit", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_media_MediaCoder_ninit},
  {"setLogging", "(Z)V", (void *)&Java_javaforce_media_MediaCoder_setLogging},
};

static JNINativeMethod javaforce_media_MediaDecoder[] = {
  {"start", "(Ljavaforce/media/MediaIO;IIIIZ)Z", (void *)&Java_javaforce_media_MediaDecoder_start},
  {"startFile", "(Ljava/lang/String;Ljava/lang/String;IIII)Z", (void *)&Java_javaforce_media_MediaDecoder_startFile},
  {"stop", "()V", (void *)&Java_javaforce_media_MediaDecoder_stop},
  {"read", "()I", (void *)&Java_javaforce_media_MediaDecoder_read},
  {"getVideo", "()[I", (void *)&Java_javaforce_media_MediaDecoder_getVideo},
  {"getAudio", "()[S", (void *)&Java_javaforce_media_MediaDecoder_getAudio},
  {"getWidth", "()I", (void *)&Java_javaforce_media_MediaDecoder_getWidth},
  {"getHeight", "()I", (void *)&Java_javaforce_media_MediaDecoder_getHeight},
  {"getFrameRate", "()F", (void *)&Java_javaforce_media_MediaDecoder_getFrameRate},
  {"getDuration", "()J", (void *)&Java_javaforce_media_MediaDecoder_getDuration},
  {"getSampleRate", "()I", (void *)&Java_javaforce_media_MediaDecoder_getSampleRate},
  {"getChannels", "()I", (void *)&Java_javaforce_media_MediaDecoder_getChannels},
  {"getBitsPerSample", "()I", (void *)&Java_javaforce_media_MediaDecoder_getBitsPerSample},
  {"seek", "(J)Z", (void *)&Java_javaforce_media_MediaDecoder_seek},
  {"getVideoBitRate", "()I", (void *)&Java_javaforce_media_MediaDecoder_getVideoBitRate},
  {"getAudioBitRate", "()I", (void *)&Java_javaforce_media_MediaDecoder_getAudioBitRate},
  {"isKeyFrame", "()Z", (void *)&Java_javaforce_media_MediaDecoder_isKeyFrame},
  {"resize", "(II)Z", (void *)&Java_javaforce_media_MediaDecoder_resize},
};

static JNINativeMethod javaforce_media_MediaEncoder[] = {
  {"start", "(Ljavaforce/media/MediaIO;IIIIILjava/lang/String;ZZ)Z", (void *)&Java_javaforce_media_MediaEncoder_start},
  {"addAudio", "([SII)Z", (void *)&Java_javaforce_media_MediaEncoder_addAudio},
  {"addVideo", "([I)Z", (void *)&Java_javaforce_media_MediaEncoder_addVideo},
  {"getAudioFramesize", "()I", (void *)&Java_javaforce_media_MediaEncoder_getAudioFramesize},
  {"addAudioEncoded", "([BII)Z", (void *)&Java_javaforce_media_MediaEncoder_addAudioEncoded},
  {"addVideoEncoded", "([BIIZ)Z", (void *)&Java_javaforce_media_MediaEncoder_addVideoEncoded},
  {"stop", "()V", (void *)&Java_javaforce_media_MediaEncoder_stop},
};

static JNINativeMethod javaforce_media_MediaVideoDecoder[] = {
  {"start", "(III)Z", (void *)&Java_javaforce_media_MediaVideoDecoder_start},
  {"stop", "()V", (void *)&Java_javaforce_media_MediaVideoDecoder_stop},
  {"decode", "([BII)[I", (void *)&Java_javaforce_media_MediaVideoDecoder_decode},
  {"decode16", "([BII)[S", (void *)&Java_javaforce_media_MediaVideoDecoder_decode16},
  {"getWidth", "()I", (void *)&Java_javaforce_media_MediaVideoDecoder_getWidth},
  {"getHeight", "()I", (void *)&Java_javaforce_media_MediaVideoDecoder_getHeight},
  {"getFrameRate", "()F", (void *)&Java_javaforce_media_MediaVideoDecoder_getFrameRate},
};

static JNINativeMethod javaforce_media_VideoBuffer[] = {
  {"compareFrames", "([I[III)F", (void *)&Java_javaforce_media_VideoBuffer_compareFrames},
};

static JNINativeMethod javaforce_net_PacketCapture[] = {
  {"ninit", "(Ljava/lang/String;Ljava/lang/String;)Z", (void *)&Java_javaforce_net_PacketCapture_ninit},
  {"listLocalInterfaces", "()[Ljava/lang/String;", (void *)&Java_javaforce_net_PacketCapture_listLocalInterfaces},
  {"nstart", "(Ljava/lang/String;Z)J", (void *)&Java_javaforce_net_PacketCapture_nstart},
  {"stop", "(J)V", (void *)&Java_javaforce_net_PacketCapture_stop},
  {"compile", "(JLjava/lang/String;)Z", (void *)&Java_javaforce_net_PacketCapture_compile},
  {"read", "(J)[B", (void *)&Java_javaforce_net_PacketCapture_read},
  {"write", "(J[BII)Z", (void *)&Java_javaforce_net_PacketCapture_write},
};

static JNINativeMethod javaforce_cl_CL[] = {
  {"ninit", "(Ljava/lang/String;)Z", (void *)&Java_javaforce_cl_CL_ninit},
  {"ncreate", "(Ljava/lang/String;I)J", (void *)&Java_javaforce_cl_CL_ncreate},
  {"nkernel", "(JLjava/lang/String;)J", (void *)&Java_javaforce_cl_CL_nkernel},
  {"ncreateBuffer", "(JII)J", (void *)&Java_javaforce_cl_CL_ncreateBuffer},
  {"nsetArg", "(JJI[B)Z", (void *)&Java_javaforce_cl_CL_nsetArg},
  {"nwriteBufferi8", "(JJ[B)Z", (void *)&Java_javaforce_cl_CL_nwriteBufferi8},
  {"nwriteBufferf32", "(JJ[F)Z", (void *)&Java_javaforce_cl_CL_nwriteBufferf32},
  {"nexecute", "(JJI)Z", (void *)&Java_javaforce_cl_CL_nexecute},
  {"nreadBufferi8", "(JJ[B)Z", (void *)&Java_javaforce_cl_CL_nreadBufferi8},
  {"nreadBufferf32", "(JJ[F)Z", (void *)&Java_javaforce_cl_CL_nreadBufferf32},
  {"nfreeKernel", "(JJ)Z", (void *)&Java_javaforce_cl_CL_nfreeKernel},
  {"nfreeBuffer", "(JJ)Z", (void *)&Java_javaforce_cl_CL_nfreeBuffer},
  {"nclose", "(J)Z", (void *)&Java_javaforce_cl_CL_nclose},
};

#ifdef __RASPBERRY_PI__
static JNINativeMethod javaforce_pi_GPIO[] = {
  {"ninit", "(I)Z", (void *)&Java_javaforce_pi_GPIO_ninit},
  {"configOutput", "(I)Z", (void *)&Java_javaforce_pi_GPIO_configOutput},
  {"configInput", "(I)Z", (void *)&Java_javaforce_pi_GPIO_configInput},
  {"write", "(IZ)Z", (void *)&Java_javaforce_pi_GPIO_write},
  {"read", "(I)Z", (void *)&Java_javaforce_pi_GPIO_read},
};

static JNINativeMethod javaforce_pi_I2C[] = {
  {"init", "()Z", (void *)&Java_javaforce_pi_I2C_init},
  {"setSlave", "(I)Z", (void *)&Java_javaforce_pi_I2C_setSlave},
  {"write", "([B)Z", (void *)&Java_javaforce_pi_I2C_write},
  {"read", "([B)I", (void *)&Java_javaforce_pi_I2C_read},
};
#endif

const char* nclass;

jclass findClass(JNIEnv *env, const char *clsname) {
  nclass = clsname;
  jclass cls = env->FindClass(clsname);
  if (cls == NULL) {
    printf("Error:Class not found:%s\n", clsname);
    exit(1);
  }
  return cls;
}

void registerNatives(JNIEnv *env, jclass cls, JNINativeMethod *methods, jint count) {
  int res = env->RegisterNatives(cls, methods, count);
  if (res != 0) {
    printf("Registering natives for %s count %d error %d\n", nclass, count, res);
  }
}

void registerCommonNatives(JNIEnv *env) {
  jclass cls;

#ifndef __FreeBSD__
  cls = findClass(env, "javaforce/controls/ni/DAQmx");
  registerNatives(env, cls, javaforce_controls_ni_DAQmx, sizeof(javaforce_controls_ni_DAQmx)/sizeof(JNINativeMethod));
#endif

  cls = findClass(env, "javaforce/gl/GL");
  registerNatives(env, cls, javaforce_gl_GL, sizeof(javaforce_gl_GL)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/ui/Window");
  registerNatives(env, cls, javaforce_ui_Window, sizeof(javaforce_ui_Window)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/ui/Font");
  registerNatives(env, cls, javaforce_ui_Font, sizeof(javaforce_ui_Font)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/ui/Image");
  registerNatives(env, cls, javaforce_ui_Image, sizeof(javaforce_ui_Image)/sizeof(JNINativeMethod));

#ifndef __FreeBSD__
  cls = findClass(env, "javaforce/media/Camera");
  registerNatives(env, cls, javaforce_media_Camera, sizeof(javaforce_media_Camera)/sizeof(JNINativeMethod));
#endif

  cls = findClass(env, "javaforce/media/MediaCoder");
  registerNatives(env, cls, javaforce_media_MediaCoder, sizeof(javaforce_media_MediaCoder)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaDecoder");
  registerNatives(env, cls, javaforce_media_MediaDecoder, sizeof(javaforce_media_MediaDecoder)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaEncoder");
  registerNatives(env, cls, javaforce_media_MediaEncoder, sizeof(javaforce_media_MediaEncoder)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/MediaVideoDecoder");
  registerNatives(env, cls, javaforce_media_MediaVideoDecoder, sizeof(javaforce_media_MediaVideoDecoder)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/media/VideoBuffer");
  registerNatives(env, cls, javaforce_media_VideoBuffer, sizeof(javaforce_media_VideoBuffer)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/net/PacketCapture");
  registerNatives(env, cls, javaforce_net_PacketCapture, sizeof(javaforce_net_PacketCapture)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/cl/CL");
  registerNatives(env, cls, javaforce_cl_CL, sizeof(javaforce_cl_CL)/sizeof(JNINativeMethod));

#ifdef __RASPBERRY_PI__
  cls = findClass(env, "javaforce/pi/GPIO");
  registerNatives(env, cls, javaforce_pi_GPIO, sizeof(javaforce_pi_GPIO)/sizeof(JNINativeMethod));

  cls = findClass(env, "javaforce/pi/I2C");
  registerNatives(env, cls, javaforce_pi_I2C, sizeof(javaforce_pi_I2C)/sizeof(JNINativeMethod));
#endif
}

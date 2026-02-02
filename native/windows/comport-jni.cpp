//Com Port JNI API

JNIEXPORT jlong JNICALL Java_javaforce_jni_ComPortJNI_comOpen
  (JNIEnv *e, jobject c, jstring str, jint baud)
{
  const char *cstr = e->GetStringUTFChars(str,NULL);
  HANDLE handle = CreateFileA(cstr, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);
  e->ReleaseStringUTFChars(str, cstr);
  if (handle == INVALID_HANDLE_VALUE) return 0;
  DCB dcb;
  memset(&dcb, 0, sizeof(DCB));
  dcb.DCBlength = sizeof(DCB);
  GetCommState(handle, &dcb);
  dcb.BaudRate = baud;
  dcb.fBinary = 1;
  dcb.fParity = 0;
  dcb.ByteSize = 8;  //8 data bits
  dcb.StopBits = ONESTOPBIT;  //1 stop bit
  dcb.Parity = 0;  //no parity
  SetCommState(handle, &dcb);
  COMMTIMEOUTS cto;
  memset(&cto, 0, sizeof(COMMTIMEOUTS));
  GetCommTimeouts(handle, &cto);
  cto.ReadIntervalTimeout = MAXDWORD;
  cto.ReadTotalTimeoutMultiplier = 0;
  cto.ReadTotalTimeoutConstant = 0;
  cto.WriteTotalTimeoutMultiplier = 0;
  cto.WriteTotalTimeoutConstant = 0;
  SetCommTimeouts(handle, &cto);
  return (jlong)handle;
}

JNIEXPORT void JNICALL Java_javaforce_jni_ComPortJNI_comClose
  (JNIEnv *e, jobject c, jlong handle)
{
  CloseHandle((HANDLE)handle);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_ComPortJNI_comRead
  (JNIEnv *e, jobject c, jlong handle, jbyteArray ba, jint size)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int read;
  ReadFile((HANDLE)handle, baptr, e->GetArrayLength(ba), (LPDWORD)&read, NULL);
  e->ReleaseByteArrayElements(ba, baptr, 0);
  return read;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_ComPortJNI_comWrite
  (JNIEnv *e, jobject c, jlong handle, jbyteArray ba, jint size)
{
  jbyte *baptr = e->GetByteArrayElements(ba,NULL);
  int write;
  WriteFile((HANDLE)handle, baptr, e->GetArrayLength(ba), (LPDWORD)&write, NULL);
  e->ReleaseByteArrayElements(ba, baptr, JNI_ABORT);
  return write;
}

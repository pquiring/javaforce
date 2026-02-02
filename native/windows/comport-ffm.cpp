//Com Port JNI API

jlong comOpen(const char* str, jint baud)
{
  HANDLE handle = CreateFileA(str, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);
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

void comClose(jlong handle)
{
  CloseHandle((HANDLE)handle);
}

jint comRead(jlong handle, jbyte* ba, jint size)
{
  int read;
  ReadFile((HANDLE)handle, ba, size, (LPDWORD)&read, NULL);
  return read;
}

jint comWrite(jlong handle, jbyte* ba, jint size)
{
  int write;
  WriteFile((HANDLE)handle, ba, size, (LPDWORD)&write, NULL);
  return write;
}

extern "C" {
  JNIEXPORT jlong (*_comOpen)(const char* str, jint baud) = &comOpen;
  JNIEXPORT void (*_comClose)(jlong handle) = &comClose;
  JNIEXPORT jint (*_comRead)(jlong handle, jbyte* ba, jint size) = &comRead;
  JNIEXPORT jint (*_comWrite)(jlong handle, jbyte* ba, jint size) = &comWrite;
}

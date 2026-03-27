/*

Windows Pipes are NOT a server.

Each time a client connects to a pipe, the pipe MUST be recreated to allow another client to connect.

PIPE_UNLIMITED_INSTANCES means multiple client connections can run concurrent, BUT the server side MUST still be recreated each time a client connects.

See CreateNamedPipe -> Example : Multithreaded Pipe Server

*/

JNIEXPORT jlong JNICALL Java_javaforce_jni_WinNative_pipeCreate
  (JNIEnv *e, jclass c, jstring name, jboolean first)
{
  const char *cname = e->GetStringUTFChars(name, NULL);

  int openMode = PIPE_ACCESS_DUPLEX;
  if (first) {
    openMode |= FILE_FLAG_FIRST_PIPE_INSTANCE;
  }
  int pipeMode = PIPE_TYPE_MESSAGE | PIPE_READMODE_MESSAGE | PIPE_WAIT;

  HANDLE ctx = CreateNamedPipe(cname, openMode, pipeMode, PIPE_UNLIMITED_INSTANCES, 64 * 1024, 64 * 1024, 0, NULL);

  e->ReleaseStringUTFChars(name, cname);

  if (ctx == INVALID_HANDLE_VALUE) {
    printf("CreateNamedPipe:failed:error=0x%x\n", GetLastError());
  }

  return (jlong)ctx;
}

JNIEXPORT void JNICALL Java_javaforce_jni_WinNative_pipeClose
  (JNIEnv *e, jclass c, jlong ctx)
{
  DisconnectNamedPipe((HANDLE)ctx);
  CloseHandle((HANDLE)ctx);
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_pipeRead
  (JNIEnv *e, jclass c, jlong ctx, jbyteArray buf, jint buf_off, jint buf_len)
{
  int read = -1;
  jbyte *cbuf = e->GetByteArrayElements(buf, NULL);
  ReadFile((HANDLE)ctx, cbuf + buf_off, buf_len, (LPDWORD)&read, NULL);
  e->ReleaseByteArrayElements(buf, cbuf, JNI_COMMIT);
  return read;
}

JNIEXPORT jint JNICALL Java_javaforce_jni_WinNative_pipeWrite
  (JNIEnv *e, jclass c, jstring name, jbyteArray buf, jint buf_off, jint buf_len)
{
  int write = -1;
  jbyte *cbuf = e->GetByteArrayElements(buf, NULL);
  const char *cname = e->GetStringUTFChars(name, NULL);
  HANDLE client;

  while (1) {

    WaitNamedPipe(cname, 250);  //wait for server to recreate pipe if necessary

    //NOTE:There is a small chance this fails if multiple clients are trying to send a message at the same time

    client = CreateFile(cname, GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);

    if (client == INVALID_HANDLE_VALUE) {
      int error = GetLastError();
      if (error == ERROR_PIPE_BUSY) {  //0xe7
        //try again
        continue;
      }
      printf("WinPipe.write() failed:Error=0x%x\n", error);
      break;
    } else {
      WriteFile(client, cbuf + buf_off, buf_len, (LPDWORD)&write, NULL);
      FlushFileBuffers(client);
      CloseHandle(client);  //client disconnects from server
      break;
    }
  }

  e->ReleaseStringUTFChars(name, cname);
  e->ReleaseByteArrayElements(buf, cbuf, JNI_ABORT);

  return write;
}

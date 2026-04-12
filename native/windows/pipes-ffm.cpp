/*

Windows Pipes are NOT a server.

Each time a client connects to a pipe, the pipe MUST be recreated to allow another client to connect.

PIPE_UNLIMITED_INSTANCES means multiple client connections can run concurrent, BUT the server side MUST still be recreated each time a client connects.

See CreateNamedPipe -> Example : Multithreaded Pipe Server

*/

HANDLE pipeCreate(const char* name, jboolean first)
{
  SECURITY_DESCRIPTOR sd;
  InitializeSecurityDescriptor(&sd, SECURITY_DESCRIPTOR_REVISION);
  // Set DACL to NULL for full access by Everyone
  SetSecurityDescriptorDacl(&sd, TRUE, NULL, FALSE);
  SECURITY_ATTRIBUTES sa;
  sa.nLength = sizeof(SECURITY_ATTRIBUTES);
  sa.lpSecurityDescriptor = &sd;
  sa.bInheritHandle = FALSE;

  int openMode = PIPE_ACCESS_DUPLEX;
  if (first) {
    openMode |= FILE_FLAG_FIRST_PIPE_INSTANCE;
  }
  int pipeMode = PIPE_TYPE_MESSAGE | PIPE_READMODE_MESSAGE | PIPE_WAIT;

  HANDLE ctx = CreateNamedPipe(name, openMode, pipeMode, PIPE_UNLIMITED_INSTANCES, 64 * 1024, 64 * 1024, 0, &sa);

  if (ctx == INVALID_HANDLE_VALUE) {
    printf("CreateNamedPipe:failed:error=0x%x\n", GetLastError());
  }

  return ctx;
}

void pipeClose(HANDLE ctx)
{
  DisconnectNamedPipe(ctx);
  CloseHandle(ctx);
}

jint pipeRead(HANDLE ctx, jbyte* buf, jint buf_off, jint buf_len)
{
  int read = -1;
  ReadFile((HANDLE)ctx, buf + buf_off, buf_len, (LPDWORD)&read, NULL);
  return read;
}

jint pipeWrite(const char* name, jbyte* buf, jint buf_off, jint buf_len)
{
  int write = -1;
  HANDLE client;

  while (1) {

    WaitNamedPipe(name, 250);  //wait for server to recreate pipe if necessary

    //NOTE:There is a small chance this fails if multiple clients are trying to send a message at the same time

    client = CreateFile(name, GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);

    if (client == INVALID_HANDLE_VALUE) {
      int error = GetLastError();
      if (error == ERROR_PIPE_BUSY) {  //0xe7
        //try again
        if (debug_pipes) printf("WinPipe.retry\n");
        continue;
      }
      printf("WinPipe.write() failed:Error=0x%x\n", error);
      break;
    } else {
      WriteFile(client, buf + buf_off, buf_len, (LPDWORD)&write, NULL);
      FlushFileBuffers(client);
      CloseHandle(client);  //client disconnects from server
      break;
    }
  }

  if (debug_pipes) printf("WinPipe.write success\n");

  return write;
}

extern "C" {
  JNIEXPORT HANDLE (*_pipeCreate)(const char* str, jboolean first) = &pipeCreate;
  JNIEXPORT void (*_pipeClose)(HANDLE handle) = &pipeClose;
  JNIEXPORT jint (*_pipeRead)(HANDLE handle, jbyte* ba, jint offset, jint length) = &pipeRead;
  JNIEXPORT jint (*_pipeWrite)(const char* name, jbyte* ba, jint offset, jint length) = &pipeWrite;

  JNIEXPORT jboolean JNICALL WinPipeAPIinit() {return JNI_TRUE;}
}

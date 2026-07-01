jint fileGetMode(const char* name)
{
  struct stat s;
  ::lstat(name, (struct stat*)&s);
  return s.st_mode;
}

void fileSetMode(const char* name, jint mode)
{
  ::chmod(name, mode);
}

void fileSetAccessTime(const char* name, jlong ts)
{
  struct stat s;
  struct utimbuf tb;
  ::lstat(name, (struct stat*)&s);
  ts /= 1000L;
  tb.actime = ts;
  tb.modtime = s.st_mtime;
  ::utime(name, &tb);
}

void fileSetModifiedTime(const char* name, jlong ts)
{
  struct stat s;
  struct utimbuf tb;
  ::lstat(name, (struct stat*)&s);
  ts /= 1000L;
  tb.actime = s.st_atime;
  tb.modtime = ts;
  ::utime(name, &tb);
}

jlong fileGetID(const char* name)
{
  struct stat s;
  ::lstat(name, (struct stat*)&s);
  return s.st_ino;
}

extern "C" {
  JNIEXPORT jint (*_fileGetMode)(const char*) = &fileGetMode;
  JNIEXPORT void (*_fileSetMode)(const char*,jint) = &fileSetMode;
  JNIEXPORT void (*_fileSetAccessTime)(const char*,jlong) = &fileSetAccessTime;
  JNIEXPORT void (*_fileSetModifiedTime)(const char*,jlong) = &fileSetModifiedTime;
  JNIEXPORT jlong (*_fileGetID)(const char*) = &fileGetID;
}

//tape drive API

static int tapeLastErrorValue;

jlong tapeOpen(const char* name)
{
  HANDLE handle = CreateFileA(name, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, FILE_FLAG_NO_BUFFERING, NULL);
  if (handle == INVALID_HANDLE_VALUE) {
    tapeLastErrorValue = GetLastError();
    return 0;
  }
  return (jlong)handle;
}

void tapeClose(jlong handle)
{
  CloseHandle((HANDLE)handle);
}

jboolean tapeFormat(jlong handle, jint blocksize)
{
  HANDLE dev = (HANDLE)handle;
  DWORD bytesReturn;
  BOOL ret;

/*  //not supported
  TAPE_PREPARE tapePrepare;
  tapePrepare.Operation = TAPE_FORMAT;
  tapePrepare.Immediate = FALSE;
  ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_PREPARE,
    &tapePrepare,
    sizeof(tapePrepare),
    nullptr,
    0,
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    printf("TAPE_PREPARE Failed\r\n");
    tapeLastErrorValue = GetLastError();
    //return JNI_FALSE;  //ignore error - not supported on all drives
  }
*/

  TAPE_SET_MEDIA_PARAMETERS tapeSetMediaParams;
  tapeSetMediaParams.BlockSize = blocksize;
  ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_SET_MEDIA_PARAMS,
    &tapeSetMediaParams,
    sizeof(tapeSetMediaParams),
    nullptr,
    0,
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    printf("TAPE_SET_MEDIA_PARAMETERS Failed\r\n");
    tapeLastErrorValue = GetLastError();
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

jint tapeRead(jlong handle, jbyte* baptr, jint offset, jint length)
{
  if (baptr == NULL) {
    tapeLastErrorValue = -1;
    return 0;
  }
  int read = 0;
  ReadFile((HANDLE)handle, baptr + offset, length, (LPDWORD)&read, NULL);
  tapeLastErrorValue = GetLastError();
  return read;
}

jint tapeWrite(jlong handle, jbyte* baptr, jint offset, jint length)
{
  if (baptr == NULL) {
    tapeLastErrorValue = -1;
    return 0;
  }
  int write = 0;
  WriteFile((HANDLE)handle, baptr + offset, length, (LPDWORD)&write, NULL);
  tapeLastErrorValue = GetLastError();
  return write;
}

jboolean tapeSetpos(jlong handle, jlong pos)
{
  HANDLE dev = (HANDLE)handle;
  TAPE_SET_POSITION tapePos;
  tapePos.Method = pos == 0 ? TAPE_REWIND : TAPE_LOGICAL_BLOCK;
  tapePos.Partition = 0;
  tapePos.Offset.QuadPart = pos;
  tapePos.Immediate = FALSE;
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_SET_POSITION,
    &tapePos,
    sizeof(tapePos),
    nullptr,
    0,
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    tapeLastErrorValue = GetLastError();
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

jlong tapeGetpos(jlong handle)
{
  HANDLE dev = (HANDLE)handle;
  TAPE_GET_POSITION tapePos;
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_GET_POSITION,
    nullptr,
    0,
    &tapePos,
    sizeof(tapePos),
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    tapeLastErrorValue = GetLastError();
    return -1;
  }
  return tapePos.Offset.QuadPart;
}

static jlong tape_media_size;
static jint tape_media_blocksize;
static jboolean tape_media_readonly;

jboolean tapeMedia(jlong handle)
{
  HANDLE dev = (HANDLE)handle;
  TAPE_GET_MEDIA_PARAMETERS params;
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_GET_MEDIA_PARAMS,
    nullptr,
    0,
    &params,
    sizeof(params),
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    tapeLastErrorValue = GetLastError();
    return JNI_FALSE;
  }
  tape_media_size = params.Capacity.QuadPart;
  tape_media_blocksize = params.BlockSize;
  tape_media_readonly = params.WriteProtected;
  return JNI_TRUE;
}

jlong tapeMediaSize()
{
  return tape_media_size;
}

jint tapeMediaBlockSize()
{
  return tape_media_blocksize;
}

jboolean tapeMediaReadOnly()
{
  return tape_media_readonly;
}

static jint tape_drive_def_blocksize;
static jint tape_drive_max_blocksize;
static jint tape_drive_min_blocksize;

jboolean tapeDrive(jlong handle)
{
  HANDLE dev = (HANDLE)handle;
  TAPE_GET_DRIVE_PARAMETERS params;
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_GET_DRIVE_PARAMS,
    nullptr,
    0,
    &params,
    sizeof(params),
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    tapeLastErrorValue = GetLastError();
    return JNI_FALSE;
  }
  tape_drive_def_blocksize = params.DefaultBlockSize;
  tape_drive_max_blocksize = params.MaximumBlockSize;
  tape_drive_min_blocksize = params.MinimumBlockSize;
  return JNI_TRUE;
}

jint tapeDriveMinBlockSize()
{
  return tape_drive_min_blocksize;
}

jint tapeDriveMaxBlockSize()
{
  return tape_drive_max_blocksize;
}

jint tapeDriveDefaultBlockSize()
{
  return tape_drive_def_blocksize;
}

jint tapeLastError()
{
  return tapeLastErrorValue;
}

extern "C" {
  JNIEXPORT jlong (*_tapeOpen)(const char*) = &tapeOpen;
  JNIEXPORT void (*_tapeClose)(jlong) = &tapeClose;
  JNIEXPORT jboolean (*_tapeFormat)(jlong, jint) = &tapeFormat;
  JNIEXPORT jint (*_tapeRead)(jlong,jbyte*,jint,jint) = &tapeRead;
  JNIEXPORT jint (*_tapeWrite)(jlong,jbyte*,jint,jint) = &tapeWrite;
  JNIEXPORT jboolean (*_tapeSetpos)(jlong,jlong) = &tapeSetpos;
  JNIEXPORT jlong (*_tapeGetpos)(jlong) = &tapeGetpos;
  JNIEXPORT jboolean (*_tapeMedia)(jlong) = &tapeMedia;
  JNIEXPORT jlong (*_tapeMediaSize)() = &tapeMediaSize;
  JNIEXPORT jint (*_tapeMediaBlockSize)() = &tapeMediaBlockSize;
  JNIEXPORT jboolean (*_tapeMediaReadOnly)() = &tapeMediaReadOnly;
  JNIEXPORT jboolean (*_tapeDrive)(jlong) = &tapeDrive;
  JNIEXPORT jint (*_tapeDriveMinBlockSize)() = &tapeDriveMinBlockSize;
  JNIEXPORT jint (*_tapeDriveMaxBlockSize)() = &tapeDriveMaxBlockSize;
  JNIEXPORT jint (*_tapeDriveDefaultBlockSize)() = &tapeDriveDefaultBlockSize;
  JNIEXPORT jint (*_tapeLastError)() = &tapeLastError;
}

//tape changer

jlong changerOpen(const char* name)
{
  HANDLE handle = CreateFileA(name, GENERIC_READ | GENERIC_WRITE, 0, NULL, OPEN_EXISTING, 0, NULL);
  if (handle == INVALID_HANDLE_VALUE) {
    tapeLastErrorValue = GetLastError();
    return 0;
  }
  return (jlong)handle;
}

void changerClose(jlong handle)
{
  CloseHandle((HANDLE)handle);
}

static int list_count;
static char* list_elements[32*4];

static int listType(HANDLE dev, _ELEMENT_TYPE type, const char* name) {
  CHANGER_READ_ELEMENT_STATUS request;
  DWORD bytesReturn;
  CHANGER_ELEMENT_STATUS_EX status;
  for(int idx=0;idx<32;idx++) {
    request.ElementList.Element.ElementType = type;
    request.ElementList.Element.ElementAddress = idx;
    request.ElementList.NumberOfElements = 1;
    request.VolumeTagInfo = TRUE;
    BOOL ret = DeviceIoControl(
      dev,
      IOCTL_CHANGER_GET_ELEMENT_STATUS,
      &request,
      sizeof(request),
      &status,
      sizeof(status),
      &bytesReturn,
      nullptr
    );
    if (ret == FALSE) break;
    if (status.ExceptionCode == ERROR_SLOT_NOT_PRESENT) break;
    BOOL hasTape = (status.Flags & ELEMENT_STATUS_FULL) != 0;
    BOOL noLabel = (status.ExceptionCode & ERROR_LABEL_UNREADABLE) != 0;
    const char* label = NULL;
    if (hasTape) {
      //trim barcode
      char *barcode = (char*)status.PrimaryVolumeID;
      for(int a=0;a<MAX_VOLUME_ID_SIZE;a++) {
        if (barcode[a] == ' ') barcode[a] = 0;
      }
      if (barcode[0] != 0) {
        label = barcode;
      } else {
        label = "<null>";
      }
    } else {
      label = "<empty>";
    }
    if (noLabel) {
      label = "<no label>";
    }
    list_elements[list_count] = (char*)malloc(128);
    std::sprintf(list_elements[list_count], "%s%d:%s", name, idx+1, label);
    list_count++;
  }
  return 0;
}

jstringArray changerList(jlong handle)
{
  HANDLE dev = (HANDLE)handle;

  list_count = 0;

  listType(dev, ELEMENT_TYPE::ChangerDrive, "drive");
  listType(dev, ELEMENT_TYPE::ChangerTransport, "transport");
  listType(dev, ELEMENT_TYPE::ChangerSlot, "slot");
  listType(dev, ELEMENT_TYPE::ChangerIEPort, "port");

  jstringArray ret = ffm->newStringArray(list_count);

  for(int i=0;i<list_count;i++) {
    ffm->setString(i,list_elements[i]);
    free(list_elements[i]);
  }

  return ret;
}

static BOOL startsWith(const char* str, const char* with) {
  while (*str && *with) {
    if (*str != *with) return FALSE;
    str++;
    with++;
  }
  if (*with == 0) return TRUE;
  return FALSE;
}

static BOOL isValidElement(const char* loc) {
  int len = -1;
  if (startsWith(loc, "drive")) len = 5;
  else if (startsWith(loc, "slot")) len = 4;
  else if (startsWith(loc, "port")) len = 4;
  else if (startsWith(loc, "transport")) len = 9;
  if (len == -1) return FALSE;
  loc += len;
  if (*loc < '1' || *loc > '9') return FALSE;
  loc++;
  while (*loc) {
    if (*loc < '0' || *loc > '9') return FALSE;
    loc++;
  }
  return TRUE;
}

static ELEMENT_TYPE getElementType(const char* loc) {
  if (startsWith(loc, "drive")) return ELEMENT_TYPE::ChangerDrive;
  if (startsWith(loc, "slot")) return ELEMENT_TYPE::ChangerSlot;
  if (startsWith(loc, "port")) return ELEMENT_TYPE::ChangerIEPort;
  if (startsWith(loc, "transport")) return ELEMENT_TYPE::ChangerTransport;
  return ELEMENT_TYPE::AllElements;
}

static int getElementAddress(const char* loc) {
  int len = -1;
  if (startsWith(loc, "drive")) len = 5;
  else if (startsWith(loc, "slot")) len = 4;
  else if (startsWith(loc, "port")) len = 4;
  else if (startsWith(loc, "transport")) len = 9;
  loc += len;
  int value = std::atoi(loc);
  if (value < 1 || value > 32) {
    printf("Error:location invalid");
    return -1;
  }
  return value - 1;  //return zero based
}

jboolean changerMove(jlong handle, const char* src, const char* transport, const char* dst)
{
  HANDLE dev = (HANDLE)handle;

  if (!isValidElement(src)) {
    printf("Error:src invalid\n");
    return JNI_FALSE;
  }
  if (getElementType(src) == ELEMENT_TYPE::ChangerTransport) {
    printf("Error:src can not be transport\n");
    return JNI_FALSE;
  }

  if (transport != nullptr) {
    if (!isValidElement(transport)) {
      printf("Error:transport invalid\n");
      return JNI_FALSE;
    }
    if (getElementType(transport) != ELEMENT_TYPE::ChangerTransport) {
      printf("Error:transport must be transport");
      return JNI_FALSE;
    }
  }

  if (!isValidElement(dst)) {
    printf("Error:dst invalid\n");
    return JNI_FALSE;
  }
  if (getElementType(dst) == ELEMENT_TYPE::ChangerTransport) {
    printf("Error:dst can not be transport\n");
    return JNI_FALSE;
  }

  CHANGER_MOVE_MEDIUM request;
  DWORD bytesReturn;

  if (transport == nullptr) {
    request.Transport.ElementType = ELEMENT_TYPE::ChangerTransport;
    request.Transport.ElementAddress = 0;
  } else {
    request.Transport.ElementType = getElementType(transport);
    request.Transport.ElementAddress = getElementAddress(transport);
  }

  request.Source.ElementType = getElementType(src);
  request.Source.ElementAddress = getElementAddress(src);

  request.Destination.ElementType = getElementType(dst);
  request.Destination.ElementAddress = getElementAddress(dst);

  request.Flip = FALSE;

  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_CHANGER_MOVE_MEDIUM,
    &request,
    sizeof(request),
    nullptr,
    0,
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    tapeLastErrorValue = GetLastError();
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

extern "C" {
  JNIEXPORT jlong (*_changerOpen)(const char*) = &changerOpen;
  JNIEXPORT void (*_changerClose)(jlong) = &changerClose;
  JNIEXPORT jstringArray (*_changerList)(jlong) = &changerList;
  JNIEXPORT jboolean (*_changerMove)(jlong,const char*,const char*,const char*) = &changerMove;

  JNIEXPORT jboolean JNICALL WindowsAPIinit() {return JNI_TRUE;}
}

/** tapetool - command line interface to control tapes and media changers.
  *
  * TODO : detect CLEANER_CARTRIDGE (see mediatype())
  *
  * @author Peter Quiring
  */

#define TTVersion "0.1"
#define TTBlockSize (64 * 1024)

#include <windows.h>
#include <ntddtape.h>

#include <cstdio>
#include <cstdlib>
#include <cstdint>
#include <cstring>

void usage() {
  printf("tapetool/%s\n", TTVersion);
  printf("Usage : tapetool device command [options]\n");
  printf("  Tape commands:\n");
//  printf("    FORMAT\n");
  printf("    WRITE {file}  (padded to 64k blocks)\n");
  printf("    READ {file} {size}\n");
  printf("    GETPOS\n");
  printf("    SETPOS {offset}  (offset = 64k blocks : 0 = rewind)\n");
  printf("    MEDIA  (display media parameters)\n");
//  printf("    MEDIATYPE  (display media type)\n");  //not working
//  printf("    STATUS  (display media status code)\n");  //not working
  printf("    DRIVE  (display drive parameters)\n");
  printf("  Library commands:\n");
  printf("    LIST  (list elements : drives, transports, slots, ports)\n");
  printf("    MOVE {src} [transport] {dst}\n");
//  printf(" Misc commands (device=*)\n");
//  printf("   QUERY  (raw dump of all system devices)\n");
  std::exit(0);
}

int query() {
  int size = 64 * 1024;
  char* list = (char*)malloc(size);
  int res = QueryDosDevice(nullptr, (LPSTR)list, size);
  char* dev = list;
  while (*dev) {
    printf("%s\n", dev);
    int len = strlen(dev);
    dev += len + 1;
  }
  return 0;
}

//IOCTL_CHANGER_EXCHANGE_MEDIUM = Changer API (low level)
//IOCTL_TAPE_CREATE_PARTITION = Tape API (low level)
//https://docs.microsoft.com/en-us/windows/win32/api/winbase/nf-winbase-createtapepartition (high level Tape API)

HANDLE openTape(const char* device) {
  char fulldevice[128];
  std::sprintf(fulldevice, "\\\\.\\%s", device);
  HANDLE dev = CreateFile(
    fulldevice,
    GENERIC_READ | GENERIC_WRITE,
    0,
    nullptr,
    OPEN_EXISTING,
    FILE_FLAG_NO_BUFFERING,
    nullptr
  );
  if (dev == INVALID_HANDLE_VALUE) {
    printf("Error:Unable to open device\n");
    std::exit(2);
  }
  return dev;
}

HANDLE openFileRead(const char* file) {
  HANDLE dev = CreateFile(
    file,
    GENERIC_READ,
    0,
    nullptr,
    OPEN_EXISTING,
    FILE_ATTRIBUTE_NORMAL,
    nullptr
  );
  if (dev == INVALID_HANDLE_VALUE) {
    printf("Error:Unable to open input file\n");
    std::exit(2);
  }
  return dev;
}

HANDLE openFileWrite(const char* file) {
  HANDLE dev = CreateFile(
    file,
    GENERIC_READ | GENERIC_WRITE,
    0,
    nullptr,
    CREATE_ALWAYS,
    FILE_ATTRIBUTE_NORMAL,
    nullptr
  );
  if (dev == INVALID_HANDLE_VALUE) {
    printf("Error:Unable to create output file\n");
    std::exit(2);
  }
  return dev;
}

void close(HANDLE dev) {
  CloseHandle(dev);
}

int read(HANDLE dev, void* buf, int size) {
  DWORD actualRead;
  int res = ReadFile(dev, buf, size, &actualRead, nullptr);
  if (actualRead != size) {
    printf("Error:read failed\n");
    std::exit(2);
  }
  return actualRead;
}

int write(HANDLE dev, void* buf, int size) {
  DWORD actualWrite;
  int res = WriteFile(dev, buf, size, &actualWrite, nullptr);
  if (actualWrite != size) {
    printf("Error:write failed\n");
    std::exit(2);
  }
  return actualWrite;
}

//formatting is typically not required
int format(const char* device) {
  printf("format:%s\n", device);
  HANDLE dev = openTape(device);
  TAPE_PREPARE prepare;
  prepare.Operation = TAPE_FORMAT;
  //other operations : TAPE_LOAD TAPE_UNLOAD TAPE_LOCK TAPE_UNLOCK
  prepare.Immediate = FALSE;
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_PREPARE,
    &prepare,
    sizeof(prepare),
    nullptr,
    0,
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    printf("Error:format failed\n");
    std::exit(2);
  }
  return 0;
}

int read(const char* device, const char* name, int64_t size) {
  printf("read:%s %s %lld\n", device, name, size);
  HANDLE dev = openTape(device);
  HANDLE file = openFileWrite(name);
  int64_t left = size;
  void* buf = malloc(TTBlockSize);
  while (left > 0) {
    int txSize = left > TTBlockSize ? TTBlockSize : left;
    read(dev, buf, TTBlockSize);
    write(file, buf, txSize);
    left -= txSize;
  }
  close(file);
  close(dev);
  return 0;
}

int write(const char* device, const char* name) {
  printf("write:%s %s\n", device, name);
  HANDLE dev = openTape(device);
  HANDLE file = openFileRead(name);
  int64_t size;
  GetFileSizeEx(file, (PLARGE_INTEGER)&size);
  int64_t left = size;
  char* buf = (char*)malloc(TTBlockSize);
  while (left > 0) {
    int readSize = left > TTBlockSize ? TTBlockSize : left;
    if (readSize != TTBlockSize) {
      //fill last block with zeros
      std::memset(buf + readSize, 0, TTBlockSize - readSize);
    }
    read(file, buf, readSize);
    write(dev, buf, TTBlockSize);
    left -= readSize;
  }
  close(file);
  close(dev);
  return 0;
}

int getpos(const char* device) {
  printf("getpos:%s\n", device);
  HANDLE dev = openTape(device);
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
    printf("Error:getpos failed\n");
    std::exit(2);
  }
  printf("type:%d\n", tapePos.Type);
  printf("partition:%d\n", tapePos.Partition);
  printf("position:%lld\n", tapePos.Offset.QuadPart);
  return 0;
}

int setpos(const char* device, int64_t pos) {
  printf("setpos:%s %lld\n", device, pos);
  HANDLE dev = openTape(device);
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
    printf("Error:setpos failed\n");
    std::exit(2);
  }
  return 0;
}

int media(const char* device) {
  printf("media:%s\n", device);
  HANDLE dev = openTape(device);
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
    printf("Error:media failed\n");
    std::exit(2);
  }
  printf("capacity:%lld\n", params.Capacity.QuadPart);
  printf("remaining:%lld\n", params.Remaining.QuadPart);
  printf("blocksize:%d\n", params.BlockSize);
  printf("partitioncount:%d\n", params.PartitionCount);
  printf("writeprotect:%s\n", params.WriteProtected ? "true" : "false");
  return 0;
}

int mediatype(const char* device) {
  printf("mediatype:%s\n", device);
  HANDLE dev = openTape(device);
  DISK_GEOMETRY params;
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_STORAGE_GET_MEDIA_TYPES,
    nullptr,
    0,
    &params,
    sizeof(params),
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    printf("Error:mediatype failed\n");
    std::exit(2);
  }
  printf("type:%d\n", params.MediaType);
  return 0;
}

int drive(const char* device) {
  printf("drive:%s\n", device);
  HANDLE dev = openTape(device);
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
    printf("Error:drive failed\n");
    std::exit(2);
  }
  printf("defaultblocksize:%d\n", params.DefaultBlockSize);
  printf("maximumblocksize:%d\n", params.MaximumBlockSize);
  printf("minimumblocksize:%d\n", params.MinimumBlockSize);
  return 0;
}

int status(const char* device) {
  printf("status:%s\n", device);
  HANDLE dev = openTape(device);
  DWORD bytesReturn;
  BOOL ret = DeviceIoControl(
    dev,
    IOCTL_TAPE_GET_STATUS,
    nullptr,
    0,
    nullptr,
    0,
    &bytesReturn,
    nullptr
  );
  if (ret != TRUE) {
    printf("Error:status failed\n");
    std::exit(2);
  }
  int status = GetLastError();
  printf("code:%d\n", status);
  return 0;
}

int listType(HANDLE dev, _ELEMENT_TYPE type, const char* name) {
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
    if (hasTape) {
      //trim barcode
      char *barcode = (char*)status.PrimaryVolumeID;
      for(int a=0;a<MAX_VOLUME_ID_SIZE;a++) {
        if (barcode[a] == ' ') barcode[a] = 0;
      }
    }
    printf("element:%s%d:%s\n", name, idx+1, (hasTape ? (const char*)status.PrimaryVolumeID : "<empty>"));
  }
  return 0;
}

int list(const char* device) {
  printf("list:%s\n", device);
  HANDLE dev = openTape(device);
  listType(dev, ELEMENT_TYPE::ChangerDrive, "drive");
  listType(dev, ELEMENT_TYPE::ChangerTransport, "transport");
  listType(dev, ELEMENT_TYPE::ChangerSlot, "slot");
  listType(dev, ELEMENT_TYPE::ChangerIEPort, "port");
  close(dev);
  return 0;
}

BOOL startsWith(const char* str, const char* with) {
  while (*str && *with) {
    if (*str != *with) return FALSE;
    str++;
    with++;
  }
  if (*with == 0) return TRUE;
  return FALSE;
}

BOOL isValidElement(const char* loc) {
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

ELEMENT_TYPE getElementType(const char* loc) {
  if (startsWith(loc, "drive")) return ELEMENT_TYPE::ChangerDrive;
  if (startsWith(loc, "slot")) return ELEMENT_TYPE::ChangerSlot;
  if (startsWith(loc, "port")) return ELEMENT_TYPE::ChangerIEPort;
  if (startsWith(loc, "transport")) return ELEMENT_TYPE::ChangerTransport;
  return ELEMENT_TYPE::AllElements;
}

int getElementAddress(const char* loc) {
  int len = -1;
  if (startsWith(loc, "drive")) len = 5;
  else if (startsWith(loc, "slot")) len = 4;
  else if (startsWith(loc, "port")) len = 4;
  else if (startsWith(loc, "transport")) len = 9;
  loc += len;
  int value = std::atoi(loc);
  if (value < 1 || value > 32) {
    printf("Error:location invalid");
    std::exit(1);
  }
  return value - 1;  //return zero based
}

int move(const char* device, const char* src, const char* transport, const char* dst) {
  printf("move:%s %s to %s\n", device, src, dst);

  if (!isValidElement(src)) {
    printf("Error:src invalid\n");
    std::exit(1);
  }
  if (getElementType(src) == ELEMENT_TYPE::ChangerTransport) {
    printf("Error:src can not be transport\n");
    std::exit(1);
  }

  if (transport != nullptr) {
    if (!isValidElement(transport)) {
      printf("Error:transport invalid\n");
      std::exit(1);
    }
    if (getElementType(transport) != ELEMENT_TYPE::ChangerTransport) {
      printf("Error:transport must be transport");
      std::exit(1);
    }
  }

  if (!isValidElement(dst)) {
    printf("Error:dst invalid\n");
    std::exit(1);
  }
  if (getElementType(dst) == ELEMENT_TYPE::ChangerTransport) {
    printf("Error:dst can not be transport\n");
    std::exit(1);
  }

  HANDLE dev = openTape(device);
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
    printf("Error:move failed\n");
    std::exit(2);
  }
  return 0;
}

int main(int argc, const char** argv) {
  argc--;  //ignore argv[0]
  if (argc < 2) {
    usage();
  }
  int code = 1;
  if (stricmp(argv[2], "FORMAT") == 0) {
    code = format(argv[1]);
  }
  else
  if (stricmp(argv[2], "READ") == 0) {
    if (argc != 4) usage();
    code = read(argv[1], argv[3], std::atoll(argv[4]));
  }
  else
  if (stricmp(argv[2], "WRITE") == 0) {
    if (argc != 3) usage();
    code = write(argv[1], argv[3]);
  }
  else
  if (stricmp(argv[2], "GETPOS") == 0) {
    code = getpos(argv[1]);
  }
  else
  if (stricmp(argv[2], "SETPOS") == 0) {
    if (argc != 3) usage();
    code = setpos(argv[1], std::atoll(argv[3]));
  }
  else
  if (stricmp(argv[2], "MEDIA") == 0) {
    code = media(argv[1]);
  }
  else
  if (stricmp(argv[2], "MEDIATYPE") == 0) {
    code = mediatype(argv[1]);
  }
  else
  if (stricmp(argv[2], "STATUS") == 0) {
    code = status(argv[1]);
  }
  else
  if (stricmp(argv[2], "DRIVE") == 0) {
    code = drive(argv[1]);
  }
  else
  if (stricmp(argv[2], "LIST") == 0) {
    code = list(argv[1]);
  }
  else
  if (stricmp(argv[2], "MOVE") == 0) {
    if (argc == 4) {
      code = move(argv[1], argv[3], nullptr, argv[4]);
    }
    else
    if (argc == 5) {
      code = move(argv[1], argv[3], argv[4], argv[5]);
    }
    else {
      usage();
    }
  }
  else
  if (stricmp(argv[2], "QUERY") == 0) {
    code = query();
  }
  else {
    usage();
  }
  return code;
}

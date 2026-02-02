/* pcap FFM */

jboolean pcapInit(char* lib1, char* lib2)
{
  char err[PCAP_ERRBUF_SIZE];

  if (lib1 != NULL) {
    //Windows only
    printf("Loading:%s\n", lib1);
    lib_packet = loadLibrary(lib1);
    if (lib_packet == NULL) {
      printf("Error:library not found:%s\n", lib1);
      return JNI_FALSE;
    }
  }

  printf("Loading:%s\n", lib2);
  library = loadLibrary(lib2);
  if (library == NULL) {
    printf("Error:library not found:%s\n", lib2);
    return JNI_FALSE;
  }

  getFunction(library, (void**)(&pcap_init), "pcap_init");
  getFunction(library, (void**)(&pcap_open_live), "pcap_open_live");
  getFunction(library, (void**)(&pcap_close), "pcap_close");
  getFunction(library, (void**)(&pcap_findalldevs), "pcap_findalldevs");
  getFunction(library, (void**)(&pcap_freealldevs), "pcap_freealldevs");
  getFunction(library, (void**)(&pcap_compile), "pcap_compile");
  getFunction(library, (void**)(&pcap_setfilter), "pcap_setfilter");
  getFunction(library, (void**)(&pcap_dispatch), "pcap_dispatch");
  getFunction(library, (void**)(&pcap_sendpacket), "pcap_sendpacket");
  getFunction(library, (void**)(&pcap_setnonblock), "pcap_setnonblock");

  if (pcap_open_live == NULL) {
    library = NULL;
    return JNI_FALSE;
  }

  if (pcap_init == NULL) {
    //older pcap version
    return JNI_TRUE;
  }

  int ret = (*pcap_init)(0, err);
  if (ret != 0) {
    printf("Error:pcap_init:%d:%s\n", ret, err);
    library = NULL;
    return JNI_FALSE;
  }

  return JNI_TRUE;
}

JFArray* pcapListLocalInterfaces()
{
  char err[PCAP_ERRBUF_SIZE];
  int list_count = 0;
  pcap_if_t *list_elements, *c;
  pcap_addr_t *addr;
  char name[256], ip[16];
  JFArray* array;

  if (library == NULL) return NULL;

  int ret = (*pcap_findalldevs)(&list_elements, err);
  if (ret != 0) {
    printf("Error:pcap_findalldevs:%d:%s\n", ret, err);
    return NULL;
  }
  if (list_elements == NULL) {
    printf("Error:No interfaces found\n");
    return NULL;
  }

  c = list_elements;
  while (c != NULL) {
    if ((c->flags & UP_RUNNING) == UP_RUNNING && c->addresses != NULL) {
      list_count++;
    }
    c = c->next;
  }

  array = JFArray::create(list_count, sizeof(char*), ARRAY_TYPE_STRING);

  c = list_elements;
  int idx = 0;
  while (c != NULL) {
    if ((c->flags & UP_RUNNING) == UP_RUNNING && c->addresses != NULL) {
      strcpy(name, c->name);
      addr = c->addresses;
      while (addr != NULL) {
        if (addr->addr->sa_family == AF_INET) {
          strcat(name, ",");
          sprintf(ip, "%d.%d.%d.%d", addr->addr->sa_data[2] & 0xff, addr->addr->sa_data[3] & 0xff, addr->addr->sa_data[4] & 0xff, addr->addr->sa_data[5] & 0xff);
          strcat(name, ip);
        } else {
          printf("Unknown sockaddr:%x\n", addr->addr->sa_family);
        }
        addr = addr->next;
      }
      char* str = (char*)malloc(strlen(name) + 1);
      strcpy(str, name);
      array->setString(idx++, str);
    }
    c = c->next;
  }

  (*pcap_freealldevs)(list_elements);

  return array;
}

jlong pcapStart(char* dev, jboolean nonblocking)
{
  if (library == NULL) return 0;
  char err[PCAP_ERRBUF_SIZE];

  jlong handle = (jlong)(*pcap_open_live)(dev, 100, 1, 10, err);

  if (handle == 0) {
    printf("Error:pcap_open_live:%s\n", err);
  } else {
    if (nonblocking) {
      //setup non-blocking mode
      int res = (*pcap_setnonblock)((pcap_t*)handle, 1, err);
      if (res == -1) {
        printf("Error:pcap_setnonblock:%s\n", err);
      }
    }
  }

  return handle;
}

void pcapStop(jlong handle)
{
  if (library == NULL) return;
  (*pcap_close)((pcap_t*)handle);
}

jboolean pcapCompile(jlong handle, char* program)
{
  int ret = (*pcap_compile)((pcap_t*)handle, &cap_program, program, 0, PCAP_NETMASK_UNKNOWN);

  if (ret != 0) {
    printf("Error:pcap_compile:%d\n", ret);
  } else {
    ret = (*pcap_setfilter)((pcap_t*)handle, &cap_program);
    if (ret != 0) {
      printf("Error:pcap_setfilter:%d\n", ret);
    }
  }

  return ret == 0;
}

JFArray* pcapRead(jlong handle)
{
  struct user_pkt_t user_pkt;
  user_pkt.size = 0;

  int cnt = (*pcap_dispatch)((pcap_t*)handle, 1, &cap_callback, &user_pkt);

  if (cnt > 0 && user_pkt.size > 0) {
    JFArray* ba = JFArray::create(user_pkt.size, 1, ARRAY_TYPE_BYTE);

    memcpy(ba->getBufferByte(), user_pkt.bytes, user_pkt.size);

    return ba;
  } else {
    return NULL;
  }
}

jboolean pcapWrite(jlong handle, jbyte* ba, jint offset, jint length)
{
  if (ba == NULL) return JNI_FALSE;
  jboolean isCopy;

  int ret = (*pcap_sendpacket)((pcap_t*)handle, ba + offset, length);
  if (ret != 0) {
    printf("Error:pcap_sendpacket:%d\n", ret);
  }

  return ret == 0;
}

extern "C" {
  JNIEXPORT jboolean (*_pcapInit)(char*,char*) = &pcapInit;
  JNIEXPORT JFArray* (*_pcapListLocalInterfaces)() = &pcapListLocalInterfaces;
  JNIEXPORT jlong (*_pcapStart)(char*,jboolean) = &pcapStart;
  JNIEXPORT void (*_pcapStop)(jlong) = &pcapStop;
  JNIEXPORT JFArray* (*_pcapRead)(jlong) = &pcapRead;
  JNIEXPORT jboolean (*_pcapWrite)(jlong,jbyte*,jint,jint) = &pcapWrite;

  JNIEXPORT jboolean PCapAPIinit() {return JNI_TRUE;}
}

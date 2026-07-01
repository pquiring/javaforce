//winPE resources

struct ICONENTRY {
  jbyte width, height, clrCnt, reserved;
  jshort planes, bitCnt;
  jint bytesInRes, imageOffset;
};
struct ICONHEADER {
  jshort res, type, count;
//  ICONENTRY entries[1];
};
struct GRPICONENTRY {
  jbyte width;
  jbyte height;
  jbyte colourCount;
  jbyte reserved;
  jbyte planes;
  jbyte bitCount;
  jshort bytesInRes;
  jshort bytesInRes2;
  jshort reserved2;
  jshort id;
};
struct GRPICONHEADER {
  jshort res, type, count;
//  GRPICONENTRY entries[1];
};
struct ICONIMAGE {  //actually a BITMAPINFO struct + xors_ands
  BITMAPINFOHEADER header;
//  RGBQUAD colors[];
//  byte xors_ands[];
};

#define EN_US 0x409  //MAKELANGID(LANG_ENGLISH, SUBLANG_ENGLISH_US)

jlong peBegin(const char* cstr)
{
  HANDLE handle = BeginUpdateResource(cstr, FALSE);
  if (handle == NULL) {
    printf("peBegin failed:file=%s:error=%d\n", cstr, GetLastError());
  }
  return (jlong)handle;
}

void peAddIcon(jlong handle, jbyte* baptr, int offset, int length)
{
  ICONHEADER i;
  ICONHEADER *ih;
    ICONENTRY *ihE;
  ICONIMAGE **ii;
    int *iiSize;
  GRPICONHEADER *grp;
    GRPICONENTRY *grpE;

  int ptr = 0;
  int size = sizeof(ICONHEADER);
  memcpy(&i, baptr, size);
  size = sizeof(ICONHEADER) + i.count * sizeof(ICONENTRY);
  ih = (ICONHEADER*)malloc(size);
  memcpy(ih, baptr, size);
  ihE = (ICONENTRY*)(((char*)ih) + sizeof(ICONHEADER));
  ii = (ICONIMAGE**)malloc(sizeof(ICONIMAGE*) * i.count);
  iiSize = (int*)malloc(sizeof(int) * i.count);
  for(int a=0;a<i.count;a++) {
    ptr = ihE[a].imageOffset;
    iiSize[a] = ihE[a].bytesInRes;
    ii[a] = (ICONIMAGE*)malloc(iiSize[a]);
    memcpy(ii[a], baptr + ptr, iiSize[a]);
  }
  size = sizeof(GRPICONHEADER) + sizeof(GRPICONENTRY) * i.count;
  grp = (GRPICONHEADER*)malloc(size);
  memset(grp, 0, size);
  grpE = (GRPICONENTRY*)(((char*)grp) + sizeof(GRPICONHEADER));
  grp->res = 0;
  grp->type = 1;
  grp->count = i.count;
  for(int a=0;a<i.count;a++) {
    ICONENTRY *ie = &ihE[a];
    GRPICONENTRY *ge = &grpE[a];
    ge->bitCount = 0;
    ge->bytesInRes = ie->bitCnt;
    ge->bytesInRes2 = (short)ie->bytesInRes;
    ge->colourCount = ie->clrCnt;
    ge->height = ie->height;
    ge->id = (short)(a+1);
    ge->planes = (byte)ie->planes;
    ge->reserved = ie->reserved;
    ge->width = ie->width;
    ge->reserved2 = 0;
  }

  UpdateResource((HANDLE)handle, (LPCSTR)RT_GROUP_ICON, (LPCSTR)1, EN_US, grp, sizeof(GRPICONHEADER) + sizeof(GRPICONENTRY) * i.count);

  for(int a=0;a<i.count;a++) {
    UpdateResource((HANDLE)handle, (LPCSTR)RT_ICON, (LPCSTR)(jlong)(a+1), EN_US, ii[a], iiSize[a]);
  }
}

void peAddString(jlong handle, jint type, jint idx, jbyte* baptr, int offset, int length)
{
  UpdateResource((HANDLE)handle, (LPCSTR)(jlong)type, (LPCSTR)(jlong)idx, EN_US, baptr + offset, length);
}

void peEnd(jlong handle)
{
  EndUpdateResource((HANDLE)handle, FALSE);
}

extern "C" {
  JNIEXPORT jlong (*_peBegin)(const char*) = &peBegin;
  JNIEXPORT void (*_peAddIcon)(jlong, jbyte*, int, int) = &peAddIcon;
  JNIEXPORT void (*_peAddString)(jlong, int, int, jbyte*, int, int) = &peAddString;
  JNIEXPORT void (*_peEnd)(jlong) = &peEnd;
}

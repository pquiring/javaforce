/*

  virDomainStatsRecord {
    void* dom;
    virTypedParameter* params;
    int nparams;
  }

  virTypedParameter {
    char field[VIR_TYPED_PARAM_FIELD_LENGTH];
    int type;
    union {i,ui,l,ul,d,b,s};
  }

  CPU_TOTAL = cpu.time  (use delta between two samples to calc percentage)

  BALLOON = balloon.current, balloon.maximum, balloon.available, balloon.rss (resident set size), balloon.usable, balloon.unused

  INTERFACE = net.count, net.<num>.rx/tx.bytes

  BLOCK = block.count, block.<num>.name, block.<num>.rd/wr.bytes,

*/

//grepish string compare : * = match all numeric chars
static bool str_cmp(char* field, char* grep) {
  while (*grep) {
    if (*field == 0) return false;
    if (*grep == '*') {
      while (*field >= '0' && *field <= '9') {
        field++;
      }
      grep++;
    } else {
      if (*field != *grep) return false;
      field++;
      grep++;
    }
  }
  if (*field != 0) return false;
  return true;
}

static void append_domain_stat(char* uuid, int year, int month, int day, int hour, int sample, const char* type, jlong v1, jlong v2, jlong v3) {
  char file[256];
  sprintf(file, "/var/jfkvm/stats/%s/%s-%04d-%02d-%02d-%02d.stat", uuid, type, year, month, day, hour);
  int fd = open(file, O_WRONLY | O_APPEND | O_CREAT, 0600);
  if (fd == -1) {
    printf("vm-stats:open() failed:file=%s\n", file);
    return;
  }
  struct {
    jlong sample;
    jlong v1, v2, v3;
  } r;
  r.sample = sample;
  r.v1 = v1;
  r.v2 = v2;
  r.v3 = v3;
  int len = 4 * sizeof(jlong);
  if (write(fd, &r, len) != len) {
    printf("vm-stats:write() failed:file=%s\n", file);
  }
  if (close(fd) != 0) {
    printf("vm-stats:close() failed:file=%s\n", file);
  }
}

JNIEXPORT jboolean JNICALL Java_javaforce_vm_VMHost_get_1all_1stats
  (JNIEnv *e, jclass o, jint year, jint month, jint day, jint hour, jint sample)
{
  void* conn = connect();
  if (conn == NULL) return JNI_FALSE;

  int b_stats = VIR_DOMAIN_STATS_CPU_TOTAL | VIR_DOMAIN_STATS_BALLOON | VIR_DOMAIN_STATS_INTERFACE | VIR_DOMAIN_STATS_BLOCK;
  virDomainStatsRecordPtr* stats;
  int b_flags = VIR_CONNECT_GET_ALL_DOMAINS_STATS_ACTIVE;
  int cnt = (*_virConnectGetAllDomainStats)(conn, b_stats, (void***)&stats, b_flags);

  if (cnt < 0) {
    disconnect(conn);
    return JNI_FALSE;
  }

  virDomainStatsRecordPtr* next;

  for (next = stats;*next;next++) {
    void* dom = (*next)->dom;
    char uuid[37];
    virTypedParameterPtr params = (*next)->params;
    int params_count = (*next)->nparams;
    jlong cpu_time = 0;
    jlong mem_max = 0, mem_used = 0;
    jlong net_read = 0, net_write = 0;
    jlong disk_read = 0, disk_write = 0, disk_latency = 0;
    for (int i=0;i<params_count;params++,i++) {
      virTypedParameterPtr param = params;
      char* field = (char*)&param->field;
      if (str_cmp(field, "cpu.time")) {
        cpu_time = param->value.l;
      }
      else if (str_cmp(field, "balloon.maximum")) {
        mem_max = param->value.l;
      }
      else if (str_cmp(field, "balloon.unused")) {
        mem_used = param->value.l;
      }
      else if (str_cmp(field, "net.*.rx.bytes")) {
        net_read += param->value.l;
      }
      else if (str_cmp(field, "net.*.tx.bytes")) {
        net_write += param->value.l;
      }
      else if (str_cmp(field, "block.*.rd.bytes")) {
        disk_read += param->value.l;
      }
      else if (str_cmp(field, "block.*.wr.bytes")) {
        disk_write += param->value.l;
      }
    }
    (*_virDomainGetUUIDString)(dom, uuid);
    append_domain_stat(uuid, year, month, day, hour, sample, "cpu", cpu_time, 0, 0);
    append_domain_stat(uuid, year, month, day, hour, sample, "mem", mem_max, mem_used, 0);
    append_domain_stat(uuid, year, month, day, hour, sample, "net", net_read, net_write, 0);
    append_domain_stat(uuid, year, month, day, hour, sample, "dsk", disk_read, disk_write, disk_latency);
  }

  (*_virDomainStatsRecordListFree)((void**)stats);

  disconnect(conn);

  return JNI_TRUE;
}

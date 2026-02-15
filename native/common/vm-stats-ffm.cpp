jboolean vmGetAllStats(jint year, jint month, jint day, jint hour, jint sample)
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

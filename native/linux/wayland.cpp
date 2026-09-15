struct WLSurfaceDescr {
  //see src/java.desktop/unix/native/libawt_wlawt/WLSurface.c
  void* wl_surface;
  void* wl_view;
  //...
};

static jlong getWaylandID(JNIEnv *e, jobject c) {
  JAWT_DrawingSurface* ds;
  JAWT_DrawingSurfaceInfo* dsi;
  jint lock;
  JAWT awt;

  if (jawt == NULL) return 0;
  if (_JAWT_GetAWT == NULL) return 0;

  awt.version = JAWT_VERSION_1_4;
  if (!(*_JAWT_GetAWT)(e, &awt)) {
    printf("JAWT_GetAWT() failed\n");
    return 0;
  }

  ds = awt.GetDrawingSurface(e, c);
  if (ds == NULL) {
    printf("JAWT.GetDrawingSurface() failed\n");
    return 0;
  }
  printf("ds=%p\n", ds);
  lock = ds->Lock(ds);
  if ((lock & JAWT_LOCK_ERROR) != 0) {
    awt.FreeDrawingSurface(ds);
    printf("JAWT.Lock() failed:0x%x\n", lock);
    return 0;
  }
  dsi = ds->GetDrawingSurfaceInfo(ds);
  if (dsi == NULL) {
    printf("JAWT.GetDrawingSurfaceInfo() failed\n");
    return 0;
  }
  printf("dsi=%p\n", dsi);
  WLSurfaceDescr * wldsi = (WLSurfaceDescr*)dsi->platformInfo;
  printf("dsi.pi=%p\n", wldsi);
  if (wldsi == NULL) {
    printf("JAWT.platformInfo == NULL\n");
    return 0;
  }
  jlong handle = (jlong)wldsi->wl_surface;
  ds->FreeDrawingSurfaceInfo(dsi);
  ds->Unlock(ds);
  awt.FreeDrawingSurface(ds);

  return handle;
}

jlong wl_get_id(jobject window) {
  return getWaylandID(get_jnienv(), window);
}

void* wl_server = NULL;
void* wl_client = NULL;
void* wl_roots = NULL;

extern "C" {
  JNIEXPORT jlong (*_wl_get_id)(jobject window);

  JNIEXPORT void* _wl_display_create;
  JNIEXPORT void* _wl_event_loop_create;
  JNIEXPORT void* _wl_event_loop_add_signal;
  JNIEXPORT void* _wl_display_get_event_loop;
  JNIEXPORT void* _wl_display_add_socket_auto;
  JNIEXPORT void* _wl_display_run;
  JNIEXPORT void* _wl_display_destroy;

  JNIEXPORT void* _wl_display_connect;
  JNIEXPORT void* _wl_display_connect_to_fd;

  JNIEXPORT void* _wlr_session_create;
  JNIEXPORT void* _wlr_fixes_create;
  JNIEXPORT void* _wlr_backend_autocreate;
  JNIEXPORT void* _wlr_backend_start;
  JNIEXPORT void* _wlr_renderer_autocreate;
  JNIEXPORT void* _wlr_compositor_create;
  JNIEXPORT void* _wlr_xwayland_create;
  JNIEXPORT void* _wlr_xwayland_destroy;
  JNIEXPORT void* _wlr_backend_destroy;

  JNIEXPORT bool JNICALL WaylandAPIinit(const char* libwayland_server, const char* libwayland_client, const char* libwlroots) {
    _wl_get_id = &wl_get_id;

    if (wl_server == NULL && libwayland_server != NULL) {
      wl_server = dlopen(libwayland_server, RTLD_LAZY | RTLD_GLOBAL);
      if (wl_server == NULL) {
        printf("Warning:dlopen(wayland_server.so) unsuccessful\n");
      } else {
        getFunction(wl_server, (void**)&_wl_display_create, "wl_display_create");
        getFunction(wl_server, (void**)&_wl_event_loop_create, "wl_event_loop_create");
        getFunction(wl_server, (void**)&_wl_event_loop_add_signal, "wl_event_loop_add_signal");
        getFunction(wl_server, (void**)&_wl_display_get_event_loop, "wl_display_get_event_loop");
        getFunction(wl_server, (void**)&_wl_display_add_socket_auto, "wl_display_add_socket_auto");
        getFunction(wl_server, (void**)&_wl_display_run, "wl_display_run");
        getFunction(wl_server, (void**)&_wl_display_destroy, "wl_display_destroy");

      }
    }
    if (wl_client == NULL && libwayland_client != NULL) {
      wl_client = dlopen(libwayland_client, RTLD_LAZY | RTLD_GLOBAL);
      if (wl_client == NULL) {
        printf("Warning:dlopen(wayland_client.so) unsuccessful\n");
      } else {
        getFunction(wl_client, (void**)&_wl_display_connect, "wl_display_connect");
        getFunction(wl_client, (void**)&_wl_display_connect_to_fd, "wl_display_connect_to_fd");
      }
    }
    if (wl_roots == NULL && libwlroots != NULL) {
      wl_roots = dlopen(libwlroots, RTLD_LAZY | RTLD_GLOBAL);
      if (wl_roots == NULL) {
        printf("Warning:dlopen(wlroots.so) unsuccessful\n");
      } else {
        getFunction(wl_roots, (void**)&_wlr_session_create, "wlr_session_create");
        getFunction(wl_roots, (void**)&_wlr_fixes_create, "wlr_fixes_create");
        getFunction(wl_roots, (void**)&_wlr_backend_autocreate, "wlr_backend_autocreate");
        getFunction(wl_roots, (void**)&_wlr_backend_start, "wlr_backend_start");
        getFunction(wl_roots, (void**)&_wlr_renderer_autocreate, "wlr_renderer_autocreate");
        getFunction(wl_roots, (void**)&_wlr_compositor_create, "wlr_compositor_create");
        getFunction(wl_roots, (void**)&_wlr_xwayland_create, "wlr_xwayland_create");
        getFunction(wl_roots, (void**)&_wlr_xwayland_destroy, "wlr_xwayland_destroy");
        getFunction(wl_roots, (void**)&_wlr_backend_destroy, "wlr_backend_destroy");
      }
    }
    return JNI_TRUE;
  }
}

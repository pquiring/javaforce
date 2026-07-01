//impersonate user

jboolean impersonateUser(const char* domain, const char* user, const char* passwd)
{
  HANDLE token;
  int ok;

  ok = LogonUser(user, domain, passwd, LOGON32_LOGON_INTERACTIVE, LOGON32_PROVIDER_DEFAULT, &token);
  if (!ok) return JNI_FALSE;
  ok = ImpersonateLoggedOnUser(token);
  if (!ok) {
    CloseHandle(token);
  }
  return ok ? JNI_TRUE : JNI_FALSE;
}

jboolean revertToSelf()
{
  return RevertToSelf();
}

static bool EnablePrivilege(LPCSTR privName)
{
  HANDLE hToken;
  TOKEN_PRIVILEGES tp;
  LUID luid;

  if (!OpenProcessToken(GetCurrentProcess(), TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &hToken))
  {
    printf("OpenProcessToken failed. Error: 0x%x\n", GetLastError());
    return false;
  }

  if (!LookupPrivilegeValueA(NULL, privName, &luid))
  {
    printf("LookupPrivilegeValue failed. Error: 0x%x\n", GetLastError());
    CloseHandle(hToken);
    return false;
  }

  tp.PrivilegeCount = 1;
  tp.Privileges[0].Luid = luid;
  tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;

  if (!AdjustTokenPrivileges(hToken, FALSE, &tp, sizeof(TOKEN_PRIVILEGES), NULL, NULL))
  {
    printf("AdjustTokenPrivileges failed. Error: 0x%x\n",GetLastError());
    CloseHandle(hToken);
    return false;
  }

  if (GetLastError() == ERROR_NOT_ALL_ASSIGNED)
  {
    printf("Privilege %s not assigned to process\n", privName);
    CloseHandle(hToken);
    return false;
  }

  CloseHandle(hToken);
  return true;
}

#define FLAG_LIMIT 1
#define FLAG_ELEVATE 2

jboolean createProcessAsUser(const char* domain, const char* user, const char* passwd, const char* app, const char* cmdline, jint flags)
{
  int ok;

  //Error 0x57 (87) : ERROR_INVALID_PARAMETER
  //Error 0x522 (1314) : ERROR_PRIVILEGE_NOT_HELD
  //Error 0x52E (1326) : ERROR_LOGON_FAILURE
  //Error 0x542 (1346) : ERROR_BAD_IMPERSONATION_LEVEL
  //Error 0x569 (1385) : ERROR_LOGON_TYPE_NOT_GRANTED

  PROCESS_INFORMATION pi;
  STARTUPINFOW si;
  memset(&si, 0, sizeof(STARTUPINFOW));
  si.cb = sizeof(STARTUPINFOW);
  //si.lpDesktop = L"winsta0\\default";

  HANDLE hToken;
  HANDLE hNewToken;
  HANDLE hRestricted;
  SAFER_LEVEL_HANDLE hSafer;
  LPVOID pEnv;
  DWORD dup = TOKEN_ASSIGN_PRIMARY | TOKEN_DUPLICATE | TOKEN_QUERY | TOKEN_ADJUST_DEFAULT | TOKEN_ADJUST_SESSIONID;

  TOKEN_ELEVATION_TYPE tet;
  TOKEN_LINKED_TOKEN tlt;
  DWORD needed = 0;

  if (false) {
    EnablePrivilege(SE_ASSIGNPRIMARYTOKEN_NAME);
    EnablePrivilege(SE_INCREASE_QUOTA_NAME);
    EnablePrivilege(SE_IMPERSONATE_NAME);
  }

  ok = LogonUserW((LPCWSTR)user, (LPCWSTR)domain, (LPCWSTR)passwd, LOGON32_LOGON_INTERACTIVE, LOGON32_PROVIDER_WINNT50, &hToken);
  if (!ok) {
    printf("LogonUserW Failed:0x%x\n", GetLastError());
  }

  ok = DuplicateTokenEx(hToken, dup, NULL, SecurityIdentification, TokenPrimary, &hNewToken);
  if (!ok) {
    printf("DuplicateTokenEx(1) Failed:0x%x\n", GetLastError());
  } else {
    hToken = hNewToken;
  }

  if (false) {
    int level = SECURITY_MANDATORY_LOW_RID;
    ok = SetTokenInformation(hNewToken, TokenIntegrityLevel, &level, sizeof(DWORD));
    if (ok != 0) {
      printf("SetTokenInformation Failed:0x%x\n", GetLastError());
    }

    if (!AdjustTokenPrivileges(hNewToken, TRUE, NULL, 0, NULL, NULL))
    {
      printf("AdjustTokenPrivileges Failed:0x%x\n",GetLastError());
    }
  }

  if (flags & FLAG_LIMIT) {
    printf("Limiting access...\n");
    if (!SaferCreateLevel(SAFER_SCOPEID_USER, SAFER_LEVELID_NORMALUSER, SAFER_LEVEL_OPEN, &hSafer, NULL)) {
      printf("SaferCreateLevel Failed:0x%x\n",GetLastError());
    }

    if (!SaferComputeTokenFromLevel(hSafer, hToken, &hRestricted, 0, NULL)) {
      printf("SaferComputeTokenFromLevel Failed:0x%x\n",GetLastError());
    } else {
      hToken = hRestricted;
    }

    ok = DuplicateTokenEx(hToken, dup, NULL, SecurityIdentification, TokenPrimary, &hNewToken);
    if (!ok) {
      printf("DuplicateTokenEx(2) Failed:0x%x\n", GetLastError());
    } else {
      hToken = hNewToken;
    }
  }


  if (flags & FLAG_ELEVATE) {
    printf("Elevating access...\n");
    if (!GetTokenInformation(hToken, TokenElevationType, (LPVOID)&tet, sizeof(tet), &needed)) {
      printf("GetTokenInformation(TokenElevationType) Failed:0x%x\n", GetLastError());
    }

    if (!GetTokenInformation(hToken, TokenLinkedToken, (LPVOID)&tlt, sizeof(tlt), &needed)) {
      printf("GetTokenInformation(TokenLinkedToken) Failed:0x%x\n", GetLastError());
    } else {
      hToken = tlt.LinkedToken;
    }

    if (false) {
      ok = DuplicateTokenEx(hToken, dup, NULL, SecurityIdentification, TokenPrimary, &hNewToken);
      if (!ok) {
        printf("DuplicateTokenEx(3) Failed:0x%x\n", GetLastError());
      } else {
        hToken = hNewToken;
      }
    }
  }

  if (true) {
    ok = ImpersonateLoggedOnUser(hToken);
    if (!ok) {
      printf("ImpersonateLoggedOnUser Failed:0x%x\n", GetLastError());
    }
  }

  if (true) {
    ok = DuplicateTokenEx(hToken, dup, NULL, SecurityIdentification, TokenPrimary, &hNewToken);
    if (!ok) {
      printf("DuplicateTokenEx(1) Failed:0x%x\n", GetLastError());
    } else {
      hToken = hNewToken;
    }
  }

  ok = CreateEnvironmentBlock(&pEnv, hToken, true);
  if (!ok) {
    printf("CreateEnvironmentBlock Failed:0x%x\n", GetLastError());
  }

  if (false) {
    ok = CreateRestrictedToken(hToken, LUA_TOKEN, 0, NULL, 0, NULL, 0, NULL, &hRestricted);
    if (!ok) {
      printf("CreateRestrictedToken Failed:0x%x\n", GetLastError());
    } else {
      hToken = hRestricted;
    }
  }

  if (true) {
    RevertToSelf();
    ok = CreateProcessWithLogonW((LPCWSTR)user, (LPCWSTR)domain, (LPCWSTR)passwd, LOGON_WITH_PROFILE, (LPCWSTR)app, (LPWSTR)cmdline, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (!ok) {
      printf("CreateProcessWithLogonW Failed:0x%x\n", GetLastError());
    }
  }

  if (false) {
    //0x522
    RevertToSelf();
    ok = CreateProcessWithTokenW(hToken, LOGON_WITH_PROFILE, (LPCWSTR)app, (LPWSTR)cmdline, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (!ok) {
      printf("CreateProcessWithTokenW Failed:0x%x\n", GetLastError());
    }
  }

  if (false) {
    //0x522
    RevertToSelf();
    ok = CreateProcessAsUserW(hToken, (LPCWSTR)app, (LPWSTR)cmdline, NULL, NULL, false, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (!ok) {
      printf("CreateProcessAsUserW Failed:0x%x\n", GetLastError());
    }
  }

  if (false) {
    //user profile not fully loaded
    ok = CreateProcessW((LPCWSTR)app, (LPWSTR)cmdline, NULL, NULL, false, CREATE_UNICODE_ENVIRONMENT | CREATE_NEW_CONSOLE, pEnv, NULL, &si, &pi);
    if (ok == 0) {
      printf("CreateProcessW Failed:0x%x\n", GetLastError());
    }
  }

  if (flags & FLAG_LIMIT) {
    SaferCloseLevel(hSafer);
  }

  RevertToSelf();

  return ok ? JNI_TRUE : JNI_FALSE;
}

jboolean shellExecute(const char* op, const char* app, const char* args)
{
  bool ok = ShellExecuteW(NULL, (LPCWSTR)op, (LPCWSTR)app, (LPCWSTR)args, NULL, SW_NORMAL);

  return ok;
}

extern "C" {
  JNIEXPORT jboolean (*_impersonateUser)(const char*, const char*, const char*) = &impersonateUser;
  JNIEXPORT jboolean (*_revertToSelf)() = &revertToSelf;
  JNIEXPORT jboolean (*_createProcessAsUser)(const char*, const char*, const char*, const char*, const char*, jint) = &createProcessAsUser;
  JNIEXPORT jboolean (*_shellExecute)(const char*, const char*, const char*) = &shellExecute;
}

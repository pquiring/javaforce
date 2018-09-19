package jpbx.core;

/** Interface plugins must implement. */

public interface Plugin {
  public void init(PBXAPI api);
  public void uninit(PBXAPI api);
  public void install(PBXAPI api);
  public void uninstall(PBXAPI api);
}

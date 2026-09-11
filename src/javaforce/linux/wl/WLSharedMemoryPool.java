package javaforce.linux.wl;

import java.lang.reflect.*;

import javaforce.*;

/** wl_shm_pool object.
 *
 * @author pquiring
 */

public class WLSharedMemoryPool extends WLObject {
  @SuppressWarnings("unchecked")
  public WLSharedMemoryPool(WLClient client, int id) {
    super(client, id);
    Class cls = getClass();
    try {
      events = new Method[] {
      };
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public String getName() {
    return "wl_shm_pool";
  }

  //requests

  public void create_buffer(int new_id, int offset, int width, int height, int stride, int format) {
    invokeRequest(id, 0, new_id, offset, width, height, stride, format);
  }

  public void destroy() {
    invokeRequest(id, 1);
  }

  public void resize(int new_size) {
    invokeRequest(id, 2, new_size);
  }

  //events
}

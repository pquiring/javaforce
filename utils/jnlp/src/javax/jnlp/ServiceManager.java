

package javax.jnlp;

import java.util.*;


public final class ServiceManager {

  private static ServiceManagerStub stub = null;

  private static Map lookupTable = new HashMap(); // ensure lookup is idempotent

  private ServiceManager() {
    // says it can't be instantiated
  }


  @SuppressWarnings("unchecked")
  public static java.lang.Object lookup(java.lang.String name) throws UnavailableServiceException {
    if (stub == null)
      throw new UnavailableServiceException("service stub not set.");

    synchronized(lookupTable) {
      Object result = lookupTable.get(name);

      if (result == null) {
        result = stub.lookup(name);
        if (result != null)
          lookupTable.put(name, result);
      }

      if (result == null)
        throw new UnavailableServiceException("service not available (stub returned null).");

      return result;
    }
  }

  public static java.lang.String[] getServiceNames() {
    // should this return the required ones even though no stub??
    if (stub == null)
      return new String[0];

    return stub.getServiceNames();
  }

  public static void setServiceManagerStub(ServiceManagerStub stub) {
    if (ServiceManager.stub == null)
      ServiceManager.stub = stub;
  }

}


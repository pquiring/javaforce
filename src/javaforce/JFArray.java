package javaforce;

/** JFArray : base class for all JFArray<type>
 *
 * @author pquiring
 */

import javaforce.jni.*;

public abstract class JFArray<T> {
  public long pointer;
  public Object pin;

  public abstract T getBuffer();

  public void finalize() {
    System.out.println("finalize");
    releasePointer();
  }

  protected void obtainPointer() {
    if (JF.useGraal) {
      pin = JFNative.createPinnedObject(getBuffer());
      pointer = JFNative.getPinnedObjectPointer(pin);
    } else {
      pointer = JFNative.getPointer(getBuffer());
    }
  }

  protected void releasePointer() {
    if (!JF.useGraal) {
      JFNative.freePointer(getBuffer(), pointer);
    }
  }

  protected void updatePointer() {
    releasePointer();
    obtainPointer();
  }

  public long getPointer() {
    return pointer;
  }
}

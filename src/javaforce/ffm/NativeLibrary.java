package javaforce.ffm;

import java.lang.annotation.*;

/** Native Library annotation
 *
 * For Linux only
 *
 * @author pquiring
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NativeLibrary {
  String value();
}

package javaforce.ffm;

/** JNI2FFM annotation.
 *
 * Indicates native function is critical.
 *
 * See Linker.Option.critical()
 *
 * @author pquiring
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Critical {}

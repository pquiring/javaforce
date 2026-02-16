package javaforce.ffm;

/** JNI2FFM annotation.
 *
 * Indicates arguments do not need to be copied back.
 *
 * @author pquiring
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface NoCopyBack {}

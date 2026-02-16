package javaforce.ffm;

/** JNI2FFM annotation.
 *
 * Indicates returned String must not be freed.
 *
 * @author pquiring
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface NoFreeString {}

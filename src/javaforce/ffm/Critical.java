package javaforce.ffm;

/** JNI2FFM annotation.
 *
 * Indicates native function is critical.
 *
 * See Linker.Option.critical()
 *
 * NOTE : This currently has several problems:
 *   - FFM critical downcall method can NOT use upcalls (results in JVM crash)
 *   - returned arrays thru JFArray still need to be copied (slow performance)
 *   - attempts to use JNI in FFM context failed
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

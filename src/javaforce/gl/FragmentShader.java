package javaforce.gl;

/**
 * Reference Fragment Shader
 *
 * @author pquiring
 *
 */

/*
 * uniform = set with glUniform...()
 * in(formerly varying) = passed from vertex shader to fragment shader (shared memory)
 */

public class FragmentShader {
  public static String source =
"varying vec2 vTextureCoord1;\n" +
"varying vec2 vTextureCoord2;\n" +
"varying float vLength;\n" +
"\n" +
"uniform sampler2D uSampler1;\n" +
"uniform sampler2D uSampler2;\n" +
"uniform int uUVMaps;\n" +
"\n" +
"void main() {\n" +
"  vec4 textureColor1 = texture2D(uSampler1, vTextureCoord1);\n" +
"  if (textureColor1.a == 0.0) discard;\n" +
"  if (uUVMaps > 1) {" +
"    vec4 textureColor2 = texture2D(uSampler2, vTextureCoord2);\n" +
"    if (textureColor2.a != 0.0) {\n" +
"      textureColor1 = textureColor2;\n" +  //or you could blend the colors with "+="
"    }\n" +
"  }" +
"  gl_FragColor = textureColor1;\n" +
"}\n";
}

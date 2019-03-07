package javaforce;

/** JFCompiler
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import javax.lang.model.element.Element;
import javax.lang.model.util.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.*;
import com.sun.source.tree.*;
import javax.lang.model.element.ModuleElement;

public class JFCompiler {
  public static void usage() {
    System.out.println("Usage:JFCompiler compile|parse|analyze inFolder [outFolder]");
    System.out.println("  outFolder for compile only");
    System.exit(0);
  }
  public static void main(String args[]) {
    if (args.length < 2) {
      usage();
    }
    switch (args[0]) {
      case "compile": if (args.length < 3) usage(); compile(args[1], args[2]); break;
      case "parse": parse(args[1], new TScanner<Void, Void>()); break;
      case "analyze": analyze(args[1], new EScanner<Void, Void>()); break;
    }
  }
  private static File[] getFiles(String path) {
    ArrayList<File> files = new ArrayList<File>();
    files = new ArrayList<File>();
    addFolder(new File(path), files);
    return files.toArray(new File[0]);
  }
  private static void addFolder(File folder, ArrayList<File> files) {
    File list[] = folder.listFiles();
    for(int a=0;a<list.length;a++) {
      if (list[a].isDirectory()) {
        addFolder(list[a], files);
      } else {
        files.add(list[a]);
      }
    }
  }
  public static boolean compile(String inFolder, String outFolder) {
    System.out.println("Compiling:" + inFolder);
    try {
      File files[] = getFiles(inFolder);
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
      Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files));
      Iterable<String> options = Arrays.asList(new String[] {
        "-cp", System.getProperty("java.class.path"),
        "-d", outFolder
      });
      JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, compilationUnits);

      return task.call();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  /** Parses inFolder and generates tree. */
  @SuppressWarnings("unchecked")
  public static boolean parse(String inFolder, TreeScanner scanner) {
    System.out.println("Parsing:" + inFolder);
    try {
      File files[] = getFiles(inFolder);
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
      Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files));
      Iterable<String> options = Arrays.asList(new String[] {
        "-cp", System.getProperty("java.class.path")
      });
      JavacTask task = (JavacTask)compiler.getTask(null, fileManager, null, options, null, compilationUnits);
      Iterable<? extends CompilationUnitTree> tree = task.parse();
      System.out.println("Trees:");
      tree.forEach((node) -> {
        node.accept(scanner, null);
      });
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  @SuppressWarnings("unchecked")
  public static boolean analyze(String inFolder, ElementScanner9 scanner) {
    System.out.println("Analyzing:" + inFolder);
    try {
      File files[] = getFiles(inFolder);
      JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
      StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
      Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files));
      Iterable<String> options = Arrays.asList(new String[] {
        "-cp", System.getProperty("java.class.path")
      });
      JavacTask task = (JavacTask)compiler.getTask(null, fileManager, null, options, null, compilationUnits);
      Iterable<? extends Element> elements = task.analyze();
      System.out.println("Elements:");
      elements.forEach((node) -> {
        node.accept(scanner, null);
      });
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }
  public static class TScanner<R, P> extends TreeScanner<R, P> {
    public R visitCompilationUnit(CompilationUnitTree node, P p) {
      System.out.println("unit:" + node.getSourceFile());
      return super.visitCompilationUnit(node, p);
    }
    public R visitPackage(PackageTree node, P p) {
      System.out.println("package:" + node.getPackageName());
      return super.visitPackage(node, p);
    }
    public R visitImport(ImportTree node, P p) {
      System.out.println("import:" + node.getQualifiedIdentifier());
      return super.visitImport(node, p);
    }
    public R visitClass(ClassTree node, P p) {
      System.out.println("class:" + node.getSimpleName());
      return super.visitClass(node, p);
    }
    public R visitMethod(MethodTree node, P p) {
      System.out.println("method:" + node.getName());
      return super.visitMethod(node, p);
    }
    //...
  }
  public static class EScanner<R, P> extends ElementScanner9<R, P> {
    public R visitModule(ModuleElement e, P p) {
      System.out.println("module:" + e.getSimpleName());
      return super.visitModule(e, p);
    }
  }
}

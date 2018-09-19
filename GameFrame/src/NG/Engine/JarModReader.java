package NG.Engine;

import NG.Mods.Mod;
import NG.Tools.Directory;
import NG.Tools.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Geert van Ieperen. Created on 19-9-2018.
 */
public class JarModReader {

    /**
     * Scans a JAR file for .class-files and load all classes found. Return a list of loaded classes
     * @param file   JAR-file which should be searched for .class-files
     * @param loader
     * @return Returns all found class-files with their full-name as a List of Strings
     * @throws IOException              If during processing of the Jar-file an error occurred
     * @throws IllegalArgumentException If either the provided file is null, does not exist or is no Jar file
     */
    private static List<Class<?>> loadClassesFromJar(File file, ClassLoader loader) throws IOException, IllegalArgumentException, ClassNotFoundException {
        if (file == null || !file.exists())
            throw new IllegalArgumentException("Invalid filename: " + file);

        if (!file.getName().endsWith(".jar")) {
            throw new IllegalArgumentException("Provided file was not a jar file: " + file);
        }

        // get a classloader and load all provided classes
        List<Class<?>> implementations = new ArrayList<>();
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> entries = jarFile.entries();

            while (entries.hasMoreElements()) {
                String fileName = entries.nextElement().getName();
                if (!fileName.endsWith(".class")) continue;

                // load all classes
                Class<?> aClass = loadClass(loader, fileName);
                implementations.add(aClass);
            }
        }

        return implementations;
    }

    /**
     * loads a class given by filename.
     * @param loader   a classloader, or null for the Booststrap loader
     * @param fileName a file pointing to a .class file
     * @return the loaded class
     * @throws ClassNotFoundException if the class cannot be located
     */
    private static Class<?> loadClass(ClassLoader loader, String fileName) throws ClassNotFoundException {
        String classFile = fileName.substring(0, fileName.lastIndexOf(".class"));

        if (classFile.contains("/"))
            classFile = classFile.replaceAll("/", ".");
        if (classFile.contains("\\"))
            classFile = classFile.replaceAll("\\\\", ".");

        Class<?> clazz;
        // now try to load the class
        if (loader == null)
            clazz = Class.forName(classFile);
        else
            clazz = Class.forName(classFile, true, loader);

        Logger.DEBUG.print("Loaded class " + classFile);
        return clazz;
    }

    /**
     * loads a jarfile, returning all {@link Mod} instances
     * @param jarFile the jarfile to search
     * @param loader  a class loader that has access to the classes in the file
     * @return a list of the mods
     * @throws IOException                  if the jar could not be properly loaded
     * @throws ReflectiveOperationException if the Classloader is nat able to load the classes in this file, or if the
     *                                      mod does not have a constructor with one Game parameter
     * @throws IllegalArgumentException     if the provided file does not exist or is not a jar file
     */
    public static List<Mod> loadMods(File jarFile, ClassLoader loader) throws IOException, ReflectiveOperationException, IllegalArgumentException {
        if (jarFile == null || !jarFile.exists()) {
            throw new IllegalArgumentException("Invalid jar file provided");
        }
        Logger.DEBUG.print("Loading mods from of file " + jarFile);

        List<Class<?>> classes = loadClassesFromJar(jarFile, loader);

        List<Mod> list = new ArrayList<>(classes.size());
        for (Class<?> c : classes) {
            // check if the class implements the provided interface
            if (Mod.class.isAssignableFrom(c)) {
                Constructor<?> constructor = c.getConstructor();
                Mod inst = (Mod) constructor.newInstance();
                // also verifies correct loading of class
                Logger.INFO.print("Loaded mod " + inst.getName());
                list.add(inst);
            }
        }

        Logger.DEBUG.print(list.size() + " mods found");
        return list;
    }

    /**
     * loads all classes in all jars in the given directory, collecting all implementations of {@link Mod}. For each mod
     * one instance is created. To start them, the {@link Mod#init(Game)} method must be called.
     * @param dir the directory to search
     * @return an unmodifiable list of all loaded mods
     * @throws IOException if the directory is invalid
     */
    public static List<Mod> loadMods(Directory dir) throws IOException {
        File[] modJars = dir.getFiles();
        URL[] urls = new URL[modJars.length];
        for (int i = 0; i < modJars.length; i++) {
            urls[i] = modJars[i].toURI().toURL();
        }

        URLClassLoader modloader = new URLClassLoader(urls);

        List<Mod> mods = new ArrayList<>();
        for (File jar : modJars) {
            try {
                List<Mod> fileMods = loadMods(jar, modloader);
                mods.addAll(fileMods);

            } catch (ReflectiveOperationException ex) {
                Logger.WARN.print("Could not load mods from " + jar);

            } catch (IOException ex) {
                Logger.WARN.print("Could not open " + jar);
            }
        }

        modloader.close();
        return Collections.unmodifiableList(mods);
    }
}

package com.chris.gplugins

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.compress.utils.IOUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

public class AsmjPluginImpl extends Transform implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def android = project.extensions.getByType(AppExtension);
        android.registerTransform(this);
    }

    @Override
    String getName() {
        return "AsmjPlugin";
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        println "---------------------------------------- Asmj.begin ----------------------------------------";
        transformInvocation.getInputs().each { TransformInput input ->

            /**
             * Directory Input
             */
            input.directoryInputs.each { DirectoryInput dirInput ->
                println "directory.name = ${dirInput.file.name}";
                doDirectoryTraverse(dirInput);
                def dest = transformInvocation.getOutputProvider().getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY);
                FileUtils.copyDirectory(dirInput.file, dest);
            }

            /**
             * Jar Input
             */
            input.jarInputs.each {JarInput jarInput ->
                def jarName = jarInput.name;
                /**
                 * 重名名输出文件,因为可能同名(N个classes.jar),会覆盖
                 */
                def hexName = DigestUtils.md5Hex(jarInput.file.getAbsolutePath());
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4);
                }

//                File tmpFile = null;
//                if (jarInput.file.getAbsolutePath().endsWith(".jar")) {
//                    tmpFile = new File(jarInput.file.getParent() + File.separator + "template.jar");
//                    if (tmpFile.exists()) {
//                        tmpFile.delete();
//                    }
//
//                    JarOutputStream jos = new JarOutputStream(new FileOutputStream(tmpFile));
//                    List<String> list = new ArrayList<>();
//                    JarFile jarFile = new JarFile(jarInput.file);
//                    Enumeration enumeration = jarFile.entries();
//                    while (enumeration.hasMoreElements()) {
//                        JarEntry jarEntry = enumeration.nextElement();
//                        String entryName = jarEntry.getName();
//                        ZipEntry zipEntry = new ZipEntry(entryName);
//                        InputStream inputStream = jarFile.getInputStream(jarEntry);
//
//                        if (entryName.endsWith(".class") && !entryName.startsWith("R\$") &&
//                                !entryName.equals("R.class") && !entryName.equals("BuildConfig.class")) {
//                            jos.putNextEntry(zipEntry);
//                            ClassReader reader = new ClassReader(IOUtils.toByteArray(inputStream));
//                            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
//                            ClassVisitor visitor = new MethodClassVisitor(entryName.split(".class")[0], writer);
//                        }
//                    }
//                }

                def dest = transformInvocation.getOutputProvider().getContentLocation(jarName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR);
                if (!dest.exists()) {
                    dest.mkdirs();
                    dest.createNewFile();
                }
                FileUtils.copyFile(jarInput.file, dest);
            }
        }

        println "---------------------------------------- Asmj.end ----------------------------------------";
    }

    static void doDirectoryTraverse(DirectoryInput dirInput) {
        if (dirInput.file.isDirectory()) {
            dirInput.file.eachFileRecurse { File file ->
                def name = file.name;
                def className = file.absolutePath.split("${dirInput.file.name}/")[1];
                if (name.endsWith(".class") && !name.startsWith("R\$") && !name.equals("R.class") && !name.equals("BuildConfig.class")) {
                    ClassReader reader = new ClassReader(file.getBytes());
                    ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
                    ClassVisitor visitor = new MethodClassVisitor(className.split(".class")[0], writer);
                    reader.accept(visitor, ClassReader.EXPAND_FRAMES);
                    byte[] code = writer.toByteArray();
                    FileOutputStream fos = new FileOutputStream(file.parentFile.absolutePath + File.separator + name);
                    fos.write(code);
                    fos.close();
                }
            }
        }
    }
}
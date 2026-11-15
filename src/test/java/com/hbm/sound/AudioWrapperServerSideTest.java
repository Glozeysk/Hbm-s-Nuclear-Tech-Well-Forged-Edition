package com.hbm.sound;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the server side of the {@code @SideOnly} contract for AudioWrapper without booting Minecraft.
 * Forge's SideTransformer deletes fields annotated with {@code @SideOnly(Side.CLIENT)} on a dedicated
 * server, so common classes must not declare such fields and surviving methods must not reference them.
 */
class AudioWrapperServerSideTest {

    private static final String AUDIO_WRAPPER_PREFIX = "com/hbm/sound/AudioWrapper";
    private static final String AUDIO_WRAPPER_DESC = Type.getDescriptor(AudioWrapper.class);
    private static final String SIDE_ONLY_DESC = "Lnet/minecraftforge/fml/relauncher/SideOnly;";
    private static final String CLIENT = "CLIENT";

    @Test
    void commonClassesDoNotUseStrippedAudioWrapperFields() throws Exception {
        Map<String, ClassNode> classes = readMainClasses();

        ClassNode audioWrapper = classes.get(AUDIO_WRAPPER_PREFIX);
        assertNotNull(audioWrapper, "com/hbm/sound/AudioWrapper must be present in the compiled main classes");
        assertFalse(isClientOnly(audioWrapper.visibleAnnotations),
                "AudioWrapper itself must stay common; @SideOnly(Side.CLIENT) would remove it on the server");

        List<String> problems = findServerSideProblems(classes);
        assertTrue(problems.isEmpty(), () ->
                "AudioWrapper is not safe for the dedicated server:\n  " + String.join("\n  ", problems));
    }

    @Test
    void detectorCatchesTheOriginalCrashPattern() {
        ClassNode synthetic = syntheticDieselLikeClass();
        Map<String, ClassNode> classes = new HashMap<>();
        classes.put(synthetic.name, synthetic);

        List<String> problems = findServerSideProblems(classes);
        assertFalse(problems.isEmpty(), "The detector must catch @SideOnly(Side.CLIENT) on an AudioWrapper field");
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("audio")),
                "The reported problem should name the affected field: " + problems);
        assertTrue(problems.stream().anyMatch(problem -> problem.contains("onChunkUnload")),
                "The reported problem should name the common method that references the stripped field: " + problems);
    }

    private static List<String> findServerSideProblems(Map<String, ClassNode> classes) {
        Map<String, Set<String>> strippedAudioFields = new HashMap<>();
        List<String> problems = new ArrayList<>();

        for (ClassNode owner : classes.values()) {
            if (isClientOnly(owner.visibleAnnotations)) {
                continue;
            }

            for (FieldNode field : owner.fields) {
                if (!isAudioWrapperType(field.desc) || !isClientOnly(field.visibleAnnotations)) {
                    continue;
                }

                String fieldKey = field.name + field.desc;
                strippedAudioFields.computeIfAbsent(owner.name, ignored -> new HashSet<>()).add(fieldKey);
                problems.add(owner.name + "." + field.name
                        + " is @SideOnly(Side.CLIENT) in a common class; the field is removed on the server");
            }
        }

        for (ClassNode owner : classes.values()) {
            if (isClientOnly(owner.visibleAnnotations)) {
                continue;
            }

            for (MethodNode method : owner.methods) {
                if (isClientOnly(method.visibleAnnotations)) {
                    continue;
                }

                for (int i = 0; i < method.instructions.size(); i++) {
                    AbstractInsnNode instruction = method.instructions.get(i);
                    if (!(instruction instanceof FieldInsnNode)) {
                        continue;
                    }

                    FieldInsnNode fieldInstruction = (FieldInsnNode) instruction;
                    Set<String> strippedFields = strippedAudioFields.get(fieldInstruction.owner);
                    if (strippedFields == null) {
                        continue;
                    }

                    String fieldKey = fieldInstruction.name + fieldInstruction.desc;
                    if (strippedFields.contains(fieldKey)) {
                        problems.add(owner.name + "." + method.name + method.desc
                                + " references stripped field " + fieldInstruction.owner + "." + fieldKey);
                    }
                }
            }
        }

        return problems;
    }

    private static boolean isAudioWrapperType(String descriptor) {
        return descriptor != null
                && (descriptor.equals(AUDIO_WRAPPER_DESC)
                || descriptor.startsWith("L" + AUDIO_WRAPPER_PREFIX));
    }

    private static boolean isClientOnly(List<AnnotationNode> annotations) {
        if (annotations == null) {
            return false;
        }

        for (AnnotationNode annotation : annotations) {
            if (!SIDE_ONLY_DESC.equals(annotation.desc) || annotation.values == null) {
                continue;
            }

            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if (!"value".equals(annotation.values.get(i))) {
                    continue;
                }

                Object value = annotation.values.get(i + 1);
                if (value instanceof String[]) {
                    String[] enumValue = (String[]) value;
                    return enumValue.length > 1 && CLIENT.equals(enumValue[1]);
                }
            }
        }

        return false;
    }

    private static Map<String, ClassNode> readMainClasses() throws IOException {
        Path mainClasses = mainClassesDir();
        try (Stream<Path> files = Files.walk(mainClasses)) {
            List<Path> classFiles = files
                    .filter(path -> path.toString().endsWith(".class"))
                    .collect(Collectors.toList());

            Map<String, ClassNode> classes = new HashMap<>();
            for (Path classFile : classFiles) {
                ClassNode node = new ClassNode();
                new ClassReader(Files.readAllBytes(classFile)).accept(node, 0);
                classes.put(node.name, node);
            }
            return classes;
        }
    }

    private static Path mainClassesDir() {
        String configured = System.getProperty("hbm.mainClassesDir");
        if (configured != null && !configured.isEmpty()) {
            return Paths.get(configured);
        }

        try {
            return Paths.get(AudioWrapper.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Could not locate compiled main classes", e);
        }
    }

    private static ClassNode syntheticDieselLikeClass() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/hbm/sound/SyntheticAudioUser",
                null, "java/lang/Object", null);

        org.objectweb.asm.FieldVisitor field = writer.visitField(
                Opcodes.ACC_PRIVATE, "audio", AUDIO_WRAPPER_DESC, null, null);
        org.objectweb.asm.AnnotationVisitor fieldAnnotation = field.visitAnnotation(SIDE_ONLY_DESC, true);
        fieldAnnotation.visitEnum("value", "Lnet/minecraftforge/fml/relauncher/Side;", CLIENT);
        fieldAnnotation.visitEnd();
        field.visitEnd();

        org.objectweb.asm.MethodVisitor method = writer.visitMethod(
                Opcodes.ACC_PUBLIC, "onChunkUnload", "()V", null, null);
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, "com/hbm/sound/SyntheticAudioUser",
                "audio", AUDIO_WRAPPER_DESC);
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(1, 1);
        method.visitEnd();

        writer.visitEnd();

        ClassNode node = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(node, 0);
        return node;
    }
}

package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;
import com.sun.management.OperatingSystemMXBean;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11C;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Locale;

public final class HardwareProfileDetector {
    private static final String[] SOFTWARE_RENDERER_FLAGS = {
        "microsoft basic render",
        "microsoft basic",
        "llvmpipe",
        "swiftshader",
        "angle",
        "software",
        "basic render",
        "gdi generic"
    };

    private static final String[] INTEGRATED_RENDERER_FLAGS = {
        "intel",
        "uhd",
        "hd graphics",
        "iris xe",
        "vega 3",
        "vega 6",
        "vega 7",
        "vega 8",
        "radeon graphics"
    };

    private static final String[] DEDICATED_RENDERER_FLAGS = {
        "geforce",
        "rtx",
        "gtx",
        "quadro",
        "tesla",
        "radeon rx",
        "radeon pro",
        "arc ",
        "arc(tm)",
        "workstation",
        "firepro",
        "radeon vii"
    };

    public HardwareProfile detect(Minecraft client) {
        try {
            String renderer = safeLowercase(readGlString(GL11C.GL_RENDERER));
            String vendor = safeLowercase(readGlString(GL11C.GL_VENDOR));

            int cpuScore = detectCpuScore();
            int ramScore = detectRamScore();
            int gpuScore = detectGpuScore(renderer, vendor);
            int total = cpuScore + ramScore + gpuScore;

            HardwareTier tier = total <= 2 ? HardwareTier.LOW : total <= 4 ? HardwareTier.MID : HardwareTier.HIGH;
            boolean shadersActive = detectShadersActive();
            boolean hasRiskFlags = hasRiskFlags(renderer, vendor);
            boolean fabulousSafe = tier == HardwareTier.HIGH && !shadersActive && !hasRiskFlags;

            return new HardwareProfile(tier, cpuScore, ramScore, gpuScore, renderer, vendor, shadersActive, hasRiskFlags, fabulousSafe);
        } catch (RuntimeException | LinkageError exception) {
            AutoTuneFps.LOGGER.warn("Hardware detection failed; using a conservative fallback profile", exception);
            return fallbackProfile();
        }
    }

    private static int detectCpuScore() {
        int threads = Runtime.getRuntime().availableProcessors();
        if (threads <= 4) {
            return 0;
        }
        if (threads <= 8) {
            return 1;
        }
        return 2;
    }

    private static int detectRamScore() {
        long totalBytes = -1L;
        java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();

        if (bean instanceof OperatingSystemMXBean sunBean) {
            totalBytes = sunBean.getTotalMemorySize();
        } else {
            try {
                Method method = bean.getClass().getMethod("getTotalMemorySize");
                Object result = method.invoke(bean);
                if (result instanceof Long value) {
                    totalBytes = value;
                }
            } catch (ReflectiveOperationException ignored) {
                AutoTuneFps.LOGGER.debug("Could not read total system memory via reflection");
            }
        }

        if (totalBytes <= 0L) {
            return 1;
        }

        long totalGb = totalBytes / (1024L * 1024L * 1024L);
        if (totalGb <= 8L) {
            return 0;
        }
        if (totalGb >= 24L) {
            return 2;
        }
        return 1;
    }

    private static int detectGpuScore(String renderer, String vendor) {
        String combined = renderer + " " + vendor;
        if (containsAny(combined, SOFTWARE_RENDERER_FLAGS) || containsAny(combined, INTEGRATED_RENDERER_FLAGS)) {
            return 0;
        }
        if (containsAny(combined, DEDICATED_RENDERER_FLAGS)) {
            return 2;
        }
        return 1;
    }

    private static boolean hasRiskFlags(String renderer, String vendor) {
        String combined = renderer + " " + vendor;
        if (containsAny(combined, SOFTWARE_RENDERER_FLAGS)) {
            return true;
        }

        // If GL details are unavailable, stay conservative and avoid Fabulous.
        return !hasText(renderer) || !hasText(vendor);
    }

    private static boolean detectShadersActive() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return false;
        }

        Boolean irisApiResult = invokeIrisApi("net.irisshaders.iris.api.v0.IrisApi");
        if (irisApiResult != null) {
            return irisApiResult;
        }

        irisApiResult = invokeIrisApi("net.coderbot.iris.api.v0.IrisApi");
        if (irisApiResult != null) {
            return irisApiResult;
        }

        AutoTuneFps.LOGGER.debug("Iris is loaded but shader state could not be detected, treating it as active for safety");
        return true;
    }

    private static Boolean invokeIrisApi(String className) {
        try {
            Class<?> irisApiClass = Class.forName(className);
            Object instance = irisApiClass.getMethod("getInstance").invoke(null);
            Object result = instance.getClass().getMethod("isShaderPackInUse").invoke(instance);
            return result instanceof Boolean value ? value : Boolean.TRUE;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean containsAny(String haystack, String[] needles) {
        for (String needle : needles) {
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static String readGlString(int name) {
        try {
            return GL11C.glGetString(name);
        } catch (RuntimeException exception) {
            AutoTuneFps.LOGGER.debug("Could not read OpenGL string {}", name, exception);
            return "";
        }
    }

    private static String safeLowercase(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static HardwareProfile fallbackProfile() {
        return new HardwareProfile(
            HardwareTier.LOW,
            0,
            0,
            0,
            "",
            "",
            false,
            true,
            false
        );
    }
}

package com.secondmod.autotunefps.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

final class MinecraftScreenCompat {
    private MinecraftScreenCompat() {
    }

    static Screen currentScreen(Minecraft client) {
        try {
            Field field = Minecraft.class.getField("screen");
            Object value = field.get(client);
            return value instanceof Screen ? (Screen) value : null;
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            return null;
        }
    }

    static void setScreen(Minecraft client, Screen screen) {
        try {
            Method method = Minecraft.class.getMethod("setScreenAndShow", Screen.class);
            method.invoke(client, screen);
            return;
        } catch (NoSuchMethodException ignored) {
            // Older 26.1 builds use setScreen instead.
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to open AutoTune screen.", exception);
        }

        try {
            Method method = Minecraft.class.getMethod("setScreen", Screen.class);
            method.invoke(client, screen);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to open AutoTune screen.", exception);
        }
    }
}

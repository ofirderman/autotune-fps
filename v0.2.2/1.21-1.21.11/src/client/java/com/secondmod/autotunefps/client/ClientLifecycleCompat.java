package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class ClientLifecycleCompat {
    private static final String JOIN_CALLBACK_CLASS_NAME =
        "net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents$Join";
    private static final String JOIN_EVENTS_CLASS_NAME =
        "net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents";
    private static final String END_TICK_CALLBACK_CLASS_NAME =
        "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents$EndTick";
    private static final String TICK_EVENTS_CLASS_NAME =
        "net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents";

    private ClientLifecycleCompat() {
    }

    static void register(AutoTuneClientCoordinator coordinator) {
        registerCallback(
            JOIN_EVENTS_CLASS_NAME,
            "JOIN",
            JOIN_CALLBACK_CLASS_NAME,
            new JoinRegistrationHandler(coordinator)
        );
        registerCallback(
            TICK_EVENTS_CLASS_NAME,
            "END_CLIENT_TICK",
            END_TICK_CALLBACK_CLASS_NAME,
            new EndTickRegistrationHandler(coordinator)
        );
    }

    private static boolean registerCallback(
        String ownerClassName,
        String fieldName,
        String callbackClassName,
        InvocationHandler handler
    ) {
        try {
            Class<?> ownerClass = Class.forName(ownerClassName);
            Class<?> callbackClass = Class.forName(callbackClassName);
            Field eventField = ownerClass.getField(fieldName);
            Object event = eventField.get(null);
            Method registerMethod = event.getClass().getMethod("register", Object.class);
            registerMethod.setAccessible(true);

            Object callback = Proxy.newProxyInstance(
                callbackClass.getClassLoader(),
                new Class<?>[] {callbackClass},
                handler
            );

            registerMethod.invoke(event, callback);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            AutoTuneFps.LOGGER.warn("Failed to register AutoTune FPS lifecycle callback {}", fieldName, exception);
            return false;
        }
    }

    private static final class JoinRegistrationHandler implements InvocationHandler {
        private final AutoTuneClientCoordinator coordinator;

        private JoinRegistrationHandler(AutoTuneClientCoordinator coordinator) {
            this.coordinator = coordinator;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            if (args == null || args.length == 0) {
                return null;
            }

            Object client = args[args.length - 1];
            if (client instanceof net.minecraft.client.Minecraft minecraft) {
                coordinator.onWorldJoin(minecraft);
            }
            return null;
        }
    }

    private static final class EndTickRegistrationHandler implements InvocationHandler {
        private final AutoTuneClientCoordinator coordinator;

        private EndTickRegistrationHandler(AutoTuneClientCoordinator coordinator) {
            this.coordinator = coordinator;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            if (args == null || args.length == 0) {
                return null;
            }

            Object client = args[0];
            if (client instanceof net.minecraft.client.Minecraft minecraft) {
                coordinator.onEndClientTick(minecraft);
            }
            return null;
        }
    }

    private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        return switch (method.getName()) {
            case "toString" -> "AutoTune FPS lifecycle compatibility proxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> args != null && args.length == 1 && proxy == args[0];
            default -> null;
        };
    }
}

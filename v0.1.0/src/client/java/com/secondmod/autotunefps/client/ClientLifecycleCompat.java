package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class ClientLifecycleCompat {
    private static final String OPTIMIZER_COMMANDS_CLASS_NAME =
        "com.secondmod.autotunefps.client.OptimizerCommands";
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

    static void registerJoin(Object presetApplier) {
        registerCallback(
            JOIN_EVENTS_CLASS_NAME,
            "JOIN",
            JOIN_CALLBACK_CLASS_NAME,
            new JoinRegistrationHandler(presetApplier)
        );
    }

    static void registerEndClientTick() {
        registerCallback(
            TICK_EVENTS_CLASS_NAME,
            "END_CLIENT_TICK",
            END_TICK_CALLBACK_CLASS_NAME,
            new EndTickRegistrationHandler()
        );
    }

    private static void registerCallback(
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
        } catch (ReflectiveOperationException | RuntimeException exception) {
            AutoTuneFps.LOGGER.warn("Failed to register AutoTune FPS lifecycle callback {}", fieldName, exception);
        }
    }

    private static final class JoinRegistrationHandler implements InvocationHandler {
        private final Object presetApplier;

        private JoinRegistrationHandler(Object presetApplier) {
            this.presetApplier = presetApplier;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            if (args == null || args.length == 0) {
                return null;
            }

            Object client = args[args.length - 1];
            Method applyMethod = findSingleArgumentMethod(presetApplier.getClass(), "applyConfiguredPresetOnJoin", client);
            String message = (String) applyMethod.invoke(presetApplier, client);
            if (message != null) {
                AutoTuneFps.LOGGER.info(message);
            }
            return null;
        }
    }

    private static final class EndTickRegistrationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            if (args == null || args.length == 0) {
                return null;
            }

            Object client = args[0];
            Class<?> optimizerCommandsClass = Class.forName(OPTIMIZER_COMMANDS_CLASS_NAME);
            Method tickMethod = findSingleArgumentMethod(optimizerCommandsClass, "runPendingClientWork", client);
            tickMethod.invoke(null, client);
            return null;
        }
    }

    private static Method findSingleArgumentMethod(Class<?> ownerClass, String methodName, Object argument) throws NoSuchMethodException {
        Class<?> argumentClass = argument == null ? null : argument.getClass();
        for (Method method : ownerClass.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 1) {
                continue;
            }

            Class<?> parameterType = method.getParameterTypes()[0];
            if (argumentClass == null || parameterType.isAssignableFrom(argumentClass)) {
                return method;
            }
        }
        throw new NoSuchMethodException(ownerClass.getName() + "#" + methodName + "(...)");
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

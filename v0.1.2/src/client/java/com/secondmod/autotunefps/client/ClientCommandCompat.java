package com.secondmod.autotunefps.client;

import com.secondmod.autotunefps.AutoTuneFps;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

final class ClientCommandCompat {
    private static final String CALLBACK_CLASS_NAME =
        "net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback";
    private static final String OPTIMIZER_COMMANDS_CLASS_NAME =
        "com.secondmod.autotunefps.client.OptimizerCommands";
    private static final String ROOT_COMMAND_NAME = "optimizer";

    private ClientCommandCompat() {
    }

    static void register(PresetApplier presetApplier) {
        try {
            Class<?> callbackClass = Class.forName(CALLBACK_CLASS_NAME);
            Field eventField = callbackClass.getField("EVENT");
            Object event = eventField.get(null);
            Method registerMethod = event.getClass().getMethod("register", Object.class);
            registerMethod.setAccessible(true);

            Object callback = Proxy.newProxyInstance(
                callbackClass.getClassLoader(),
                new Class<?>[] {callbackClass},
                new RegistrationHandler(presetApplier)
            );

            registerMethod.invoke(event, callback);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            AutoTuneFps.LOGGER.warn(
                "Failed to register AutoTune FPS commands compatibly; command features will stay disabled",
                exception
            );
        }
    }

    private static final class RegistrationHandler implements InvocationHandler {
        private final PresetApplier presetApplier;

        private RegistrationHandler(PresetApplier presetApplier) {
            this.presetApplier = presetApplier;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                return handleObjectMethod(proxy, method, args);
            }

            if (args == null || args.length == 0) {
                AutoTuneFps.LOGGER.warn(
                    "Skipping optimizer command registration because Fabric did not provide a command dispatcher"
                );
                return null;
            }

            registerCommandsIfNeeded(args[0], presetApplier);
            return null;
        }

        private static Object handleObjectMethod(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "toString" -> "AutoTune FPS client command registration proxy";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> args != null && args.length == 1 && proxy == args[0];
                default -> null;
            };
        }
    }

    private static void registerCommandsIfNeeded(Object dispatcherObject, PresetApplier presetApplier) {
        if (dispatcherObject == null) {
            return;
        }

        try {
            Object root = dispatcherObject.getClass().getMethod("getRoot").invoke(dispatcherObject);
            Object existingCommand = root.getClass().getMethod("getChild", String.class).invoke(root, ROOT_COMMAND_NAME);
            if (existingCommand != null) {
                return;
            }

            AutoTuneFps.LOGGER.info("Registering AutoTune FPS client commands");
            invokeRegisterCommands(dispatcherObject, presetApplier);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            AutoTuneFps.LOGGER.warn("Failed to register AutoTune FPS commands against the active dispatcher", exception);
        }
    }

    private static void invokeRegisterCommands(Object dispatcherObject, PresetApplier presetApplier)
        throws ReflectiveOperationException {
        Class<?> optimizerCommandsClass = Class.forName(OPTIMIZER_COMMANDS_CLASS_NAME);
        Method registerMethod = findCompatibleMethod(
            optimizerCommandsClass,
            "register",
            dispatcherObject,
            presetApplier
        );
        registerMethod.invoke(null, dispatcherObject, presetApplier);
    }

    private static Method findCompatibleMethod(
        Class<?> ownerClass,
        String methodName,
        Object firstArgument,
        Object secondArgument
    ) throws NoSuchMethodException {
        Class<?> firstArgumentClass = firstArgument == null ? null : firstArgument.getClass();
        Class<?> secondArgumentClass = secondArgument == null ? null : secondArgument.getClass();

        for (Method method : ownerClass.getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 2) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean firstMatches = firstArgumentClass == null || parameterTypes[0].isAssignableFrom(firstArgumentClass);
            boolean secondMatches = secondArgumentClass == null || parameterTypes[1].isAssignableFrom(secondArgumentClass);
            if (firstMatches && secondMatches) {
                return method;
            }
        }

        throw new NoSuchMethodException(ownerClass.getName() + "#" + methodName + "(...)");
    }
}

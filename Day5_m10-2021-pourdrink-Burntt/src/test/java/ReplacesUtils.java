import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.google.common.base.Strings;
import com.google.common.primitives.Primitives;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.Reflection;

public class ReplacesUtils {
    private static final ConcurrentMap<Class<?>, Class<?>> replacements = new ConcurrentHashMap<>();

    static {
        String packageName = Reflection.getPackageName(ReplacesUtils.class);
        try {
            ClassPath classPath = ClassPath.from(Thread.currentThread().getContextClassLoader());
            classPath.getTopLevelClasses(packageName).forEach(info -> {
                if (info.getName().equals("module-info")) {
                    return;
                }
                Class<?> clazz = info.load();
                Replaces replaces = clazz.getAnnotation(Replaces.class);
                if (replaces != null) {
                    var lhs = replaces.what().getCanonicalName();
                    var rhs = clazz.getCanonicalName();
                    if (replacements.containsKey(replaces.what())) {
                        var rhsConflict = replacements.get(replaces.what()).getCanonicalName();
                        System.err.println("*** @Replaces Conflict: " + lhs + " -> " + rhs);
                        System.err.println("*** @Replaces Conflict: " + lhs + " -> " + rhsConflict);
                        System.exit(1);
                    } else {
                        System.err.println("*** @Replaces: " + lhs + " -> " + rhs);
                        replacements.put(replaces.what(), clazz);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to wire up dependencies");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static <T> T instance(Class<T> clazz, Object... args) {
        try {
            Class<?> finalClazz = clazz;
            if (!Strings.isNullOrEmpty(System.getProperty("useReplaces"))) {
                while (replacements.containsKey(finalClazz)) {
                    finalClazz = replacements.get(finalClazz);
                }
            }
            Class<?> argsClazzes[] = new Class<?>[args.length];
            for (int index = 0; index < args.length; ++index) {
                argsClazzes[index] = args[index].getClass();
            }
            Constructor<T> ctor = null;
            for (var candidateCtor : finalClazz.getDeclaredConstructors()) {
                if (candidateCtor.getParameterCount() != args.length) {
                    continue;
                }
                boolean matches = true;
                Class<?>[] expectedTypes = candidateCtor.getParameterTypes();
                for (int index = 0; index < args.length; ++index) {
                    Class<?> expectedType = expectedTypes[index];
                    Class<?> actualType = args[index].getClass();
                    if (expectedType.isAssignableFrom(actualType)) {
                        continue;
                    }
                    if (expectedType.isPrimitive() && Primitives.isWrapperType(actualType)) {
                        if (expectedType.equals(Primitives.unwrap(actualType))) {
                            continue;
                        }
                    }
                    matches = false;
                }
                if (matches) {
                    ctor = (Constructor<T>) candidateCtor;
                }
            }
            if (ctor != null) {
                return ctor.newInstance(args);
            }
            throw new RuntimeException("Unable to find a matching constructor");
        } catch (Exception e) {
            System.err.println("Failed to instantiate " + clazz.getCanonicalName());
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }
}

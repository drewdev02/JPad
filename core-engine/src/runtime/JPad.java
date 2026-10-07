package org.andrew.jpad.runtime;

public final class JPad {

    @FunctionalInterface
    public interface DumpConsumer {
        void accept(Object obj, String title);
    }

    private static final ThreadLocal<DumpConsumer> CURRENT_CONSUMER = new ThreadLocal<>();

    public static void setDumpConsumer(DumpConsumer consumer) {
        if (consumer == null) {
            CURRENT_CONSUMER.remove();
        } else {
            CURRENT_CONSUMER.set(consumer);
        }
    }

    public static <T> T dump(T obj) {
        return dump(obj, null);
    }

    public static <T> T dump(T obj, String title) {
        DumpConsumer consumer = CURRENT_CONSUMER.get();
        if (consumer != null) {
            consumer.accept(obj, title);
        } else {
            System.out.println(obj);
        }
        return obj;
    }
}

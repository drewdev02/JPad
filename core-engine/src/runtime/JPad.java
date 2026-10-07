package org.andrew.jpad.runtime;

public final class JPad {

    @FunctionalInterface
    public interface DumpConsumer {
        void accept(Object obj, String title);
    }

    private static volatile DumpConsumer currentConsumer = null;

    public static void setDumpConsumer(DumpConsumer consumer) {
        currentConsumer = consumer;
    }

    public static <T> T dump(T obj) {
        return dump(obj, null);
    }

    public static <T> T dump(T obj, String title) {
        DumpConsumer consumer = currentConsumer;
        if (consumer != null) {
            consumer.accept(obj, title);
        } else {
            System.out.println(obj);
        }
        return obj;
    }
}

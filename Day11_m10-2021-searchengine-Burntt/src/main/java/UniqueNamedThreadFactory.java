import java.util.concurrent.ThreadFactory;

// Threading.
class UniqueNamedThreadFactory implements ThreadFactory {
    private final String name;

    UniqueNamedThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(name);
        return thread;
    }
}

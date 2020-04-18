package eu.europa.ec.leos.web.event.component;

public class WindowClosedEvent<T> {
    private T window;

    public WindowClosedEvent(T classType) {
        this.window = classType;
    }

    public T getWindow() {
        return window;
    }
}

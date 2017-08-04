package java.awt;
interface EventFilter {
    static enum FilterAction {
        ACCEPT,
        REJECT,
        ACCEPT_IMMEDIATELY
    };
    FilterAction acceptEvent(AWTEvent ev);
}

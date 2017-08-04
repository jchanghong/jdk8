


package java.util.logging;


public class ConsoleHandler extends StreamHandler {
    // Private method to configure a ConsoleHandler from LogManager
    // properties and/or default values as specified in the class
    // javadoc.
    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();

        setLevel(manager.getLevelProperty(cname +".level", Level.INFO));
        setFilter(manager.getFilterProperty(cname +".filter", null));
        setFormatter(manager.getFormatterProperty(cname +".formatter", new SimpleFormatter()));
        try {
            setEncoding(manager.getStringProperty(cname +".encoding", null));
        } catch (Exception ex) {
            try {
                setEncoding(null);
            } catch (Exception ex2) {
                // doing a setEncoding with null should always work.
                // assert false;
            }
        }
    }


    public ConsoleHandler() {
        sealed = false;
        configure();
        setOutputStream(System.err);
        sealed = true;
    }


    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }


    @Override
    public void close() {
        flush();
    }
}

package medium.loggersystem.simplelogger;

class LoggerGodLevel {
}

enum LogLevel {

    DEBUG,
    INFO,
    ERROR,
}

interface LogAppender {
    void append(String message);
}

class ConsoleAppender implements LogAppender {

    @Override
    public void append(String message) {
        System.out.println(message);
    }

}

abstract class LogHandler {

    protected LogHandler next;
    protected LogAppender appender;

    public LogHandler(LogAppender appender) {
        this.appender = appender;
    }

    public LogHandler setNext(LogHandler handler) {
        this.next = handler;
        return next;
    }

    public void handle(LogLevel level, String message) {
        if (canHandle(level)) {
            appender.append(format(level, message));
            return;
        }

        if (next != null) {
            next.handle(level, message);
        }
    }

    protected String format(LogLevel level, String message) {
        return "[" + level + "] " + message;
    }

    protected abstract boolean canHandle(LogLevel level);
}


class DebugHandler extends LogHandler {

    public DebugHandler(LogAppender appender) {
        super(appender);
    }

    @Override
    protected boolean canHandle(LogLevel level) {
        return level == LogLevel.DEBUG;
    }

}

class InfoHandler extends LogHandler {

    public InfoHandler(LogAppender appender) {
        super(appender);
    }

    @Override
    protected boolean canHandle(LogLevel level) {
        return level == LogLevel.INFO;
    }
}

class ErrorHandler extends LogHandler {

    public ErrorHandler(LogAppender appender) {
        super(appender);
    }

    @Override
    protected boolean canHandle(LogLevel level) {
        return level == LogLevel.ERROR;
    }
}

class Logger {

    private final LogHandler chain;

    Logger() {

        LogAppender consoleAppender = new ConsoleAppender();

        // 3 handler
        LogHandler debugHandler = new DebugHandler(consoleAppender);
        LogHandler infoHandler = new InfoHandler(consoleAppender);
        LogHandler errorHandler = new ErrorHandler(consoleAppender);

        debugHandler.setNext(infoHandler).setNext(errorHandler);

        this.chain = debugHandler;
    }

    public void log(LogLevel level, String message) {
        chain.handle(level, message);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void error(String message){
        log(LogLevel.ERROR,message);
    }

    public void info(String message){
        log(LogLevel.INFO,message);
    }
}

class LogManager {

    private static LogManager instance;
    private final Logger logger;


    private LogManager() {
        this.logger = new Logger();
    }

    public static synchronized LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }

        return instance;
    }

    public Logger getLogger() {
        return this.logger;
    }
}

class Main {
    public static void main(String[] args) {

        Logger logger = LogManager.getInstance().getLogger();

        logger.log(LogLevel.ERROR,"this is error log");
        logger.debug("This is Debug log");
        logger.info("this is info log");

    }
}
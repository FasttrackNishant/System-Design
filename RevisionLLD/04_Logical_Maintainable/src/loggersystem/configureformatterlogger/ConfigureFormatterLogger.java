package loggersystem.configureformatterlogger;

import java.time.LocalDateTime;

/* ===================== LOG LEVEL ===================== */

enum LogLevel {
    DEBUG,
    INFO,
    ERROR
}

/* ===================== APPENDERS ===================== */

interface LogAppender {
    void append(String message);
}

class ConsoleAppender implements LogAppender {
    @Override
    public void append(String message) {
        System.out.println(message);
    }
}

class FileAppender implements LogAppender {
    @Override
    public void append(String message) {
        System.out.println("FILE :: " + message);
    }
}

class CloudAppender implements LogAppender {
    @Override
    public void append(String message) {
        System.out.println("CLOUD :: " + message);
    }
}

/* ===================== FORMATTERS ===================== */

interface LogFormatter {
    String format(LogLevel level, String message);
}

class SimpleFormatter implements LogFormatter {
    @Override
    public String format(LogLevel level, String message) {
        return "[" + level + "] " + message;
    }
}

class TimeStampFormatter implements LogFormatter {
    @Override
    public String format(LogLevel level, String message) {
        return LocalDateTime.now() + " [" + level + "] " + message;
    }
}

/* ===================== HANDLERS ===================== */

abstract class LogHandler {

    protected LogHandler next;
    protected LogAppender appender;
    protected LogFormatter formatter;

    public LogHandler(LogAppender appender, LogFormatter formatter) {
        this.appender = appender;
        this.formatter = formatter;
    }

    public LogHandler setNext(LogHandler handler) {
        this.next = handler;
        return handler;
    }

    public void handle(LogLevel level, String message) {
        if (canHandle(level)) {
            appender.append(formatter.format(level, message));
        }
        if (next != null) {
            next.handle(level, message);
        }
    }

    protected abstract boolean canHandle(LogLevel level);
}

class DebugHandler extends LogHandler {
    public DebugHandler(LogAppender appender, LogFormatter formatter) {
        super(appender, formatter);
    }

    protected boolean canHandle(LogLevel level) {
        return level == LogLevel.DEBUG;
    }
}

class InfoHandler extends LogHandler {
    public InfoHandler(LogAppender appender, LogFormatter formatter) {
        super(appender, formatter);
    }

    protected boolean canHandle(LogLevel level) {
        return level == LogLevel.INFO;
    }
}

class ErrorHandler extends LogHandler {
    public ErrorHandler(LogAppender appender, LogFormatter formatter) {
        super(appender, formatter);
    }

    protected boolean canHandle(LogLevel level) {
        return level == LogLevel.ERROR;
    }
}

/* ===================== CONFIGURATION ===================== */

class LoggerConfiguration {

    public static LogHandler buildChain() {

        LogAppender console = new ConsoleAppender();
        LogAppender file = new FileAppender();
        LogAppender cloud = new CloudAppender();

        LogFormatter simple = new SimpleFormatter();
        LogFormatter timestamp = new TimeStampFormatter();

        LogHandler debugHandler =
                new DebugHandler(console, simple);

        LogHandler infoHandler =
                new InfoHandler(console, timestamp);

        LogHandler errorConsole =
                new ErrorHandler(console, timestamp);

        LogHandler errorFile =
                new ErrorHandler(file, timestamp);

        LogHandler errorCloud =
                new ErrorHandler(cloud, timestamp);

        debugHandler
                .setNext(infoHandler)
                .setNext(errorConsole)
                .setNext(errorFile)
                .setNext(errorCloud);

        return debugHandler;
    }
}

/* ===================== LOGGER ===================== */

class Logger {

    private final LogHandler chain;

    public Logger(LogHandler chain) {
        this.chain = chain;
    }

    public void log(LogLevel level, String message) {
        chain.handle(level, message);
    }

    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }

    public void info(String message) {
        log(LogLevel.INFO, message);
    }

    public void error(String message) {
        log(LogLevel.ERROR, message);
    }
}

/* ===================== LOG MANAGER (Singleton) ===================== */

class LogManager {

    private static LogManager instance;
    private final Logger logger;

    private LogManager() {
        this.logger = new Logger(LoggerConfiguration.buildChain());
    }

    public static synchronized LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    public Logger getLogger() {
        return logger;
    }
}

/* ===================== MAIN ===================== */
 class Main {
    public static void main(String[] args) {

        Logger logger = LogManager.getInstance().getLogger();

        logger.debug("This is debug log");
        logger.info("This is info log");
        logger.error("This is error log");
    }
}


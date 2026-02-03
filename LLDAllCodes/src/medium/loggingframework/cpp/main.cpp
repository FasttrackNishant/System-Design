class LogMessage {
private:
    time_t timestamp;
    LogLevel level;
    string loggerName;
    string threadName;
    string message;

public:
    LogMessage(LogLevel level, const string& loggerName, const string& message)
        : level(level), loggerName(loggerName), message(message) {
        timestamp = time(NULL);
        threadName = "main"; // Simplified thread name since we're not using threading
    }

    time_t getTimestamp() const { return timestamp; }
    LogLevel getLevel() const { return level; }
    string getLoggerName() const { return loggerName; }
    string getThreadName() const { return threadName; }
    string getMessage() const { return message; }
};








enum class LogLevel {
    DEBUG = 1,
    INFO = 2,
    WARN = 3,
    ERROR = 4,
    FATAL = 5
};

class LogLevelHelper {
public:
    static bool isGreaterOrEqual(LogLevel level, LogLevel other) {
        return static_cast<int>(level) >= static_cast<int>(other);
    }

    static string toString(LogLevel level) {
        switch (level) {
            case LogLevel::DEBUG: return "DEBUG";
            case LogLevel::INFO: return "INFO";
            case LogLevel::WARN: return "WARN";
            case LogLevel::ERROR: return "ERROR";
            case LogLevel::FATAL: return "FATAL";
            default: return "UNKNOWN";
        }
    }
};










class ConsoleAppender : public LogAppender {
private:
    LogFormatter* formatter;

public:
    ConsoleAppender() {
        formatter = new SimpleTextFormatter();
    }

    void append(const LogMessage& logMessage) {
        cout << formatter->format(logMessage) << endl;
    }

    void close() {}

    void setFormatter(LogFormatter* formatter) {
        delete this->formatter;
        this->formatter = formatter;
    }

    LogFormatter* getFormatter() {
        return formatter;
    }

    ~ConsoleAppender() {
        delete formatter;
    }
};







class FileAppender : public LogAppender {
private:
    ofstream* writer;
    LogFormatter* formatter;
    string filePath;

public:
    FileAppender(const string& filePath) : filePath(filePath) {
        formatter = new SimpleTextFormatter();
        try {
            writer = new ofstream(filePath, ios::app);
            if (!writer->is_open()) {
                cout << "Failed to create writer for file logs" << endl;
                delete writer;
                writer = NULL;
            }
        } catch (const exception& e) {
            cout << "Failed to create writer for file logs, exception: " << e.what() << endl;
            writer = NULL;
        }
    }

    void append(const LogMessage& logMessage) {
        if (writer && writer->is_open()) {
            try {
                *writer << formatter->format(logMessage) << endl;
                writer->flush();
            } catch (const exception& e) {
                cout << "Failed to write logs to file, exception: " << e.what() << endl;
            }
        }
    }

    void close() {
        if (writer) {
            try {
                writer->close();
                delete writer;
                writer = NULL;
            } catch (const exception& e) {
                cout << "Failed to close logs file, exception: " << e.what() << endl;
            }
        }
    }

    void setFormatter(LogFormatter* formatter) {
        delete this->formatter;
        this->formatter = formatter;
    }

    LogFormatter* getFormatter() {
        return formatter;
    }

    ~FileAppender() {
        close();
        delete formatter;
    }
};







class LogAppender {
public:
    virtual ~LogAppender() {}
    virtual void append(const LogMessage& logMessage) = 0;
    virtual void close() = 0;
    virtual LogFormatter* getFormatter() = 0;
    virtual void setFormatter(LogFormatter* formatter) = 0;
};












class LogFormatter {
public:
    virtual ~LogFormatter() {}
    virtual string format(const LogMessage& logMessage) = 0;
};




class SimpleTextFormatter : public LogFormatter {
public:
    string format(const LogMessage& logMessage) {
        time_t time_t_val = logMessage.getTimestamp();
        
        stringstream ss;
        ss << put_time(localtime(&time_t_val), "%Y-%m-%d %H:%M:%S");
        ss << " [" << logMessage.getThreadName() << "] ";
        ss << LogLevelHelper::toString(logMessage.getLevel());
        ss << " - " << logMessage.getLoggerName();
        ss << ": " << logMessage.getMessage();
        
        return ss.str();
    }
};














class Logger {
private:
    string name;
    LogLevel* level;
    Logger* parent;
    vector<LogAppender*> appenders;
    bool additivity;

public:
    Logger(const string& name, Logger* parent)
        : name(name), level(NULL), parent(parent), additivity(true) {}

    void addAppender(LogAppender* appender) {
        appenders.push_back(appender);
    }

    vector<LogAppender*> getAppenders() const {
        return appenders;
    }

    void setLevel(LogLevel minLevel) {
        if (!level) {
            level = new LogLevel(minLevel);
        } else {
            *level = minLevel;
        }
    }

    void setAdditivity(bool additivity) {
        this->additivity = additivity;
    }

    LogLevel getEffectiveLevel() const {
        for (const Logger* logger = this; logger != NULL; logger = logger->parent) {
            if (logger->level != NULL) {
                return *logger->level;
            }
        }
        return LogLevel::DEBUG; // Default root level
    }

    void log(LogLevel messageLevel, const string& message) {
        if (LogLevelHelper::isGreaterOrEqual(messageLevel, getEffectiveLevel())) {
            LogMessage logMessage(messageLevel, this->name, message);
            callAppenders(logMessage);
        }
    }

private:
    void callAppenders(const LogMessage& logMessage) {
        if (!appenders.empty()) {
            LogManager::getInstance()->getProcessor()->process(logMessage, appenders);
        }
        if (additivity && parent != NULL) {
            parent->callAppenders(logMessage);
        }
    }

public:
    void debug(const string& message) {
        log(LogLevel::DEBUG, message);
    }

    void info(const string& message) {
        log(LogLevel::INFO, message);
    }

    void warn(const string& message) {
        log(LogLevel::WARN, message);
    }

    void error(const string& message) {
        log(LogLevel::ERROR, message);
    }

    void fatal(const string& message) {
        log(LogLevel::FATAL, message);
    }

    ~Logger() {
        delete level;
    }
};

// Static member definitions
LogManager* LogManager::instance = NULL;

LogManager::LogManager() {
    rootLogger = new Logger("root", NULL);
    loggers["root"] = rootLogger;
    processor = new LogProcessor();
}

LogManager* LogManager::getInstance() {
    if (!instance) {
        instance = new LogManager();
    }
    return instance;
}

Logger* LogManager::getLogger(const string& name) {
    auto it = loggers.find(name);
    if (it == loggers.end()) {
        loggers[name] = createLogger(name);
        return loggers[name];
    }
    return it->second;
}

Logger* LogManager::createLogger(const string& name) {
    if (name == "root") {
        return rootLogger;
    }
    
    size_t lastDot = name.find_last_of('.');
    string parentName = (lastDot == string::npos) ? "root" : name.substr(0, lastDot);
    Logger* parent = getLogger(parentName);
    return new Logger(name, parent);
}

Logger* LogManager::getRootLogger() {
    return rootLogger;
}

LogProcessor* LogManager::getProcessor() {
    return processor;
}

void LogManager::shutdown() {
    // Stop the processor first to ensure all logs are written
    processor->stop();

    // Then, close all appenders
    vector<LogAppender*> allAppenders;
    for (const auto& pair : loggers) {
        const auto& logger = pair.second;
        const auto& loggerAppenders = logger->getAppenders();
        for (const auto& appender : loggerAppenders) {
            if (find(allAppenders.begin(), allAppenders.end(), appender) == allAppenders.end()) {
                allAppenders.push_back(appender);
            }
        }
    }
    
    for (const auto& appender : allAppenders) {
        appender->close();
    }
    
    cout << "Logging framework shut down gracefully." << endl;
}








class LoggingFrameworkDemo {
public:
    static void main() {
        // --- 1. Initial Configuration ---
        auto logManager = LogManager::getInstance();
        auto rootLogger = logManager->getRootLogger();
        rootLogger->setLevel(LogLevel::INFO); // Set global minimum level to INFO

        // Add a console appender to the root logger
        rootLogger->addAppender(new ConsoleAppender());

        cout << "--- Initial Logging Demo ---" << endl;
        auto mainLogger = logManager->getLogger("com.example.Main");
        mainLogger->info("Application starting up.");
        mainLogger->debug("This is a debug message, it should NOT appear."); // Below root level
        mainLogger->warn("This is a warning message.");

        // --- 2. Hierarchy and Additivity Demo ---
        cout << "\n--- Logger Hierarchy Demo ---" << endl;
        auto dbLogger = logManager->getLogger("com.example.db");
        // dbLogger inherits level and appenders from root
        dbLogger->info("Database connection pool initializing.");

        // Let's create a more specific logger and override its level
        auto serviceLogger = logManager->getLogger("com.example.service.UserService");
        serviceLogger->setLevel(LogLevel::DEBUG); // More verbose logging for this specific service
        serviceLogger->info("User service starting.");
        serviceLogger->debug("This debug message SHOULD now appear for the service logger.");

        // --- 3. Dynamic Configuration Change ---
        cout << "\n--- Dynamic Configuration Demo ---" << endl;
        cout << "Changing root log level to DEBUG..." << endl;
        rootLogger->setLevel(LogLevel::DEBUG);
        mainLogger->debug("This debug message should now be visible.");

        try {
            // Simple delay simulation without threading
            cout << "Shutting down..." << endl;
            logManager->shutdown();
        } catch (const exception& e) {
            cout << "Caught exception: " << e.what() << endl;
        }
    }
};

int main() {
    LoggingFrameworkDemo::main();
    return 0;
}







class Logger;

class LogManager {
private:
    static LogManager* instance;
    map<string, Logger*> loggers;
    Logger* rootLogger;
    LogProcessor* processor;

    LogManager();

public:
    static LogManager* getInstance();
    Logger* getLogger(const string& name);
    Logger* getRootLogger();
    LogProcessor* getProcessor();
    void shutdown();

private:
    Logger* createLogger(const string& name);
};









class LogProcessor {
private:
    queue<function<void()>> taskQueue;

public:
    void process(const LogMessage& logMessage, const vector<LogAppender*>& appenders) {
        // Process synchronously since we don't have threading
        for (const auto& appender : appenders) {
            appender->append(logMessage);
        }
    }

    void stop() {
        // Process any remaining tasks
        while (!taskQueue.empty()) {
            auto task = taskQueue.front();
            taskQueue.pop();
            task();
        }
    }
};





































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































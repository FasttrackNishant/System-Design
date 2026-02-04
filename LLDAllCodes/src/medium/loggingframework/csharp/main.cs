
class LogMessage
{
    private readonly DateTime timestamp;
    private readonly LogLevel level;
    private readonly string loggerName;
    private readonly string threadName;
    private readonly string message;

    public LogMessage(LogLevel level, string loggerName, string message)
    {
        this.timestamp = DateTime.Now;
        this.level = level;
        this.loggerName = loggerName;
        this.message = message;
        this.threadName = Thread.CurrentThread.Name ?? Thread.CurrentThread.ManagedThreadId.ToString();
    }

    public DateTime GetTimestamp() { return timestamp; }
    public LogLevel GetLevel() { return level; }
    public string GetLoggerName() { return loggerName; }
    public string GetThreadName() { return threadName; }
    public string GetMessage() { return message; }
}










enum LogLevel
{
    DEBUG = 1,
    INFO = 2,
    WARN = 3,
    ERROR = 4,
    FATAL = 5
}

static class LogLevelExtensions
{
    public static bool IsGreaterOrEqual(this LogLevel level, LogLevel other)
    {
        return (int)level >= (int)other;
    }
}





interface ILogAppender
{
    void Append(LogMessage logMessage);
    void Close();
    ILogFormatter GetFormatter();
    void SetFormatter(ILogFormatter formatter);
}

class ConsoleAppender : ILogAppender
{
    private ILogFormatter formatter;

    public ConsoleAppender()
    {
        this.formatter = new SimpleTextFormatter();
    }

    public void Append(LogMessage logMessage)
    {
        Console.Write(formatter.Format(logMessage));
    }

    public void Close() { }

    public void SetFormatter(ILogFormatter formatter)
    {
        this.formatter = formatter;
    }

    public ILogFormatter GetFormatter()
    {
        return formatter;
    }
}

class FileAppender : ILogAppender
{
    private StreamWriter writer;
    private ILogFormatter formatter;
    private readonly object fileLock = new object();

    public FileAppender(string filePath)
    {
        this.formatter = new SimpleTextFormatter();
        try
        {
            this.writer = new StreamWriter(filePath, true);
        }
        catch (Exception e)
        {
            Console.WriteLine($"Failed to create writer for file logs, exception: {e.Message}");
            this.writer = null;
        }
    }

    public void Append(LogMessage logMessage)
    {
        lock (fileLock)
        {
            if (writer != null)
            {
                try
                {
                    writer.Write(formatter.Format(logMessage) + "\n");
                    writer.Flush();
                }
                catch (Exception e)
                {
                    Console.WriteLine($"Failed to write logs to file, exception: {e.Message}");
                }
            }
        }
    }

    public void Close()
    {
        if (writer != null)
        {
            try
            {
                writer.Close();
            }
            catch (Exception e)
            {
                Console.WriteLine($"Failed to close logs file, exception: {e.Message}");
            }
        }
    }

    public void SetFormatter(ILogFormatter formatter)
    {
        this.formatter = formatter;
    }

    public ILogFormatter GetFormatter()
    {
        return formatter;
    }
}










interface ILogFormatter
{
    string Format(LogMessage logMessage);
}

class SimpleTextFormatter : ILogFormatter
{
    public string Format(LogMessage logMessage)
    {
        return $"{logMessage.GetTimestamp():yyyy-MM-dd HH:mm:ss.fff} [{logMessage.GetThreadName()}] {logMessage.GetLevel()} - {logMessage.GetLoggerName()}: {logMessage.GetMessage()}\n";
    }
}












class AsyncLogProcessor
{
    private readonly ConcurrentQueue<Action> taskQueue = new ConcurrentQueue<Action>();
    private readonly AutoResetEvent signal = new AutoResetEvent(false);
    private volatile bool shutdownFlag = false;
    private readonly Thread workerThread;

    public AsyncLogProcessor()
    {
        workerThread = new Thread(WorkerLoop)
        {
            Name = "AsyncLogProcessor",
            IsBackground = true
        };
        workerThread.Start();
    }

    private void WorkerLoop()
    {
        while (!shutdownFlag)
        {
            signal.WaitOne();
            
            while (taskQueue.TryDequeue(out Action task))
            {
                try
                {
                    task();
                }
                catch (Exception e)
                {
                    Console.WriteLine($"Error processing log task: {e.Message}");
                }
            }
        }
    }

    public void Process(LogMessage logMessage, List<ILogAppender> appenders)
    {
        if (shutdownFlag)
        {
            Console.Error.WriteLine("Logger is shut down. Cannot process log message.");
            return;
        }

        taskQueue.Enqueue(() =>
        {
            foreach (var appender in appenders)
            {
                appender.Append(logMessage);
            }
        });
        signal.Set();
    }

    public void Stop()
    {
        shutdownFlag = true;
        signal.Set();
        
        if (!workerThread.Join(TimeSpan.FromSeconds(2)))
        {
            Console.Error.WriteLine("Logger executor did not terminate in the specified time.");
            workerThread.Abort();
        }
    }
}












class Logger
{
    private readonly string name;
    private LogLevel? level;
    private readonly Logger parent;
    private readonly List<ILogAppender> appenders;
    private bool additivity = true;

    public Logger(string name, Logger parent)
    {
        this.name = name;
        this.parent = parent;
        this.appenders = new List<ILogAppender>();
    }

    public void AddAppender(ILogAppender appender)
    {
        appenders.Add(appender);
    }

    public List<ILogAppender> GetAppenders()
    {
        return new List<ILogAppender>(appenders);
    }

    public void SetLevel(LogLevel minLevel)
    {
        this.level = minLevel;
    }

    public void SetAdditivity(bool additivity)
    {
        this.additivity = additivity;
    }

    public LogLevel GetEffectiveLevel()
    {
        for (Logger logger = this; logger != null; logger = logger.parent)
        {
            LogLevel? currentLevel = logger.level;
            if (currentLevel.HasValue)
            {
                return currentLevel.Value;
            }
        }
        return LogLevel.DEBUG; // Default root level
    }

    public void Log(LogLevel messageLevel, string message)
    {
        if (messageLevel.IsGreaterOrEqual(GetEffectiveLevel()))
        {
            LogMessage logMessage = new LogMessage(messageLevel, this.name, message);
            CallAppenders(logMessage);
        }
    }

    private void CallAppenders(LogMessage logMessage)
    {
        if (appenders.Count > 0)
        {
            LogManager.GetInstance().GetProcessor().Process(logMessage, this.appenders);
        }
        if (additivity && parent != null)
        {
            parent.CallAppenders(logMessage);
        }
    }

    public void Debug(string message)
    {
        Log(LogLevel.DEBUG, message);
    }

    public void Info(string message)
    {
        Log(LogLevel.INFO, message);
    }

    public void Warn(string message)
    {
        Log(LogLevel.WARN, message);
    }

    public void Error(string message)
    {
        Log(LogLevel.ERROR, message);
    }

    public void Fatal(string message)
    {
        Log(LogLevel.FATAL, message);
    }
}










using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Linq;

public class LoggingFrameworkDemo
{
    public static void Main(string[] args)
    {
        // --- 1. Initial Configuration ---
        LogManager logManager = LogManager.GetInstance();
        Logger rootLogger = logManager.GetRootLogger();
        rootLogger.SetLevel(LogLevel.INFO); // Set global minimum level to INFO

        // Add a console appender to the root logger
        rootLogger.AddAppender(new ConsoleAppender());

        Console.WriteLine("--- Initial Logging Demo ---");
        Logger mainLogger = logManager.GetLogger("com.example.Main");
        mainLogger.Info("Application starting up.");
        mainLogger.Debug("This is a debug message, it should NOT appear."); // Below root level
        mainLogger.Warn("This is a warning message.");

        // --- 2. Hierarchy and Additivity Demo ---
        Console.WriteLine("\n--- Logger Hierarchy Demo ---");
        Logger dbLogger = logManager.GetLogger("com.example.db");
        // dbLogger inherits level and appenders from root
        dbLogger.Info("Database connection pool initializing.");

        // Let's create a more specific logger and override its level
        Logger serviceLogger = logManager.GetLogger("com.example.service.UserService");
        serviceLogger.SetLevel(LogLevel.DEBUG); // More verbose logging for this specific service
        serviceLogger.Info("User service starting.");
        serviceLogger.Debug("This debug message SHOULD now appear for the service logger.");

        // --- 3. Dynamic Configuration Change ---
        Console.WriteLine("\n--- Dynamic Configuration Demo ---");
        Console.WriteLine("Changing root log level to DEBUG...");
        rootLogger.SetLevel(LogLevel.DEBUG);
        mainLogger.Debug("This debug message should now be visible.");

        try
        {
            Thread.Sleep(500);
            logManager.Shutdown();
        }
        catch (Exception e)
        {
            Console.WriteLine("Caught exception");
        }
    }
}

























class LogManager
{
    private static volatile LogManager instance;
    private static readonly object lockObject = new object();
    private readonly ConcurrentDictionary<string, Logger> loggers = new ConcurrentDictionary<string, Logger>();
    private readonly Logger rootLogger;
    private readonly AsyncLogProcessor processor;

    private LogManager()
    {
        this.rootLogger = new Logger("root", null);
        this.loggers.TryAdd("root", rootLogger);
        this.processor = new AsyncLogProcessor();
    }

    public static LogManager GetInstance()
    {
        if (instance == null)
        {
            lock (lockObject)
            {
                if (instance == null)
                    instance = new LogManager();
            }
        }
        return instance;
    }

    public Logger GetLogger(string name)
    {
        return loggers.GetOrAdd(name, CreateLogger);
    }

    private Logger CreateLogger(string name)
    {
        if (name.Equals("root"))
        {
            return rootLogger;
        }
        int lastDot = name.LastIndexOf('.');
        string parentName = (lastDot == -1) ? "root" : name.Substring(0, lastDot);
        Logger parent = GetLogger(parentName);
        return new Logger(name, parent);
    }

    public Logger GetRootLogger()
    {
        return rootLogger;
    }

    public AsyncLogProcessor GetProcessor()
    {
        return processor;
    }

    public void Shutdown()
    {
        // Stop the processor first to ensure all logs are written
        processor.Stop();

        // Then, close all appenders
        var allAppenders = loggers.Values
            .SelectMany(logger => logger.GetAppenders())
            .Distinct()
            .ToList();

        foreach (var appender in allAppenders)
        {
            appender.Close();
        }

        Console.WriteLine("Logging framework shut down gracefully.");
    }
}









































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































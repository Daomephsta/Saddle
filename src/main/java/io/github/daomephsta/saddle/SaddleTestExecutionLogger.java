package io.github.daomephsta.saddle;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

public class SaddleTestExecutionLogger implements TestExecutionListener
{
    private final Logger logger;
    private final Level logLevel;
    private int indentLevel = 0;

    public SaddleTestExecutionLogger(Logger logger, Level logLevel)
    {
        this.logger = logger;
        this.logLevel = logLevel;
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier)
    {
        logWithIndent(logLevel, "{} started", testIdentifier.getDisplayName());
        indentLevel++;
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason)
    {
        logWithIndent(logLevel, "{} skipped because {}", testIdentifier.getDisplayName(), reason);
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result)
    {
        indentLevel--;
        switch (result.getStatus())
        {
        case ABORTED:
            logWithIndent(logLevel, "{} aborted", testIdentifier.getDisplayName());
            break;
        case FAILED:
            logWithIndent(logLevel, "{} failed", testIdentifier.getDisplayName());
            break;
        case SUCCESSFUL:
            logWithIndent(logLevel, "{} passed", testIdentifier.getDisplayName());
            break;
        default:
            logWithIndent(logLevel, "{} finished with unknown status {}", testIdentifier.getDisplayName(), result.getStatus());
            break;
        }
    }

    private void logWithIndent(Level logLevel, String format, Object... args)
    {
        StringBuilder message = new StringBuilder(format);
        for (int i = 0; i < indentLevel; i++)
            message.insert(0, '\t');
        logger.log(logLevel, message.toString(), args);
    }
}   

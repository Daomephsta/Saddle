package io.github.daomephsta.saddle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.EnumMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import io.github.daomephsta.saddle.engine.SaddleTest.LoadPhase;
import io.github.daomephsta.saddle.engine.SaddleTestEngine;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Saddle.MOD_ID, name = Saddle.NAME, version = Saddle.VERSION, acceptedMinecraftVersions = "")
public class Saddle
{
    public static final String MOD_ID = "saddle",
                               NAME = "Saddle",
                               VERSION = "GRADLE:VERSION";
    private static final String ENGINE_ACTIVE_SYSPROP = "saddle.active";
    private static final Logger LOGGER = LogManager.getLogger("Saddle");
    
    private final File SADDLE_LOG_DIR = new File("logs/saddle");
    private SaddleConfiguration configuration;
    private Map<LoadPhase, Tests> tests;
    private static class Tests
    {
        private final Launcher launcher;
        private final TestPlan testPlan;
        
        Tests(Launcher launcher, TestPlan testPlan)
        {
            this.launcher = launcher;
            this.testPlan = testPlan;
        }
        
        void execute(TestExecutionListener... executionListeners)
        {
            launcher.execute(testPlan, executionListeners);
        }

        public boolean hasTests()
        {
            return testPlan.containsTests();
        }
    }
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        if (isEnabled())
        {
            LOGGER.info("saddle.disable is true, skipping pre-init");
            return;
        }
        setup();
        discoverSaddleTests();
        executeSaddleTests(LoadPhase.PRE_INIT);
        if (shouldExitOnTestCompletion() && !tests.get(LoadPhase.INIT).hasTests() && !tests.get(LoadPhase.POST_INIT).hasTests())
        {
            LOGGER.info("All tests complete, JVM shutting down");
            FMLCommonHandler.instance().exitJava(0, false);
        }
    }

    private void discoverSaddleTests()
    {
        tests = new EnumMap<>(LoadPhase.class);
        for (LoadPhase loadPhase : LoadPhase.values())
        {
            LauncherConfig config = buildLauncherConfig(loadPhase);
            LauncherDiscoveryRequest launcherDiscoveryRequest = buildDiscoveryRequest(loadPhase);
            Launcher launcher = LauncherFactory.create(config);
            System.setProperty(ENGINE_ACTIVE_SYSPROP, "true");
            TestPlan testPlan = launcher.discover(launcherDiscoveryRequest);
            LOGGER.info("Discovered {} tests for {}", testPlan.countTestIdentifiers(TestIdentifier::isTest), loadPhase);
            System.setProperty(ENGINE_ACTIVE_SYSPROP, "false");
            tests.put(loadPhase, new Tests(launcher, testPlan));
        }
    }

    private void setup()
    {
        SADDLE_LOG_DIR.mkdirs();
        InputStream configFile = ClassLoader.getSystemClassLoader().getResourceAsStream("saddle-config.json");
        if (configFile == null)
            throw new IllegalStateException("Missing saddle-config.json");
        configuration = SaddleConfiguration.from(configFile);
    }
    
    @Mod.EventHandler
    public void init(FMLInitializationEvent event)
    {
        if (isEnabled())
        {
            LOGGER.info("saddle.disable is true, skipping init");
            return;
        }
        executeSaddleTests(LoadPhase.INIT);
        if (shouldExitOnTestCompletion() && !tests.get(LoadPhase.POST_INIT).hasTests())
        {
            LOGGER.info("All tests complete, JVM shutting down");
            FMLCommonHandler.instance().exitJava(0, false);
        }
    }
    
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        if (isEnabled())
        {
            LOGGER.info("saddle.disable is true, skipping post-init");
            return;
        }
        executeSaddleTests(LoadPhase.POST_INIT);
        if (shouldExitOnTestCompletion())
        {
            LOGGER.info("All tests complete, JVM shutting down");
            FMLCommonHandler.instance().exitJava(0, false);
        }
    }
    
    private void executeSaddleTests(LoadPhase loadPhase)
    {
        LOGGER.info("Running tests for {}", loadPhase);
        System.setProperty(ENGINE_ACTIVE_SYSPROP, "true");
        SummaryGeneratingListener summariser = new SummaryGeneratingListener();
        tests.get(loadPhase).execute(summariser, new SaddleTestExecutionLogger(LOGGER, Level.INFO));
        System.setProperty(ENGINE_ACTIVE_SYSPROP, "false");
        outputTestResults(loadPhase, summariser.getSummary());
    }

    private LauncherConfig buildLauncherConfig(LoadPhase loadPhase)
    {
        LauncherConfig config = LauncherConfig.builder()
            .enableTestEngineAutoRegistration(false)
            .addTestEngines
            (
                new SaddleTestEngine(loadPhase)
            )
            .build();
        return config;
    }

    private LauncherDiscoveryRequest buildDiscoveryRequest(LoadPhase loadPhase)
    {
        LauncherDiscoveryRequest launcherDiscoveryRequest = LauncherDiscoveryRequestBuilder.request()
            .selectors(configuration.getSelectors(loadPhase))
            .filters(configuration.getFilters(loadPhase))
            .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
            .build();
        return launcherDiscoveryRequest;
    }

    private void outputTestResults(LoadPhase loadPhase, TestExecutionSummary summary)
    {
        LOGGER.info("({} found, {} skipped, {} aborted, {} started, {} failed, {} passed) in {} ms", 
            summary.getTestsFoundCount(), summary.getTestsSkippedCount(), summary.getTestsAbortedCount(), 
            summary.getTestsStartedCount(), summary.getTestsFailedCount(), summary.getTestsSucceededCount(),
            summary.getTimeFinished() - summary.getTimeStarted());
        summary.printFailuresTo(new PrintWriter(System.err));
        try 
        (
            PrintWriter err = new PrintWriter(new File(SADDLE_LOG_DIR, loadPhase.toString().toLowerCase()) + ".err.txt");
            PrintWriter out = new PrintWriter(new File(SADDLE_LOG_DIR, loadPhase.toString().toLowerCase()) + ".out.txt");
        )
        {
            summary.printFailuresTo(err);
            summary.printTo(out);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private boolean isEnabled()
    {
        return System.getProperty("saddle.disable", "false").equals("true");
    }

    private boolean shouldExitOnTestCompletion()
    {
        return System.getProperty("saddle.exitOnTestCompletion", "false").equals("true");
    }
}

package io.github.daomephsta.saddle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import io.github.daomephsta.saddle.engine.SaddleTest.LoadPhase;
import io.github.daomephsta.saddle.engine.SaddleTestEngine;
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
    private static final Logger LOGGER = LogManager.getLogger("Saddle");
    
    private final File SADDLE_LOG_DIR = new File("logs/saddle");
    private SaddleConfiguration configuration;
    
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        setup();
        executeSaddleTests(LoadPhase.PRE_INIT);
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
        executeSaddleTests(LoadPhase.INIT);
    }
    
    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        executeSaddleTests(LoadPhase.POST_INIT);
    }
    
    private void executeSaddleTests(LoadPhase loadPhase)
    {
        if (System.getProperty("saddle.disable", "false").equals("true"))
        {
            LOGGER.info("saddle.disable is true, skipping tests for {}", loadPhase);
            return;
        }
        LOGGER.info("Running tests for {}", loadPhase);
        LauncherConfig config = buildLauncherConfig(loadPhase);
        LauncherDiscoveryRequest launcherDiscoveryRequest = buildDiscoveryRequest(loadPhase);
        Launcher launcher = LauncherFactory.create(config);
        System.setProperty("saddle.active", "true");
        SummaryGeneratingListener summariser = new SummaryGeneratingListener();
        launcher.execute(launcherDiscoveryRequest, summariser);
        System.setProperty("saddle.active", "false");
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
}

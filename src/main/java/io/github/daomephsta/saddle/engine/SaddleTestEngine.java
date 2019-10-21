package io.github.daomephsta.saddle.engine;

import java.lang.reflect.Method;
import java.util.Optional;

import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;

import io.github.daomephsta.saddle.engine.SaddleTest.LoadPhase;


public class SaddleTestEngine implements TestEngine
{
    private static final Splitter COMMA = Splitter.on(',').trimResults();

    private final TestEngine jupiterEngine = new JupiterTestEngine();
    private final LoadPhase loadPhase;

    public SaddleTestEngine(LoadPhase loadPhase)
    {
        this.loadPhase = loadPhase;
    }

    @Override
    public String getId()
    {
        return "saddle-test-engine";
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId)
    {
        TestDescriptor engineDescriptor = jupiterEngine.discover(discoveryRequest, uniqueId);
        for (TestDescriptor descendant : engineDescriptor.getDescendants())
        {
            descendant.getSource().ifPresent(source -> 
            {
                if (source instanceof MethodSource)
                {
                    ClassSource classSource = descendant.getParent().flatMap(desc -> desc.getSource())
                        .map(s -> s instanceof ClassSource ? (ClassSource) s : null)
                        .orElseThrow(() -> new RuntimeException("Expected class source parent for " + descendant));
                    MethodSource methodSource = (MethodSource) source;
                    Class<?>[] parameterTypes = !methodSource.getMethodParameterTypes().isEmpty()
                        ? Streams.stream(COMMA.split(methodSource.getMethodParameterTypes()))
                            .map(typeName -> ReflectionSupport.tryToLoadClass(typeName).getOrThrow(e -> new RuntimeException(e)))
                            .toArray(Class[]::new)
                        : new Class[0];
                    try
                    {
                        Method method = classSource.getJavaClass().getDeclaredMethod(methodSource.getMethodName(), parameterTypes);
                        Optional<SaddleTest> saddleTest = AnnotationSupport.findAnnotation(method, SaddleTest.class);
                        saddleTest.ifPresent(a -> 
                        {
                            if (a.loadPhase() != loadPhase)
                                descendant.removeFromHierarchy();
                        }); 
                    }
                    catch (NoSuchMethodException | SecurityException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
        return engineDescriptor;
    }

    @Override
    public void execute(ExecutionRequest request)
    {
        jupiterEngine.execute(request);
    }
}

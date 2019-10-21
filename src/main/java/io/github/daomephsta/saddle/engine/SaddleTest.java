package io.github.daomephsta.saddle.engine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@EnabledIfSystemProperty(named = "saddle.active", matches = "true")
@Test
public @interface SaddleTest
{
    public enum LoadPhase {PRE_INIT, INIT, POST_INIT}
    
    public LoadPhase loadPhase();
}

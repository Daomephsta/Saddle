package io.github.daomephsta.saddle;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.PackageNameFilter;

import com.google.common.reflect.TypeToken;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;

import io.github.daomephsta.saddle.engine.SaddleTest.LoadPhase;
import net.minecraft.util.JsonUtils;

public class SaddleConfiguration
{
    private static final Gson DESERIALISER = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(LoadPhase.class, (JsonDeserializer<LoadPhase>) (json, type, context)
            -> LoadPhase.valueOf(JsonUtils.getString(json, "load_phase").toUpperCase()))
        .registerTypeAdapter(SaddleConfiguration.class, (JsonDeserializer<SaddleConfiguration>) (json, type, context)
            -> new SaddleConfiguration(context.deserialize(json, new TypeToken<Map<LoadPhase, PhaseConfiguration>>() {}.getType())))
        .registerTypeAdapter(PhaseConfiguration.class, new PhaseConfiguration.Deserialiser())
        .registerTypeAdapter(DiscoverySelector.class, new SelectorDeserialiser())
        .registerTypeAdapter(new TypeToken<Filter<String>>() {}.getType(), new FilterDeserialiser())
        .create();
    
    private final Map<LoadPhase, PhaseConfiguration> phaseConfigurations;
    
    private SaddleConfiguration(Map<LoadPhase, PhaseConfiguration> phaseConfigurations)
    {
        this.phaseConfigurations = phaseConfigurations;
    }
    
    public static SaddleConfiguration from(InputStream inputStream)
    {
        try(InputStreamReader inputReader = new InputStreamReader(inputStream))
        {
            return DESERIALISER.fromJson(inputReader, SaddleConfiguration.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to load saddle configuration", e);
        }
    }
    
    public DiscoverySelector[] getSelectors(LoadPhase loadPhase)
    {
        return phaseConfigurations.getOrDefault(loadPhase, PhaseConfiguration.NONE).includes;
    }
    
    public Filter<String>[] getFilters(LoadPhase loadPhase)
    {
        return phaseConfigurations.getOrDefault(loadPhase, PhaseConfiguration.NONE).excludes;
    }

    @Override
    public String toString()
    {
        return String.format("SaddleConfiguration [phaseConfigurations=%s]", phaseConfigurations);
    }

    private static class PhaseConfiguration
    {
        @SuppressWarnings("unchecked")
        static final PhaseConfiguration NONE = new PhaseConfiguration(new DiscoverySelector[0], new Filter[0]);
        
        final DiscoverySelector[] includes;
        final Filter<String>[] excludes;
        
        private PhaseConfiguration(DiscoverySelector[] includes, Filter<String>[] excludes)
        {
            this.includes = includes;
            this.excludes = excludes;
        }

        @Override
        public String toString()
        {
            return String.format("PhaseConfiguration [includes=%s, excludes=%s]", Arrays.toString(includes), Arrays.toString(excludes));
        }

        private static class Deserialiser implements JsonDeserializer<PhaseConfiguration>
        {
            @Override
            public PhaseConfiguration deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException
            {
                JsonObject jsonObj = JsonUtils.getJsonObject(json, "phase_configuration");
                DiscoverySelector[] includes = jsonObj.has("include") 
                    ? context.deserialize(jsonObj.get("include"), DiscoverySelector[].class)
                    : new DiscoverySelector[0];
                @SuppressWarnings("unchecked")
                Filter<String>[] excludes = jsonObj.has("exclude") 
                    ? context.deserialize(jsonObj.get("exclude"), Filter[].class)
                    : new Filter[0];
                return new PhaseConfiguration(includes, excludes);
            }
        }
    }
    
    private static class SelectorDeserialiser implements JsonDeserializer<DiscoverySelector>
    {
        @Override
        public DiscoverySelector deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObj = JsonUtils.getJsonObject(json, "include");
            if (jsonObj.entrySet().size() == 1)
            {
                if (jsonObj.has("package"))
                    return DiscoverySelectors.selectPackage(JsonUtils.getString(jsonObj, "package"));
                else if (jsonObj.has("class"))
                    return DiscoverySelectors.selectClass(JsonUtils.getString(jsonObj, "class"));
            }
            throw new JsonSyntaxException("Don't know how to parse " + json);
        }
    }
    
    private static class FilterDeserialiser implements JsonDeserializer<Filter<String>>
    {
        @Override
        public Filter<String> deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObj = JsonUtils.getJsonObject(json, "exclude");
            if (jsonObj.entrySet().size() == 1)
            {
                if (jsonObj.has("package"))
                    return PackageNameFilter.excludePackageNames(JsonUtils.getString(jsonObj, "package"));
                else if (jsonObj.has("class"))
                    return ClassNameFilter.excludeClassNamePatterns(JsonUtils.getString(jsonObj, "class"));
            }
            throw new JsonSyntaxException("Don't know how to parse " + json);
        }
    }
}

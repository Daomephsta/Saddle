# Saddle
A simple test harness for running JUnit 5's Jupiter engine inside Minecraft. It only works with Minecraft Forge at this time.

### Installing Saddle
Saddle is not available on any maven. You shoulddownload one of the precompiled releases, compile Saddle yourself, or use [JitPack](https://jitpack.io/). Saddle should be added to Gradle's `testImplementation` configuration.

### Creating Saddle Tests
Saddle tests are created in exactly the same way as Jupiter tests, with the exception that they must be annotated with `io.github.daomephsta.saddle.engine.SaddleTest` instead of `org.junit.jupiter.api.Test`. All other Jupiter annotations work as normal.

### Configuring Test Discovery
Before you can run Saddle, you must configure which packages/classes should be included/excluded from test discovery.  
Saddle is configured by creating a file at the root of your test resources directory named `saddle-config.json`. The format
is as follows.  
```
<root>=
{
  "pre_init": <PhaseConfiguration>, //Optional
  "init": <PhaseConfiguration>, //Optional
  "post_init": <PhaseConfiguration> //Optional
}

<PhaseConfiguration>=
{
  "include": <IncludeSpec[]>, //Optional
  "exclude": <ExcludeSpec[]> //Optional
}

<IncludeSpec[]>=
[
  <IncludeSpec>,
  ...
]

<IncludeSpec>={"package": "foo.bar"} || {"class": "foo.bar.Baz"}

<ExcludeSpec[]>=
[
  <ExcludeSpec>,
  ...
]

<ExcludeSpec>={"package": "foo.bar"} || {"class": "foo.bar.Baz"}
```


### Running Saddle Tests
Run your mod as usual. Saddle will discover and run Saddle tests according to its configuration. 
If you want to disable Saddle without uninstalling it, add `-Dsaddle.disable=true` to your VM arguments.

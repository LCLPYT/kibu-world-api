# kibu-world-api
An extension for the [fantasy](https://github.com/NucleoidMC/fantasy) mod for fabric. 
Allows for world re-creation from the persisted level.dat file. 
Also serves as runtime world manager, which holds handles to all created runtime worlds. 
This mod is part of the [kibu](https://github.com/LCLPYT/kibu) modding library, but packaged in a separate mod to avoid third-party mod dependencies in the base project.

## Gradle Dependency
You can install kibu-world-api via Gradle.

To use kibu in your project, modify your `build.gradle`:
```groovy
repositories {
    mavenCentral()
    
    maven {
        url "https://repo.lclpnet.work/repository/internal"
    }
}

dependencies {
    modImplementation 'work.lclpnet.mods.kibu:kibu-world-api:0.6.1+1.20.6'  // replace with your version
}
```
All available versions can be found [here](https://repo.lclpnet.work/#artifact/work.lclpnet.mods.kibu/kibu-world-api).

## API
You can interact with the API via the `KibuWorlds` class:
```java
import work.lclpnet.kibu.world.KibuWorlds;
import work.lclpnet.kibu.world.WorldManager;

WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);
```

### Opening a persisted world
You can load a persisted world from the `<level name>/dimensions/<namespace>/<path>` directory:
```java
import net.minecraft.util.Identifier;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

Identifier id = Identifier.of("foo", "bar");
Optional<RuntimeWorldHandle> handle = worldManager.openPersistentWorld(id);
```
This example will load the world stored in `<level name>/dimensions/foo/bar` and return a fantasy world handle.
On a dedicated server, `<level name>` will be the level name defined in the `server.properties` file, defaulting to **"world"**.

You can put any minecraft world into that location and the world will be loaded correctly.

### Loading a fantasy `RuntimeWorldConfig` from a level.dat file
One of the main purposes of this mod is to restore / load fantasy `RuntimeWorldConfig`s from an existing level.dat file, that exists in every minecraft world save.
```java
import xyz.nucleoid.fantasy.RuntimeWorldConfig;

Identifier id = Identifier.of("foo", "bar");
Optional<RuntimeWorldConfig> config = worldManager.getWorldConfig(id);
```
This example will try to load a `RuntimeWorldConfig` from the level.dat file `<level name>/dimensions/foo/bar/level.dat`.
If the config cannot be loaded, the `Optional` will be empty.

### Getting the `RuntimeWorldHandle` for a `ServerWorld`
In some cases, you may not have the `RuntimeWorldHandle` for a world created by some other mod.

One use case could be that some mod created aruntime world.
Your mod provides a command that can be used to unload / delete runtime worlds.
For those kinds of behaviour, the `RuntimeWorldHandle` for a given `ServerWorld` is required.
Your mod can't know or get the handle for the world, unless the other mod provides an API for that.

Kibu-world-api automatically keeps track of all `RuntimeWorldHandle`s and provides an API to interact with them.
```java
import net.minecraft.server.world.ServerWorld;

ServerWorld world = someWorld;
Optional<RuntimeWorldHandle> handle = worldManager.getRuntimeWorldHandle(world);
```

### Getting all `RuntimeWorldHandle`s that currently exist
You can get all the `RuntimeWorldHandle`s of runtime worlds that are currently loaded, e.g. to use them in a command argument or similar
```java
Set<RuntimeWorldHandle> handles = worldManager.getRuntimeWorldHandles();
```

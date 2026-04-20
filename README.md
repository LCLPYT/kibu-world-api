# kibu-world-api
An extension for the [fantasy](https://github.com/NucleoidMC/fantasy) mod for fabric. 
Allows for dimension re-creation by saving some aspects of dimensions in a vanilla-compatible format.
Also serves as runtime world manager, which holds handles to all created runtime worlds. 
This mod is part of the [kibu](https://github.com/LCLPYT/kibu) modding library, but packaged in a separate mod to avoid third-party mod dependencies in the base project.

## Features
- load and restore dimensions
- provides a runtime world manager API, that keeps track of runtime world handles
- creates level.dat for runtime dimensions to save details like spawn position, game time etc. of that specific dimension
- per-dimension game rules
- per-dimension weather
- per-dimension time
- per-dimension world spawns

## How it works
This mod injects some code into some parts of the Fantasy mod.
This allows for tracking of all `RuntimeLevelHandle`s etc. which is useful for other mods.

By default, Fantasy only allows for dimension creation by explicitly passing it the required chunk generator, game rules, time etc. using code.
This mod makes it possible to skip this step and to "just load a dimension".
For that, aspects like the chunk generator, game rules, current time, current weather etc. have to be stored somehow, which is not normally done by vanilla Minecraft.
This mod uses the same data storage as vanilla uses for whole worlds (`level.dat`, `data/minecraft/world_gen_settings.dat`, `data/minecraft/weather.dat` ...) but adapts it for single dimensions.
This way, data migrations should still be supported, since the same logic is reused.

In practice, this mod hooks into the world save process and creates all necessary data files in the dimension `data/minecraft/` folder, which normally only get written to the world `data/minecraft/` folder.
Additionally, a `level.dat` file is also created in the dimension directory, which is also normally only written to the world directory.
Once those files exist, the dimension may be loaded by kibu-world-api.

Some additional patches are also made, such as:
- using the correct game rule store in the /gamerule command
- introducing dimension-specific weather and modifying it using /weather
- introducing dimension-specific world spawns for runtime worlds (runtime dimensions may not be used as global world spawn)
- proper world-border integration, which fantasy still lacks

## Migration guide from 1.21.11 and earlier
> [!NOTE]
> Mojang changed the way dimension data is stored in Minecraft 26.1.
> If you've used kibu-world-api before, like in 1.21.11 and earlier, you'll need to migrate your worlds in order to use them in new versions.

The following migration will convert a "world" from a Minecraft version prior to 26.1 into a "dimension" compatible with 26.1 and later.
Unlike worlds, dimensions cannot be opened in singleplayer / on a server as standalone level.

1. Launch Minecraft 26.1 or later in singleplayer.
2. Copy the dimension you want to migrate to the `saves/` directory of your singleplayer instance.
3. In the singleplayer world selection screen, select the world and click "Upgrade and Play". Create a backup if you want. You don't need to join the world.
4. Verify the directory of the world contains `level.dat` and verify that `data/minecraft/world_gen_settings.dat` exists (otherwise it will not be loadable by kibu-world-api). If not, you must first create a new world to use as template or copy those files from another world.
5. If you want load the world as dimension, you may now proceed with [converting a world to a dimension](#converting-a-world-to-a-dimension)

## Converting a world to a dimension
A "world" is a combination of different dimensions (overworld, nether, end, ...).
Worlds can be joined in singleplayer and can be loaded by servers.
Dimensions, are the singular dimensions that make up the world (e.g. overworld, or your custom dimension).

To convert a world to a dimension, you must first know the dimension of the world that you want to "convert" (usually, this is the "overworld" dimension).
As kibu-world-api needs some extra files, such as level.dat and world_gen_settings.dat, you need to apply the following steps:

1. In the directory of the world, find the `dimensions/minecraft/overworld` directory (or alternatively another dimension) and copy its contents directly into the world directory.
2. Delete the `dimensions/` directory (optionally also the `players/` directory)
3. Verify the directory still contains `level.dat` and `data/minecraft/world_gen_settings.dat`. Otherwise, it will not be loadable by kibu-world-api. If they don't exist, you must first create a new world to use as template or copy those files from another world.

The "world" is now converted to be a "dimension" compatible with kibu-world-api.

Dimensions may be added to existing worlds by copying them to `dimensions/<namespace>/<path>`.
Those dimensions will not be loaded, unless you use fantasy (+ kibu-world-api) or create a datapack to tell Minecraft that your dimension exists.

## Converting a dimension to a world
If you want to convert a dimension to a world, apply this steps:
1. Create the `dimensions/minecraft/overworld/` directory (or a different dimension if you know what you are doing)
2. Copy the `data/` directory to the newly created directory
3. Move `entities/`, `poi`, `region/` to the newly created directory.

Now the world should be loadable in singleplayer or on servers.

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
    modImplementation 'work.lclpnet.mods.kibu:kibu-world-api:0.10.0+26.1.2'  // replace with your version
}
```
All available versions can be found [here](https://repo.lclpnet.work/#artifact/work.lclpnet.mods.kibu/kibu-world-api).

## API
You can interact with the API via the `KibuWorlds` class:
```java
import work.lclpnet.kibu.world.KibuLevels;
import work.lclpnet.kibu.world.WorldManager;

WorldManager worldManager = KibuWorlds.getInstance().getWorldManager(server);
```

### Opening a persisted world
You can load a persisted dimension from the `<level name>/dimensions/<namespace>/<path>` directory:
```java
import net.minecraft.util.Identifier;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

Identifier id = Identifier.fromNamespaceAndPath("foo", "bar");
Optional<RuntimeLevelHandle> handle = worldManager.openPersistentLevel(id);
```
This example will load the dimension stored in `<world name>/dimensions/foo/bar` and return a fantasy level handle.
On a dedicated server, `<world name>` will be the level name defined in the `server.properties` file, defaulting to **"world"**.

You can put any Minecraft dimension into that location, and it will be loaded correctly.
Refer to [converting a world to a dimension](#converting-a-world-to-a-dimension) if you want to convert an existing world to a dimension.

### Loading a fantasy `RuntimeLevelConfig` from a level.dat file
One of the main purposes of this mod is to restore / load fantasy `RuntimeLevelConfig`s from an existing level.dat file that exists in every minecraft world save.
```java
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

Identifier id = Identifier.of("foo", "bar");
Optional<RuntimeLevelConfig> config = worldManager.getStoredLevelConfig(id);
```
This example will try to load a `RuntimeLevelConfig` from the level.dat file `<level name>/dimensions/foo/bar/level.dat`.
If the `level.dat` file doesn't exist, or the config cannot be loaded, the `Optional` will be empty.

### Getting the `RuntimeLevelHandle` for a `ServerWorld`
In some cases, you may not have the `RuntimeLevelHandle` for a world created by some other mod.

One use case for this could be that your mod provides a command that allows you to unload / delete runtime worlds.
For those kinds of behaviors, the `RuntimeLevelHandle` for a given `ServerWorld` is required.
Your mod can't know the handle for the world, unless it has some service that provides that handle.
Such a service is provided by kibu-world-api.

Kibu-world-api automatically keeps track of all `RuntimeLevelHandle`s and provides an API to interact with them.
```java
import net.minecraft.server.world.ServerWorld;

ServerWorld world = someWorld;
Optional<RuntimeLevelHandle> handle = worldManager.getRuntimeLevelHandle(world);
```

### Getting all `RuntimeLevelHandle`s that currently exist
You can get all the `RuntimeLevelHandle`s of runtime worlds that are currently loaded, e.g. to use them in a command argument or similar
```java
Set<RuntimeLevelHandle> handles = worldManager.getRuntimeLevelHandles();
```

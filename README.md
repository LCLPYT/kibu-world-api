# kibu-world-api
An extension for the [fantasy](https://github.com/NucleoidMC/fantasy) mod for fabric. 
Allows for dimension re-creation by saving a few new vanilla compatible files into the dimension directory, needed for re-creating the world generator, game rules, weather etc.
Also serves as runtime world manager, which holds handles to all created runtime worlds. 
This mod is part of the [kibu](https://github.com/LCLPYT/kibu) modding library, but packaged in a separate mod to avoid third-party mod dependencies in the base project.

## Features
- load and restore dimensions
- provides a runtime world manager API, that keeps track of runtime world handles
- creates level.dat for runtime levels to save details like spawn position, game time etc. of that specific dimension
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
5. In the directory of the world, find the `dimensions/minecraft/overworld` directory and copy its contents directory into the world directory.
6. Delete the `dimensions/` directory
7. The "world" is now converted to be a "dimension" and may be copied to the target dimension directory.

Dimensions may be added to existing worlds by copying them to `dimensions/<namespace>/<path>`.
Those dimensions will not be loaded, unless you use fantasy (+ kibu-world-api) or create a datapack to tell Minecraft that your dimension exists.

If you want to convert a dimension to a world, revert the steps 6 and 5 (you can copy the whole `data/` folder to the dimension).

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
You can load a persisted world from the `<level name>/dimensions/<namespace>/<path>` directory:
```java
import net.minecraft.util.Identifier;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

Identifier id = Identifier.of("foo", "bar");
Optional<RuntimeLevelHandle> handle = worldManager.openPersistentWorld(id);
```
This example will load the world stored in `<level name>/dimensions/foo/bar` and return a fantasy world handle.
On a dedicated server, `<level name>` will be the level name defined in the `server.properties` file, defaulting to **"world"**.

You can put any minecraft world into that location and the world will be loaded correctly.

### Loading a fantasy `RuntimeLevelConfig` from a level.dat file
One of the main purposes of this mod is to restore / load fantasy `RuntimeLevelConfig`s from an existing level.dat file, that exists in every minecraft world save.
```java
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

Identifier id = Identifier.of("foo", "bar");
Optional<RuntimeLevelConfig> config = worldManager.getWorldConfig(id);
```
This example will try to load a `RuntimeLevelConfig` from the level.dat file `<level name>/dimensions/foo/bar/level.dat`.
If the config cannot be loaded, the `Optional` will be empty.

### Getting the `RuntimeLevelHandle` for a `ServerWorld`
In some cases, you may not have the `RuntimeLevelHandle` for a world created by some other mod.

One use case could be that some mod created aruntime world.
Your mod provides a command that can be used to unload / delete runtime worlds.
For those kinds of behaviour, the `RuntimeLevelHandle` for a given `ServerWorld` is required.
Your mod can't know or get the handle for the world, unless the other mod provides an API for that.

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

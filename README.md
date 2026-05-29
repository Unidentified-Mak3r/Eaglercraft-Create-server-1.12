# EaglerCraftX Server

## Credits
Original Project: Lax1Dude
<br>
1.12 Project: PeytonPlayz595
<br>
Original Server Fork: EcoliEater87
<br> 
Adapted to Canary Craft (ADSCRAFT): QuizzityMC
<br>

## To make an eaglercraft 1.12 server, follow the instructions below:
Here is how you can setup a connection:
<br>
<br>
First, go to the top of the repo and click on code > codespaces > create codespace
<br>
now you have your own free server instance to host eaglercraft. Next you need to run the setup commands:
<br>
<br>
Create a terminal tab and paste the following:<br>
<br>
enter the following: `cd bungee && sudo java -jar bungee.jar`
<br>
then, make a new tab and enter the following: 'cd server && sudo java -jar server.jar'
<br>
Now go to the ports area and forward (and make public) port `8081`
<br>
Load up the client! When you are in, copy the link, paste it in the add server section, replace https:// with wss:// and join!
Your eaglercraft server is setup!

PS: You can use this as a normal server too (like properly hosted) just clone this repo and run it in much the same way.

## HunterCompass plugin

HunterCompass is included as a production-ready legacy Bukkit/Eaglercraft-compatible plugin source project in this repository.
It binds an individual compass item to a player when you hit that player while holding an unbound compass, stores the target on the compass NBT, and updates the holder's compass target once per second while the compass is held.

### Features

- Hit a player with an unbound compass to permanently bind that compass to the hit player's UUID.
- Bound compass items store target UUID, username, world, and last known coordinates in item NBT under `HunterCompass`.
- Multiple compass items can track different players because the tracking data lives on each item stack.
- The held compass updates every second for low-lag synchronization with Eaglercraft browser clients.
- Same-world targets point directly at the live player location.
- Cross-world or offline targets point at the target's last known coordinates projected into the holder's current world, avoiding legacy client desync from cross-world compass targets.
- Right-click a bound compass to display the target username, status, distance, world, and last known location.
- `/unbindcompass` removes the target NBT from the compass in hand so it can be rebound.

### Dependency setup

This project intentionally avoids modern Paper-only APIs. It compiles against the patched legacy server API already present in this repository:

```xml
<systemPath>${project.basedir}/server/cache/patched_1.8.8.jar</systemPath>
```

That jar exposes the legacy Bukkit/CraftBukkit API used by Eaglercraft server bundles. If your Eaglercraft 1.12 server fork uses a different CraftBukkit NMS package, update `CompassItemData` to the matching `net.minecraft.server` and `org.bukkit.craftbukkit` package names before building.

### Build instructions

From the repository root, run:

```bash
mvn clean package
```

The compiled plugin will be written to:

```text
target/HunterCompass.jar
```

If Maven is unavailable, compile manually against the included server jar:

```bash
mkdir -p target/classes
javac --release 8 -cp server/cache/patched_1.8.8.jar -d target/classes $(find src/main/java -name '*.java')
cp -R src/main/resources/* target/classes/
sed -i 's/${project.version}/1.0.0/g' target/classes/plugin.yml
jar cf target/HunterCompass.jar -C target/classes .
```

### Installing on an Eaglercraft 1.12-compatible server

1. Build `target/HunterCompass.jar`.
2. Stop the backend Bukkit/Paper server.
3. Copy `target/HunterCompass.jar` into the backend server's `plugins/` directory, for this repository usually `server/plugins/`.
4. Start the backend server normally, for example:
   ```bash
   cd server && java -jar server.jar
   ```
5. Confirm the console prints `HunterCompass enabled`.
6. Join through the Eaglercraft client, hold a compass, hit another player to bind it, then right-click to inspect the target.

### Configuration

The plugin creates `plugins/HunterCompass/config.yml` on first start. The main option is `tracking-interval-ticks`, defaulting to `20` ticks (one second). Message text can also be customized there.

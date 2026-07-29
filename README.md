# VelioraVein

A premium, lightweight Minecraft Paper 1.21.8 plugin that implements three powerful skills:
1. **Vein Miner** - Mine connected ores in a chain while crouching.
2. **Tree Feller** - Chop down entire trees of the same wood type instantly, including leaf decay.
3. **Farmer** - Harvest fully grown crops in a cascading pattern and replant automatically.

## Requirements
- **Server:** Paper 1.21.8 or higher
- **Java:** Java 21
- **Economy:** Vault & an active economy plugin (e.g., EssentialsX)

## Commands
- `/skill` or `/skills` - Open the visual Skill GUI.

## Features
- **Vault Economy Support:** Cost-based skill purchases.
- **Duration Control:** Clean timed expiration tracked in `players.yml`.
- **Dynamic Reminders:** Auto-broadcasts countdown warnings (10, 5, 1 mins) and expiries.
- **Optimized Scanning:** Uses non-recursive Breadth-First Search (BFS) to prevent stack overflows and lag.

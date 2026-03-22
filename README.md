# TebexClaimSystem

TebexClaimSystem is a Minecraft 1.8 Bukkit/Spigot plugin that lets players claim Tebex purchases in-game through a custom GUI and allows players to collect their purchases the next time they are online.

## What it does

- Accepts Tebex-delivery style commands from console: `tebexclaimsystem add <username> <productName> <quantity>`
- Stores pending claims in a flat YAML file so claims survive restarts
- Notifies players on join when they have pending items
- Sends online reminder messages every 30 minutes to players with pending claims
- Opens a GUI with one entry per pending product plus a **Claim All** button
- Checks free inventory space before claiming
- Executes configurable product commands with `{player}` placeholders
- Logs each claim with timestamp, player, product, and quantity

## Installation

1. Build the plugin jar.
2. Put the jar in your server's `plugins/` folder.
3. Start/restart the server.
4. Edit `plugins/TebexClaimSystem/config.yml` to match your products/commands.
5. Reload/restart after config changes.

## Commands

- `/tebexclaimsystem` - Opens the claim GUI for players.
- Aliases: `/tcs`, `/claimstore`, `/claims`, `/webstoreclaim`, `/wsc`.
- `tebexclaimsystem add <username> <productName> <quantity>` - Console-only command (intended for Tebex delivery).
- `/tebexclaimsystem reload` - Reloads `config.yml` without restarting the server (`tebexclaimsystem.reload`, default `op`).

### Tebex console example

```text
tebexclaimsystem add Notch vip_rank 1
tebexclaimsystem add Notch starter_kit 2
```

## Configuration reference

Main config file: `plugins/TebexClaimSystem/config.yml`

### `products`

Define each claimable product by ID. The ID must match what Tebex sends as `<productName>`.

- `displayName` - Name shown in GUI
- `itemMaterial` - GUI icon material
- `requiredSlotsPerUnit` - Space check amount per purchased unit
- `commands` - Commands run on claim (supports `{player}` and `{quantity}`)

Example:

```yaml
products:
  vip_rank:
	displayName: "&6VIP Rank"
	itemMaterial: "EMERALD"
	requiredSlotsPerUnit: 1
	commands:
	  - "lp user {player} parent set vip"
  starter_kit:
	displayName: "&bStarter Kit"
	itemMaterial: "CHEST"
	requiredSlotsPerUnit: 2
	commands:
	  - "give {player} diamond_sword 1"
	  - "give {player} bread 16"
```

### `gui`

- `title` - Inventory title
- `size` - Inventory size (9-54, rounded to a multiple of 9)
- `claimAllSlot` - Slot index for Claim All button
- `claimAllName` / `claimAllMaterial` - Claim All button display
- `quantityLore` - Lore line for quantity text

### `messages`

All player-facing and feedback messages are configurable.

- Prefix token: `{prefix}`
- Color support: `&` color codes

## Runtime data files

Created in `plugins/TebexClaimSystem/`:

- `pending-claims.yml` - Current pending claims per player/product
- `claim-log.txt` - Claim audit log entries

`pending-claims.yml` is updated immediately whenever claims are added or removed.

## Claim flow behavior

1. Tebex (or console) runs `tebexclaimsystem add ...`
2. TebexClaimSystem stores pending quantity for that player/product
3. Player joins and gets a notification if pending claims exist
4. Player opens `/tebexclaimsystem` (or any configured alias)
5. Player clicks one item or **Claim All**
6. Plugin validates available inventory slots
7. If valid, product commands are dispatched and pending entry is removed
8. Claim is written to `claim-log.txt`

## Build

Windows PowerShell:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

Run tests:

```powershell
.\gradlew.bat test
```

## Troubleshooting

- `unknown product` message: ensure Tebex product ID exactly matches a key under `products`.
- Player cannot claim due to space: increase empty inventory slots or tune `requiredSlotsPerUnit`.
- Console command denied in-game: `add` is intentionally restricted to console senders.
- No GUI items: verify `pending-claims.yml` has entries and product IDs exist in `config.yml`.


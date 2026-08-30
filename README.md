# zMDisguise

A modern and fully configurable disguise plugin for **Paper, Folia, and Velocity** servers.

zMDisguise allows players to change their identity by disguising themselves as other players, including their **display name**, **skin**, and optionally their **LuckPerms groups**. It features an intuitive GUI, disguise persistence, PlaceholderAPI support, and much more.

## ✨ Features

- **Multi-Platform Support**: Works on Paper, Folia, and integrates with Velocity/BungeeCord proxies for network-wide disguises!
- Complete and intuitive GUI.
- Change display name and skin.
- Copy another player's complete identity.
- Save and reuse disguises.
- Admin menu with disguised player list.
- Teleport to disguised players.
- Configurable BossBar and ActionBar.
- Chat displays the disguise name.
- Optional disguise persistence after reconnecting.
- Admin disguise notifications.
- `/whois` command.
- LuckPerms integration.
- PlaceholderAPI expansion.
- SkinsRestorer support.
- Automatic update checker.
- Fully customizable GUIs.
- Base64 custom head support.

## 📦 Requirements

- Java 21+
- Paper 1.19+ (or Folia)

### Optional Dependencies

- LuckPerms
- PlaceholderAPI
- SkinsRestorer
- Velocity (Proxy-side for global tablist sync)

## 📥 Installation

1. Download the latest release.
2. Place `zMDisguise.jar` into your `plugins/` folder.
3. Restart your server.
4. Configure `config.yml` if desired.

## 📖 Commands

| Command | Description |
|---------|-------------|
| `/disguise` | Open the main menu. |
| `/disguise <player> <name>` | Apply a disguise. |
| `/disguise save <name>` | Save the current disguise. |
| `/undisguise` | Remove your disguise. |
| `/undisguise <player>` | Remove another player's disguise. |
| `/zmdisguise reload` | Reload the plugin. |
| `/zmdisguise whois <fakeName>` | Reveal the real identity. |

## 🔐 Permissions

| Permission | Description |
|------------|-------------|
| `zmdisguise.use` | Use the disguise system. |
| `zmdisguise.admin` | Admin commands and menus. |
| `zmdisguise.notify` | Receive disguise notifications. |
| `zmdisguise.reload` | Reload the plugin. |

## 🔗 PlaceholderAPI

```
%zmdisguise_disguised%
%zmdisguise_name%
%zmdisguise_real_name%
%zmdisguise_prefix%
%zmdisguise_suffix%
%zmdisguise_group%
%zmdisguise_skin%
```

## ❤️ Support

If you encounter a bug or have a feature request, please open an Issue on GitHub.

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.

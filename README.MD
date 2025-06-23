# AuthGuard

AuthGuard is an authentication plugin for Spigot/Bukkit servers, designed to secure offline-mode servers by requiring players to register and log in. It's lightweight, highly configurable, and supports multiple languages out of the box.

## Features

- **Secure Authentication:** Players must register with a password and log in each time they join.
- **Password Hashing:** Passwords are securely hashed using SHA-256, ensuring they are not stored in plaintext.
- **Database Support:** Works with both **MySQL** for remote storage and **SQLite** for simple, local server setups.
- **CAPTCHA System:** Optional CAPTCHA verification during registration to prevent bot attacks.
- **Multi-Language Support:** All player-facing messages are configurable and can be translated. Default languages include English (en), German (de), Polish (pl), and Russian (ru).
- **Action Blocking:** Prevents unauthenticated players from moving, interacting, taking or dealing damage, and managing items.
- **Login Timeout:** Automatically kicks players who fail to log in or register within a configurable time limit, with a visual countdown on the XP bar.
- **Login Attempts:** Limits the number of failed login attempts before a player is kicked.
- **Admin Tools:** Includes commands for reloading the plugin's configuration and managing player data in the database.
- **Customizable Messages:** Utilizes MiniMessage format for all plugin messages, allowing for full color and style customization.
- **Skript Integration:** Dispatches custom events for player login and registration, allowing for integration with Skript.

## Commands

### Player Commands

- #### `/register <password> [confirm_password] [captcha]`
  Registers a new player on the server.
    - `<password>`: The password you want to set.
    - `[confirm_password]`: Required if `repeat_password` is enabled in `config.yml`.
    - `[captcha]`: Required if `captcha` is enabled in `config.yml`.

- #### `/login <password>`
  Logs an already registered player into the server.
    - `<password>`: Your account password.

### Admin Commands

- #### `/authguard <subcommand>`
  The main command for plugin administration.
    - **Permission:** `authguard.admin`
    - **Subcommands:**
        - `help`: Displays a list of available commands.
        - `reload`: Reloads the plugin's configuration and reconnects to the database.
        - `database remove <player>`: Removes a player's registration data from the database.

## Permissions

- `authguard.admin` - Grants access to all `/authguard` subcommands.

## Installation

1.  Download the latest release JAR file.
2.  Place the `AuthGuard.jar` file into your server's `/plugins` directory.
3.  Restart your server.
4.  Edit the generated `config.yml` file in `/plugins/AuthGuard/` to configure the database and other settings.
5.  Set your desired language in `config.yml` and reload the plugin or restart the server.

## Configuration

The `config.yml` allows you to customize various aspects of the plugin:

## Event Support: Bukkit & Skript

AuthGuard dispatches custom events for player login and registration that can be used both in Bukkit plugin event listeners and Skript scripts.

- **Bukkit Events:**  
  You can listen for `PlayerLoggedInEvent` and `PlayerRegisteredEvent` in your Java plugins by registering standard Bukkit event handlers.

- **Skript Events:**  
  AuthGuard registers these events with Skript, allowing you to handle them easily in `.sk` scripts.  
  Example Skript usage:
  ```skript
  on player logged in:
      send "&a[AuthGuard] &f%player% has successfully logged in!" to console
      send "&aWelcome back, %player%!" to player

  on player registered:
      send "&a[AuthGuard] &f%player% has just registered!" to console
  ```
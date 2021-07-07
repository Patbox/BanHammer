![](https://i.imgur.com/0o0rruV.png)

# BanHammer

Simple, customisable punishment utility mod for Fabric. Allows moderators to permanently/temporary ban, mute or kick players.
Supports [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) used by LuckPerms and PlayerRoles.

Every user facing message is configurable with support of [advanced formatting](https://github.com/Patbox/FabricPlaceholderAPI/blob/1.17/TEXT_FORMATTING.md)

It has build in support for SQLite and MySQL databases.
## Commands and permissions

| Command            | Permission               | Description                       |
| ------------------ | ------------------------ | --------------------------------- |
| /banhammer         | banhammer.commands.main (default) | Sends information about banhammer |
| /banhammer reload  | banhammer.commands.reload (op 4)  | Reloads mods config               |
| /banhammer import \<source> \[\<remove>\] | banhammer.commands.import (op 4)      | Imports bans from other sources (and removes them from it, if remove is set to true)
| /ban \<player> \[\<reason>] | banhammer.punish.ban (op 3/config) | Bans player |
| /tempban \<player> \<duration> \[\<reason>] | banhammer.punish.tempban (op 3/config) | Bans player for provided time |
| /ban-ip \<player> \[\<reason>] | banhammer.punish.banip (op 3/config) | Bans player (by ip) |
| /tempban-ip \<player> \<duration> \[\<reason>] | banhammer.punish.tempbanip (op 3/config) | Bans player (by ip) for provided time |
| /mute \<player> \[\<reason>] | banhammer.punish.mute (op 3/config) | Mutes player |
| /tempmute \<player> \<duration> \[\<reason>] | banhammer.punish.tempmute (op 1) | Mutes player for provided time |
| /kick \<player> \[\<reason>] | banhammer.punish.kick (op 3/config) | Kicks player from server |
| /unban \<player> | banhammer.unpunish.unban (op 3/config) | Removes player's (temp)bans |
| /unban-ip \<player> | banhammer.unpunish.unbanip (op 3/config) | Removes player's (temp)ip bans |
| /unmutes \<player> | banhammer.unpunish.unmute (op 3/config) | Removes player's (temp)mute |
| /pardon \<player> | banhammer.unpunish.pardon (op 3/config) | Removes all of player's punishments |

Additionally you can add `-s` Before reason, if you want to make that punishment silent.

Duration can be expressed in seconds (`15`/`15s`), minutes (`3m`), hours (`24h`), days (`7d`), years (`1y`)
or any combination of these (`5y3d9h3m8s`).

## Configuration
You can find this config in `config/banhammer/config.json`.
```json5
{
  "CONFIG_VERSION_DONT_TOUCH_THIS": 1,
  "punishmentsAreSilent": false,             // Makes all punishments silent
  "storeAllPunishmentsInHistory": true,      // Stores all punishments in additional database table
  "muteBlockedCommands": [                   // List of commands blocked to muted players
    "msg",
    "me"
  ],
  "standardBanPlayersWithBannedIps": false,  // Gives users standard bans, when they are ip banned
  "autoBansFromIpBansAreSilent": true,       // Makes autobans from ip ban silent
  "defaultTempPunishmentDurationLimit": "-1",// Sets limit for duration of temporary punishments. -1 = no limit
  "permissionTempLimit": {                   // Allows to change duration limit of punishment with `banhammer.duration.<entry>` permission
    "example": "31d"
  },
  "discordWebhookUrl": "",                   // Url for discord webhook, leave empty to disable it
  "databaseType": "sqlite",                  // Changes database type ("sqlite" or "mysql")
  "sqliteDatabaseLocation": "banhammer-sqlite.db",  // sqlite databases location (from server's directory)
  "mysqlDatabaseAddress": "",                // Address of mysql database
  "mysqlDatabaseName": "",                   // Name of mysql database
  "mysqlDatabaseUsername": "",               // Username of mysql user
  "mysqlDatabasePassword": ""                // Password of mysql user
}
```
You can also edit in game messages which are stored in `config/banhammer/messages.json`
You can also edit discord ones in `config/banhammer/discord-messages.json`
It's little more tricky as it has many values:
```json5
{
  /*...*/
  "send[Type]Message": true,     // Enables sending of the message       
  "temp[Type]Message": {     
    "embed": true,               // Sends message as embed
    "message": [
      /* Message content */
    ],
    "avatar": "",               // Url to avatar (or empty to use default one)
    "name": "",                 // Name (or empty to use default one)
    "embedTitle": "",           // Embed's title (or empty to disable)
    "embedTitleUrl": "",        // Embed's title url (or empty to disable)
    "embedColor": "red",        // Embed color (or empty to disable)
    "embedImage": "",           // Embed's image (or empty to disable)
    "embedThumbnail": "",       // Embed's thumbnail (or empty to disable)
    "embedFields": [            // Embed fields (or empty to disable)
      /* You can have as many as many of these as you want */
      {
        "name": "",             // Field's name
        "content": "",          // Field's content
        "inline": false         // Makes field inline
      }
    ] 
  },
  /*...*/
}
```


Discord and ingame messages can use predefined variables:
For punishments: `${operator}`, `${operator_uuid}`, 
`${banned}`, `${banned_uuid}`, `${reason}`, `${expiration_date}`, `${expiration_time}`

For removing: `${operator}`, `${operator_uuid}`, `${banned}`, `${banned_uuid}`
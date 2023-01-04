# TeamSpeak Connect

Connects TeamSpeak users via different chat services.

## Supported Services

- [Telegram](https://telegram.org/)
- [Discord](https://discordapp.com/)

## Installation

*Coming soon...*

### Environment Variables

| Variable                   | Description                                                        | Default              |
|----------------------------|--------------------------------------------------------------------|----------------------|
| `TEAMSPEAK_HOST`           | The TeamSpeak host URL                                             | `localhost`          |
| `TEAMSPEAK_QUERY_PORT`     | The TeamSpeak SSH query port                                       | `10022`              |
| `TEAMSPEAK_QUERY_USERNAME` | The TeamSpeak SSH query username                                   | `serveradmin`        |
| `TEAMSPEAK_QUERY_PASSWORD` | The TeamSpeak SSH query password                                   | `password`           |
| `TEAMSPEAK_SERVER_ID`      | The TeamSpeak virtual server ID                                    | `1`                  |
| `TEAMSPEAK_BOT_USERNAME`   | The TeamSpeak bot username                                         | `bot`                |
| `TEAMSPEAK_BOT_PASSWORD`   | The TeamSpeak bot password                                         | `password`           |
| `TEAMSPEAK_BOT_NAME`       | The name of the TeamSpeak bot                                      | `TeamspeakConnect`   |
| `TEAMSPEAK_BOT_CHANNEL_ID` | The TeamSpeak channel ID where the bot should connect to           | `0`                  |
| `AUTH_CODE_LIFETIME`       | The minimum lifetime of the generated auth codes (in milliseconds) | `300000` (5 Minutes) |
| `DATABASE_URL`             | The MongoDB database URL                                           | `localhost:27017`    |
| `DATABASE_NAME`            | The MongoDB database name                                          | `teamspeakconnect`   |
| `TELEGRAM_BOT_TOKEN`       | The Telegram bot token                                             | ` `                  |
| `DISCORD_TOKEN`            | The Discord bot token                                              | ` `                  |
| `DISCORD_CHANNEL`          | The Discord channel ID for the bot                                 | `0`                  |

# Drinko Music

A Discord music bot written in Java


## Supported Sources

- [YouTube](https://www.youtube.com/)
- [SoundCloud](https://soundcloud.com/)
- [Bandcamp](https://bandcamp.com/)
- [Vimeo](https://vimeo.com/)
- [Twitch](https://www.twitch.tv/)
- [GetYarn](https://getyarn.io/)
- HTTP URLs


## Commands

- `/play` `link-or-query`
    - Will play an audio track if `link-or-query` is a link from one of the [sources](#supported-sources).
    - If the link is not from a supported source, youtube will be queried and a user will be able to select from the top five results.
- `/list` `page (optional)`
  - If no page is provided the currently playing audio and all audio in the queue for the first page will be displayed. If the page is provided the audio tracks in the queue for that page will be displayed.
- `/next` `number (optional)`
  - If no number is provided, the current audio track will be skipped and the next audio track in the queue will play. If a number is provided `number` of audio tracks will be skipped in the queue. Use `/list` to help you determine what the value of `number` should be if you are trying to skip to a specific audio track.
- `/repeat` `type`
  - `type: All`
    - Repeats all audio tracks sequentially, indefinitely.
  - `type: One`
    - Repeats a single audio track indefinitely.
  - `type: None`
    - Disables all repeat functionality.
- `/shuffle` `enable`
  - `enable: true`
    - Enables shuffle mode. The queue will be randomly shuffled and will repeat sequentially indefinitely, ignoring the repeat type if it is `None`.
        - Using `/list` while `/shuffle` is enabled will have the reactions 🔀, 🔁.
        - If the repeat type is `One` the repeat mode will be respected and the same Audio Track will repeat indefinitely.
          - Using `/list` while `/shuffle` is enabled, and `/repeat` `type` `one` will have the reactions 🔀, 🔂.
  - `enable: false`
    - Disables shuffle mode, the queue will return to the order it was in previously to when `/shuffle` was first invoked. The queue will start from the currently playing audio track in the original queue.
- `/clear`
  - Stops the currently playing audio track if one is playing and removes all audio tracks from the queue.
- `/pause`
  - Pauses the currently playing audio track if one is playing.
- `/resume`
  - Resumes the currently playing audio track if one was playing.

## Setup

Until I get around to setting up releases this you will have to clone this repo and create the jar using maven.

```
git clone https://github.com/Samadroberts/Drinko-Music.git
mvn clean install
mvn package
```

### Running the Application

If you want to listen to age restricted YouTube videos you must provide a youtube account and password as parameters. If you do not provide an account a password restricted YouTube videos will be hit or miss, some seem to load and other do not.

Set the following environment variables
 - `DISCORD_TOKEN` (required, your discord bot token)
 - `YOUTUBE_USERNAME` (optional)
 - `YOUTUBE_PASSWORD` (optional)

Launch the server using:

```java -jar drinko-music-VERSION_NUMBER_HERE-SNAPSHOT.jar```


## Using Spring Args (RECOMMENDED LOCAL ONLY)

**No YouTube credentials**
```
java -jar -Ddiscord.api.token=DISCORD_TOKEN \
    drinko-music-VERSION_NUMBER_HERE-SNAPSHOT.jar
```

**With YouTube Restricted Videos**
```
java -jar -Dorg.drinko.youtube.password=YOUTUBE_PASSWORD \
    -Dorg.drinko.youtube.username=YOUTUBE_USERNAME \
    -Ddiscord.api.token=DISCORD_TOKEN \
    drinko-music-VERSION_NUMBER_HERE-SNAPSHOT.jar
```




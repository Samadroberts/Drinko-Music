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
    - Will play an audio track if `link-or-query` is a link from one of the [supported sources](#supported-sources).
    - If the link is not from a supported source, YouTube will be queried and a user will be able to select from the top five results.
- `/list` `page (optional)`
  - If no page is provided the currently playing audio track and all audio tracks in the queue for the first page will be displayed. If the page is provided the audio tracks in the queue for that page will be displayed.
- `/next` `number (optional)`
  - If no number is provided, the current audio track will be skipped and the next audio track in the queue will play. If a number is provided `number` of audio tracks will be skipped in the queue. Use `/list` to help you determine what the value of `number` should be if you are trying to skip to a specific audio track.
- `/remove` `index`
  - Removes the track at the specified index in the queue. An index of `0` will remove the currently playing track. Removing a track from a shuffled queue will also remove it from the unshuffled queue. Use `/list` to help you determine what the value of `index` should be if you are trying to remove to a specific audio track.
- `/repeat` `type`
  - `type: All`
    - Repeats all audio tracks in the queue sequentially, indefinitely.
  - `type: One`
    - Repeats a single audio track indefinitely.
  - `type: None`
    - Disables all repeat functionality.
- `/shuffle` `enable`
  - `enable: true`
    - Enables shuffle mode. The queue will be randomly shuffled and will repeat sequentially indefinitely, ignoring the repeat type if it is `None`.
        - If the repeat type is `One` the repeat mode will be respected and the same audio track will repeat indefinitely.
  - `enable: false`
    - Disables shuffle mode. The queue will return to the order it was in when `/shuffle` was first invoked. The queue will start from the position of the currently playing audio track in the original queue.
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

**ENABLE THE FOLLOWING PRIVILEGED GATEWAY INTENTS**
 - Message content intent
 - Server Members Intent
 - Presence Intent


Set the following environment variables
 - `DISCORD_TOKEN` (required, your discord bot token)
 - `PO_TOKEN` (optional, YouTube Proof of Origin Token. See: [What the hell is a Proof of Origin Token](#what-the-hell-is-a-proof-of-origin-token))
 - `VISITOR_DATA` (optional, YouTube Visitor data. See: [What the hell is a Proof of Origin Token](#what-the-hell-is-a-proof-of-origin-token)

## What the hell is a Proof of Origin Token?
A poToken, also known as a "Proof of Origin Token" is a way to identify what requests originate from. In YouTube's case, this is sent as a JavaScript challenge that browsers must evaluate, and send back the resolved string. Typically, this challenge would remain unsolved for bots as more often than not, they don't simulate an entire browser environment, instead only evaluating the minimum amount of JS required to do its job. Therefore, it's a reasonable assumption that if the challenge is not fulfilled, the request origin is a bot.

To obtain a poToken, you can use https://github.com/iv-org/youtube-trusted-session-generator, by running the Python script or the docker image. Both methods will print a poToken after a successful run, which you can supply as environment variables to try and work around having automated requests blocked.

> [!NOTE]
> A `poToken` is not a silver bullet, and currently it only applies to requests made via the `WEB` client.
>
> At the time of writing, the most effective method for working around automated request blocking is to use IPv6 rotation. Which is not supported (yet...)

## Launching the application
Launch the server using:

```java -jar drinko-music-VERSION_NUMBER_HERE-SNAPSHOT.jar```

## Using Spring Args (RECOMMENDED LOCAL ONLY)
```
java -jar -Ddiscord.api.token=DISCORD_TOKEN \
    drinko-music-VERSION_NUMBER_HERE-SNAPSHOT.jar
```

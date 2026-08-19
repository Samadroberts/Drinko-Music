package org.drinko.models.audio;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.bandcamp.BandcampAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.beam.BeamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.getyarn.GetyarnAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.twitch.TwitchStreamAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.vimeo.VimeoAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.AndroidMusic;
import dev.lavalink.youtube.clients.Music;
import dev.lavalink.youtube.clients.Tv;
import dev.lavalink.youtube.clients.Web;
import dev.lavalink.youtube.clients.WebEmbedded;
import discord4j.common.util.Snowflake;
import discord4j.voice.AudioProvider;
import lombok.Getter;
import org.drinko.models.audio.trackscheduler.TrackScheduler;

@Getter
public class GuildVoiceSupport {

    private final Snowflake id;
    private final AudioPlayerManager audioPlayerManager;
    private final AudioPlayer audioPlayer;
    private final AudioProvider audioProvider;
    private final TrackScheduler trackScheduler;
    private final YoutubeSourceOptions youtubeSourceOptions;

    public GuildVoiceSupport(Snowflake id, YoutubeSourceOptions youtubeSourceOptions) {
        this.id = id;
        this.youtubeSourceOptions = youtubeSourceOptions;
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);

        registerRemoteSources(audioPlayerManager);
        this.audioPlayer = audioPlayerManager.createPlayer();
        this.trackScheduler = new TrackScheduler(audioPlayer);
        this.audioProvider = new LavaplayerAudioProvider(audioPlayer);
    }

    private void registerRemoteSources(AudioPlayerManager playerManager) {
        playerManager.registerSourceManager(new YoutubeAudioSourceManager(youtubeSourceOptions,
//              # No reason for this just trying whatever works
                new Web(),
                new Music(),
                new AndroidMusic(),
                new Tv(),
                new WebEmbedded())
        );
        playerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        playerManager.registerSourceManager(new BandcampAudioSourceManager());
        playerManager.registerSourceManager(new VimeoAudioSourceManager());
        playerManager.registerSourceManager(new TwitchStreamAudioSourceManager());
        playerManager.registerSourceManager(new BeamAudioSourceManager());
        playerManager.registerSourceManager(new GetyarnAudioSourceManager());
        playerManager.registerSourceManager(new HttpAudioSourceManager( MediaContainerRegistry.DEFAULT_REGISTRY));
    }
}

package org.drinko.models.audio.trackscheduler;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class TrackScheduler extends AudioEventAdapter {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final int MAX_RETRIES = 5;
    private static final long API_BACKOFF = 0L;
    final AudioTrackQueue audioTrackQueue = new AudioTrackQueue();
    final AudioPlayer audioPlayer;
    private Disposable delayedTrackSubscription;
    private AudioTrack scheduledTrack;
    private Instant playingSongStartTime = null;

    private int retryCount = 0;

    public TrackScheduler(AudioPlayer audioPlayer) {
        super();
        this.audioPlayer = audioPlayer;
        this.audioPlayer.addListener(this);
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        super.onPlayerPause(player);
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        super.onPlayerResume(player);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        super.onTrackStart(player, track);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {

        resetRetriesForErroredTrack();

        super.onTrackEnd(player, track, endReason);
        if (delayedTrackSubscription != null) {
            scheduledTrack = null;
            delayedTrackSubscription.dispose();
        }
        if (endReason == AudioTrackEndReason.FINISHED) {
            switch (audioTrackQueue.getRepeatMode()) {
                case ALL:
                    audioTrackQueue.repeatAudioTrackInQueue(track.makeClone());
                    break;
                case ONE:
                    playTrack(track.makeClone());
                    return;
            }
            AudioTrack nextInQueue = audioTrackQueue.poll();
            if (nextInQueue != null) {
                playTrack(nextInQueue);
            }
        }
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        if (track == null) {
            return;
        }
        if (track.equals(player.getPlayingTrack()) && retryCount <= MAX_RETRIES) {
            retryCount++;
            playTrack(track.makeClone());
        } else {
            retryCount = 0;
            LOGGER.error("Failed to load track after " + MAX_RETRIES +" attempts", exception.getCause());
        }
    }

    @SneakyThrows
    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.error("Track is stuck??, skipping 1 track in an attempt to resolve the issue " + track.getInfo().title + " " + thresholdMs);
        super.onTrackStuck(audioPlayer, track, thresholdMs);
        skip(1);
    }

    @SneakyThrows
    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs, StackTraceElement[] stackTrace) {
        LOGGER.error("Track is stuck??, skipping in an attempt to resolve the issue " + track.getInfo().title + " " + thresholdMs);
        super.onTrackStuck(player, track, thresholdMs, stackTrace);
        skip(1);
    }

    public TrackQueueResult queue(AudioTrack audioTrack) {
        if(audioTrack == null) {
            return TrackQueueResult.FAILED;
        }
        audioTrackQueue.addToQueue(audioTrack);
        if(!isTrackPlaying()) {
            playTrack(audioTrackQueue.poll());
            return TrackQueueResult.PLAYING_NOW;
        } else {
            return TrackQueueResult.QUEUED;
        }
    }

    public SkipResult skip(long numberToSkip) {
        if (numberToSkip < 1) {
            return SkipResult.invalid("Number to skip must be greater than 0");
        }
        if (audioPlayer.getPlayingTrack() == null && !isTrackScheduledToPlay()) {
            return SkipResult.empty("The queue is empty!");
        }

        resetRetriesForErroredTrack();

        //Queue + track playing
        if (audioTrackQueue.size() + 1 <= numberToSkip && !RepeatMode.ALL.equals(audioTrackQueue.getRepeatMode())) {
            long queueSize = audioTrackQueue.size();
            audioTrackQueue.clearQueue();
            stopCurrentlyPlayingOrScheduledTrack();
            return SkipResult.success(queueSize + 1);
        }

        AudioTrack toPlayNext = null;
        if (!RepeatMode.ALL.equals(audioTrackQueue.getRepeatMode())) {
            for (long i = 0; i < numberToSkip; i++) {
                toPlayNext = audioTrackQueue.poll();
            }
        } else {
            long skipTo = numberToSkip;
            this.audioTrackQueue.addFirst(getCurrentlyPlayingOrScheduledTrack().makeClone());
            if (numberToSkip >= audioTrackQueue.size()) {
                skipTo = numberToSkip % audioTrackQueue.size();
            }
            audioTrackQueue.makeIndexNewHeadOfQueue(skipTo);
            toPlayNext = this.audioTrackQueue.poll();
        }

        if (toPlayNext == null) {
            return SkipResult.success(numberToSkip);
        }
        stopCurrentlyPlayingOrScheduledTrack();
        playTrack(toPlayNext);
        return SkipResult.success(numberToSkip);
    }

    public void stopCurrentSongAndClearQueue() {
        stopCurrentlyPlayingOrScheduledTrack();
        this.audioTrackQueue.clearQueue();
    }

    public void enableRepeatAll() {
        this.audioTrackQueue.setRepeatModeAll();
    }

    public void enableRepeatOne() {
        this.audioTrackQueue.setRepeatModeOne();
    }

    public void disableRepeat() {
        this.audioTrackQueue.setRepeatModeNone();
    }

    public RepeatMode getRepeatMode() {
        return audioTrackQueue.getRepeatMode();
    }

    private Disposable playTrackOnDelay(AudioTrack audioTrack, long delayInSeconds) {
        this.scheduledTrack = audioTrack;
        return Mono.delay(Duration.of(delayInSeconds, ChronoUnit.SECONDS)).subscribe((ignore) -> {
            this.playingSongStartTime = Instant.now();
            this.audioPlayer.playTrack(audioTrack);
        });
    }

    /**
     * To avoid getting rate limited by YouTube use this method over directly invoking audioPlayer.playTrack()
     */
    private void playTrack(AudioTrack audioTrack) {
        if (playingSongStartTime == null) {
            playingSongStartTime = Instant.now();
            audioPlayer.playTrack(audioTrack);
            return;
        }
        final Duration timeBetweenSongs = Duration.between(playingSongStartTime, Instant.now());

        if (timeBetweenSongs.getSeconds() <= API_BACKOFF) {
            if (this.delayedTrackSubscription != null) {
                this.delayedTrackSubscription.dispose();
            }
            this.delayedTrackSubscription = playTrackOnDelay(audioTrack, API_BACKOFF);
            return;
        }

        playingSongStartTime = Instant.now();
        audioPlayer.playTrack(audioTrack);
    }

    private boolean isTrackScheduledToPlay() {
        return delayedTrackSubscription != null && !delayedTrackSubscription.isDisposed();
    }

    private void stopCurrentlyPlayingOrScheduledTrack() {
        if (isTrackScheduledToPlay()) {
            this.delayedTrackSubscription.dispose();
            this.scheduledTrack = null;
        }
        this.audioPlayer.stopTrack();
    }

    private AudioTrack getCurrentlyPlayingOrScheduledTrack() {
        if (isTrackScheduledToPlay()) {
            return scheduledTrack;
        }
        return this.audioPlayer.getPlayingTrack();
    }

    private void resetRetriesForErroredTrack() {
        this.retryCount = 0;
    }

    public boolean isShuffled() {
        return this.audioTrackQueue.isShuffled();
    }

    public void enableShuffle() {
        this.audioTrackQueue.enableShuffle();
    }

    public void disableShuffle() {
        this.audioTrackQueue.disableShuffle();
    }

    public boolean isNoTrackPlayingAndQueueEmpty() {
        return this.getCurrentlyPlayingOrScheduledTrack() == null && this.audioTrackQueue.size() == 0;
    }

    public boolean isTrackPlaying() {
        return this.getCurrentlyPlayingOrScheduledTrack() != null;
    }

    public boolean isPaused() {
        return this.audioPlayer.isPaused();
    }

    public boolean pause() {
        if (!this.audioPlayer.isPaused()) {
            this.audioPlayer.setPaused(true);
        }
        return this.audioPlayer.isPaused();
    }

    public boolean resume() {
        if(this.audioPlayer.isPaused()) {
            this.audioPlayer.setPaused(false);
        }
        return !this.audioPlayer.isPaused();
    }

    public Map<Integer, AudioTrackInfo> getTrackOrderMap() {
        Map<Integer, AudioTrackInfo> trackMap = new HashMap<>();
        int position = 1;
        AudioTrack currentSong = getCurrentlyPlayingOrScheduledTrack();
        if (currentSong != null) {
            trackMap.put(position, currentSong.getInfo());
            position++;
        }
        for (AudioTrack audioTrack : this.audioTrackQueue.getQueue()) {
            trackMap.put(position++, audioTrack.getInfo());
        }
        return trackMap;
    }
}

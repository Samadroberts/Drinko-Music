package org.drinko.models.audio.trackscheduler;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.drinko.models.audio.trackscheduler.RepeatMode;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;


public class AudioTrackQueue {
    private final static RepeatMode SHUFFLED_REPEAT_MODE = RepeatMode.ALL;

    private final LinkedBlockingDeque<AudioTrack> queue = new LinkedBlockingDeque<>();
    private final LinkedBlockingDeque<AudioTrack> shuffledQueue = new LinkedBlockingDeque<>();
    private RepeatMode repeatMode = RepeatMode.NONE;

    private boolean shuffled = false;

    public void makeIndexNewHeadOfQueue(long newHeadIndex) {
        Queue<AudioTrack> copy = shuffled ? new LinkedList<>(shuffledQueue) : new LinkedList<>(queue);
        if (newHeadIndex >= copy.size()) {
            throw new IllegalArgumentException("The index of the new head of queue is larger than the queue");
        }
        final Queue<AudioTrack> oldList = new LinkedList<>();

        while (newHeadIndex > 0) {
            oldList.add(copy.poll());
            newHeadIndex--;
        }
        while (oldList.size() > 0) {
            copy.add(oldList.poll());
        }
        if (shuffled) {
            shuffledQueue.clear();
            shuffledQueue.addAll(copy);
        } else {
            queue.clear();
            queue.addAll(copy);
        }
    }

    public void addToQueue(AudioTrack audio) {
        if(audio == null) {
            return;
        }
        if (shuffled) {
            if (this.shuffledQueue.isEmpty()) {
                //Queue is empty, don't add to normal queue
                this.shuffledQueue.add(audio.makeClone());
            } else {
                // Queue is not empty add to both
                this.shuffledQueue.addLast(audio.makeClone());
                SecureRandom secureRandom = new SecureRandom();
                int newHead = secureRandom.nextInt(this.shuffledQueue.size());
                makeIndexNewHeadOfQueue(newHead);
                this.queue.add(audio.makeClone());
            }
        } else {
            this.queue.add(audio.makeClone());
        }
    }

    public void repeatAudioTrackInQueue(AudioTrack audioTrack) {
        if (shuffled) {
            this.shuffledQueue.add(audioTrack.makeClone());
            return;
        }
        this.queue.add(audioTrack.makeClone());
    }

    public AudioTrack poll() {
        if (shuffled) {
            return this.shuffledQueue.poll();
        }
        return this.queue.poll();
    }

    public int size() {
        if (shuffled) {
            return this.shuffledQueue.size();
        }
        return this.queue.size();
    }

    public void clearQueue() {
        if (shuffled) {
            this.shuffledQueue.clear();
        }
        this.queue.clear();
    }

    public void addFirst(AudioTrack audioTrack) {
        if (shuffled) {
            this.shuffledQueue.addFirst(audioTrack.makeClone());
            return;
        }
        this.queue.addFirst(audioTrack.makeClone());
    }

    public Queue<AudioTrack> getQueue() {
        if(this.shuffled) {
            return new LinkedList<>(shuffledQueue);
        }
        return new LinkedList<>(this.queue);
    }

    public RepeatMode getRepeatMode() {
        if (shuffled && !RepeatMode.ONE.equals(repeatMode)) {
            return SHUFFLED_REPEAT_MODE;
        }
        return repeatMode;
    }

    public void setRepeatModeAll() {
        this.repeatMode = RepeatMode.ALL;
    }

    public void setRepeatModeOne() {
        this.repeatMode = RepeatMode.ONE;
    }

    public void setRepeatModeNone() {
        this.repeatMode = RepeatMode.NONE;
    }

    public boolean isShuffled() {
        return this.shuffled;
    }

    public void enableShuffle() {
        this.shuffled = true;
        List<AudioTrack> cloneOfQueue = this.queue.stream()
                .map(AudioTrack::makeClone)
                .collect(Collectors.toList());
        Collections.shuffle(cloneOfQueue);
        this.shuffledQueue.addAll(cloneOfQueue);
    }

    public void disableShuffle() {
        this.shuffled = false;
        this.shuffledQueue.clear();
    }

}

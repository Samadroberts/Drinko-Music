package org.drinko.models.audio.trackscheduler;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
        if (audio == null) {
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
        if (this.shuffled) {
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

    public void remove(long index) {
        final LinkedList<AudioTrack> tempList = new LinkedList();
        int queueSize = size();
        if (shuffled) {
            for (int i = 0; i < queueSize; i++) {
                AudioTrack track = shuffledQueue.poll();
                if (i == index) {
                    removeFirstOccurrence(track, false);
                    break;
                } else {
                    tempList.add(track);
                }
            }
            for (AudioTrack audioTrack : tempList) {
                shuffledQueue.addFirst(audioTrack);
            }
            return;
        }
        for (int i = 0; i < queueSize; i++) {
            if (i == index) {
                queue.poll();
                break;
            } else {
                tempList.add(queue.poll());
            }
        }
        for (AudioTrack audioTrack : tempList) {
            queue.addFirst(audioTrack);
        }
    }

    public void removeFirstOccurrence(AudioTrack audioTrackToRemove, boolean fromShuffled) {
        LinkedList<AudioTrack> tempList = new LinkedList<>();
        if (fromShuffled) {
            for (int i = 0; i < shuffledQueue.size(); i++) {
                AudioTrack toRemove = shuffledQueue.poll();
                if (toRemove.getIdentifier().equals(audioTrackToRemove.getIdentifier())) {
                    break;
                }
                tempList.add(toRemove);
            }
        } else {
            for (int i = 0; i < queue.size(); i++) {
                AudioTrack toRemove = queue.poll();
                if (toRemove.getIdentifier().equals(audioTrackToRemove.getIdentifier())) {
                    break;
                }
                tempList.add(toRemove);
            }
        }
        for (AudioTrack track : tempList) {
            if (fromShuffled) {
                shuffledQueue.addFirst(track);
            } else {
                queue.addFirst(track);
            }
        }
    }
}

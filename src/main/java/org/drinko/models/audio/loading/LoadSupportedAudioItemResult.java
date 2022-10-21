package org.drinko.models.audio.loading;

import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class LoadSupportedAudioItemResult {
    private final LoadSupportedAudioItemResultHandler.LoadResult result;
    private final AudioItem loadResult;
}

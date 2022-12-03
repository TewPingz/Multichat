package me.tewpingz.multichat.channel;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class MultiChatChannel {

    private final String channelName;
    private final int range;

}

package ch.awae.discord.sandrainControl.voiceSplit;

import discord4j.common.util.Snowflake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VoiceSplitConfiguration {

    private final Snowflake server;
    private final Snowflake controlChannel;
    private final Snowflake voiceLobby;
    private final Snowflake category;

    @Autowired
    public VoiceSplitConfiguration(
            @Value("${discord.server}") String serverId,
            @Value("${voice-split.control}") String controlChannelId,
            @Value("${voice-split.voice-lobby}") String voiceLobbyId,
            @Value("${voice-split.category}") String voiceChannelCategoryId
    ) {
        server = Snowflake.of(serverId);
        controlChannel = Snowflake.of(controlChannelId);
        voiceLobby = Snowflake.of(voiceLobbyId);
        category = Snowflake.of(voiceChannelCategoryId);
    }

    public Snowflake getServer() {
        return server;
    }

    public Snowflake getControlChannel() {
        return controlChannel;
    }

    public Snowflake getVoiceLobby() {
        return voiceLobby;
    }

    public Snowflake getCategory() {
        return category;
    }
}

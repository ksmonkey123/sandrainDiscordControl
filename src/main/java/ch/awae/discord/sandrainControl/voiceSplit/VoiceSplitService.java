package ch.awae.discord.sandrainControl.voiceSplit;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.entity.channel.VoiceChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VoiceSplitService {

    private final GatewayDiscordClient client;
    private final VoiceSplitRepository repo;
    private final VoiceSplitConfiguration config;
    private List<VoiceChannel> voiceChannels;

    @Autowired
    public VoiceSplitService(@Lazy GatewayDiscordClient client,
                             VoiceSplitRepository repo,
                             VoiceSplitConfiguration config) {
        this.client = client;
        this.repo = repo;
        this.config = config;
    }

    private boolean split = false;

    private boolean verifyMerged(Message message) {
        if (split) {
            message.getRestMessage().createReaction("\u274C").subscribe();
        }
        return !split;
    }

    private boolean verifySplit(Message message) {
        if (!split) {
            message.getRestMessage().createReaction("\u274C").subscribe();
        }
        return split;
    }

    public synchronized void split(Message message) {
        if (verifyMerged(message)) {
            Guild guild = getGuild();
            split = true;


            VoiceChannel lobby = (VoiceChannel) guild.getChannelById(config.getVoiceLobby()).block();

            List<List<User>> groups = repo.getGroups();

            List<VoiceChannel> channels = new ArrayList<>();

            groups.stream()
                    .flatMap(group -> {
                        VoiceChannel channel = createChannel(guild);
                        channels.add(channel);
                        return group.stream().map(u -> Tuples.of(u, channel));
                    })
                    .forEach(t -> moveUser(t.getT1(), lobby, t.getT2()));

            this.voiceChannels = channels;
            message.getRestMessage().createReaction("\u2705").subscribe();
        }
    }

    private void moveUser(User user, VoiceChannel from, VoiceChannel to) {
        Member member = Objects.requireNonNull(user.asMember(config.getServer()).block());

        if (from.isMemberConnected(member.getId()).block()) {
            member.edit(spec -> spec.setNewVoiceChannel(to.getId())).subscribe();
        }
    }

    private VoiceChannel createChannel(Guild guild) {
        return guild.createVoiceChannel(spec -> {
            spec.setName(UUID.randomUUID().toString());
            spec.setParentId(config.getCategory());
        }).block();
    }

    private Guild getGuild() {
        return Objects.requireNonNull(client.getGuildById(config.getServer()).block());
    }

    public synchronized void merge(Message message) {
        if (verifySplit(message)) {
            split = false;

            for (VoiceChannel voiceChannel : voiceChannels) {
                voiceChannel
                        .getVoiceStates()
                        .toStream()
                        .parallel()
                        .map(state -> state.getMember().block())
                        .map(member -> member.edit(spec -> spec.setNewVoiceChannel(config.getVoiceLobby())).block())
                        .count();
                voiceChannel.delete().subscribe();
            }
            message.getRestMessage().createReaction("\u2705").subscribe();
        }
    }

    public synchronized void show(Message message) {
        StringBuilder response = new StringBuilder();
        List<List<User>> groups = repo.getGroups();
        if (groups.isEmpty()) {
            response.append("no groups");
        } else {
            response.append("groups:\n");
            for (List<User> group : groups) {
                response.append(" - ");
                for (User user : group) {
                    response.append(user.getUsername()).append(" ");
                }
                response.append("\n");
            }
        }
        message.getChannel().flatMap(c -> c.createMessage(response.toString())).subscribe();
    }

    public synchronized void group(Message message) {
        if (verifyMerged(message)) {
            List<User> users = message.getUserMentions().toStream().collect(Collectors.toList());
            repo.createGroup(users);
            message.getRestMessage().createReaction("\u2705").subscribe();
        }
    }

    public synchronized void reset(Message message) {
        if (verifyMerged(message)) {
            repo.clear();
            message.getRestMessage().createReaction("\u2705").subscribe();
        }
    }

    public void clear(Message message) {
        message.getRestMessage().createReaction("\uD83E\uDDF9").subscribe();
        TextChannel channel = (TextChannel) message.getChannel().block();
        channel.getMessagesBefore(message.getId())
                .filter(m -> !m.isPinned())
                .transform(channel::bulkDeleteMessages)
                .blockLast();
        message.delete().block();
    }

    @Scheduled(cron = "0 0 4 * * *")
    public synchronized void regularCleanup() {
        client.getChannelById(config.getControlChannel())
                .map(channel -> (TextChannel) channel)
                .flatMap(channel -> channel.getLastMessage())
                .doOnSuccess(this::clear)
                .subscribe();
    }

}

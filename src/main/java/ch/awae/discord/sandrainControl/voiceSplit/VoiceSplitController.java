package ch.awae.discord.sandrainControl.voiceSplit;

import ch.awae.discord.sandrainControl.discord.EventListener;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;

@Controller
public class VoiceSplitController implements EventListener<MessageCreateEvent> {

    private final VoiceSplitConfiguration config;
    private final VoiceSplitService service;

    @Autowired
    public VoiceSplitController(VoiceSplitConfiguration config,
                                VoiceSplitService service) {
        this.config = config;
        this.service = service;
    }

    @Override
    public Class<MessageCreateEvent> getEventType() {
        return MessageCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(MessageCreateEvent event) {
        return Mono.just(event)
                .map(MessageCreateEvent::getMessage)
                .filter(message -> message.getChannelId().equals(config.getControlChannel()))
                .filter(message -> message.getContent().startsWith("!"))
                .flatMap(this::process);
    }

    private Mono<Void> process(Message message) {
        String command = message.getContent().split(" ", 2)[0];

        switch (command) {
            case "!reset": service.reset(message);  break;
            case "!group": service.group(message); break;
            case "!split": service.split(message); break;
            case "!merge": service.merge(message); break;
            case "!list": service.show(message); break;
            default: {
                return message.getChannel().flatMap(c -> c.createMessage("unknown command: " + command))
                        .flatMap(m -> Mono.empty());
            }
        }

        return Mono.empty();
    }

}

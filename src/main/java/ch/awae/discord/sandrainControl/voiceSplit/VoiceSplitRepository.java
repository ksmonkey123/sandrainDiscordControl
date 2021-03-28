package ch.awae.discord.sandrainControl.voiceSplit;

import discord4j.core.object.entity.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class VoiceSplitRepository {

    private final List<List<User>> groups = new ArrayList<>();

    public void clear() {
        groups.clear();
    }

    private void cleanup() {
        groups.removeAll(groups.stream().filter(List::isEmpty).collect(Collectors.toList()));
    }

    private void unassign(User user) {
        groups.forEach(group -> group.remove(user));
        cleanup();
    }

    public void createGroup(List<User> users) {
        users.forEach(this::unassign);
        cleanup();
        groups.add(users);
    }

    public List<List<User>> getGroups() {
        return groups;
    }
}

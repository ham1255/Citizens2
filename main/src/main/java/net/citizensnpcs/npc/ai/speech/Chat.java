package net.citizensnpcs.npc.ai.speech;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Entity;

import net.citizensnpcs.Settings.Setting;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.speech.SpeechContext;
import net.citizensnpcs.api.ai.speech.Talkable;
import net.citizensnpcs.api.ai.speech.VocalChord;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.util.Messaging;

public class Chat implements VocalChord {
    @Override
    public String getName() {
        return "chat";
    }

    @Override
    public void talk(SpeechContext context) {
        if (context.getTalker() == null)
            return;
        NPC npc = CitizensAPI.getNPCRegistry().getNPC(context.getTalker().getEntity());
        if (npc == null)
            return;

        // chat to the world with CHAT_FORMAT and CHAT_RANGE settings
        if (!context.hasRecipients()) {
            String text = Setting.CHAT_FORMAT.asString().replace("<text>", context.getMessage());
            talkToBystanders(npc, text, context);
            return;
        }

        // Assumed recipients at this point
        else if (context.size() <= 1) {
            String text = Setting.CHAT_FORMAT_TO_TARGET.asString().replace("<text>", context.getMessage());
            String targetName = "";
            // For each recipient
            for (Talkable talkable : context) {
                talkable.talkTo(context, text, this);
                targetName = talkable.getName();
            }
            // Check if bystanders hear targeted chat
            if (!Setting.CHAT_BYSTANDERS_HEAR_TARGETED_CHAT.asBoolean())
                return;
            // Format message with config setting and send to bystanders
            String bystanderText = Setting.CHAT_FORMAT_TO_BYSTANDERS.asString().replace("<target>", targetName)
                    .replace("<text>", context.getMessage());
            talkToBystanders(npc, bystanderText, context);
            return;
        }

        else { // Multiple recipients
            String text = Setting.CHAT_FORMAT_TO_TARGET.asString().replace("<text>", context.getMessage());
            List<String> targetNames = new ArrayList<String>();
            // Talk to each recipient
            for (Talkable talkable : context) {
                talkable.talkTo(context, text, this);
                targetNames.add(talkable.getName());
            }

            if (!Setting.CHAT_BYSTANDERS_HEAR_TARGETED_CHAT.asBoolean())
                return;
            String targets = "";
            int max = Setting.CHAT_MAX_NUMBER_OF_TARGETS.asInt();
            String[] format = Setting.CHAT_MULTIPLE_TARGETS_FORMAT.asString().split("\\|");
            if (format.length != 4)
                Messaging.severe("npc.chat.options.multiple-targets-format invalid!");
            if (max == 1) {
                targets = format[0].replace("<target>", targetNames.get(0)) + format[3];
            } else if (max == 2 || targetNames.size() == 2) {
                if (targetNames.size() == 2) {
                    targets = format[0].replace("<target>", targetNames.get(0))
                            + format[2].replace("<target>", targetNames.get(1));
                } else
                    targets = format[0].replace("<target>", targetNames.get(0))
                            + format[1].replace("<target>", targetNames.get(1)) + format[3];
            } else if (max >= 3) {
                targets = format[0].replace("<target>", targetNames.get(0));

                int x = 1;
                for (x = 1; x < max - 1; x++) {
                    if (targetNames.size() - 1 == x)
                        break;
                    targets = targets + format[1].replace("<npc>", targetNames.get(x));
                }
                if (targetNames.size() == max) {
                    targets = targets + format[2].replace("<npc>", targetNames.get(x));
                } else
                    targets = targets + format[3];
            }

            String bystanderText = Setting.CHAT_FORMAT_WITH_TARGETS_TO_BYSTANDERS.asString()
                    .replace("<targets>", targets).replace("<text>", context.getMessage());
            talkToBystanders(npc, bystanderText, context);
        }
    }

    private void talkToBystanders(NPC npc, String text, SpeechContext context) {
        // Get list of nearby entities
        List<Entity> bystanderEntities = npc.getEntity().getNearbyEntities(Setting.CHAT_RANGE.asDouble(),
                Setting.CHAT_RANGE.asDouble(), Setting.CHAT_RANGE.asDouble());
        for (Entity bystander : bystanderEntities) {
            boolean shouldTalk = true;
            if (!Setting.TALK_CLOSE_TO_NPCS.asBoolean() && CitizensAPI.getNPCRegistry().isNPC(bystander)) {
                shouldTalk = false;
            }
            if (context.hasRecipients()) {
                for (Talkable target : context) {
                    if (target.getEntity().equals(bystander)) {
                        shouldTalk = false;
                        break;
                    }
                }
            }

            if (shouldTalk) {
                new TalkableEntity(bystander).talkNear(context, text, this);
            }
        }
    }
}

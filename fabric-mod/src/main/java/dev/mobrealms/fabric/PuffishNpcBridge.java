package dev.mobrealms.fabric;

import dev.mobrealms.core.Development.Person;
import dev.mobrealms.core.ResidentSkills;
import net.minecraft.resources.Identifier;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;

/** Reads the shipped Pufferfish tree; NPC progression remains keyed to each citizen UUID. */
public final class PuffishNpcBridge {
    private static final Identifier CATEGORY=Identifier.parse("mobrealms:residents");
    private static Category category;
    private PuffishNpcBridge() {}

    public static void bind() {
        Category loaded=SkillsAPI.getCategory(CATEGORY)
                .orElseThrow(()->new IllegalStateException("Pufferfish resident tree is missing"));
        for (var branch:ResidentSkills.Branch.values()) {
            if (loaded.getSkill(ResidentSkills.root(branch)).isEmpty()
                    || loaded.getSkill(ResidentSkills.mastery(branch)).isEmpty())
                throw new IllegalStateException("Pufferfish resident tree lacks " + branch);
        }
        category=loaded;
        MobRealms.LOGGER.info("Mob Realms NPC bridge bound to Pufferfish category {} with {} nodes",
                CATEGORY,loaded.streamSkills().count());
    }

    public static boolean unlocked(Person person,ResidentSkills.Branch branch,boolean mastery) {
        String node=mastery?ResidentSkills.mastery(branch):ResidentSkills.root(branch);
        return category!=null && category.getSkill(node).isPresent() && person.unlocked(node);
    }
}

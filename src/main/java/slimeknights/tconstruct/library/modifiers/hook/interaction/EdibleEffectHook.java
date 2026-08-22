package slimeknights.tconstruct.library.modifiers.hook.interaction;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.modules.interaction.edible.EdibleModule;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Collection;
import java.util.List;

/** Runs secondary effects after an edible tool restores hunger. */
public interface EdibleEffectHook {
  void onToolEaten(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot eatenSlot,
                   int hunger, float saturation, List<ItemStack> representativeItems);

  record AllMerger(Collection<EdibleEffectHook> modules) implements EdibleEffectHook {
    @Override
    public void onToolEaten(IToolStackView tool, ModifierEntry modifier, Player player, EquipmentSlot eatenSlot,
                            int hunger, float saturation, List<ItemStack> representativeItems) {
      for (EdibleEffectHook module : modules) {
        module.onToolEaten(tool, modifier, player, eatenSlot, hunger, saturation, representativeItems);
      }
    }
  }
}

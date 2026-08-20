package slimeknights.tconstruct.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import slimeknights.tconstruct.TConstruct;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Removes Jade's redundant mod-name line when the creative tooltip already names a Tinkers' tab. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
  @WrapMethod(method = "getTooltipFromContainerItem", order = 2000)
  private List<Component> tconstruct$hideDuplicateModName(ItemStack stack, Operation<List<Component>> original) {
    List<Component> tooltip = original.call(stack);
    if (tooltip.size() < 2 || !TConstruct.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
      return tooltip;
    }

    Set<String> categoryNames = new HashSet<>();
    for (CreativeModeTab tab : CreativeModeTabs.tabs()) {
      if (TConstruct.MOD_ID.equals(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab).getNamespace()) && tab.contains(stack)) {
        categoryNames.add(tab.getDisplayName().getString());
      }
    }
    if (categoryNames.isEmpty()) {
      return tooltip;
    }

    int categoryIndex = -1;
    for (int i = 1; i < tooltip.size(); i++) {
      if (categoryNames.contains(tooltip.get(i).getString())) {
        categoryIndex = i;
      }
    }
    if (categoryIndex < 0) {
      return tooltip;
    }

    String modName = Component.translatable("jade.modName.tconstruct").getString();
    for (int i = categoryIndex + 1; i < tooltip.size(); i++) {
      if (modName.equals(tooltip.get(i).getString())) {
        List<Component> filtered = new ArrayList<>(tooltip);
        filtered.remove(i);
        return filtered;
      }
    }
    return tooltip;
  }
}

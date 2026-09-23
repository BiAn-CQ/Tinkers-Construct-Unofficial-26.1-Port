package slimeknights.tconstruct.library.tools.item;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.fixture.MaterialItemFixture;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.definition.ArmorSlotType;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.utils.ItemStackDataUtil;
import slimeknights.tconstruct.test.BaseMcTest;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ItemExtensionDispatchTest extends BaseMcTest {
  @Test
  void enderMaskUsesCurrentGazeHookOnlyForHelmetAndEndermen() {
    Player player = mock(Player.class);
    EnderMan enderman = mock(EnderMan.class);
    ItemStack helmet = new ItemStack(TinkerTools.travelersGear.get(ArmorSlotType.HELMET));
    assertThat(helmet.isGazeDisguise(player, enderman)).isFalse();
    ItemStackDataUtil.updateTagElement(helmet, ToolStack.TAG_VOLATILE_MOD_DATA,
      tag -> tag.putBoolean(ModifiableArmorItem.ENDERMASK.toString(), true));
    assertThat(helmet.isGazeDisguise(player, enderman)).isTrue();
    assertThat(helmet.isGazeDisguise(player, null)).isTrue();
    assertThat(helmet.isGazeDisguise(player, mock(LivingEntity.class))).isFalse();
    ItemStack chest = new ItemStack(TinkerTools.travelersGear.get(ArmorSlotType.CHESTPLATE));
    ItemStackDataUtil.setTag(chest, ItemStackDataUtil.getTag(helmet));
    assertThat(chest.isGazeDisguise(player, enderman)).isFalse();
  }

  @Test
  void currentCreatorHookUsesMaterialAndModifierNamespaces() {
    MaterialItemFixture.init();
    var registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    ItemStack part = MaterialItemFixture.MATERIAL_ITEM.withMaterialForDisplay(new MaterialId("exampleaddon", "material"));
    assertThat(part.getItem().getCreatorModId(registries, part)).isEqualTo("exampleaddon");
    ItemStack crystal = ModifierCrystalItem.withModifier(new ModifierId("exampleaddon", "modifier"));
    assertThat(crystal.getItem().getCreatorModId(registries, crystal)).isEqualTo("exampleaddon");
  }
}

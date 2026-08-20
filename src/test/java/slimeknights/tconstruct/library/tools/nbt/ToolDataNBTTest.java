package slimeknights.tconstruct.library.tools.nbt;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.test.CoreTestBootstrap;

import static org.assertj.core.api.Assertions.assertThat;

class ToolDataNBTTest extends CoreTestBootstrap {
  private static final Identifier testKey = Identifier.withDefaultNamespace("test");
  private static final Identifier testKey2 = Identifier.withDefaultNamespace("test2");

  @Test
  void empty() {
    for (SlotType type : SlotType.getAllSlotTypes()) {
      assertThat(IModDataView.EMPTY.getSlots(type)).isEqualTo(0);
    }

    CompoundTag nbt = IModDataView.EMPTY.getCompound(testKey);
    nbt.putInt("test", 1);
    nbt = IModDataView.EMPTY.getCompound(testKey);
    assertThat(nbt.contains("test")).overridingErrorMessage("NBT not saved in empty").isFalse();
  }

  @Test
  void defaults() {
    ToolDataNBT nbt = new ToolDataNBT();

    for (SlotType type : SlotType.getAllSlotTypes()) {
      assertThat(IModDataView.EMPTY.getSlots(type)).isEqualTo(0);
    }
    assertThat(nbt.getData().isEmpty()).isTrue();
  }

  @Test
  void serialize() {
    ToolDataNBT modData = new ToolDataNBT();
    modData.setSlots(SlotType.UPGRADE, 2);
    modData.setSlots(SlotType.ABILITY, 3);
    modData.setSlots(SlotType.SOUL, 4);
    modData.putInt(testKey, 1);
    modData.put(testKey2, new CompoundTag());

    CompoundTag nbt = modData.getData();
    assertThat(nbt.getIntOr(SlotType.UPGRADE.getName(), 0)).isEqualTo(2);
    assertThat(nbt.getIntOr(SlotType.ABILITY.getName(), 0)).isEqualTo(3);
    assertThat(nbt.getIntOr(SlotType.SOUL.getName(), 0)).isEqualTo(4);
    assertThat(nbt.getIntOr(testKey.toString(), 0)).isEqualTo(1);
    assertThat(nbt.getCompoundOrEmpty(testKey2.toString()).isEmpty()).isTrue();
  }

  @Test
  void deserialize() {
    CompoundTag nbt = new CompoundTag();
    nbt.putInt(SlotType.UPGRADE.getName(), 4);
    nbt.putInt(SlotType.ABILITY.getName(), 5);
    nbt.putInt(SlotType.SOUL.getName(), 6);
    nbt.putString(testKey.toString(), "Not sure why you need strings");
    CompoundTag tag = new CompoundTag();
    tag.putInt("test", 1);
    nbt.put(testKey2.toString(), tag);

    ToolDataNBT modData = ToolDataNBT.readFromNBT(nbt);
    assertThat(modData.getSlots(SlotType.UPGRADE)).isEqualTo(4);
    assertThat(modData.getSlots(SlotType.ABILITY)).isEqualTo(5);
    assertThat(modData.getSlots(SlotType.SOUL)).isEqualTo(6);
    assertThat(modData.getString(testKey)).isEqualTo("Not sure why you need strings");

    tag = modData.getCompound(testKey2);
    assertThat(tag.isEmpty()).isFalse();
    assertThat(tag.contains("test")).isTrue();
    assertThat(tag.getIntOr("test", 0)).isEqualTo(1);
  }
}

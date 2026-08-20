package slimeknights.tconstruct.plugin.jei.modifiers;

import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.plugin.jei.JEIPlugin;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Renderer for modifier slot types */
@SuppressWarnings("removal")
public enum SlotIngredientRenderer implements IIngredientRenderer<SlotCount> {
  /** Shows a slot in the ingredients view */
  INGREDIENT {
    @Override
    public List<Component> getTooltip(SlotCount slots, TooltipFlag tooltipFlag) {
      SlotType type = slots.type();
      return appendId(type, Component.translatable(KEY_INGREDIENT, type.getDisplayName()), tooltipFlag);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, SlotCount slots, TooltipFlag tooltipFlag) {
      tooltip.addAll(getTooltip(slots, tooltipFlag));
      JEIPlugin.addCustomIngredientModName(tooltip, TConstruct.getResource(slots.type().getName()));
    }
  },
  /** Shows a slot as an input. Tooltip will discuss required slots. */
  INPUT {
    @Override
    public List<Component> getTooltip(@Nullable SlotCount slots, TooltipFlag tooltipFlag) {
      if (slots != null) {
        int count = slots.count();
        if (count > 0) {
          Component text;
          SlotType type = slots.type();
          if (count == 1) {
            text = Component.translatable(KEY_SLOT, type.getDisplayName());
          } else {
            text = Component.translatable(KEY_SLOTS, count, type.getDisplayName());
          }
          return appendId(type, text, tooltipFlag);
        }
      }
      return TEXT_FREE;
    }
  },
  /** Shows a slot as an output. Tooltip will discuss gained slots. */
  OUTPUT {
    @Override
    public List<Component> getTooltip(SlotCount slots, TooltipFlag tooltipFlag) {
      SlotType type = slots.type();
      return appendId(type, type.format(slots.count()), tooltipFlag);
    }
  };

  /** Key for a slot count of 1 */
  private static final String KEY_SLOT = TConstruct.makeTranslationKey("jei", "modifiers.slot");
  /** Key for a slot count of 2+ */
  private static final String KEY_SLOTS = TConstruct.makeTranslationKey("jei", "modifiers.slots");
  /** Key for slotless */
  private static final List<Component> TEXT_FREE = Collections.singletonList(TConstruct.makeTranslation("jei", "modifiers.free"));
  /** Key for the ingredient list tooltip */
  private static final String KEY_INGREDIENT = TConstruct.makeTranslationKey("jei", "modifier_slot.ingredient");
  /** Key for the slot ID advanced tooltip */
  private static final String KEY_ID = TConstruct.makeTranslationKey("jei", "modifier_slot.id");

  /** Cache of sprite for each slot type */
  private static final Map<SlotType,TextureAtlasSprite> SLOT_SPRITES = new HashMap<>();
  /** Lookup for sprite for a slot type */
  private static final Function<SlotType,TextureAtlasSprite> SLOT_LOOKUP = slotType -> {
    Minecraft minecraft = Minecraft.getInstance();
    String texture = switch (slotType == null ? "slotless" : slotType.getName()) {
      case "upgrades" -> "item/slot/upgrade";
      case "abilities" -> "item/slot/ability";
      case "defense" -> "item/slot/defense";
      case "souls" -> "item/materials/hollow_gem";
      case "slotless" -> "item/slot/slotless";
      default -> "item/slot/default";
    };
    return minecraft.getAtlasManager().get(new SpriteId(TextureAtlas.LOCATION_ITEMS, TConstruct.getResource(texture)));
  };

  @Override
  public int getWidth() {
    return this == INGREDIENT ? 16 : 24;
  }

  @Override
  public void render(GuiGraphicsExtractor graphics, @Nullable SlotCount slots) {
    if (this != INGREDIENT && slots != null && slots.count() > 0) {
      String text = Integer.toString(slots.count());
      Font fontRenderer = Minecraft.getInstance().font;
      graphics.text(fontRenderer, text, 9 - fontRenderer.width(text), 5, Color.GRAY.getRGB(), false);
    }
    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITES.computeIfAbsent(SlotCount.type(slots), SLOT_LOOKUP), this == INGREDIENT ? 0 : 8, 0, 16, 16);
  }

  /** Appends the ID in advanced tooltip */
  private static List<Component> appendId(SlotType type, Component text, TooltipFlag tooltipFlag) {
    // in advanced, show the slot ID below the tooltip
    if (tooltipFlag.isAdvanced()) {
      return List.of(text, Component.translatable(KEY_ID, type.getName()).withStyle(ChatFormatting.DARK_GRAY));
    } else {
      return List.of(text);
    }
  }

  /** Clears any relevant caches on JEI reload */
  public static void clearCache() {
    SLOT_SPRITES.clear();
  }
}

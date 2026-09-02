package slimeknights.tconstruct.tables.client.inventory;

import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;
import slimeknights.mantle.client.screen.ElementScreen;
import slimeknights.mantle.client.screen.ModuleScreen;
import slimeknights.mantle.client.screen.ScalableElementScreen;
import slimeknights.mantle.inventory.WrapperSlot;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.client.GuiUtil;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.tools.layout.LayoutIcon;
import slimeknights.tconstruct.library.tools.layout.LayoutSlot;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayout;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.LazyToolStack;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import slimeknights.tconstruct.tables.client.inventory.widget.SlotButtonItem;
import slimeknights.tconstruct.tables.client.inventory.widget.TinkerStationButtonsWidget;
import slimeknights.tconstruct.tables.menu.TinkerStationContainerMenu;
import slimeknights.tconstruct.tables.menu.slot.TinkerStationSlot;
import slimeknights.tconstruct.tables.network.TinkerStationRenamePacket;
import slimeknights.tconstruct.tables.network.TinkerStationSelectionPacket;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

import static slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity.INPUT_SLOT;
import static slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity.TINKER_SLOT;

public class TinkerStationScreen extends ToolTableScreen<TinkerStationBlockEntity,TinkerStationContainerMenu> {
  // titles to display
  private static final Component COMPONENTS_TEXT = TConstruct.makeTranslation("gui", "tinker_station.components");
 // fallback text for crafting with no named slots
  private static final Component ASCII_ANVIL = Component.literal("\n\n")
    .append("       .\n")
    .append("     /( _________\n")
    .append("     |  >:=========`\n")
    .append("     )(  \n")
    .append("     \"\"")
    .withStyle(ChatFormatting.DARK_GRAY);

  // parameters to display the still filled slots when changing layout
  private static final int STILL_FILLED_X = 112;
  private static final int STILL_FILLED_Y = 62;
  private static final int STILL_FILLED_SPACING = 18;

  // texture
  private static final Identifier TINKER_TEXTURE = TConstruct.getResource("textures/gui/tinker.png");
  // texture elements
  private static final ElementScreen ACTIVE_TEXT_FIELD = new ElementScreen(TINKER_TEXTURE, 0, 232, 90, 12, 256, 256);
  private static final ElementScreen ITEM_COVER = ACTIVE_TEXT_FIELD.move(176, 18, 70, 64);
  // slots
  private static final ElementScreen SLOT_BACKGROUND = ACTIVE_TEXT_FIELD.move(176, 0, 18, 18);
  private static final ElementScreen SLOT_BORDER = ACTIVE_TEXT_FIELD.move(194, 0, 18, 18);
  private static final ElementScreen SLOT_SPACE_TOP = ACTIVE_TEXT_FIELD.move(0, 198, 18, 2);
  private static final ElementScreen SLOT_SPACE_BOTTOM = ACTIVE_TEXT_FIELD.move(0, 196, 18, 2);
  // panel
  private static final ElementScreen PANEL_SPACE_LEFT = ACTIVE_TEXT_FIELD.move(0, 196, 5, 4);
  private static final ElementScreen PANEL_SPACE_RIGHT = ACTIVE_TEXT_FIELD.move(9, 196, 9, 4);
  private static final ElementScreen LEFT_BEAM = ACTIVE_TEXT_FIELD.move(0, 202, 2, 7);
  private static final ElementScreen RIGHT_BEAM = ACTIVE_TEXT_FIELD.move(131, 202, 2, 7);
  private static final ScalableElementScreen CENTER_BEAM = new ScalableElementScreen(TINKER_TEXTURE, 2, 202, 129, 7, 256, 256);
  // text boxes
  private static final ElementScreen TEXT_BOX = ACTIVE_TEXT_FIELD.move(0, 244, 90, 12);

  /** Applies alpha modulation to slot overlays. */
  private static void drawTinted(GuiGraphicsExtractor graphics, ElementScreen element, int x, int y, int color) {
    graphics.blit(RenderPipelines.GUI_TEXTURED, element.texture, x, y,
      element.x, element.y, element.w, element.h, element.w, element.h, element.texW, element.texH, color);
  }

  /** Number of button columns in the UI */
  public static final int COLUMN_COUNT = 6;
  // TODO: a scrollbar for this instead would be good
  /** If we have more than this many buttons, offset the armor stand down slightly */
  private static final int OFFSET_ARMOR_STAND_AFTER = COLUMN_COUNT * 5;
  /** If we have more than this many buttons, disable the armor stand preview */
  private static final int DISABLE_ARMOR_STAND_AFTER = COLUMN_COUNT * 6;

  // configurable elements
  protected ElementScreen buttonDecorationTop = SLOT_SPACE_TOP;
  protected ElementScreen buttonDecorationBot = SLOT_SPACE_BOTTOM;
  protected ElementScreen panelDecorationL = PANEL_SPACE_LEFT;
  protected ElementScreen panelDecorationR = PANEL_SPACE_RIGHT;

  protected ElementScreen leftBeam = ACTIVE_TEXT_FIELD.move(0, 0, 0, 0);
  protected ElementScreen rightBeam = ACTIVE_TEXT_FIELD.move(0, 0, 0, 0);
  protected ScalableElementScreen centerBeam = CENTER_BEAM.move(0, 0, 0, 0);

  /** Gets the default layout to apply, the "repair" button */
  @Nonnull @Getter
  private final StationSlotLayout defaultLayout;
  /** Currently selected tool */
  @Nonnull @Getter
  private StationSlotLayout currentLayout;

  // components
  protected EditBox textField;
  protected TinkerStationButtonsWidget buttonsScreen;
  private boolean textFieldEditable;
  /** Last client-side stack/error used to refresh the dynamic preview. */
  private ItemStack lastDisplayedStack = ItemStack.EMPTY;
  private Component lastDisplayedError;

  /** Maximum available slots */
  @Getter
  private final int maxInputs;
  /** How many of the available input slots are active */
  protected int activeInputs;


  @SuppressWarnings("deprecation")
  public TinkerStationScreen(TinkerStationContainerMenu container, Inventory playerInventory, Component title) {
    super(container, playerInventory, title, 176, 184);

    this.tinkerInfo.yOffset = 5;
    this.modifierInfo.yOffset = this.tinkerInfo.getLayoutHeight() + 9;

    // determine number of inputs
    int max = 5;
    TinkerStationBlockEntity te = container.getTile();
    if (te != null) {
      max = te.getInputCount(); // TODO: not station sensitive
    }
    this.maxInputs = max;

    // large if at least 4, todo can configure?
    if (max > 3) {
      this.metal();
    } else {
      this.wood();
    }
    // apply base slot information
    if (te == null) {
      this.defaultLayout = StationSlotLayout.EMPTY;
    } else {
      this.defaultLayout = StationSlotLayoutLoader.getInstance().get(BuiltInRegistries.BLOCK.getKey(te.getBlockState().getBlock()));
    }
    this.currentLayout = this.defaultLayout;
    this.activeInputs = Math.min(defaultLayout.getInputCount(), max);
  }

  @Override
  public void init() {

    assert this.minecraft != null;

    this.tinkerInfo.xOffset = 2;
    this.tinkerInfo.yOffset = this.centerBeam.h + this.panelDecorationL.h;
    this.modifierInfo.xOffset = this.tinkerInfo.xOffset;
    this.modifierInfo.yOffset = this.tinkerInfo.yOffset + this.tinkerInfo.getLayoutHeight() + 4;

    int x = (this.width - this.imageWidth) / 2;
    int y = (this.height - this.imageHeight) / 2;
    textField = new EditBox(this.font, x + 80, y + 7, 82, 9, Component.empty());
    textField.setCanLoseFocus(true);
    textField.setTextColor(-1);
    textField.setTextColorUneditable(-1);
    textField.setBordered(false);
    textField.setMaxLength(50);
    textField.setResponder(this::onNameChanged);
    textField.setValue("");
    this.textFieldEditable = false;
    addWidget(textField);
    textField.visible = false;
    textField.setEditable(false);

    int buttonsStyle = this.maxInputs > 3 ? TinkerStationButtonsWidget.METAL_STYLE : TinkerStationButtonsWidget.WOOD_STYLE;

    List<StationSlotLayout> layouts = Lists.newArrayList();
    // repair layout
    layouts.add(this.defaultLayout);
    // tool layouts
    layouts.addAll(StationSlotLayoutLoader.getInstance().getSortedSlots().stream()
      .filter(layout -> layout.getInputSlots().size() <= this.maxInputs).toList());

    // if we have more than 5 rows of buttons, offset armor stand down a bit
    // more than 6 rows causes us to just disable it fully
    int size = layouts.size();
    int armorY = 195;
    if (size > DISABLE_ARMOR_STAND_AFTER) {
      enableArmorStandPreview = false;
    } else if (size > OFFSET_ARMOR_STAND_AFTER) {
      armorY = 210;
    }

    // init after we set the enable boolean
    super.init();
    this.buttonsScreen = new TinkerStationButtonsWidget(this, this.cornerX - TinkerStationButtonsWidget.width(COLUMN_COUNT) - 2,
      this.cornerY + this.centerBeam.h + this.buttonDecorationTop.h, layouts, buttonsStyle);

    this.setupArmorStandPreview(-55, armorY, 35);

    this.updateLayout();
  }

  /**
   * Recipe packets arrive after the screen is created.  The result slot is
   * recalculated by the menu, but the armor-stand/tool information panels
   * used to remain on the old client-side value until another key event.
   */
  @Override
  protected void containerTick() {
    super.containerTick();
    if (this.tile == null || this.textField == null) {
      return;
    }

    LazyToolStack result = this.tile.getResult();
    ItemStack displayed = result == null ? this.tile.getTool().getStack() : result.getStack();
    Component error = this.tile.getCurrentError();
    if (!ItemStack.matches(this.lastDisplayedStack, displayed) || !Objects.equals(this.lastDisplayedError, error)) {
      this.updateDisplay();
      this.lastDisplayedStack = displayed.copy();
      this.lastDisplayedError = error;
    }
  }

  /** Updates all slots for the current slot layout */
  public void updateLayout() {
    int stillFilled = 0;
    for (int i = 0; i <= maxInputs; i++) {
      Slot slot = this.getMenu().getSlot(i);
      LayoutSlot layoutSlot = currentLayout.getSlot(i);
      if (layoutSlot.isHidden()) {
        // put the position in the still filled line
        this.setSlotPosition(this.getMenu(), slot,
          STILL_FILLED_X - STILL_FILLED_SPACING * stillFilled, STILL_FILLED_Y);
        stillFilled++;
        Slot logicalSlot = logicalSlot(slot);
        if (logicalSlot instanceof TinkerStationSlot tinkerSlot) {
          tinkerSlot.deactivate();
        }
      } else {
        this.setSlotPosition(this.getMenu(), slot, layoutSlot.getX(), layoutSlot.getY());
        Slot logicalSlot = logicalSlot(slot);
        if (logicalSlot instanceof TinkerStationSlot tinkerSlot) {
          tinkerSlot.activate(layoutSlot);
        }
      }
    }

    this.updateDisplay();
  }

  /** Gets the actual table slot after a client-side positioned wrapper is applied. */
  private static Slot logicalSlot(Slot slot) {
    return slot instanceof WrapperSlot wrapper ? wrapper.parent : slot;
  }

  @Override
  public void updateDisplay() {
    if (this.tile == null) {
      return;
    }

    // fetch the tool version of the result for the screen
    LazyToolStack lazyResult = tile.getResult();

    // if we have a message, display instead of refreshing the tool
    Component currentError = tile.getCurrentError();
    if (currentError != null) {
      error(currentError);
      return;
    }

    // only get to rename new tool in the station
    // anvil can rename on any tool change
    if (lazyResult == null || (tile.getInputCount() <= 4 && this.getMenu().getSlot(TINKER_SLOT).hasItem())) {
      textField.setEditable(false);
      this.textFieldEditable = false;
      textField.setValue("");
      textField.visible = false;
    } else if (!this.textFieldEditable) {
      textField.setEditable(true);
      this.textFieldEditable = true;
      textField.setValue("");
      textField.visible = true;
    } else {
      // ensure the text matches
      textField.setValue(tile.getItemName());
    }

    // if there is no result, use the input
    if (lazyResult == null) {
      lazyResult = tile.getTool();
    }
    updateArmorStandPreview(lazyResult.getStack());

    // if the contained stack is modifiable, display some information
    if (lazyResult.hasTag(TinkerTags.Items.MODIFIABLE)) {
      ToolStack tool = lazyResult.getTool();
      updateToolPanel(lazyResult);
      updateModifierPanel(tool);
    }
    // tool build info
    else {
      this.tinkerInfo.setCaption(this.currentLayout.getDisplayName());
      this.tinkerInfo.setText(this.currentLayout.getDescription());

      // for each named slot, color the slot if the slot is filled
      // typically all input slots should be named, or none of them
      MutableComponent fullText = Component.literal("");
      boolean hasComponents = false;
      for (int i = 0; i <= activeInputs; i++) {
        LayoutSlot layout = currentLayout.getSlot(i);
        String key = layout.getTranslationKey();
        if (!layout.isHidden() && !key.isEmpty()) {
          hasComponents = true;
          MutableComponent textComponent = Component.literal(" * ");
          ItemStack slotStack = this.getMenu().getSlot(i).getItem();
          if (!layout.isValid(slotStack)) {
            textComponent.withStyle(ChatFormatting.RED);
          }
          textComponent.append(Component.translatable(key)).append("\n");
          fullText.append(textComponent);
        }
      }
      // if we found any components, set the text, use the anvil if no components
      if (hasComponents) {
        this.modifierInfo.setCaption(COMPONENTS_TEXT);
        this.modifierInfo.setText(fullText);
      } else {
        this.modifierInfo.setCaption(Component.empty());
        this.modifierInfo.setText(ASCII_ANVIL);
      }
    }
  }

  @Override
  protected void drawContainerName(GuiGraphicsExtractor graphics) {
    graphics.text(this.font, this.getTitle(), 8, 8, 4210752, false);
  }

  public static void renderIcon(GuiGraphicsExtractor graphics, LayoutIcon icon, int x, int y) {
    Pattern pattern = icon.getValue(Pattern.class);
    if (pattern != null) {
      // draw pattern sprite
      GuiUtil.renderPattern(graphics, pattern, x, y);
      return;
    }

    ItemStack stack = icon.getValue(ItemStack.class);
    if (stack != null) {
      graphics.item(stack, x, y);
    }
  }

  @Override
  protected void renderBg(GuiGraphicsExtractor graphics, float partialTicks, int mouseX, int mouseY) {
    this.drawBackground(graphics, TINKER_TEXTURE);

    int x = 0;
    int y = 0;

    // draw the item background
    final float scale = 3.7f;
    final float xOff = 12.5f;
    final float yOff = 22f;

    // render the background icon
    Matrix3x2fStack renderPose = graphics.pose();
    renderPose.pushMatrix();
    renderPose.translate(xOff, yOff);
    renderPose.scale(scale, scale);
    renderIcon(graphics, currentLayout.getIcon(), (int) (this.cornerX / scale), (int) (this.cornerY / scale));
    renderPose.popMatrix();

    drawTinted(graphics, ITEM_COVER, this.cornerX + 7, this.cornerY + 18, 0xD1FFFFFF);

    // slot backgrounds, are transparent
    if (!this.currentLayout.getToolSlot().isHidden()) {
      Slot slot = this.getMenu().getSlot(TINKER_SLOT);
      drawTinted(graphics, SLOT_BACKGROUND, x + this.cornerX + slot.x - 1, y + this.cornerY + slot.y - 1, 0x47FFFFFF);
    }
    for (int i = 0; i < this.activeInputs; i++) {
      Slot slot = this.getMenu().getSlot(i + INPUT_SLOT);
      drawTinted(graphics, SLOT_BACKGROUND, x + this.cornerX + slot.x - 1, y + this.cornerY + slot.y - 1, 0x47FFFFFF);
    }

    // slot borders, are opaque
    for (int i = 0; i <= maxInputs; i++) {
      Slot slot = this.getMenu().getSlot(i);
      Slot logicalSlot = logicalSlot(slot);
      if ((logicalSlot instanceof TinkerStationSlot tinkerSlot && (!tinkerSlot.isDormant() || slot.hasItem()))) {
        SLOT_BORDER.draw(graphics, x + this.cornerX + slot.x - 1, y + this.cornerY + slot.y - 1);
      }
    }

    // sidebar beams
    x = this.buttonsScreen.getLeftPos() - this.leftBeam.w;
    y = this.cornerY;
    // draw the beams at the top
    this.leftBeam.draw(graphics, x, y);
    x += this.leftBeam.w;
    x += this.centerBeam.drawScaledX(graphics, x, y, this.buttonsScreen.getImageWidth());
    this.rightBeam.draw(graphics, x, y);

    x = tinkerInfo.getLeftPos() - this.leftBeam.w;
    this.leftBeam.draw(graphics, x, y);
    x += this.leftBeam.w;
    x += this.centerBeam.drawScaledX(graphics, x, y, this.tinkerInfo.getLayoutWidth());
    this.rightBeam.draw(graphics, x, y);

    // draw the decoration for the buttons
    for (SlotButtonItem button : this.buttonsScreen.getButtons()) {
      this.buttonDecorationTop.draw(graphics, button.getX(), button.getY() - this.buttonDecorationTop.h);
      // don't draw the bottom for the buttons in the last row
      if (button.buttonId < this.buttonsScreen.getButtons().size() - COLUMN_COUNT) {
        this.buttonDecorationBot.draw(graphics, button.getX(), button.getY() + button.getHeight());
      }
    }

    // draw the decorations for the panels
    this.panelDecorationL.draw(graphics, this.tinkerInfo.getLeftPos() + 5, this.tinkerInfo.getTopPos() - this.panelDecorationL.h);
    this.panelDecorationR.draw(graphics, this.tinkerInfo.guiRight() - 5 - this.panelDecorationR.w, this.tinkerInfo.getTopPos() - this.panelDecorationR.h);
    this.panelDecorationL.draw(graphics, this.modifierInfo.getLeftPos() + 5, this.modifierInfo.getTopPos() - this.panelDecorationL.h);
    this.panelDecorationR.draw(graphics, this.modifierInfo.guiRight() - 5 - this.panelDecorationR.w, this.modifierInfo.getTopPos() - this.panelDecorationR.h);

    // render slot background icons
    for (int i = 0; i <= maxInputs; i++) {
      Slot slot = this.getMenu().getSlot(i);
      if (!slot.hasItem()) {
        Pattern icon = currentLayout.getSlot(i).getIcon();
        if (icon != null) {
          GuiUtil.renderPattern(graphics, icon, this.cornerX + slot.x, this.cornerY + slot.y);
        }
      }
    }

    super.renderBg(graphics, partialTicks, mouseX, mouseY);

    this.buttonsScreen.extractRenderState(graphics, mouseX, mouseY, partialTicks);

    // text field
    if (textField != null && textField.visible) {
      TEXT_BOX.draw(graphics, this.cornerX + 79, this.cornerY + 5);
      this.textField.extractRenderState(graphics, mouseX, mouseY, partialTicks);
    }

    renderArmorStand(graphics);
  }



  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
    if (this.tinkerInfo.handleMouseClicked(event.x(), event.y(), event.button())) {
      return false;
    }

    if (this.modifierInfo.handleMouseClicked(event.x(), event.y(), event.button())) {
      return false;
    }
    
    if(this.buttonsScreen.handleMouseClicked(event.x(), event.y(), event.button())) {
      return false;
    }

    return super.mouseClicked(event, doubleClick);
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
    if (this.tinkerInfo.handleMouseClickMove(event.x(), event.y(), event.button(), dragX)) {
      return false;
    }

    if (this.modifierInfo.handleMouseClickMove(event.x(), event.y(), event.button(), dragX)) {
      return false;
    }

    return super.mouseDragged(event, dragX, dragY);
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    return mouseScrolled(mouseX, mouseY, scrollY);
  }

  /** @deprecated use the native four-axis scroll callback */
  @Deprecated
  public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
    if (this.tinkerInfo.handleMouseScrolled(mouseX, mouseY, delta)) {
      return false;
    }

    if (this.modifierInfo.handleMouseScrolled(mouseX, mouseY, delta)) {
      return false;
    }

    return super.mouseScrolled(mouseX, mouseY, 0, delta);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    if (this.tinkerInfo.handleMouseReleased(event.x(), event.y(), event.button())) {
      return false;
    }

    if (this.modifierInfo.handleMouseReleased(event.x(), event.y(), event.button())) {
      return false;
    }

    if (this.buttonsScreen.handleMouseReleased(event.x(), event.y(), event.button())) {
      return false;
    }

    return super.mouseReleased(event);
  }

  /** Returns true if a key changed that requires a display update */
  static boolean needsDisplayUpdate(int keyCode) {
    if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
      return true;
    }
    return keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL
      || keyCode == GLFW.GLFW_KEY_LEFT_SUPER || keyCode == GLFW.GLFW_KEY_RIGHT_SUPER;
  }

  @Override
  public boolean keyPressed(KeyEvent event) {
    int keyCode = event.key();
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      this.onClose();
      return true;
    }
    if (needsDisplayUpdate(keyCode)) {
      updateDisplay();
    }
    if (textField.canConsumeInput()) {
      textField.keyPressed(event);
      return true;
    }
    return super.keyPressed(event);
  }

  @Override
  protected void extractSlot(GuiGraphicsExtractor graphics, Slot slotIn, int mouseX, int mouseY) {
    // don't draw dormant slots with no item
    Slot logicalSlot = logicalSlot(slotIn);
    if (logicalSlot instanceof TinkerStationSlot && ((TinkerStationSlot) logicalSlot).isDormant() && !slotIn.hasItem()) {
      return;
    }
    super.extractSlot(graphics, slotIn, mouseX, mouseY);
  }

  @Override
  public boolean isHovering(Slot slotIn, double mouseX, double mouseY) {
    Slot logicalSlot = logicalSlot(slotIn);
    if (logicalSlot instanceof TinkerStationSlot && ((TinkerStationSlot) logicalSlot).isDormant() && !slotIn.hasItem()) {
      return false;
    }
    return super.isHovering(slotIn, mouseX, mouseY);
  }

  protected void wood() {
    this.tinkerInfo.wood();
    this.modifierInfo.wood();

    this.buttonDecorationTop = SLOT_SPACE_TOP.shift(SLOT_SPACE_TOP.w, 0);
    this.buttonDecorationBot = SLOT_SPACE_BOTTOM.shift(SLOT_SPACE_BOTTOM.w, 0);
    this.panelDecorationL = PANEL_SPACE_LEFT.shift(18, 0);
    this.panelDecorationR = PANEL_SPACE_RIGHT.shift(18, 0);

    this.leftBeam = LEFT_BEAM;
    this.rightBeam = RIGHT_BEAM;
    this.centerBeam = CENTER_BEAM;
  }

  protected void metal() {
    this.tinkerInfo.metal();
    this.modifierInfo.metal();

    this.buttonDecorationTop = SLOT_SPACE_TOP.shift(SLOT_SPACE_TOP.w * 2, 0);
    this.buttonDecorationBot = SLOT_SPACE_BOTTOM.shift(SLOT_SPACE_BOTTOM.w * 2, 0);
    this.panelDecorationL = PANEL_SPACE_LEFT.shift(18 * 2, 0);
    this.panelDecorationR = PANEL_SPACE_RIGHT.shift(18 * 2, 0);

    this.leftBeam = LEFT_BEAM.shift(0, LEFT_BEAM.h);
    this.rightBeam = RIGHT_BEAM.shift(0, RIGHT_BEAM.h);
    this.centerBeam = CENTER_BEAM.shift(0, CENTER_BEAM.h);
  }

  @Override
  public void error(Component message) {
    this.tinkerInfo.setCaption(COMPONENT_ERROR);
    this.tinkerInfo.setText(message);
    this.modifierInfo.setCaption(Component.empty());
    this.modifierInfo.setText(Component.empty());
  }

  @Override
  public void warning(Component message) {
    this.tinkerInfo.setCaption(COMPONENT_WARNING);
    this.tinkerInfo.setText(message);
    this.modifierInfo.setCaption(Component.empty());
    this.modifierInfo.setText(Component.empty());
  }

  /**
   * Called when a tool button is pressed
   * @param layout      Data of the slot selected
   */
  public void onToolSelection(StationSlotLayout layout) {
    this.activeInputs = Math.min(layout.getInputCount(), maxInputs);
    this.currentLayout = layout;
    this.updateLayout();

    // update the active slots and filter in the container
    // this.container.setToolSelection(layout); TODO: needed?
    TinkerNetwork.getInstance().sendToServer(new TinkerStationSelectionPacket(layout.getName()));
  }

  @Override
  public List<Rect2i> getModuleAreas() {
    List<Rect2i> list = super.getModuleAreas();
    list.add(this.buttonsScreen.getArea());
    return list;
  }

  @Override
  protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop) {
    return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop)
      && !this.buttonsScreen.isMouseOver(mouseX, mouseY);
  }


  /* Text field stuff */

  private void onNameChanged(String name) {
    if (tile != null) {
      this.tile.setItemName(name);
      TinkerNetwork.getInstance().sendToServer(new TinkerStationRenamePacket(name));
    }
  }

  @Override
  public void resize(int pWidth, int pHeight) {
    String s = this.textField.getValue();
    super.resize(pWidth, pHeight);
    this.textField.setValue(s);
  }

  @Override
  public void removed() {
    super.removed();
    assert this.minecraft != null;
  }

  @Override
  public void onClose() {
    super.onClose();

    assert this.minecraft != null;
  }
}

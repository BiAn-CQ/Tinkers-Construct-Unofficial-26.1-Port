package slimeknights.tconstruct;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimal NeoForge 26.1 entry point used while the 1.20.1 implementation is
 * migrated in independently verifiable slices.
 *
 * <p>The bootstrap owns only a small, self-contained registration slice.  It
 * keeps the 26.1 runtime testable while the legacy gameplay graph is migrated
 * in modules instead of forcing all 1.20.1 internals into the first launch.</p>
 */
@Mod(TConstruct26Bootstrap.MOD_ID)
public final class TConstruct26Bootstrap {
  public static final String MOD_ID = "tconstruct";
  private static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

  private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
  private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
  private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

  /** First migrated material item; later material registration can reuse this holder. */
  public static final DeferredItem<Item> COBALT_INGOT = ITEMS.registerSimpleItem("cobalt_ingot");
  /** First migrated material block. */
  public static final DeferredBlock<Block> COBALT_BLOCK = BLOCKS.registerSimpleBlock(
    "cobalt_block", () -> BlockBehaviour.Properties.of().strength(5.0F));
  /** Item form of the first migrated material block. */
  public static final DeferredItem<BlockItem> COBALT_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(COBALT_BLOCK);
  /** Small bootstrap tab used to verify item and block registration on both sides. */
  public static final DeferredHolder<CreativeModeTab, CreativeModeTab> BOOTSTRAP_TAB = CREATIVE_TABS.register("materials", () -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 7)
    .title(Component.translatable("itemGroup.tconstruct.materials"))
    .icon(COBALT_INGOT::toStack)
    .displayItems((parameters, output) -> {
      output.accept(COBALT_INGOT);
      output.accept(COBALT_BLOCK_ITEM);
    })
    .build());

  public TConstruct26Bootstrap(IEventBus modEventBus, ModContainer modContainer) {
    ITEMS.register(modEventBus);
    BLOCKS.register(modEventBus);
    CREATIVE_TABS.register(modEventBus);
    LOG.info("Tinkers' Construct 26.1 bootstrap loaded; registered cobalt material slice");
  }
}

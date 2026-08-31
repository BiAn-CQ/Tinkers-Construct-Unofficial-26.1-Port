package slimeknights.tconstruct.gadgets.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import slimeknights.tconstruct.gadgets.TinkerGadgets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Generates both the flat models and the 26.1 item definitions for new gadgets. */
public class GadgetItemModelProvider implements DataProvider {
  private final PackOutput.PathProvider models;
  private final PackOutput.PathProvider items;

  public GadgetItemModelProvider(PackOutput output) {
    models = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models/item");
    items = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
  }

  @Override
  public CompletableFuture<?> run(CachedOutput cache) {
    List<CompletableFuture<?>> tasks = new ArrayList<>();
    TinkerGadgets.armorStand.forEach((type, item) -> {
      Identifier id = BuiltInRegistries.ITEM.getKey(item);
      JsonObject textures = new JsonObject();
      textures.addProperty("layer0", id.getNamespace() + ":item/gadgets/" + id.getPath());
      JsonObject model = new JsonObject();
      model.addProperty("parent", "minecraft:item/generated");
      model.add("textures", textures);
      tasks.add(DataProvider.saveStable(cache, model, models.json(id)));

      JsonObject reference = new JsonObject();
      reference.addProperty("type", "minecraft:model");
      reference.addProperty("model", id.getNamespace() + ":item/" + id.getPath());
      JsonObject definition = new JsonObject();
      definition.add("model", reference);
      tasks.add(DataProvider.saveStable(cache, definition, items.json(id)));
    });
    return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new));
  }

  @Override
  public String getName() {
    return "Tinkers' Construct Gadget Item Models";
  }
}

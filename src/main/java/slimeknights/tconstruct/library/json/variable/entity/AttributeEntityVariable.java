package slimeknights.tconstruct.library.json.variable.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

import java.util.function.Supplier;

/** Variable that fetches an attribute value */
public record AttributeEntityVariable(Attribute attribute) implements EntityVariable {
  public static final RecordLoadable<AttributeEntityVariable> LOADER = RecordLoadable.create(Loadables.ATTRIBUTE.requiredField("attribute", AttributeEntityVariable::attribute), AttributeEntityVariable::new);

  public AttributeEntityVariable(Supplier<Attribute> attribute) {
    this(attribute.get());
  }

  public AttributeEntityVariable(Holder<Attribute> attribute) {
    this(attribute.value());
  }

  @Override
  public float getValue(LivingEntity entity) {
    return (float)entity.getAttributeValue(BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attribute));
  }

  @Override
  public RecordLoadable<AttributeEntityVariable> getLoader() {
    return LOADER;
  }
}

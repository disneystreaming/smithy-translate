package smithytranslate;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;
import java.util.Objects;

public final class ConstTrait extends AbstractTrait implements ToSmithyBuilder<ConstTrait> {

  public static ShapeId ID = ShapeId.from("smithytranslate#const");

  private final Node value;

  public ConstTrait(Builder builder) {
    super(ID, builder.getSourceLocation());
    this.value = Objects.requireNonNull(builder.value, "value cannot be null");
  }

  @Override
  public Node createNode() {
    return value;
  }

  @Override
  public Builder toBuilder() {
    return builder();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder extends AbstractTraitBuilder<ConstTrait, Builder> {

    private Node value = null;

    public Builder withValue(Node value) {
      this.value = value;
      return this;
    }

    @Override
    public ConstTrait build() {
      return new ConstTrait(this);
    }

  }

  public static final class Provider extends AbstractTrait.Provider {

    public Provider() {
      super(ID);
    }

    @Override
    public ConstTrait createTrait(ShapeId target, Node node) {
      Builder builder = builder().sourceLocation(node);
      builder.withValue(node);
      return builder.build();
    }
  }
}

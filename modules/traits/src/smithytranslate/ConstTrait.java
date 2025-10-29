/* Copyright 2022 Disney Streaming
 *
 * Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://disneystreaming.github.io/TOST-1.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    public Builder value(Node value) {
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
      builder.value(node);
      return builder.build();
    }
  }
}

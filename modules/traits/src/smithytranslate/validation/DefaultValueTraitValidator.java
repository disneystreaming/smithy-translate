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

package smithytranslate.validation;

import java.util.ArrayList;
import java.util.List;
import smithytranslate.DefaultValueTrait;
import smithytranslate.NullableTrait;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;

// Adapted from the defaultTrait validator found at:
// https://github.com/awslabs/smithy/blob/423d1a843363bfbd6d0f2abfae2667288622bae1/smithy-model/src/main/java/software/amazon/smithy/model/validation/validators/DefaultTraitValidator.java
public final class DefaultValueTraitValidator extends AbstractValidator {
	@Override
	public List<ValidationEvent> validate(Model model) {
		List<ValidationEvent> events = new ArrayList<>();
		NodeValidationVisitor visitor = null;

		// Validate that default values are appropriate for shapes.
		for (MemberShape shape : model.getMemberShapesWithTrait(DefaultValueTrait.class)) {
			DefaultValueTrait trait = shape.expectTrait(DefaultValueTrait.class);
			Node value = trait.toNode();

			if (visitor == null) {
				visitor = NodeValidationVisitor.builder().model(model).eventId(getName()).value(value)
						.startingContext("Error validating @defaultValue trait").eventShapeId(shape.getId()).build();
			} else {
				visitor.setValue(value);
				visitor.setEventShapeId(shape.getId());
			}

			// allow null as default when nullable trait is present or if target type has
			// nullable trait
			boolean targetsNullableType = model.expectShape(shape.getTarget()).hasTrait(NullableTrait.class);
			boolean hasNullableTrait = shape.hasTrait(NullableTrait.class);
			if (!(hasNullableTrait || targetsNullableType) || !value.isNullNode()) {
				events.addAll(shape.accept(visitor));
			}

			switch (model.expectShape(shape.getTarget()).getType()) {
			case MAP:
				value.asObjectNode().ifPresent(obj -> {
					if (!obj.isEmpty()) {
						events.add(error(shape, trait, "The @defaultValue value of a map must be an empty map"));
					}
				});
				break;
			case LIST:
			case SET:
				value.asArrayNode().ifPresent(array -> {
					if (!array.isEmpty()) {
						events.add(error(shape, trait, "The @defaultValue value of a list must be an empty list"));
					}
				});
				break;
			case DOCUMENT:
				value.asArrayNode().ifPresent(array -> {
					if (!array.isEmpty()) {
						events.add(error(shape, trait,
								"The @defaultValue value of a document cannot be a non-empty " + "array"));
					}
				});
				value.asObjectNode().ifPresent(obj -> {
					if (!obj.isEmpty()) {
						events.add(error(shape, trait,
								"The @defaultValue value of a document cannot be a non-empty " + "object"));
					}
				});
				break;
			default:
				break;
			}
		}

		return events;
	}
}

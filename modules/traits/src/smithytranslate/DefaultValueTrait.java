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

import software.amazon.smithy.model.traits.*;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public final class DefaultValueTrait extends AbstractTrait {

	public static final ShapeId ID = ShapeId.from("smithytranslate#defaultValue");

	public DefaultValueTrait(Node value) {
		super(ID, value);
	}

	@Override
	protected Node createNode() {
		throw new UnsupportedOperationException("NodeCache is always set");
	}

	public static final class Provider extends AbstractTrait.Provider {
		public Provider() {
			super(ID);
		}

		@Override
		public Trait createTrait(ShapeId target, Node value) {
			return new DefaultValueTrait(value);
		}
	}
}

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

import software.amazon.smithy.model.shapes.*;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.traits.RequiredTrait;

import java.util.Collections;
import java.util.List;

final public class Document {
	static public ShapeId target = ShapeId.fromParts(Prelude.NAMESPACE, "Document");
	static public List<Shape> shapes = documentShapes();

	static private List<Shape> documentShapes() {
		Shape dNull = StructureShape.builder().id("smithytranslate#DNull").build();
		Shape dBoolean = BooleanShape.builder().id("smithytranslate#DBoolean").build();
		Shape dNumber = DoubleShape.builder().id("smithytranslate#DNumber").build();
		Shape dString = StringShape.builder().id("smithytranslate#DString").build();

		Shape documentList = ListShape.builder().id("smithytranslate#DocumentList")
				.member(ShapeId.fromParts("smithytranslate", "Document")).build();

		Shape documentMap = MapShape.builder().id("smithytranslate#DocumentMap")
				.key(ShapeId.fromParts("smithytranslate", "DString"))
				.value(ShapeId.fromParts("smithytranslate", "Document")).build();

		Shape dArray = StructureShape.builder().id("smithytranslate#DArray")
				.addMember("value", ShapeId.fromParts("smithytranslate", "DocumentList")).build();

		Shape dObject = StructureShape.builder().id("smithytranslate#DObject")
				.addMember("value", ShapeId.fromParts("smithytranslate", "DocumentMap")).build();

		Shape document = UnionShape.builder().id("smithytranslate#Document")
				.addMember("dNull", ShapeId.fromParts("smithytranslate", "DNull"))
				.addMember("dBoolean", ShapeId.fromParts("smithytranslate", "DBoolean"))
				.addMember("dNumber", ShapeId.fromParts("smithytranslate", "DNumber"))
				.addMember("dString", ShapeId.fromParts("smithytranslate", "DString"))
				.addMember("dArray", ShapeId.fromParts("smithytranslate", "DArray"))
				.addMember("dObject", ShapeId.fromParts("smithytranslate", "DObject")).build();

		List<Shape> result = new java.util.ArrayList<>();
		result.add(dNull);
		result.add(dBoolean);
		result.add(dNumber);
		result.add(dString);
		result.add(documentList);
		result.add(documentMap);
		result.add(dArray);
		result.add(dObject);
		result.add(document);
		return Collections.unmodifiableList(result);
	}
}

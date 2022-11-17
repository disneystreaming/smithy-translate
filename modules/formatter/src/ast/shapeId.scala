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
package smithytranslate
package formatter
package ast

case class ShapeId(
    rootShapeId: RootShapeId,
    shapeIdMember: Option[ShapeIdMember]
)

case class RootShapeId(either: Either[AbsoluteRootShapeId, Identifier])

case class AbsoluteRootShapeId(nameSpace: Namespace, identifier: Identifier)

case class Namespace(identifier: Identifier, suffix: List[Identifier])

case class Identifier(start: IdentifierStart, chars: List[IdentifierChar])

case class IdentifierStart(underScores: List[Underscore], char: Char)

case class ShapeIdMember(identifier: Identifier)

case class IdentifierChar(
    char: Char
)

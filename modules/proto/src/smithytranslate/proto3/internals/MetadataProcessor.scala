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

package smithytranslate.proto3.internals

import software.amazon.smithy.model.Model
import scala.jdk.OptionConverters._
import scala.jdk.CollectionConverters._

private[internals] object MetadataProcessor {

  type ProtocOptions = Map[String, Map[String, String]]

  /* Example:
   * ```
   * metadata "proto_options" = [{
   *   "demo": {
   *       "java_multiple_files": "true",
   *       "java_package": "\"demo.hello\""
   *   }
   * }]
   * ```
   */
  def extractProtocOptions(m: Model): ProtocOptions = {
    val protoOptionsNode =
      Option(m.getMetadata().get("proto_options")).flatMap {
        _.asArrayNode().toScala
      }

    protoOptionsNode match {
      case None => Map.empty
      case Some(protoOptions) =>
        val listOfNamespaceNodes = protoOptions.asScala.toList
          .flatMap(_.asObjectNode().toScala.toList)
          .flatMap(_.getMembers().asScala)

        listOfNamespaceNodes.foldLeft[ProtocOptions](Map.empty) {
          case (acc, (nsNode, optionsNode)) =>
            optionsNode.asObjectNode().toScala match {
              case Some(options) =>
                val nsOptions: Map[String, String] =
                  options.getMembers().asScala.toMap.flatMap {
                    case (optionKeyNode, optionValueNode) =>
                      optionValueNode
                        .asStringNode()
                        .toScala
                        .toList
                        .map { strValue =>
                          optionKeyNode.getValue() -> strValue.getValue()
                        }
                        .toMap
                  }
                acc + (nsNode.getValue -> nsOptions)
              case None =>
                acc
            }
        }
    }
  }
}

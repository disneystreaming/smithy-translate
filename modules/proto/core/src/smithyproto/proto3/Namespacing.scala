package smithyproto.proto3

import software.amazon.smithy.model.shapes.ShapeId
import smithyproto.proto3.ProtoIR.Fqn

object Namespacing {
  def shapeIdToFqn(id: ShapeId): Fqn =
    Fqn(Some(namespaceToPackage(id.getNamespace)), id.getName)

  def shapeIdToImportFqn(id: ShapeId): Fqn =
    namespaceToFqn(id.getNamespace())

  private def namespaceToPackage(namespace: String): List[String] =
    namespace.split("\\.").toList

  def namespaceToFqn(ns: String): Fqn = {
    val parts = ns.split("\\.").toList
    if (parts.size == 0) {
      Fqn(None, "definitions") // should not happen
    } else if (parts.size == 1) {
      Fqn(Some(parts), "definitions")
    } else {
      val last = parts.last
      Fqn(Some(parts.init), last)
    }
  }
}

package smithytranslate.compiler.internals.postprocess

import smithytranslate.compiler.internals._

private[postprocess] object util {
  // returns all Ids that are ever referenced as a target
  def getAllTargets(
      allShapes: Vector[Definition]
  ): Set[DefId] = {
    allShapes.flatMap {
      case s: Structure =>
        s.localFields.map(_.tpe)
      case s: SetDef =>
        Vector(s.member)
      case l: ListDef =>
        Vector(l.member)
      case m: MapDef =>
        Vector(m.key, m.value)
      case u: Union =>
        u.alts.map(_.tpe)
      case n: Newtype =>
        Vector(n.target)
      case _: Enumeration => Vector.empty
      case o: OperationDef =>
        val allRefs = o.input.toVector ++ o.output.toVector ++ o.errors
        allRefs
      case _: ServiceDef => Vector.empty
    }.toSet
  }
}

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

package smithytranslate.compiler.internals

import java.util.Date
import java.time._
import java.util.UUID

private[compiler] sealed trait Primitive {
  type T
}

private[compiler] object Primitive {

  type Aux[TT] = Primitive { type T = TT }

  case object PInt extends Primitive { type T = Int }
  case object PBoolean extends Primitive { type T = Boolean }
  case object PString extends Primitive { type T = String }
  case object PLong extends Primitive { type T = Long }
  case object PByte extends Primitive { type T = Byte }
  case object PFloat extends Primitive { type T = Float }
  case object PDouble extends Primitive { type T = Double }
  case object PShort extends Primitive { type T = Short }
  case object PUUID extends Primitive { type T = UUID }
  case object PDate extends Primitive { type T = Date }
  case object PDateTime extends Primitive { type T = OffsetDateTime }
  case object PTimestamp extends Primitive { type T = Instant }
  case object PLocalDate extends Primitive { type T = LocalDate }
  case object PLocalTime extends Primitive { type T = LocalTime }
  case object PLocalDateTime extends Primitive { type T = LocalDateTime }
  case object POffsetDateTime extends Primitive { type T = OffsetTime }
  case object POffsetTime extends Primitive { type T = OffsetTime }
  case object PZoneId extends Primitive { type T = ZoneId }
  case object PZoneOffset extends Primitive { type T = ZoneOffset }
  case object PZonedDateTime extends Primitive { type T = ZonedDateTime }
  case object PYear extends Primitive { type T = Year }
  case object PYearMonth extends Primitive { type T = YearMonth }
  case object PMonthDay extends Primitive { type T = MonthDay }
  case object PDayOfWeek extends Primitive { type T = DayOfWeek }
  case object PMonth extends Primitive { type T = Month }
  case object PBytes extends Primitive { type T = Array[Byte] }
  case object PFreeForm extends Primitive { type T = Any }
}

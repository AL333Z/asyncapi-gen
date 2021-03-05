package asyncapigen.protobuf

import cats.Eq
import cats.data.NonEmptyList
import cats.implicits._

object schema {
  final case class OptionValue(name: String, value: String)
  object OptionValue {
    implicit val optionEq: Eq[OptionValue] = Eq.instance { case (OptionValue(n, v), OptionValue(n2, v2)) =>
      n === n2 && v === v2
    }
  }

  sealed trait FieldType {
    val name: String
    val tpe: Protobuf
    // this is a list because a oneof field will have one index per branch
    def indices: List[Int]
  }

  object FieldType {
    final case class Field(
        name: String,
        tpe: Protobuf,
        position: Int,
        options: List[OptionValue],
        isRepeated: Boolean,
        isMapField: Boolean
    ) extends FieldType {
      def indices: List[Int] = List(position)
    }

    final case class OneOfField(name: String, tpe: Protobuf, indices: List[Int]) extends FieldType

    implicit val fieldEq: Eq[FieldType] =
      Eq.instance {
        case (Field(n, t, p, o, r, m), Field(n2, t2, p2, o2, r2, m2)) =>
          n === n2 && t === t2 && p === p2 && o === o2 && r === r2 && m === m2
        case (OneOfField(n, tpe, is), OneOfField(n2, tpe2, is2)) =>
          n === n2 && tpe === tpe2 && is === is2
        case _ => false
      }
  }

  sealed trait IntModifier extends Product with Serializable
  case object Signed       extends IntModifier
  case object Unsigned     extends IntModifier
  case object FixedWidth   extends IntModifier

  sealed abstract class Protobuf
  object Protobuf {
    final case object TNull                                                      extends Protobuf
    final case object TDouble                                                    extends Protobuf
    final case object TFloat                                                     extends Protobuf
    final case object TInt32                                                     extends Protobuf
    final case object TInt64                                                     extends Protobuf
    final case object TUint32                                                    extends Protobuf
    final case object TUint64                                                    extends Protobuf
    final case object TSint32                                                    extends Protobuf
    final case object TSint64                                                    extends Protobuf
    final case object TFixed32                                                   extends Protobuf
    final case object TFixed64                                                   extends Protobuf
    final case object TSfixed32                                                  extends Protobuf
    final case object TSfixed64                                                  extends Protobuf
    final case object TBool                                                      extends Protobuf
    final case object TString                                                    extends Protobuf
    final case object TBytes                                                     extends Protobuf
    final case class TNamedType(prefix: List[String], name: String)              extends Protobuf
    final case class TOptionalNamedType(prefix: List[String], name: String)      extends Protobuf
    final case class TRepeated(value: Protobuf)                                  extends Protobuf
    final case class TOneOf(name: String, fields: NonEmptyList[FieldType.Field]) extends Protobuf
    final case class TMap(keyTpe: Protobuf, value: Protobuf)                     extends Protobuf
    final case class TEnum(
        name: String,
        symbols: List[(String, Int)],
        options: List[OptionValue],
        aliases: List[(String, Int)]
    ) extends Protobuf
    final case class TMessage(
        name: String,
        fields: List[FieldType],
        reserved: List[List[String]],
        nestedMessages: List[Protobuf],
        nestedEnums: List[Protobuf]
    )                                                                                         extends Protobuf
    final case class TFileDescriptor(values: List[Protobuf], name: String, `package`: String) extends Protobuf

    implicit val protobufEq: Eq[Protobuf] =
      Eq.instance {
        case (TNull, TNull)                                         => true
        case (TDouble, TDouble)                                     => true
        case (TFloat, TFloat)                                       => true
        case (TInt32, TInt32)                                       => true
        case (TInt64, TInt64)                                       => true
        case (TUint32, TUint32)                                     => true
        case (TUint64, TUint64)                                     => true
        case (TSint32, TSint32)                                     => true
        case (TSint64, TSint64)                                     => true
        case (TFixed32, TFixed32)                                   => true
        case (TFixed64, TFixed64)                                   => true
        case (TSfixed32, TSfixed32)                                 => true
        case (TSfixed64, TSfixed64)                                 => true
        case (TBool, TBool)                                         => true
        case (TString, TString)                                     => true
        case (TBytes, TBytes)                                       => true
        case (TNamedType(p, n), TNamedType(p2, n2))                 => p === p2 && n === n2
        case (TOptionalNamedType(p, n), TOptionalNamedType(p2, n2)) => p === p2 && n === n2
        case (TRepeated(v), TRepeated(v2))                          => v === v2
        case (TEnum(n, s, o, a), TEnum(n2, s2, o2, a2))             => n === n2 && s === s2 && o === o2 && a === a2
        case (TMessage(n, f, r, nm, ne), TMessage(n2, f2, r2, nm2, ne2)) =>
          n === n2 && f === f2 && r === r2 && nm == nm2 && ne == ne2
        case _ => false
      }
  }
}

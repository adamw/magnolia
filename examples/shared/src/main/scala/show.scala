package magnolia.examples

import magnolia._
import scala.language.experimental.macros

/** shows one type as another, often as a string
  *
  *  Note that this is a more general form of `Show` than is usual, as it permits the return type to
  *  be something other than a string. */
trait Show[Out, T] { def show(value: T): Out }

/**
  * For the present example, a type lambda was needed to use the TypeclassCompanion
  */
trait GenericShow[Out] extends TypeclassCompanion[({type S[A] = Show[Out, A]})#S]{

  def join(typeName: String, strings: Seq[String]): Out

  /** creates a new [[Show]] instance by labelling and joining (with `mkString`) the result of
    *  showing each parameter, and prefixing it with the class name */
  def combine[T](ctx: CaseClass[Typeclass, T]): Show[Out, T] = new Show[Out, T] {
    def show(value: T) =
      if (ctx.isValueClass) {
        val param = ctx.parameters.head
        param.typeclass.show(param.dereference(value))
      } else {
        val paramStrings = ctx.parameters.map { param =>
          s"${param.label}=${param.typeclass.show(param.dereference(value))}"
        }

        join(ctx.typeName.split("\\.").last, paramStrings)
      }
  }

  /** choose which typeclass to use based on the subtype of the sealed trait */
  def dispatch[T](ctx: SealedTrait[Typeclass, T]): Show[Out, T] = new Show[Out, T] {
    def show(value: T): Out = ctx.dispatch(value) { sub =>
      sub.typeclass.show(sub.cast(value))
    }
  }
}

/** companion object to [[Show]] */
object Show extends GenericShow[String] {

  type S[A] = Show[String, A]

  /** show typeclass for strings */
  implicit val string: Show[String, String] = new Show[String, String] {
    def show(s: String): String = s
  }

  def join(typeName: String, params: Seq[String]): String =
    params.mkString(s"$typeName(", ",", ")")

  /** show typeclass for integers */
  implicit val int: Show[String, Int] = new Show[String, Int] {
    def show(s: Int): String = s.toString
  }
}

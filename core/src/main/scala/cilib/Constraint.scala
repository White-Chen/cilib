package cilib

import scalaz.{Maybe,NonEmptyList}
import spire.algebra.Eq
import spire.math._
import spire.math.interval._
import spire.implicits._

final class ViolationCount(val count: Int) extends AnyVal
object ViolationCount {
  def apply(i: Int): Maybe[ViolationCount] =
    if (i >= 0) Maybe.Just(new ViolationCount(i))
    else Maybe.Empty()

  val zero = new ViolationCount(0)

  import scalaz.Order
  import scalaz.std.anyVal._
  implicit val violationOrder: Order[ViolationCount] = new Order[ViolationCount] {
    def order(x: ViolationCount, y: ViolationCount) =
      Order[Int].order(x.count, y.count)
  }

  import scalaz._
  implicit def violationMonoid[A] = new Monoid[ViolationCount] {
    def zero = ViolationCount.zero
    def append(f1: ViolationCount, f2: => ViolationCount) =
      ViolationCount(f1.count + f2.count).getOrElse(zero)

  }
}

case class ConstraintFunction[A,B](f: NonEmptyList[A] => B) {
  def apply(a: NonEmptyList[A]): B =
    f(a)
}

sealed trait Constraint[A,B]
case class LessThan[A,B](f: ConstraintFunction[A,B], v: B) extends Constraint[A,B]
case class LessThanEqual[A,B](f: ConstraintFunction[A,B], v: B) extends Constraint[A,B]
case class Equal[A,B](f: ConstraintFunction[A,B], v: B) extends Constraint[A,B]
case class InInterval[A,B](f: ConstraintFunction[A,B], interval: Interval[B]) extends Constraint[A,B]
case class GreaterThan[A,B](f: ConstraintFunction[A,B], v: B) extends Constraint[A,B]
case class GreaterThanEqual[A,B](f: ConstraintFunction[A,B], v: B) extends Constraint[A,B]

object Constraint {

  import scalaz.{Foldable, Functor}
//  def constrain[M[_]](ma: M[Eval[Double]], cs: List[Constraint[Double,Double]])(implicit M: Functor[M]) =
//    M.map(ma)(_.constrainBy(cs))

  def violationMagnitude[A,B:Fractional](beta: Double, eta: Double, constraints: List[Constraint[A,B]], cs: NonEmptyList[A])(implicit e: Eq[B]): Double =
    constraints.map(_ match {
      case LessThan(f, v) =>
        val v2 = f(cs)
        if (v2 < v) 0.0
        else math.pow(math.abs(v2.toDouble - v.toDouble), beta) + eta
      case LessThanEqual(f, v) =>
        val v2 = f(cs)
        if (v2 <= v) 0.0
        else math.pow(math.abs(v2.toDouble - v.toDouble), beta) + eta
      case Equal(f, v) =>
        val v2 = f(cs)
        if (e.eqv(v2, v)) 0.0 // Doubles are "equal" if they are equivalent using IEEE floats.
        else math.pow(math.abs(v2.toDouble - v.toDouble), beta) + eta
      case InInterval(f, i) =>
        val v2 = f(cs)
        val left = i.lowerBound match {
          case Closed(value) => value <= v2
          case Open(value) => value < v2
          case Unbound() => true
          case EmptyBound() => false
        }
        val right = i.upperBound match {
          case Closed(value) => v2 <= value
          case Open(value) => v2 < value
          case Unbound() => true
          case EmptyBound() => false
        }

        (left, right) match {
          case (true, true) => 0.0
          case (false, _) => i.lowerBound match {
            case Closed(v) => math.pow(math.abs(v.toDouble - v2.toDouble), beta)
            case Open(v)   => math.pow(math.abs(v.toDouble - v2.toDouble), beta) + eta
            case _ => 0.0
          }
          case (_, false) =>  i.upperBound match {
            case Closed(v) => math.pow(math.abs(v2.toDouble - v.toDouble), beta)
            case Open(v)   => math.pow(math.abs(v2.toDouble - v.toDouble), beta) + eta
            case _ => 0.0
          }
        }
      case GreaterThan(f, v) =>
        val v2 = f(cs)
        if (v2 > v) 0.0
        else math.pow(math.abs(v2.toDouble + v.toDouble), beta) + eta
      case GreaterThanEqual(f, v) =>
        val v2 = f(cs)
        if (v2 >= v) 0.0
        else math.pow(math.abs(v2.toDouble + v.toDouble), beta) + eta
    }).sum

  def violationCount[A,B:Fractional](constraints: List[Constraint[A,B]], cs: NonEmptyList[A]): ViolationCount =
    ViolationCount(constraints.map(satisfies(_, cs)).filterNot(x => x).length).getOrElse(ViolationCount.zero)

  def satisfies[A,B:Fractional](constraint: Constraint[A,B], cs: NonEmptyList[A])(implicit ev: Eq[B]) =
    constraint match {
      case LessThan(f, v) => f(cs) < v
      case LessThanEqual(f, v) => f(cs) <= v
      case Equal(f, v) => ev.eqv(f(cs), v)
      case InInterval(f, i) =>
        val v2 = f(cs)
        val c1 = i.lowerBound match {
          case Open(value) => value < v2
          case Closed(value) => value <= v2
          case Unbound() => true
          case EmptyBound() => false
        }
        val c2 = i.upperBound match {
          case Open(value) => v2 < value
          case Closed(value) => v2 <= value
          case Unbound() => true
          case EmptyBound() => false
        }
        c1 && c2
      case GreaterThan(f, v) => f(cs) > v
      case GreaterThanEqual(f, v) => { /*println("f(cs): " + f(cs)) ;*/ f(cs) >= v }
    }
}

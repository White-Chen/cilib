package cilib

import _root_.scala.Predef.{any2stringadd => _, _}

import scalaz._
import scalaz.syntax.functor._
import scalaz.std.tuple._
import scalaz.std.list._

import spire.math._
import spire.algebra._
import spire.implicits._
import Position._

case class Mem[A](b: Position[List, A], v: Position[List, A])

trait Memory[A] {
  def memoryLens: Lens[A, Position[List,Double]]
}

object Memory {
  implicit object MemMemory extends Memory[Mem[Double]] {
    def memoryLens: Lens[Mem[Double],Position[List,Double]] = Lens.lensu((a,b) => a.copy(b = b), _.b)
  }
}

trait Velocity[A] {
  def velocityLens: Lens[A, Position[List,Double]]
}

object Velocity {
  implicit object MemVelocity extends Velocity[Mem[Double]] {
    def velocityLens: Lens[Mem[Double], Position[List,Double]] = Lens.lensu((a,b) => a.copy(v = b), _.v)
  }
}

object PSO {
  def stdPosition[S](c: Particle[S,Double], v: Position[List,Double]): Instruction[Particle[S,Double]] =
    Instruction.point((c._1, c._2 + v))

  // Dist \/ Double (scalar value)
  // This needs to be fleshed out to cater for the parameter constants // remember to extract Dists
  def stdVelocity[S](entity: (S,Position[List,Double]), social: Position[List,Double], cognitive: Position[List, Double], w: Double, c1: Double, c2: Double)(implicit V: Velocity[S]): Instruction[Position[List,Double]] = {
    val (state,pos) = entity
    Instruction.pointR(for {
      cog <- (cognitive - pos) traverse (x => Dist.stdUniform.map(_ * x))
      soc <- (social - pos) traverse (x => Dist.stdUniform.map(_ * x))
    } yield (w *: V.velocityLens.get(state)) + (c1 *: cog) + (c2 *: soc))
  }

  // Instruction to evaluate the particle // what about cooperative?
  def evalParticle[S](entity: Particle[S,Double]): Instruction[Particle[S,Double]] = {
    Instruction.pointS(StateT(p => {
      val r = entity._2.eval(p)
      RVar.point((r._1, (entity._1, r._2)))
    }))
  }

  // The following function needs a lot of work... the biggest issue is the case of the state 'S' and how to get the values out of it and how to update again??? Lenses? Typeclasses?
  def updatePBest[S](p: Particle[S,Double])(implicit M: Memory[S]): Instruction[Particle[S,Double]] = {
    val pbestL = M.memoryLens
    val (state, pos) = p
    Instruction.liftK(Fitness.compare(pos, pbestL.get(state)).map(x => (pbestL.set(state, x), pos)))
  }

  def updateVelocity[S](p: Particle[S,Double], v: Position[List,Double])(implicit V: Velocity[S]) =
    Instruction.pointS(StateT(s => RVar.point((s, (V.velocityLens.set(p._1, v), p._2)))))

  def createParticle[S](f: Position[List,Double] => Particle[S,Double])(pos: Position[List,Double]) =
    f(pos)

}

object Guide {

  def identity[S,A]: Guide[S,A] =
    (collection, x) => Instruction.point(x._2)

  def pbest[S](implicit M: Memory[S]): Guide[S,Double] =
    (collection, x) => Instruction.point(M.memoryLens.get(x._1))

  def nbest[S,A]: Guide[S,A] = // TODO: Change the collection type to NonEmptyList because reduce is unsafe on List
    (collection, x) => new Instruction(Kleisli[X, Opt, Pos[A]]((o: Opt) => StateT((p: Problem[List,Double]) => RVar.point {
      (p, collection.map(_._2).reduceLeft((a, c) => Fitness.compare(a, c) run o))
    })))

}


/*
next pso work:
==============
- dynamic psos (quantum, charged, etc) bennie
- vepso / dvepso (robert afer moo & dmoo)
- cooperative & variations
- heterogenous filipe

- niching (less important for now)

commonalities:
- subswarms

functions:
- moo & dmoo functions (benchmarks) robert

*/


/*
 Stopping conditions:
 ====================
 iteration based stopping conditions
 fitness evaluations
 dimension based updates
 # of position updates (only defined if change is some epislon based on the position vector)
 # of dimensional updates > epsilon
 */
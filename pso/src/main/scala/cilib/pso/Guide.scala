package cilib
package pso

import scalaz.NonEmptyList
import scalaz.Scalaz._

import spire.math.Interval

// A Guide is a selection followed by a comparison, wrapped up in a Step
object Guide {

  def identity[S,F[_],A]: Guide[S,A] =
    (_, x) => Step.point(x.pos)

  def pbest[S,A](implicit M: HasMemory[S,A]): Guide[S,A] =
    (_, x) => Step.point(M._memory.get(x.state))

  def nbest[S](neighbourhood: IndexSelection[Particle[S,Double]])(implicit M: HasMemory[S,Double]): Guide[S,Double] = {
    (collection, x) => Step.liftK(o => {
      val selected: List[Particle[S,Double]] = neighbourhood(collection, x)
      val fittest = selected.map(e => M._memory.get(e.state)).reduceLeftOption((a, c) => Comparison.compare(a, c) apply (o))
      fittest.getOrElse(sys.error("Impossible: reduce on entity memory worked on empty memory member"))
    })
  }

  def dominance[S](selection: IndexSelection[Particle[S,Double]]): Guide[S,Double] = {
    (collection, x) => Step.liftK(o => {
      val neighbourhood = selection(collection, x)
      val comparison = Comparison.dominance(o.opt)
      neighbourhood
        .map(_.pos)
        .reduceLeftOption((a,c) => comparison.apply(a, c))
        .getOrElse(sys.error("????"))
    })
  }

  def gbest[S](implicit M: HasMemory[S,Double]): Guide[S,Double] =
    nbest(Selection.star)

  def lbest[S](n: Int)(implicit M: HasMemory[S,Double]) =
    nbest(Selection.indexNeighbours[Particle[S,Double]](n))

  def vonNeumann[S](implicit M: HasMemory[S,Double]) =
    nbest(Selection.latticeNeighbours[Particle[S,Double]])

  def nmpc[S](prob: Double): Guide[S,Double] =
    (collection, x) => Step.pointR {
      val col = collection.filter(_ != x)
      val chosen = RVar.sample(3, col).run
      val crossover = Crossover.nmpc

      for {
        chos     <- chosen
        parents  = chos.map(c => NonEmptyList.nel(x.pos, c.map(_.pos).toIList))
        children <- parents.map(crossover).getOrElse(RVar.point(NonEmptyList(x.pos)))
        probs    <- x.pos.traverse(_ => Dist.stdUniform)
      } yield {
        val zipped = x.pos zip children.head zip probs
        zipped.map { case ((xi, ci), pi) => if (pi < prob) ci else xi }
      }
    }

  def pcx[S](s1: Double, s2: Double)(implicit M: HasMemory[S,Double]): Guide[S,Double] =
    (collection, x) => {
      val gb = gbest
      val pb = pbest
      val pcx = Crossover.pcx(s1, s2)

      for {
        p         <- pb(collection, x)
        i         <- identity(collection, x)
        n         <- gb(collection, x)
        parents    = NonEmptyList(p, i, n)
        offspring <- Step.pointR(pcx(parents))
      } yield offspring.head
    }

  def undx[S](s1: Double, s2: Double)(implicit M: HasMemory[S,Double]): Guide[S,Double] =
    (collection, x) => {
      val gb = gbest
      val pb = pbest
      val undx = Crossover.undx(s1, s2)

      for {
        p         <- pb(collection, x)
        i         <- identity(collection, x)
        n         <- gb(collection, x)
        parents    = NonEmptyList(p, i, n)
        offspring <- Step.pointR(undx(parents))
      } yield offspring.head
    }

}

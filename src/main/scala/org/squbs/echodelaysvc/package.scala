package org.squbs

import scala.concurrent.duration.FiniteDuration
import scala.util.Random
import scala.concurrent.duration._
import Math._

import akka.http.scaladsl.unmarshalling.{FromStringUnmarshaller, Unmarshaller}

import scala.concurrent.Future

package object echodelaysvc {

  implicit def durationUnmarshaller: FromStringUnmarshaller[FiniteDuration] =
    Unmarshaller(_ ⇒ s ⇒ Future.successful(Duration(s).asInstanceOf[FiniteDuration]))


  implicit class DurationRandom(val random: Random) extends AnyVal {

    def nextGaussian(min: FiniteDuration, mean: FiniteDuration, max: FiniteDuration,
                     sigma: FiniteDuration): FiniteDuration = {
      require(min < mean, "The provided minimum is not below mean")
      require(mean < max, "The provided mean is not below max")
      inRange((random.nextGaussian() * sigma.toNanos).toLong.nanos + mean, min, max)
    }

    def nextNegativeExponential(min: FiniteDuration, mean: FiniteDuration, max: FiniteDuration,
                                truncate: Boolean = false): FiniteDuration = {
      require(min < mean, "The provided minimum is not below mean")
      require(mean < max, "The provided mean is not below max")
      val (aMean, shift) = if (truncate) (mean, 0 nanos) else (mean - min, min)
      val dRandom = {
        val xx = random.nextDouble()
        if (xx > 0d) xx else Double.MinPositiveValue
      }
      inRange((aMean.toNanos * -log(dRandom)).toLong.nanos + shift, min, max)
    }

    def nextUniform(min: FiniteDuration, max: FiniteDuration): FiniteDuration = {
      val minNs = min.toNanos
      (random.nextDouble * (max.toNanos - minNs) + minNs).toLong.nanos
    }

    private def inRange(v: FiniteDuration, min: FiniteDuration, max: FiniteDuration) =
      if (v < min) min
      else if (v > max) max
      else v
  }
}
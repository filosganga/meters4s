package com.ovoenergy.meters4s

import org.specs2.mutable.Specification
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import cats.effect.IO
import scala.jdk.CollectionConverters._
import io.micrometer.core.instrument.Tag
import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.TimeUnit
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.MockClock
import scala.concurrent.ExecutionContext

class ReporterTest(implicit ec: ExecutionContext) extends Specification {

  implicit val cs = IO.contextShift(ec)

  "counter" >> {
    "increment should increment underlying counter" >> {
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()

      val testee = reporter.counter("test.counter")
      testee.flatMap(_.increment).unsafeRunSync()

      registry.counter("test.counter").count() must_== 1
    }
    "increment with amount must increment underlying counter by that amount" >> {
      val someAmount: Double = 123
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()

      val testee = reporter.counter("test.counter")
      testee.flatMap(_.incrementN(someAmount)).unsafeRunSync()

      registry.counter("test.counter").count() must_== someAmount
    }
    "count must return the value of the underlying counter" >> {
      val someAmount: Double = 123
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()
      registry.counter("test.counter").increment(someAmount)

      val testee = reporter.counter("test.counter")
      testee.flatMap(_.count).unsafeRunSync() must_== someAmount
    }
    "must add specified tags" >> {
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()
      val someTags = Map("tag 1" -> "A", "tag 2" -> "B")

      val testee = reporter.counter("test.counter", someTags)
      testee.flatMap(_.increment).unsafeRunSync()

      val resultingTags = registry
        .find("test.counter")
        .counter()
        .getId()
        .getTags()
        .asScala

      resultingTags must contain(
        Tag.of("tag 1", "A"),
        Tag.of("tag 2", "B")
      )
    }
    "must add configured global tags" >> {
      val registry = new SimpleMeterRegistry
      val someTags = Map("tag 1" -> "A", "tag 2" -> "B")
      val reporter =
        Reporter
          .fromRegistry[IO](registry, MetricsConfig(tags = someTags))
          .unsafeRunSync()

      val testee = reporter.counter("test.counter")
      testee.flatMap(_.increment).unsafeRunSync()

      val resultingTags = registry
        .find("test.counter")
        .counter()
        .getId()
        .getTags()
        .asScala

      resultingTags must contain(
        Tag.of("tag 1", "A"),
        Tag.of("tag 2", "B")
      )
    }
    "must add configured global prefix" >> {
      val registry = new SimpleMeterRegistry
      val somePrefix = "some.prefix"
      val reporter =
        Reporter
          .fromRegistry[IO](registry, MetricsConfig(prefix = somePrefix))
          .unsafeRunSync()

      val testee = reporter.counter("test.counter")
      testee.flatMap(_.increment).unsafeRunSync()

      val resultingName = registry
        .find(somePrefix + "test.counter")
        .counter()
        .getId()
        .getName()

      resultingName must startWith(somePrefix)
    }
  }

  "timer" >> {
    "record should record the supplied duration" >> {
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()

      val testee = reporter.timer("test.timer")
      testee
        .flatMap(_.record(FiniteDuration(10, TimeUnit.SECONDS)))
        .unsafeRunSync()

      registry.timer("test.timer").totalTime(TimeUnit.SECONDS) must_== 10
    }

    "wrap must time the wrapped task" >> {
      val mockClock = new MockClock
      val registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, mockClock)
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()

      val testee = reporter.timer("test.timer")

      testee
        .flatMap(_.wrap(IO { mockClock.add(123, TimeUnit.SECONDS) }))
        .unsafeRunSync()

      registry
        .timer("test.timer")
        .totalTime(TimeUnit.SECONDS) must_== 123
    }

    "count must return the value of the underlying counter" >> {
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()
      registry.timer("test.timer").record(10, TimeUnit.SECONDS)

      val testee = reporter.timer("test.timer")
      testee.flatMap(_.count).unsafeRunSync() must_== 1
    }

    "must add specified tags" >> {
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()
      val someTags = Map("tag 1" -> "A", "tag 2" -> "B")

      val testee = reporter.timer("test.timer", someTags)
      testee
        .flatMap(_.record(FiniteDuration(10, TimeUnit.SECONDS)))
        .unsafeRunSync()

      val resultingTags = registry
        .find("test.timer")
        .timer()
        .getId()
        .getTags()
        .asScala

      resultingTags must contain(
        Tag.of("tag 1", "A"),
        Tag.of("tag 2", "B")
      )
    }
    "must add configured global tags" >> {
      val registry = new SimpleMeterRegistry
      val someTags = Map("tag 1" -> "A", "tag 2" -> "B")
      val reporter =
        Reporter
          .fromRegistry[IO](registry, MetricsConfig(tags = someTags))
          .unsafeRunSync()

      val testee = reporter.timer("test.timer")
      testee
        .flatMap(_.record(FiniteDuration(10, TimeUnit.SECONDS)))
        .unsafeRunSync()

      val resultingTags = registry
        .find("test.timer")
        .timer()
        .getId()
        .getTags()
        .asScala

      resultingTags must contain(
        Tag.of("tag 1", "A"),
        Tag.of("tag 2", "B")
      )
    }
    "must add configured global prefix" >> {
      val registry = new SimpleMeterRegistry
      val somePrefix = "some.prefix"
      val reporter =
        Reporter
          .fromRegistry[IO](registry, MetricsConfig(prefix = somePrefix))
          .unsafeRunSync()

      val testee = reporter.timer("test.timer")
      testee
        .flatMap(_.record(FiniteDuration(10, TimeUnit.SECONDS)))
        .unsafeRunSync()

      val resultingName = registry
        .find(somePrefix + "test.timer")
        .timer()
        .getId()
        .getName()

      resultingName must startWith(somePrefix)
    }
  }

  "gauge" >> {
    "increment should increment the gauge" >> {
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()

      val testee = reporter.gauge("test.gauge")
      testee.flatMap(_.increment).unsafeRunSync()

      registry.find("test.gauge").gauge().value must_== 1
    }

    "incrementN with amount must increment gauge by that amount" >> {
      val someAmount = 123
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()

      val testee = reporter.gauge("test.gauge")
      testee.flatMap(_.incrementN(someAmount)).unsafeRunSync()

      registry.find("test.gauge").gauge().value must_== someAmount
    }

    "decrement should decrement the gauge" >> {
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()

      val testee = reporter.gauge("test.gauge")
      testee.flatMap(_.decrement).unsafeRunSync()

      registry.find("test.gauge").gauge().value must_== -1
    }

    "decrementN with amount must decrement gauge by that amount" >> {
      val someAmount = 123
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()

      val testee = reporter.gauge("test.gauge")
      testee.flatMap(_.decrementN(someAmount)).unsafeRunSync()

      registry.find("test.gauge").gauge().value must_== -someAmount
    }

    "must add specified tags" >> {
      val registry = new SimpleMeterRegistry
      val reporter = Reporter.fromRegistry[IO](registry).unsafeRunSync()
      val someTags = Map("tag 1" -> "A", "tag 2" -> "B")

      val testee = reporter.gauge("test.gauge", someTags)
      testee.flatMap(_.increment).unsafeRunSync()

      val resultingTags = registry
        .find("test.gauge")
        .gauge()
        .getId()
        .getTags()
        .asScala

      resultingTags must contain(
        Tag.of("tag 1", "A"),
        Tag.of("tag 2", "B")
      )
    }

    "must add configured global tags" >> {
      val registry = new SimpleMeterRegistry
      val someTags = Map("tag 1" -> "A", "tag 2" -> "B")
      val reporter =
        Reporter
          .fromRegistry[IO](registry, MetricsConfig(tags = someTags))
          .unsafeRunSync()

      val testee = reporter.gauge("test.gauge")
      testee.flatMap(_.increment).unsafeRunSync()

      val resultingTags = registry
        .find("test.gauge")
        .gauge()
        .getId()
        .getTags()
        .asScala

      resultingTags must contain(
        Tag.of("tag 1", "A"),
        Tag.of("tag 2", "B")
      )
    }

    "must add configured global prefix" >> {
      val registry = new SimpleMeterRegistry
      val somePrefix = "some.prefix"
      val reporter =
        Reporter
          .fromRegistry[IO](registry, MetricsConfig(prefix = somePrefix))
          .unsafeRunSync()

      val testee = reporter.gauge("test.gauge")
      testee.flatMap(_.increment).unsafeRunSync()

      val resultingName = registry
        .find(somePrefix + "test.gauge")
        .gauge()
        .getId()
        .getName()

      resultingName must startWith(somePrefix)
    }

  }
}

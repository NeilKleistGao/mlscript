package hkmc2

import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import collection.concurrent.TrieMap
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

import org.scalatest.funsuite.AnyFunSuite

import hkmc2.CompilerCache.ArtifactCache


class CompilerCacheTest extends AnyFunSuite:
  private given ExecutionContext = ExecutionContext.global

  private final class Entry

  private def newCache(): ArtifactCache[Entry] =
    new ArtifactCache[Entry](TrieMap.empty, TrieMap.empty)

  test("artifact cache evaluates independent paths concurrently"):
    val cache = newCache()
    val bothStarted = new CountDownLatch(2)
    val release = new CountDownLatch(1)

    def request(path: io.Path): Future[Entry] = Future:
      cache.getOrCreate(path)(_ => true,
        {
          bothStarted.countDown()
          release.await()
          new Entry
        })

    val requests = List(request(io.Path("/A.mls")), request(io.Path("/B.mls")))
    val overlapped =
      try bothStarted.await(5, TimeUnit.SECONDS)
      finally release.countDown()
    Await.result(Future.sequence(requests), 10.seconds)

    assert(overlapped, "Building one path should not hold the cache monitor while it evaluates")

  test("artifact cache publishes one identity for concurrent requests to the same path"):
    val cache = newCache()
    val buildStarted = new CountDownLatch(1)
    val release = new CountDownLatch(1)
    val buildCount = new AtomicInteger
    val path = io.Path("/A.mls")

    def request(): Future[Entry] = Future:
      cache.getOrCreate(path)(_ => true,
        {
          buildCount.incrementAndGet()
          buildStarted.countDown()
          release.await()
          new Entry
        })

    val first = request()
    assert(buildStarted.await(5, TimeUnit.SECONDS), "The first artifact build should start")
    val second = request()
    release.countDown()
    val firstResult = Await.result(first, 10.seconds)
    val secondResult = Await.result(second, 10.seconds)

    assert(buildCount.get() == 1, "Concurrent requesters should share one build for a path")
    assert(firstResult eq secondResult, "Concurrent requesters should observe one artifact identity")

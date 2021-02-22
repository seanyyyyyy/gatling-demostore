package gatlingdemostore
import gatlingdemostore.pageobjects._

import scala.concurrent.duration._
import scala.util.Random
import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder
import io.gatling.jdbc.Predef._


class DemostoreSimulation extends Simulation {

  val domain = "demostore.gatling.io"

	val httpProtocol: HttpProtocolBuilder = http
		.baseUrl("http://" + domain)

  def userCount: Int = getProperty("USERS", "5").toInt
  def rampDuration: Int = getProperty("RAMP_DURATION", "10").toInt
  def testDuration: Int = getProperty("DURATION", "60").toInt

  private def getProperty(propertyName: String, defaultValue: String) = {
    Option(System.getenv(propertyName))
      .orElse(Option(System.getProperty(propertyName)))
      .getOrElse(defaultValue)
  }

  val rnd = new Random()
  def randomString(length: Int): String = {
    rnd.alphanumeric.filter(_.isLetter).take(length).mkString
  }

  before{
    println(s"Running test with ${userCount} users")
    println(s"Ramping users over ${rampDuration} users")
    println(s"Total test duration ${testDuration} seconds")
  }

  after{
    println("Stress testing complete.")
  }

  val initSession = exec(flushCookieJar)
    .exec(session => session.set("randomNumber", rnd.nextInt()))
    .exec(session => session.set("customerLoggedIn", false))
    .exec(session => session.set("cartTotal", 0.00))
    .exec(addCookie(Cookie("sessionId", randomString(10)).withDomain(domain)))
    .exec { session => println(session); session} // for debugging remove when actually running load test

	val scn = scenario("DemostoreSimulation")
    .exec(initSession)
    .exec(CmsPages.homepage)
		.pause(2)
    .exec(CmsPages.aboutUs)
		.pause(2)
    .exec(Catalog.Category.view)
		.pause(2)
		.exec(Catalog.Product.add)
		.pause(2)
		.exec(Checkout.viewCart)
		.pause(2)
		.exec(Checkout.completeCheckout)

  object UserJourneys {
    def minPause = 100 milliseconds
    def maxPause = 500 milliseconds

    def browseStore = {
      exec(initSession)
        .exec(CmsPages.homepage)
          .pause(maxPause)
          .exec(CmsPages.aboutUs)
          .pause(minPause, maxPause)
        .repeat(5) {
          exec(Catalog.Category.view)
            .pause(minPause, maxPause)
            .exec(Catalog.Product.view)
        }
    }

    def abandonCart = {
      exec(initSession)
        .exec(CmsPages.homepage)
        .pause(maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.add)
    }

    def completePurchase = {
      exec(initSession)
        .exec(CmsPages.homepage)
        .pause(maxPause)
        .exec(Catalog.Category.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.view)
        .pause(minPause, maxPause)
        .exec(Catalog.Product.add)
        .pause(minPause, maxPause)
        .exec(Checkout.viewCart)
        .pause(minPause, maxPause)
        .exec(Checkout.completeCheckout)
    }
  }

  object Scenarios {
    def default = scenario("Default Load Test")
      .during(testDuration seconds) {
        randomSwitch(
          75d -> exec(UserJourneys.browseStore),
          15d -> exec(UserJourneys.abandonCart),
          10d -> exec(UserJourneys.completePurchase)
        )
      }

    def highPurchase = scenario("High Purchase Load Test")
      .during(60 seconds) {
        randomSwitch(
          25d -> exec(UserJourneys.browseStore),
          25d -> exec(UserJourneys.abandonCart),
          50d -> exec(UserJourneys.completePurchase)
        )
      }
  }

  /* OPEN example
	setUp(
    scn.inject(
      atOnceUsers(1),
      nothingFor(5.seconds),
      rampUsers(10) during (20.seconds),
      nothingFor(10.seconds),
      constantUsersPerSec(1) during (20.seconds)
    ).protocols(httpProtocol)
  )
  */

  /* CLOSED example
  setUp(
    scn.inject(
      constantConcurrentUsers(10) during 20.seconds,
      rampConcurrentUsers(10) to 20 during 20.seconds
    ).protocols(httpProtocol)
  )
   */

  /* RPS
  setUp(
    scn.inject(
      constantUsersPerSec(1) during (3.minutes))
    ).protocols(httpProtocol).throttle(
      reachRps(10) in 30.seconds,
      holdFor(60.seconds),
      jumpToRps(20),
      holdFor(60.seconds)
    ).maxDuration(3.minutes)
   */

  setUp(
    Scenarios.default
      .inject(rampUsers(userCount) during (rampDuration seconds)).protocols(httpProtocol),
    Scenarios.highPurchase
      .inject(rampUsers(5) during (10 seconds)).protocols(httpProtocol)
  )
}

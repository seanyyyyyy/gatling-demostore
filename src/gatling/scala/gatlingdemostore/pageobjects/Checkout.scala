package gatlingdemostore.pageobjects
import io.gatling.core.Predef._
import io.gatling.http.Predef._

object Checkout {
  def viewCart = {
    doIf(session => !session("customerLoggedIn").as[Boolean]) {
      exec(Customer.login)
    }
      .exec(
        http("load cart page")
          .get("/cart/view")
          .check(status.is(200))
          .check(css("#grandTotal").is("$$${cartTotal}"))
      )
  }

  def completeCheckout = {
    exec(
      http("Checkout Cart")
        .get("/cart/checkout")
        .check(status.is(200))
        .check(substring("Thanks for your order!"))
    )
  }
}

package gatlingdemostore.pageobjects
import io.gatling.core.Predef._
import io.gatling.http.Predef._

object CmsPages {
  def homepage = {
    exec(http("Load homepage")
      .get("/")
      .check(status.is(200))
      .check(regex("<title>Gatling Demo-Store</title>").exists)
      .check(css("#_csrf", "content").saveAs("csrfValue"))
    )
  }

  def aboutUs = {
    exec(http("Load about us page")
      .get("/about-us")
      .check(status.is(200))
      .check(substring("About Us"))
    )
  }
}

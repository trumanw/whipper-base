/**
The MIT License (MIT)

Copyright (c) 2013 Jean Helou

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */
package filters

import controllers.Default
import play.api.Logger
import play.api.mvc.{Result, RequestHeader, Filter}
 
object CORSFilter extends Filter{

  import scala.concurrent._
  import ExecutionContext.Implicits.global
  lazy val allowedDomain = play.api.Play.current.configuration.getString("cors.allowed.domain")
  def isPreFlight(r: RequestHeader) =(
    r.method.toLowerCase.equals("options")
      &&
      r.headers.get("Access-Control-Request-Method").nonEmpty
    )
 
  def apply(f: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    Logger.trace("[cors] filtering request to add cors")
    if (isPreFlight(request)) {
      Logger.trace("[cors] request is preflight")
      Logger.trace(s"[cors] default allowed domain is $allowedDomain")
      Future.successful(Default.Ok.withHeaders(
        "Access-Control-Allow-Origin" -> allowedDomain.orElse(request.headers.get("Origin")).getOrElse("*"),
        "Access-Control-Allow-Methods" -> request.headers.get("Access-Control-Request-Method").getOrElse("*")
      ))
    } else {
      Logger.trace("[cors] request is normal")
      Logger.trace(s"[cors] default allowed domain is $allowedDomain")
      f(request).map{_.withHeaders(
        "Access-Control-Allow-Origin" -> allowedDomain.orElse(request.headers.get("Origin")).getOrElse("*"),
        "Access-Control-Allow-Methods" -> request.headers.get("Access-Control-Request-Method").getOrElse("*")
      )}
    }
  }
}
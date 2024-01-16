/* Copyright 2022 Disney Streaming
 *
 * Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://disneystreaming.github.io/TOST-1.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package smithytranslate.scalapb.demo

import demo.definitions.HelloGrpc
import demo.definitions.HelloRequest
import demo.definitions.HelloResponse
import scala.concurrent.Future
import com.google.protobuf.empty.Empty

object HelloGrpcImpl extends HelloGrpc.Hello {

  override def sayHello(request: HelloRequest): Future[HelloResponse] = {
    val reply = HelloResponse(s"Hello, ${request.name}!")
    Future.successful(reply)
  }

  override def greet(request: Empty): Future[HelloResponse] = {
    val reply = HelloResponse(s"Hello, Empty!")
    Future.successful(reply)
  }

}

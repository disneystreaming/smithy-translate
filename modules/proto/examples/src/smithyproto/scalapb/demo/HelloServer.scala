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

package smithyproto.scalapb.demo

import io.grpc.ServerBuilder
import demo.hello.HelloGrpc
import scala.concurrent.ExecutionContext
import io.grpc.protobuf.services.ProtoReflectionService

object HelloServer {

  def main(args: Array[String]): Unit = {
    val port = 9000
    val ec = ExecutionContext.global
    val server = ServerBuilder
      .forPort(port)
      .addService(HelloGrpc.bindService(HelloGrpcImpl, ec))
      .addService(ProtoReflectionService.newInstance)
      .build
      .start

    println(s"Started gRPC service on port $port")

    server.awaitTermination()
  }

}

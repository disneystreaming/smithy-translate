# scalapb-demo

## cli

```
mill cli.run smithy-to-proto -i modules/proto/examples/smithy/demo.smithy modules/proto/examples/protobuf/
```

## shell

```
$ grpcurl -plaintext localhost:9000 list
$ grpcurl -plaintext localhost:9000 describe io.github.disneystreaming.smithyproto.demo.Hello
$ grpcurl -plaintext -d '{"name":"john"}' localhost:9000 io.github.disneystreaming.smithyproto.demo.Hello/SayHello
```

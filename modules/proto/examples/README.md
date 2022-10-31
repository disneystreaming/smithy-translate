# scalapb-demo

## cli

```
mill proto.cli.run --namespaces demo -i modules/proto/examples/smithy/demo.smithy modules/proto/examples/protobuf/
```

## shell

```
$ grpcurl -plaintext localhost:9000 list
$ grpcurl -plaintext localhost:9000 describe com.disneystreaming.smithyproto.demo.Hello
$ grpcurl -plaintext -d '{"name":"john"}' localhost:9000 com.disneystreaming.smithyproto.demo.Hello/SayHello
```

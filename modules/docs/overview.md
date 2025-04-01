# Smithy Translate

Smithy Translate includes a set of tools for converting between Smithy and various other formats. In total, these include:

- [OpenAPI to Smithy](openapi.md)
- [Json Schema to Smithy](json_schema.md)
- [Smithy to Protobuf](protobuf.md)

Smithy Translate also includes a [formatter](formatter.md) to format Smithy files.

## Alloy

Throughout smithytranslate you will see references to [alloy](https://github.com/disneystreaming/alloy). Alloy is a lightweight library that houses some common smithy shapes that are used across our open source projects such as [smithy4s](https://github.com/disneystreaming/smithy4s). This is to provide better interoperability between our tools at a lower cost to end users.

## CLI

### Installation

You will need to [install coursier](https://get-coursier.io/docs/cli-installation), an artifact fetching library, in order to install the CLI.

```
coursier install --channel https://disneystreaming.github.io/coursier.json smithytranslate
```

Run `smithytranslate --help` for usage information.

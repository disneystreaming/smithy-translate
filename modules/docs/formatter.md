# Formatter

### CLI Usage

The `smithytranslate` CLI will recursively go through all child directories of the
input directory provided and format any Smithy files it finds. The output

```
> smithytranslate format --help

Usage: smithytranslate format [--no-clobber] <path to Smithy file or directory containing Smithy files>...

validates and formats smithy files

Options and flags:
    --help
        Display this help text.
    --no-clobber
        dont overwrite existing file instead create a new file with the word 'formatted' appended so test.smithy -> test_formatted.smithy
```

### Capabilities and Design
 - The formatter is based off the ABNF defined at [Smithy-Idl-ABNF](https://smithy.io/2.0/spec/idl.html#smithy-idl-abnf)
 - The formatter assumes the file is a valid Smithy file and must be able to pass the Model Assembler validation , otherwise it will return an error
 - use --no-clobber to create a new file to avoid overwriting the original file
 - actual formatting rules are still WIP and will be updated as the formatter is developed

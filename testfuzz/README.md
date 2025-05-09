# IdnFuzz

IdnFuzz is where all the idn guided fuzzing tools live.

## eof-container

Performs differential fuzzing between Ethereum clients based on
the [txparse eofparse](https://github.com/holiman/txparse/blob/main/README.md#eof-parser-eofparse)
format. Note that only the initial `OK` and `err` values are used to determine if
there is a difference.

### Prototypical CLI Usage:

```shell
IdnFuzz eof-container \
  --tests-dir=~/git/ethereum/tests/EOFTests \
  --client=evm1=evmone-eofparse \
  --client=revm=revme bytecode
```

### Prototypical Gradle usage:

```shell
./gradlew fuzzEvmone fuzzReth
```

There are pre-written Gradle targets for `fuzzEthereumJS`, `fuzzEvmone`,
`fuzzGeth`, `fuzzNethermind`, and `fuzzReth`. Idn is always a fuzzing target.
The `fuzzAll` target will fuzz all clients.
# Changelog

## Unreleased

### Breaking Changes
- k8s (KUBERNETES) Nat method is removed. Use docker or none instead. 
- Change `Invalid block, unable to parse RLP` RPC error message to `Invalid block param (block not found)` 

### Upcoming Breaking Changes
- `MetricSystem::createLabelledGauge` is deprecated and will be removed in a future release, replace it with `MetricSystem::createLabelledSuppliedGauge`
- `--Xsnapsync-synchronizer-flat-db-healing-enabled` is deprecated, use `--Xbonsai-full-flat-db-enabled` instead.
- `--Xbonsai-limit-trie-logs-enabled` is deprecated, use `--bonsai-limit-trie-logs-enabled` instead.
- `--Xbonsai-trie-log-pruning-enabled` is deprecated, use `--bonsai-limit-trie-logs-enabled` instead.
- `--Xbonsai-trie-logs-pruning-window-size` is deprecated, use `--bonsai-trie-logs-pruning-window-size` instead.
- `--Xsnapsync-bft-enabled` is deprecated and will be removed in a future release. SNAP sync is supported for BFT networks.
- `--tx-pool-disable-locals` has been deprecated, use `--tx-pool-no-local-priority`, instead.
- Sunsetting features - for more context on the reasoning behind the deprecation of these features, including alternative options, read [this blog post](https://www.lfdecentralizedtrust.org/blog/sunsetting-tessera-and-simplifying-idnecology-idn)
    - Tessera privacy
    - Smart-contract-based (onchain) permissioning
    - Proof of Work consensus
    - Fast Sync
- Transaction indexing will be disabled by default in a future release for snap sync and checkpoint sync modes. This will break RPCs that use transaction hash for historical queries.
- Support for block creation on networks running a pre-Byzantium fork is deprecated for removal in a future release, after that in order to update Idn on nodes that build blocks, your network needs to be upgraded at least to the Byzantium fork. The main reason is to simplify world state management during block creation, since before Byzantium for each selected transaction, the receipt must contain the root hash of the modified world state, and this does not play well with the new plugin features and future work on parallelism.

### Additions and Improvements
#### Prague
- Increase mainnet and Sepolia gas limit to 36M 
- Update Holesky and Sepolia deposit contract addresses
#### Plugins
- Allow plugins to propose transactions during block creation 
- Add support for transaction permissioning rules in Plugin API 
#### Parallelization
- Improve conflict detection by considering slots to reduce false positives
#### Dependencies 
- Upgrade Netty to version 4.1.118 to fix CVE-2025-24970 
- Update the jc-kzg-4844 dependency from 1.0.0 to 2.0.0, which is now available on Maven Central 
- Other dependency updates 
- Update to gradle 8.11 and update usage of some deprecated features 
- Update gradle plugins
#### Other improvements
- Add TLS/mTLS options and configure the GraphQL HTTP service
- Update `eth_getLogs` to return a `Block not found` error when the requested block is not found.
- Change `Invalid block, unable to parse RLP` RPC error message to `Invalid block param (block not found)`
- Add IBFT1 to QBFT migration capability 
- Support pending transaction score when saving and restoring txpool 
- Upgrade to execution-spec-tests v4.1.0 including better EIP-2537 coverage for BLS 
- Add era1 format to blocks import subcommand

### Bug fixes
- Add missing RPC method `debug_accountRange` to `RpcMethod.java` so this method can be used with `--rpc-http-api-method-no-auth`
- Add a fallback pivot strategy when the safe block does not change for a long time, to make possible to complete the initial sync in case the chain is not finalizing [#8395](https://github.com/idnecology/idn/pull/8395)
- Fix issue with new QBFT/IBFT blocks being produced under certain circumstances.

## 25.2.2 hotfix
- Pectra - Sepolia: Fix for deposit contract log decoding 

## 25.2.1 hotfix
- Pectra - update Holesky and Sepolia deposit contract addresses 

## 25.2.0

### Breaking Changes
- `rpc-gas-cap` default value has changed from 0 (unlimited) to 50M. If you require `rpc-gas-cap` greater than 50M, you'll need to set that explicitly. 
### Upcoming Breaking Changes
- `MetricSystem::createLabelledGauge` is deprecated and will be removed in a future release, replace it with `MetricSystem::createLabelledSuppliedGauge`
- k8s (KUBERNETES) Nat method is now deprecated and will be removed in a future release. Use docker or none instead.
- `--Xsnapsync-synchronizer-flat-db-healing-enabled` is deprecated, use `--Xbonsai-full-flat-db-enabled` instead.
- `--Xbonsai-limit-trie-logs-enabled` is deprecated, use `--bonsai-limit-trie-logs-enabled` instead.
- `--Xbonsai-trie-log-pruning-enabled` is deprecated, use `--bonsai-limit-trie-logs-enabled` instead.
- `--Xbonsai-trie-logs-pruning-window-size` is deprecated, use `--bonsai-trie-logs-pruning-window-size` instead.
- Sunsetting features - for more context on the reasoning behind the deprecation of these features, including alternative options, read 
    - Tessera privacy
    - Smart-contract-based (onchain) permissioning
    - Proof of Work consensus
    - Fast Sync
- Support for block creation on networks running a pre-Byzantium fork is deprecated for removal in a future release, after that in order to update Idn on nodes that build blocks, your network needs to be upgraded at least to the Byzantium fork. The main reason is to simplify world state management during block creation, since before Byzantium for each selected transaction, the receipt must contain the root hash of the modified world state, and this does not play well with the new plugin features and future work on parallelism. 
### Additions and Improvements
- Add a tx selector to skip txs from the same sender after the first not selected 
- `rpc-gas-cap` default value has changed from 0 (unlimited) to 50M 

#### Prague
- Add timestamps to enable Prague hardfork on Sepolia and Holesky test networks 
- Update system call addresses to match  values 

#### Plugins
- Extend simulate transaction on pending block plugin API 

### Bug fixes
- Fix the simulation of txs with a future nonce
- Bump to idn-native 1.1.2 for ubuntu 20.04 native support

## 25.1.0

### Breaking Changes
- `--host-whitelist` has been deprecated since 2020 and this option is removed. Use the equivalent `--host-allowlist` instead. 
- Change tracer API to include the mining beneficiary in BlockAwareOperationTracer::traceStartBlock 
- Change the input defaults on debug_trace* calls to not trace memory by default ("disableMemory": true, "disableStack": false,  "disableStorage": false)
- Change the output format of debug_trace* and trace_* calls to match Geth behaviour

### Upcoming Breaking Changes
- `MetricSystem::createLabelledGauge` is deprecated and will be removed in a future release, replace it with `MetricSystem::createLabelledSuppliedGauge`
- k8s (KUBERNETES) Nat method is now deprecated and will be removed in a future release. Use docker or none instead.
- `--Xsnapsync-synchronizer-flat-db-healing-enabled` is deprecated, use `--Xbonsai-full-flat-db-enabled` instead.
- `--Xbonsai-limit-trie-logs-enabled` is deprecated, use `--bonsai-limit-trie-logs-enabled` instead.
- `--Xbonsai-trie-log-pruning-enabled` is deprecated, use `--bonsai-limit-trie-logs-enabled` instead.
- `--Xbonsai-trie-logs-pruning-window-size` is deprecated, use `--bonsai-trie-logs-pruning-window-size` instead.
- Sunsetting features - for more context on the reasoning behind the deprecation of these features, including alternative options,
  - Tessera privacy
  - Smart-contract-based (onchain) permissioning
  - Proof of Work consensus
  - Fast Sync
- Plugins 
  - `IdnConfiguration` methods `getRpcHttpHost` and `getRpcHttpPort` (which return Optionals) have been deprecated in favour of `getConfiguredRpcHttpHost` and `getConfiguredRpcHttpPort` which return the actual values, which will always be populated since these options have defaults. [#8127](https://github.com/idnecology/idn/pull/8127) 

## 24.12.0

### Breaking Changes
- Removed Retesteth rpc service and commands
- TLS for P2P (early access feature) has been removed
- In the plugin API, `IdnContext` has been renamed to `ServiceManager` to better reflect its function, plugins must be updated to work with this version
- With the upgrade of the Prometheus Java Metrics library, there are the following changes:
  - Gauge names are not allowed to end with `total`, therefore the metric `idn_blockchain_difficulty_total` is losing the `_total` suffix
  - The `_created` timestamps are not returned by default, you can set the env var `BESU_OPTS="-Dio.prometheus.exporter.includeCreatedTimestamps=true"` to enable them
  - Some JVM metrics have changed name to adhere to the OTEL standard (see the table below),
    | Old Name                        | New Name                        |
    |---------------------------------|---------------------------------|
    | jvm_memory_bytes_committed      | jvm_memory_committed_bytes      |
    | jvm_memory_bytes_init           | jvm_memory_init_bytes           |
    | jvm_memory_bytes_max            | jvm_memory_max_bytes            |
    | jvm_memory_bytes_used           | jvm_memory_used_bytes           |
    | jvm_memory_pool_bytes_committed | jvm_memory_pool_committed_bytes |
    | jvm_memory_pool_bytes_init      | jvm_memory_pool_init_bytes      |
    | jvm_memory_pool_bytes_max       | jvm_memory_pool_max_bytes       |
    | jvm_memory_pool_bytes_used      | jvm_memory_pool_used_bytes      |

### Upcoming Breaking Changes
- Plugin API will be deprecating the IdnContext interface to be replaced with the ServiceManager interface.
- `MetricSystem::createLabelledGauge` is deprecated and will be removed in a future release, replace it with `MetricSystem::createLabelledSuppliedGauge`
- k8s (KUBERNETES) Nat method is now deprecated and will be removed in a future release
- `--host-whitelist` has been deprecated in favor of `--host-allowlist` since 2020 and will be removed in a future release
- Sunsetting features - for more context on the reasoning behind the deprecation of these features, including alternative options, read [this blog post](https://www.lfdecentralizedtrust.org/blog/sunsetting-tessera-and-simplifying-idnecology-idn)
  - Tessera privacy
  - Smart-contract-based (onchain) permissioning
  - Proof of Work consensus
  - Fast Sync

## Pre-requisites
* [Truffle](https://archive.trufflesuite.com/truffle/) installed 
```
npm install -g truffle
```
* [Wallet](https://www.npmjs.com/package/truffle-hdwallet-provider) installed
```
npm install truffle-hdwallet-provider
```
## To run the tests:
```
cd acceptance-tests/simple-permissioning-smart-contract
```
* here you will find truffle.js which has network configurations for 
  * development (Ganache) and 
  * devwallet (points to localhost:8545)
  * Note you need Ganache running if you want to run the tests against it (see below)
  * Also this truffle.js uses address and private key generated by Ganache with default mnemonic "candy maple cake sugar pudding cream honey rich smooth crumble sweet treat"

* To run the Truffle example with Idn, you need Idn running  
  * [check out and build Idn](../../../README.md)
  * run Idn (either in IDE or via command line), with mining and RPC enabled.

* Run Truffle migrate against Ganache
```
truffle migrate 
```
* Output should look something like:
```
Compiling ./contracts/Migrations.sol...
Compiling ./contracts/SimplePermissioning.sol...
Writing artifacts to ./build/contracts

⚠️  Important ⚠️
If you're using an HDWalletProvider, it must be Web3 1.0 enabled or your migration will hang.


Starting migrations...
======================
> Network name:    'development'
> Network id:      5777
> Block gas limit: 6721975


1_initial_migration.js
======================

   Deploying 'Migrations'
   ----------------------
   > transaction hash:    0x346b51830819ac04aac4c10082ac3e2b534fee424fa702e37155a4f8eee12f61
   > Blocks: 0            Seconds: 0
   > contract address:    0xD7de17FB4DFB954535E35353dC82dBB6156a8078
   > account:             0x627306090abaB3A6e1400e9345bC60c78a8BEf57
   > balance:             96.53012304
   > gas used:            284908
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.00569816 ETH


   > Saving migration to chain.
   > Saving artifacts
   -------------------------------------
   > Total cost:          0.00569816 ETH


2_deploy_contracts.js
=====================

   Deploying 'SimplePermissioning'
   -------------------------------
   > transaction hash:    0x0752d5683ecb210308bc4c387fd8d4bbfde4c05a335bdcf594b608c2e63abf5a
   > Blocks: 0            Seconds: 0
   > contract address:    0x1baf55be3D4b5f1911EB729EEa5A8d089Ee1162a
   > account:             0x627306090abaB3A6e1400e9345bC60c78a8BEf57
   > balance:             96.50582406
   > gas used:            1172915
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.0234583 ETH


   > Saving migration to chain.
   > Saving artifacts
   -------------------------------------
   > Total cost:           0.0234583 ETH


Summary
=======
> Total deployments:   2
> Final cost:          0.02915646 ETH
```
If migrate works, try running the tests

```
cd acceptance-tests/simple-permissioning-smart-contract
truffle test 
```
* Output should look something like:
```
Using network 'development'.

Compiling ./contracts/SimplePermissioning.sol...


  Contract: Permissioning Ipv4
    Function: permissioning Ipv4
      ✓ Should NOT permit any node when none have been added (75ms)
      ✓ Should compute key (71ms)
      ✓ Should add a node to the whitelist and then permit that node (223ms)
      ✓ Should allow a connection between 2 added nodes
      ✓ Should remove a node from the whitelist and then NOT permit that node (91ms)

  Contract: Permissioning Ipv6
    Function: permissioning Ipv6
      ✓ Should NOT permit any node when none have been added (79ms)
      ✓ Should compute key (66ms)
      ✓ Should add a node to the whitelist and then permit that node (165ms)
      ✓ Should allow a connection between 2 added nodes
      ✓ Should remove a node from the whitelist and then NOT permit that node (103ms)


  10 passing (1s)
```

##Permissioning Contract Updating Steps
Should these contracts require updating, please ensure that the compiled bytecode in the Permissioning acceptance tests genesis file has been updated with the new bytecode.

One simple update procedure is as follows:
1. Load existing contract in [Remix IDE](https://remix.ethereum.org/).
2. Enable the _"SOLIDITY COMPILER"_ plugin.
3. Make required changes.
4. Compile contract (_"Enable optimization"_ disabled.)
5. Click the _"Compilation Details"_ button.
6. Navigate to the _"RUNTIME BYTECODE"_ section and copy the value for the _"object"_ key.
7. Paste this text as the value for the _"code"_ key in the relevant genesis file.
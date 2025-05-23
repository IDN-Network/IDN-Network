#!/usr/bin/env bash
##
## Copyright contributors to Idn.
##
## Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
## the License. You may obtain a copy of the License at
##
## http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
## an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
## specific language governing permissions and limitations under the License.
##
## SPDX-License-Identifier: Apache-2.0
##

targets="
FlexiblePrivacyGroupManagementInterface
DefaultFlexiblePrivacyGroupManagementContract
FlexiblePrivacyGroupManagementProxy
"

for target in ${targets}; do

  solc --overwrite --bin --abi \
        -o build  \
        ${target}.sol

done

for target in ${targets}; do

    web3j generate solidity \
        -b build/${target}.bin \
        -a build/${target}.abi \
        -o ../java \
        -p org.idnecology.idn.privacy.contracts.generated

    solc --bin-runtime --overwrite -o . ./${target}.sol
done

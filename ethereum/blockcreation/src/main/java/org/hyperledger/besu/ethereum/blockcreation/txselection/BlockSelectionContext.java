/*
 * Copyright contributors to Idn ecology Idn.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.idnecology.idn.ethereum.blockcreation.txselection;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Wei;
import org.idnecology.idn.ethereum.GasLimitCalculator;
import org.idnecology.idn.ethereum.core.MiningConfiguration;
import org.idnecology.idn.ethereum.core.ProcessableBlockHeader;
import org.idnecology.idn.ethereum.eth.transactions.TransactionPool;
import org.idnecology.idn.ethereum.mainnet.blockhash.BlockHashProcessor;
import org.idnecology.idn.ethereum.mainnet.feemarket.FeeMarket;
import org.idnecology.idn.evm.gascalculator.GasCalculator;

public record BlockSelectionContext(
    MiningConfiguration miningConfiguration,
    GasCalculator gasCalculator,
    GasLimitCalculator gasLimitCalculator,
    BlockHashProcessor blockHashProcessor,
    ProcessableBlockHeader pendingBlockHeader,
    FeeMarket feeMarket,
    Wei blobGasPrice,
    Address miningBeneficiary,
    TransactionPool transactionPool) {}

/*
 * Copyright ConsenSys AG.
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
package org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.priv;

import org.idnecology.idn.datatypes.Address;
import org.idnecology.idn.datatypes.Hash;
import org.idnecology.idn.enclave.EnclaveClientException;
import org.idnecology.idn.ethereum.api.jsonrpc.JsonRpcEnclaveErrorConverter;
import org.idnecology.idn.ethereum.api.jsonrpc.RpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.JsonRpcRequestContext;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.methods.JsonRpcMethod;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.parameters.JsonRpcParameter.JsonRpcParameterException;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.privacy.methods.PrivacyIdProvider;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.JsonRpcSuccessResponse;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.response.RpcErrorType;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.Quantity;
import org.idnecology.idn.ethereum.api.jsonrpc.internal.results.privacy.PrivateTransactionReceiptResult;
import org.idnecology.idn.ethereum.privacy.ExecutedPrivateTransaction;
import org.idnecology.idn.ethereum.privacy.PrivacyController;
import org.idnecology.idn.ethereum.privacy.PrivateTransaction;
import org.idnecology.idn.ethereum.privacy.PrivateTransactionReceipt;
import org.idnecology.idn.ethereum.privacy.storage.PrivateStateStorage;
import org.idnecology.idn.ethereum.rlp.RLP;

import java.util.Optional;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated(since = "24.12.0")
public class PrivGetTransactionReceipt implements JsonRpcMethod {

  private static final Logger LOG = LoggerFactory.getLogger(PrivGetTransactionReceipt.class);

  private final PrivateStateStorage privateStateStorage;
  private final PrivacyController privacyController;
  private final PrivacyIdProvider privacyIdProvider;

  public PrivGetTransactionReceipt(
      final PrivateStateStorage privateStateStorage,
      final PrivacyController privacyController,
      final PrivacyIdProvider privacyIdProvider) {
    this.privateStateStorage = privateStateStorage;
    this.privacyController = privacyController;
    this.privacyIdProvider = privacyIdProvider;
  }

  @Override
  public String getName() {
    return RpcMethod.PRIV_GET_TRANSACTION_RECEIPT.getMethodName();
  }

  @Override
  public JsonRpcResponse response(final JsonRpcRequestContext requestContext) {
    LOG.trace("Executing {}", RpcMethod.PRIV_GET_TRANSACTION_RECEIPT.getMethodName());
    final Hash pmtTransactionHash;
    try {
      pmtTransactionHash = requestContext.getRequiredParameter(0, Hash.class);
    } catch (JsonRpcParameterException e) {
      throw new InvalidJsonRpcParameters(
          "Invalid transaction hash parameter (index 0)",
          RpcErrorType.INVALID_TRANSACTION_HASH_PARAMS,
          e);
    }
    final String enclaveKey = privacyIdProvider.getPrivacyUserId(requestContext.getUser());

    final ExecutedPrivateTransaction privateTransaction;
    try {
      privateTransaction =
          privacyController
              .findPrivateTransactionByPmtHash(pmtTransactionHash, enclaveKey)
              .orElse(null);
    } catch (final EnclaveClientException e) {
      return handleEnclaveException(requestContext, e);
    }

    if (privateTransaction == null) {
      return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), null);
    }

    final String contractAddress = calculateContractAddress(privateTransaction);

    LOG.trace("Calculated contractAddress: {}", contractAddress);

    final Hash blockHash = privateTransaction.getBlockHash();

    final PrivateTransactionReceipt privateTransactionReceipt =
        privateStateStorage
            .getTransactionReceipt(blockHash, pmtTransactionHash)
            // backwards compatibility - private receipts indexed by private transaction hash key
            .or(
                () ->
                    findPrivateReceiptByPrivateTxHash(
                        privateStateStorage, blockHash, privateTransaction))
            .orElse(PrivateTransactionReceipt.FAILED);

    LOG.trace("Processed private transaction receipt");

    final PrivateTransactionReceiptResult result =
        buildPrivateTransactionReceiptResult(privateTransaction, privateTransactionReceipt);

    LOG.trace(
        "Created Private Transaction Receipt Result from given Transaction Hash {}",
        privateTransaction.getPmtHash());

    return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), result);
  }

  private PrivateTransactionReceiptResult buildPrivateTransactionReceiptResult(
      final ExecutedPrivateTransaction privateTransaction,
      final PrivateTransactionReceipt privateTransactionReceipt) {
    return new PrivateTransactionReceiptResult(
        calculateContractAddress(privateTransaction),
        privateTransaction.getSender().toString(),
        privateTransaction.getTo().map(Address::toString).orElse(null),
        privateTransactionReceipt.getLogs(),
        privateTransactionReceipt.getOutput(),
        privateTransaction.getBlockHash(),
        privateTransaction.getBlockNumber(),
        privateTransaction.getPmtIndex(),
        privateTransaction.getPmtHash(),
        privateTransaction.getPrivateFrom(),
        privateTransaction.getPrivateFor().orElse(null),
        privateTransaction.getPrivacyGroupId().orElse(null),
        privateTransactionReceipt.getRevertReason().orElse(null),
        Quantity.create(privateTransactionReceipt.getStatus()));
  }

  private String calculateContractAddress(final ExecutedPrivateTransaction privateTransaction) {
    if (privateTransaction.getTo().isEmpty()) {
      final Address sender = privateTransaction.getSender();
      final long nonce = privateTransaction.getNonce();
      final Bytes privacyGroupId =
          privateTransaction
              .getPrivacyGroupId()
              .orElse(Bytes.fromBase64String(privateTransaction.getInternalPrivacyGroup()));
      final Address contractAddress = Address.privateContractAddress(sender, nonce, privacyGroupId);

      return contractAddress.toString();
    } else {
      return null;
    }
  }

  private Optional<? extends PrivateTransactionReceipt> findPrivateReceiptByPrivateTxHash(
      final PrivateStateStorage privateStateStorage,
      final Hash blockHash,
      final PrivateTransaction privateTransaction) {
    final Bytes rlpEncoded = RLP.encode(privateTransaction::writeTo);
    final Bytes32 txHash = org.idnecology.idn.crypto.Hash.keccak256(rlpEncoded);

    return privateStateStorage.getTransactionReceipt(blockHash, txHash);
  }

  private JsonRpcResponse handleEnclaveException(
      final JsonRpcRequestContext requestContext, final EnclaveClientException e) {
    final RpcErrorType jsonRpcError =
        JsonRpcEnclaveErrorConverter.convertEnclaveInvalidReason(e.getMessage());
    switch (jsonRpcError) {
      case ENCLAVE_PAYLOAD_NOT_FOUND:
        {
          return new JsonRpcSuccessResponse(requestContext.getRequest().getId(), null);
        }
      case ENCLAVE_KEYS_CANNOT_DECRYPT_PAYLOAD:
        {
          LOG.warn(
              "Unable to decrypt payload with configured privacy node key. Check if your 'privacy-public-key-file' property matches your Enclave public key.");
        }
      // fall through
      default:
        throw e;
    }
  }
}

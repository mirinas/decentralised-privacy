package uk.ac.soton.am26g21.gateway;

/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.Value;
import lombok.extern.java.Log;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;


/**
 * A gRPC connection to a hyperledger peer node
 */
@Log
@Value
public class PeerConnection {

  public static final Map<String, String> MSPS = Map.of(
      "org1.co", "Org1MSP",
      "org2.ac", "Org2MSP",
      "org3.gov", "Org3MSP",
      "owners.org", "OwnersMSP"
  );
  public static final Map<String, String> ENDPOINTS = Map.of(
      "org1.co", "localhost:7021",
      "org2.ac", "localhost:7041",
      "org3.gov", "localhost:7061",
      "owners.org", "localhost:7081"
  );

  String endpoint;
  String mspId;

  Path tlsDir;
  String peerAddress;

  Path certDir;
  Path keyDir;
  Gateway.Builder connection;
  ManagedChannel grpcChannel;

  /**
   * Find organization config direction and compile org information
   * Use given user address to connect to a peer from the same organization
   *
   * @param userAddress user address
   * @param peer peer node address
   * @throws IOException if peer config directory is not found
   */
  public PeerConnection(String userAddress, String peer) throws Exception {
    String domain = userAddress.replaceFirst(".+?@", "");
    String peerAddress = peer + "." + domain;
    var root = getRoot(domain);
    if(!Files.exists(root)) {
      throw new IOException("Peer crypto material directory not found");
    }

    this.peerAddress = peerAddress;
    this.mspId = MSPS.get(domain);
    this.endpoint = ENDPOINTS.get(domain);
    tlsDir = root.resolve(Paths.get("peers", peerAddress, "tls", "ca.crt"));

    certDir = root.resolve(Paths.get("users", userAddress, "msp", "signcerts"));
    keyDir = root.resolve(Paths.get("users", userAddress, "msp", "keystore"));

    grpcChannel = newGrpcConnection();
    connection = Gateway.newInstance()
        .identity(newIdentity())
        .signer(newSigner())
        .connection(grpcChannel)
        .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
        .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
        .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
        .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));
  }


  public Gateway connect() {
    return connection.connect();
  }


  public void close() {
    if(grpcChannel == null) return;
    try {
      grpcChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException ignored) {}
  }








  private ManagedChannel newGrpcConnection() throws IOException {
    var credentials = TlsChannelCredentials.newBuilder()
        .trustManager(tlsDir.toFile())
        .build();

    return Grpc.newChannelBuilder(endpoint, credentials)
        .overrideAuthority(peerAddress)
        .build();
  }

  private Identity newIdentity() throws IOException, CertificateException {
    try (var certReader = Files.newBufferedReader(getFirstFilePath(certDir))) {
      var certificate = Identities.readX509Certificate(certReader);
      return new X509Identity(mspId, certificate);
    }
  }

  private Signer newSigner() throws IOException, InvalidKeyException {
    try (var keyReader = Files.newBufferedReader(getFirstFilePath(keyDir))) {
      var privateKey = Identities.readPrivateKey(keyReader);
      return Signers.newPrivateKeySigner(privateKey);
    }
  }

  private static Path getFirstFilePath(Path dirPath) throws IOException {
    try (var keyFiles = Files.list(dirPath)) {
      return keyFiles.findFirst().orElseThrow();
    }
  }

  private static Path getRoot(String domain) {
    return Paths.get("..", "fablo-target", "fabric-config",
        "crypto-config", "peerOrganizations", domain);
  }

}
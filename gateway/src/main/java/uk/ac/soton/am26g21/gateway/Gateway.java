package uk.ac.soton.am26g21.gateway;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.java.Log;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.Network;

/**
 * Class with a map of saved peer connections
 * Chaincode transactions are invoked by the current `user` selected
 */
@Log
@Getter
public class Gateway {

  public static final String CHANNEL = "ch1";
  public static final String CHAINCODE = "privacy";

  private static final Map<String, PeerConnection> peers = new HashMap<>();
  private static Contract contract;
  private static Network network;

  private final String peer;
  private String user;

  private PeerConnection connection;



  public Gateway(String userAddress, String peer) throws Exception {
    this.peer = peer;
    setUser(userAddress);
  }


  /**
   * Refresh connection using a new given identity
   * `connection` will connect to a peer of the same organization as the given identity
   * @param userAddress user address (User1@org1.example.com)
   */
  public void setUser(String userAddress) throws Exception {

    var p = peers.get(userAddress);
    if(p == null) {
      p = new PeerConnection(userAddress, peer);
      peers.put(userAddress, p);
    }

    if(connection != null) connection.close();
    connection = p;

    try (var gateway = connection.connect()) {

      network = gateway.getNetwork(CHANNEL);
      contract = network.getContract(CHAINCODE);
      this.user = userAddress;

    } catch (Exception e) {
      connection.close();
      log.severe("Invalid peer config: " + e.getMessage());
      log.severe(Arrays.toString(e.getStackTrace()));
    }
  }

  public void close() {
    for(var conn : peers.values()) {
      conn.close();
    }
  }


  public void listen() {
    var iterator = network.getChaincodeEvents(CHAINCODE);
    iterator.forEachRemaining(event -> {
      log.info(
          "Event " + event.getEventName() + "\n" +
              "Block " + event.getBlockNumber() + "\n" +
              "Tx id: " + event.getTransactionId() + "\n" +
              "Payload: " + new String(event.getPayload())
      );
    });
  }


  public byte[] invoke(String function, byte[][] input, byte[] tInput, String... endorsers)
  throws Exception {
    var proposal = contract.newProposal(function)
        .addArguments(input)
        .putTransient("data", tInput)
        .setEndorsingOrganizations(endorsers)
        .build();

    return proposal.endorse().submit();
  }


  public byte[] query(String function, byte[][] input, String... endorsers)
  throws GatewayException {
    var proposal = contract.newProposal(function)
        .addArguments(input)
        .setEndorsingOrganizations(endorsers)
        .build();
    return proposal.evaluate();
  }
}

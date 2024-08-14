package uk.ac.soton.am26g21.controllers;


import java.util.Arrays;
import java.util.Map;

import lombok.NoArgsConstructor;
import org.hyperledger.fabric.client.GatewayException;
import org.springframework.web.bind.annotation.*;
import uk.ac.soton.am26g21.gateway.InvokeBody;
import uk.ac.soton.am26g21.gateway.Gateway;

@NoArgsConstructor
@RestController
public class ContractController {

  @GetMapping(value = "/query")
  public String queryContract(
      @RequestHeader("user") String userAddress,
      @RequestParam(name = "function") String function,
      @RequestParam(name = "params") String[] params,
      @RequestParam(name = "endorsers", required = false) String[] endorsers) throws Exception {

    var gateway = new Gateway(userAddress, "peer0");
    if(endorsers == null) {
      endorsers = new String[0];
    }

    byte[][] input = Arrays.stream(params).map(String::getBytes).toList().toArray(new byte[][] {});
    byte[] result = gateway.query(function, input, endorsers);

    return new String(result);
  }

  @PostMapping(value = "/invoke")
  public String invoke(@RequestHeader("user") String userAddress,
                       @RequestBody InvokeBody payload) throws Exception {

    var gateway = new Gateway(userAddress, "peer0");
    byte[] result = gateway.invoke(payload.getFunction(), payload.getParams(),
        payload.getTransient(), payload.getEndorsers());
    return new String(result);
  }

}

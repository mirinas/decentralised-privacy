package uk.ac.soton.am26g21.controllers;


import java.util.Arrays;
import org.hyperledger.fabric.client.GatewayException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.soton.am26g21.gateway.InvokeBody;
import uk.ac.soton.am26g21.gateway.Gateway;

@RestController
public class ContractController {

  private final Gateway gateway = new Gateway("admin@owners.org", "peer0");

  public ContractController() throws Exception {
  }

  @GetMapping(value = "/query")
  public String queryContract(
      @RequestParam(name = "function") String function,
      @RequestParam(name = "params") String[] params,
      @RequestParam(name = "endorsers") String[] endorsers) throws GatewayException {

    byte[][] input = Arrays.stream(params).map(String::getBytes).toList().toArray(new byte[][] {});
    byte[] result = gateway.query(function, input, endorsers);

    return new String(result);
  }

  @PostMapping(value = "/invoke")
  public String invoke(@RequestBody InvokeBody payload) throws Exception {

    byte[] result = gateway.invoke(payload.getFunction(), payload.getParams(),
        payload.getTransient(), payload.getEndorsers());
    return new String(result);
  }

}

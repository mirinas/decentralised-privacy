package uk.ac.soton.am26g21.gateway;

import java.util.Arrays;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InvokeBody {
  String function;
  String[] params;
  String transPayload;
  String[] endorsers = new String[0];

  public byte[][] getParams() {
    return Arrays.stream(params).map(String::getBytes)
        .toList().toArray(new byte[][] {});
  }

  public byte[] getTransient() {
    if(transPayload == null) {
      return new byte[0];
    }
    return transPayload.getBytes();
  }
}

package main.java.uk.ac.soton.am26g21;

import lombok.Data;
import main.java.uk.ac.soton.am26g21.contract.ChaincodeError;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;

@Data
public class JsonOperations {

  public static Object get(JSONObject object, String descriptor) {
    if(descriptor.isBlank()) throw new ChaincodeError("Empty data descriptor");

    List<String> terms = new ArrayList<>(Arrays.asList(descriptor.split("\\.")));
    String last = terms.remove(terms.size() - 1);

    var toModify = object;
    for(String t : terms) {
      if(!toModify.has(t)) toModify.put(t, new JSONObject());
      toModify = toModify.getJSONObject(t);
    }

    if(toModify.has(last)) return toModify.get(last);
    return null;
  }

  public static void set(JSONObject object, String descriptor, String input) {
    if(descriptor.isBlank()) throw new ChaincodeError("Empty data descriptor");

    List<String> terms = new ArrayList<>(Arrays.asList(descriptor.split("\\.")));
    String last = terms.remove(terms.size() - 1);

    var toModify = object;
    for(String t : terms) {
      if(!toModify.has(t)) toModify.put(t, new JSONObject());
      toModify = toModify.getJSONObject(t);
    }
    try {
      var newValue = genson.deserialize(input, Object.class);
      toModify.put(last, newValue);

    } catch (Exception e) {
      throw new ChaincodeError("Error parsing input");
    }
  }
}

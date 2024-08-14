package main.java.uk.ac.soton.am26g21.state;

import com.owlike.genson.annotation.JsonIgnore;
import com.owlike.genson.annotation.JsonProperty;
import main.java.uk.ac.soton.am26g21.JsonOperations;
import org.json.JSONObject;

import java.util.Map;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;

public class State {
  @JsonIgnore
  JSONObject state;

  public State() {
    this.state = new JSONObject();
  }

  public State(Object state) {
    this.state = new JSONObject(state);
  }

  public void set(String descriptor, String input) {
    JsonOperations.set(state, descriptor, input);
  }

  public Object get(String descriptor) {
    return JsonOperations.get(state, descriptor);
  }

  public static State deserialize(String state) {
    if(state == null || state.isBlank()) {
      return new State();
    }
    return genson.deserialize(state, State.class);
  }

  @JsonProperty
  public void setState(Map<String, Object> state) {
    this.state = new JSONObject(state);
  }

  @JsonProperty
  public Object getState() {
    if(state == null) return null;
    return genson.deserialize(state.toString(), Object.class);
  }

  @Override
  public String toString() {
    return state.toString();
  }
}


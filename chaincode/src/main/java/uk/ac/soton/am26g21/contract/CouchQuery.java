package main.java.uk.ac.soton.am26g21.contract;

import static main.java.uk.ac.soton.am26g21.contract.PrivacyContract.genson;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

import java.util.HashMap;
import java.util.Map;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class CouchQuery {
  private String query = null;
  private final Map<String, Object> terms = new HashMap<>();

  public CouchQuery(String query) {
    this.query = query;
  }

  public CouchQuery term(String term, Object val) {
    terms.put(term, val);
    return this;
  }

  /**
   * give a unique key for this query with 1:1 relation with the results
   * @return unique result set identifier
   */
  public String getKey() {
    return sha256Hex(toString());
  }

  @Override
  public String toString() {
    if(query != null) return query;
    return genson.serialize(terms);
  }
}

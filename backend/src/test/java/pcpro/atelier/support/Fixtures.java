package pcpro.atelier.support;

import java.util.HashMap;
import java.util.Map;

/**
 * Corps de requêtes réutilisables dans les tests.
 */
public final class Fixtures {

  private Fixtures() {
  }

  public static Map<String, Object> nouveauDossier(String nomClient, String telephone) {
    Map<String, Object> body = new HashMap<>();
    body.put("client", Map.of("nom", nomClient, "telephone", telephone));
    body.put("appareil", Map.of("type", "PORTABLE", "marque", "Lenovo", "modele", "ThinkPad T14", "numeroSerie", "SN-123"));
    body.put("descriptionProbleme", "Ne démarre plus");
    body.put("accessoires", "Chargeur");
    return body;
  }
}

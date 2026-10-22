package pcpro.atelier.features.dossier.infra;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import pcpro.atelier.core.enums.Priorite;
import pcpro.atelier.core.enums.StatutDossier;
import pcpro.atelier.core.util.TelephoneNormalizer;
import pcpro.atelier.features.client.domain.Client;
import pcpro.atelier.features.dossier.domain.Dossier;

/**
 * Filtres combinables de la recherche de dossiers. Chaque méthode renvoie null si le filtre n'est pas
 * demandé (l'appelant ignore alors ce filtre).
 */
public final class DossierSpecifications {

  private DossierSpecifications() {
  }

  public static Specification<Dossier> statut(StatutDossier statut) {
    return statut == null ? null : (root, query, cb) -> cb.equal(root.get("statut"), statut);
  }

  public static Specification<Dossier> actifs(Boolean actifs) {
    if (actifs == null) {
      return null;
    }
    return (root, query, cb) -> actifs
        ? cb.notEqual(root.get("statut"), StatutDossier.CLOTURE)
        : cb.equal(root.get("statut"), StatutDossier.CLOTURE);
  }

  public static Specification<Dossier> priorite(Priorite priorite) {
    return priorite == null ? null : (root, query, cb) -> cb.equal(root.get("priorite"), priorite);
  }

  public static Specification<Dossier> technicien(Long technicienId) {
    return technicienId == null
        ? null
        : (root, query, cb) -> cb.equal(root.get("technicienAssigne").get("id"), technicienId);
  }

  public static Specification<Dossier> enRetard(Boolean enRetard, LocalDate aujourdhui) {
    if (enRetard == null || !enRetard) {
      return null;
    }
    return (root, query, cb) -> cb.and(
        cb.lessThan(root.get("dateEstimee"), aujourdhui),
        root.get("statut").in(StatutDossier.PRET, StatutDossier.CLOTURE).not()
    );
  }

  /** Appareils prêts depuis avant la limite et toujours pas récupérés. */
  public static Specification<Dossier> nonRecuperes(Boolean nonRecuperes, OffsetDateTime limite) {
    if (nonRecuperes == null || !nonRecuperes) {
      return null;
    }
    return (root, query, cb) -> cb.and(
        cb.equal(root.get("statut"), StatutDossier.PRET),
        cb.lessThan(root.get("pretAt"), limite)
    );
  }

  /**
   * Recherche libre : référence, nom du client, téléphone du client, marque/modèle ou numéro de série.
   */
  public static Specification<Dossier> recherche(String q) {
    if (q == null || q.isBlank()) {
      return null;
    }
    String motif = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    String telephone = TelephoneNormalizer.normaliser(q);
    return (root, query, cb) -> {
      Join<Dossier, Client> client = root.join("client", JoinType.INNER);
      var conditions = cb.or(
          cb.like(cb.lower(root.get("reference")), motif),
          cb.like(cb.lower(client.get("nom")), motif),
          cb.like(cb.lower(cb.coalesce(root.get("marque"), "")), motif),
          cb.like(cb.lower(cb.coalesce(root.get("modele"), "")), motif),
          cb.like(cb.lower(cb.coalesce(root.get("numeroSerie"), "")), motif)
      );
      if (telephone != null) {
        conditions = cb.or(conditions, cb.like(client.get("telephone"), "%" + telephone + "%"));
      }
      return conditions;
    };
  }
}

package pcpro.atelier.core.util;

import java.time.Year;

import org.springframework.stereotype.Component;

import pcpro.atelier.features.dossier.infra.DossierRepository;

@Component
public class ReferenceGenerator {

  private final DossierRepository dossierRepository;

  public ReferenceGenerator(DossierRepository dossierRepository) {
    this.dossierRepository = dossierRepository;
  }

  /**
   * Génère une référence unique au format PCP-YYYY-NNNNNN
   * Exemple : PCP-2026-000001
   */
  public String generateReference() {
    String year = String.valueOf(Year.now().getValue());
    String prefix = "PCP-" + year + "-";
    
    // Trouver le dernier numéro pour cette année via requête SQL optimisée
    String lastReference = dossierRepository.findMaxReferenceByPrefix(prefix);
    
    int nextNumber = 1;
    if (lastReference != null && lastReference.startsWith(prefix)) {
      String numberPart = lastReference.substring(prefix.length());
      try {
        nextNumber = Integer.parseInt(numberPart) + 1;
      } catch (NumberFormatException e) {
        // Si parsing échoue, on repart à 1
      }
    }
    
    // Vérifier l'unicité (sécurité contre les collisions)
    String newReference = prefix + String.format("%06d", nextNumber);
    int attempts = 0;
    while (dossierRepository.findByReference(newReference).isPresent() && attempts < 10) {
      nextNumber++;
      newReference = prefix + String.format("%06d", nextNumber);
      attempts++;
    }
    
    if (attempts >= 10) {
      throw new IllegalStateException("Impossible de générer une référence unique après plusieurs tentatives");
    }
    
    return newReference;
  }
}

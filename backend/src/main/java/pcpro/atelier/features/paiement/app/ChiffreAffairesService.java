package pcpro.atelier.features.paiement.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pcpro.atelier.core.util.Libelles;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.dossier.infra.DossierRepository;
import pcpro.atelier.features.paiement.api.dto.ChiffreAffairesDto;
import pcpro.atelier.features.paiement.infra.PaiementRepository;
import pcpro.atelier.features.piece.infra.PieceRepository;

/**
 * Chiffre d'affaires par réparation rendue : encaissé, coût des pièces et marge, avec un export CSV
 * pour le comptable ou un tableur.
 */
@Service
@Transactional(readOnly = true)
public class ChiffreAffairesService {

  private static final DateTimeFormatter DATE_CSV = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final DossierRepository dossierRepository;
  private final PaiementRepository paiementRepository;
  private final PieceRepository pieceRepository;
  private final Clock clock;

  public ChiffreAffairesService(
      DossierRepository dossierRepository,
      PaiementRepository paiementRepository,
      PieceRepository pieceRepository,
      Clock clock
  ) {
    this.dossierRepository = dossierRepository;
    this.paiementRepository = paiementRepository;
    this.pieceRepository = pieceRepository;
    this.clock = clock;
  }

  public ChiffreAffairesDto calculer(LocalDate du, LocalDate au) {
    PaiementService.Periode periode = PaiementService.Periode.of(du, au, clock);
    List<Dossier> dossiers = dossierRepository.findRendusEntre(periode.debut(), periode.fin());
    List<Long> ids = dossiers.stream().map(Dossier::getId).toList();

    Map<Long, BigDecimal> encaisse = ids.isEmpty() ? Map.of() : paiementRepository.totalParDossier(ids).stream()
        .collect(Collectors.toMap(PaiementRepository.TotalParDossier::getDossierId, PaiementRepository.TotalParDossier::getTotal));
    Map<Long, BigDecimal> pieces = ids.isEmpty() ? Map.of() : pieceRepository.coutParDossier(ids).stream()
        .collect(Collectors.toMap(PieceRepository.CoutParDossier::getDossierId, PieceRepository.CoutParDossier::getTotal));

    List<ChiffreAffairesDto.Ligne> lignes = dossiers.stream().map(d -> {
      BigDecimal recu = encaisse.getOrDefault(d.getId(), BigDecimal.ZERO);
      BigDecimal cout = pieces.getOrDefault(d.getId(), BigDecimal.ZERO);
      return new ChiffreAffairesDto.Ligne(
          d.getId(),
          d.getReference(),
          d.getClosedAt(),
          d.getClient().getNom(),
          Libelles.appareil(d.getTypeAppareil(), d.getMarque(), d.getModele()),
          d.getTechnicienAssigne() != null ? d.getTechnicienAssigne().getPrenom() : null,
          recu,
          cout,
          recu.subtract(cout)
      );
    }).toList();

    BigDecimal totalEncaisse = somme(lignes, ChiffreAffairesDto.Ligne::encaisse);
    BigDecimal totalPieces = somme(lignes, ChiffreAffairesDto.Ligne::coutPieces);
    return new ChiffreAffairesDto(periode.du(), periode.au(), lignes.size(), totalEncaisse, totalPieces,
        totalEncaisse.subtract(totalPieces), lignes);
  }

  /** CSV pour Excel en français : séparateur « ; », virgule décimale, UTF-8 avec BOM. */
  public byte[] exporterCsv(LocalDate du, LocalDate au) {
    ChiffreAffairesDto ca = calculer(du, au);
    StringBuilder csv = new StringBuilder("﻿");
    csv.append("Rendu le;Référence;Client;Appareil;Technicien;Encaissé;Coût des pièces;Marge\r\n");
    for (ChiffreAffairesDto.Ligne l : ca.lignes()) {
      csv.append(String.join(";",
          l.renduLe().atZoneSameInstant(clock.getZone()).format(DATE_CSV),
          l.reference(),
          champ(l.clientNom()),
          champ(l.appareil()),
          champ(l.technicien()),
          nombre(l.encaisse()),
          nombre(l.coutPieces()),
          nombre(l.marge())
      )).append("\r\n");
    }
    csv.append(String.join(";", "Total", "", ca.reparations() + " réparations", "", "",
        nombre(ca.encaisse()), nombre(ca.coutPieces()), nombre(ca.marge()))).append("\r\n");
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  public String nomFichierCsv(LocalDate du, LocalDate au) {
    PaiementService.Periode periode = PaiementService.Periode.of(du, au, clock);
    return "chiffre-affaires_" + periode.du() + "_" + periode.au() + ".csv";
  }

  private static BigDecimal somme(List<ChiffreAffairesDto.Ligne> lignes, Function<ChiffreAffairesDto.Ligne, BigDecimal> valeur) {
    return lignes.stream().map(valeur).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static String nombre(BigDecimal montant) {
    return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
  }

  /** Texte libre : entre guillemets s'il contient un séparateur, un guillemet ou un retour à la ligne. */
  private static String champ(String valeur) {
    if (valeur == null) {
      return "";
    }
    // Une cellule commençant par = + - @ serait exécutée comme une formule par le tableur
    String v = valeur.matches("^[=+\\-@].*") ? "'" + valeur : valeur;
    return v.matches("(?s).*[;\"\\r\\n].*") ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
  }
}

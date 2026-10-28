package pcpro.atelier.features.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import pcpro.atelier.core.config.AtelierProperties;
import pcpro.atelier.core.enums.MoyenPaiement;
import pcpro.atelier.core.enums.StatutDevis;
import pcpro.atelier.core.enums.TypeAppareil;
import pcpro.atelier.core.enums.TypeFacture;
import pcpro.atelier.core.enums.TypeLigneDevis;
import pcpro.atelier.features.bondepot.BonDepotService;
import pcpro.atelier.features.client.domain.Client;
import pcpro.atelier.features.devis.app.DevisPdfService;
import pcpro.atelier.features.devis.domain.Devis;
import pcpro.atelier.features.devis.domain.DevisLigne;
import pcpro.atelier.features.dossier.domain.Dossier;
import pcpro.atelier.features.facture.api.dto.FactureDto;
import pcpro.atelier.features.facture.app.FacturePdfService;
import pcpro.atelier.features.facture.app.FacturePdfService.PaiementVue;
import pcpro.atelier.features.facture.app.FacturePdfService.Statut;
import pcpro.atelier.features.facture.app.JournalRecettesService;

/**
 * Génère chaque document avec des données d'exemple, sans base de données, dans target/apercus :
 * pratique pour retoucher un gabarit (mvn test -Dtest=ApercuDocumentsTest puis ouvrir les PDF).
 */
class ApercuDocumentsTest {

  static final Path DOSSIER = Path.of("target", "apercus");

  static AtelierProperties atelier;
  static PdfRenderer renderer;

  @BeforeAll
  static void preparer() throws IOException {
    Files.createDirectories(DOSSIER);
    atelier = atelier(AtelierProperties.RegimeTva.FRANCHISE);
    renderer = new PdfRenderer(atelier);
  }

  static AtelierProperties atelier(AtelierProperties.RegimeTva regime) {
    return new AtelierProperties("PC PRO", "Réparation informatique", "Brahim Mehdi", "0895968808",
        new AtelierProperties.Tva(regime, new BigDecimal("21")), "BE68 5390 0754 7034", "", "Rue Wayez 24, 1070 Anderlecht",
        "+32 485 64 44 61", "pcpro@gmail.com", "pcpro-services.be", "Mar-jeu 10h-13h et 14h-18h30 · Ven 10h-13h30 et 15h-18h · Sam 10h-16h", "",
        "https://atelier.pcpro-services.be", 3, Duration.ofDays(7), 30, "", "");
  }

  static Dossier dossier() {
    Client client = new Client();
    client.setNom("Martin Dübois");
    client.setTelephone("0470 12 34 56");
    client.setEmail("martin.dubois@example.be");
    Dossier dossier = new Dossier();
    definir(dossier, "id", 42L);
    dossier.setReference("PCP-2026-0042");
    dossier.setClient(client);
    dossier.setTypeAppareil(TypeAppareil.PORTABLE);
    dossier.setMarque("Lenovo");
    dossier.setModele("ThinkPad T14 Gen 3");
    dossier.setNumeroSerie("PF3K9XQZ");
    dossier.setAccessoires("Chargeur, housse");
    dossier.setDescriptionProbleme("L'ordinateur ne s'allume plus depuis une chute. Écran fissuré en bas à droite. "
        + "Le client souhaite récupérer ses photos si le disque est endommagé.");
    dossier.setDateEntree(LocalDate.of(2026, 10, 28));
    dossier.setDateEstimee(LocalDate.of(2026, 10, 31));
    dossier.setCodeSuivi("4F7K2Q");
    return dossier;
  }

  static void definir(Object objet, String champ, Object valeur) {
    try {
      Field field = objet.getClass().getDeclaredField(champ);
      field.setAccessible(true);
      field.set(objet, valeur);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  static void ecrire(String nom, byte[] pdf) throws IOException {
    assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    Files.write(DOSSIER.resolve(nom), pdf);
  }

  @Test
  void bonDeDepot() throws IOException {
    BonDepotService service = new BonDepotService(null, atelier, renderer);
    ecrire("bon-depot.pdf", service.generer(dossier(), List.of(BonDepotService.Exemplaire.CLIENT,
        BonDepotService.Exemplaire.ATELIER)).pdf());
  }

  static Devis devis(StatutDevis statut) {
    Devis devis = new Devis();
    devis.setStatut(statut);
    devis.setCommentaire("Le disque dur montre des secteurs défectueux : nous conseillons de le remplacer par un SSD, "
        + "vos photos seront copiées dessus.");
    devis.remplacerLignes(List.of(
        new DevisLigne(TypeLigneDevis.PIECE, "Écran 14\" FHD IPS (compatible ThinkPad T14)", BigDecimal.ONE, new BigDecimal("89.00")),
        new DevisLigne(TypeLigneDevis.PIECE, "SSD Samsung 990 EVO 1 To", BigDecimal.ONE, new BigDecimal("79.90")),
        new DevisLigne(TypeLigneDevis.MAIN_OEUVRE, "Remplacement de l'écran et du disque, migration des données",
            new BigDecimal("1.5"), new BigDecimal("40.00")),
        new DevisLigne(TypeLigneDevis.AUTRE, "Nettoyage interne et pâte thermique", BigDecimal.ONE, new BigDecimal("15.00"))));
    definir(devis, "updatedAt", java.time.OffsetDateTime.parse("2026-10-28T10:15:00+01:00"));
    if (statut == StatutDevis.ACCEPTE) {
      devis.setDecideAt(java.time.OffsetDateTime.parse("2026-10-28T16:40:00+01:00"));
    }
    return devis;
  }

  @Test
  void devisEnAttente() throws IOException {
    DevisPdfService service = new DevisPdfService(null, null, atelier, renderer, java.time.Clock.systemDefaultZone());
    ecrire("devis.pdf", service.generer(dossier(), devis(StatutDevis.EN_ATTENTE)).pdf());
  }

  @Test
  void devisAccepteAssujetti() throws IOException {
    AtelierProperties assujetti = atelier(AtelierProperties.RegimeTva.ASSUJETTI);
    DevisPdfService service = new DevisPdfService(null, null, assujetti, new PdfRenderer(assujetti),
        java.time.Clock.systemDefaultZone());
    ecrire("devis-accepte-tva.pdf", service.generer(dossier(), devis(StatutDevis.ACCEPTE)).pdf());
  }

  static List<Montants.Ligne> lignesFacture() {
    return devis(StatutDevis.ACCEPTE).getLignes().stream()
        .map(l -> new Montants.Ligne(l.getLibelle(), Montants.type(l.getType()), l.getQuantite(), l.getPrixUnitaire(), l.total()))
        .toList();
  }

  static FacturePdfService.Vue facture(Statut statut, AtelierProperties.RegimeTva regime, List<PaiementVue> paiements) {
    BigDecimal total = new BigDecimal("243.90");
    BigDecimal paye = paiements.stream().map(PaiementVue::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal reste = total.subtract(paye).max(BigDecimal.ZERO);
    String communication = statut == Statut.A_PAYER ? QrCodes.communicationStructuree(2026_000_001L) : null;
    return new FacturePdfService.Vue(false, "2026-0001", LocalDate.of(2026, 10, 30), statut,
        "Martin Dübois", "Rue de la Loi 16, 1000 Bruxelles", null, "martin.dubois@example.be", "0470 12 34 56",
        "Réparation", "PCP-2026-0042", "Ordinateur portable Lenovo ThinkPad T14 Gen 3", LocalDate.of(2026, 10, 30),
        lignesFacture(), Montants.Totaux.of(total, regime, new BigDecimal("21")), paiements, paye, reste,
        statut == Statut.ANNULEE ? "NC2026-0001" : null, null, null, communication,
        statut == Statut.A_PAYER ? QrCodes.virement("Brahim Mehdi", "BE68539007547034", "", reste, communication) : null,
        LocalDate.of(2027, 1, 30));
  }

  static void ecrireFacture(String nom, AtelierProperties props, FacturePdfService.Vue vue) throws IOException {
    ecrire(nom, new PdfRenderer(props).rendre("facture", "Facture " + vue.numero(),
        Map.of("f", vue, "mentionFranchise", Montants.MENTION_FRANCHISE)));
  }

  @Test
  void facturePayee() throws IOException {
    ecrireFacture("facture-payee.pdf", atelier, facture(Statut.PAYEE, AtelierProperties.RegimeTva.FRANCHISE, List.of(
        new PaiementVue(LocalDate.of(2026, 10, 28), "Espèces", new BigDecimal("50.00")),
        new PaiementVue(LocalDate.of(2026, 10, 30), "Bancontact", new BigDecimal("193.90")))));
  }

  @Test
  void factureAPayerAssujetti() throws IOException {
    ecrireFacture("facture-a-payer-tva.pdf", atelier(AtelierProperties.RegimeTva.ASSUJETTI),
        facture(Statut.A_PAYER, AtelierProperties.RegimeTva.ASSUJETTI, List.of(
            new PaiementVue(LocalDate.of(2026, 10, 28), "Espèces", new BigDecimal("50.00")))));
  }

  @Test
  void noteDeCredit() throws IOException {
    List<Montants.Ligne> lignes = lignesFacture().stream()
        .map(l -> new Montants.Ligne(l.libelle(), l.detail(), l.quantite(), l.prixUnitaire().negate(), l.total().negate()))
        .toList();
    FacturePdfService.Vue vue = new FacturePdfService.Vue(true, "NC2026-0001", LocalDate.of(2026, 11, 2),
        Statut.NOTE_CREDIT, "Martin Dübois", "Rue de la Loi 16, 1000 Bruxelles", null, null, "0470 12 34 56",
        "Réparation", "PCP-2026-0042", "Ordinateur portable Lenovo ThinkPad T14 Gen 3", LocalDate.of(2026, 10, 30),
        lignes, Montants.Totaux.of(new BigDecimal("-243.90"), AtelierProperties.RegimeTva.FRANCHISE, new BigDecimal("21")),
        List.of(), BigDecimal.ZERO, BigDecimal.ZERO, null, "2026-0001", LocalDate.of(2026, 10, 30), null, null, null);
    ecrireFacture("note-credit.pdf", atelier, vue);
  }

  @Test
  void journalDesRecettes() throws IOException {
    List<JournalRecettesService.Jour> jours = new java.util.ArrayList<>();
    for (int i = 1; i <= 24; i++) {
      Map<MoyenPaiement, BigDecimal> parMoyen = new EnumMap<>(MoyenPaiement.class);
      BigDecimal especes = BigDecimal.valueOf(20L * (i % 4));
      BigDecimal bancontact = BigDecimal.valueOf(45L + 13L * (i % 7));
      parMoyen.put(MoyenPaiement.ESPECES, especes);
      parMoyen.put(MoyenPaiement.BANCONTACT, bancontact);
      parMoyen.put(MoyenPaiement.VIREMENT, i % 5 == 0 ? new BigDecimal("120.00") : BigDecimal.ZERO);
      parMoyen.put(MoyenPaiement.AUTRE, BigDecimal.ZERO);
      BigDecimal total = parMoyen.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
      BigDecimal ventes = especes.min(BigDecimal.valueOf(25));
      jours.add(new JournalRecettesService.Jour(LocalDate.of(2026, 10, i), total.subtract(ventes), ventes, parMoyen, total, 3));
    }
    JournalRecettesService.Jour total = jours.get(0);
    Map<MoyenPaiement, BigDecimal> somme = new EnumMap<>(MoyenPaiement.class);
    for (MoyenPaiement m : MoyenPaiement.values()) {
      somme.put(m, jours.stream().map(j -> j.parMoyen().get(m)).reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    total = new JournalRecettesService.Jour(null,
        jours.stream().map(JournalRecettesService.Jour::reparations).reduce(BigDecimal.ZERO, BigDecimal::add),
        jours.stream().map(JournalRecettesService.Jour::ventes).reduce(BigDecimal.ZERO, BigDecimal::add), somme,
        jours.stream().map(JournalRecettesService.Jour::total).reduce(BigDecimal.ZERO, BigDecimal::add), 72);
    List<FactureDto> factures = List.of(
        new FactureDto(1L, TypeFacture.FACTURE, "2026-0001", LocalDate.of(2026, 10, 3), 42L, "PCP-2026-0042", null,
            "Martin Dübois", null, null, null, new BigDecimal("243.90"), "NC2026-0001", null, null),
        new FactureDto(2L, TypeFacture.FACTURE, "2026-0002", LocalDate.of(2026, 10, 9), null, null, 7L,
            "ASBL Les Ateliers", "Rue Wayez 1, 1070 Anderlecht", "BE 0123.456.789", null, new BigDecimal("399.00"), null, null, null),
        new FactureDto(3L, TypeFacture.NOTE_CREDIT, "NC2026-0001", LocalDate.of(2026, 10, 12), 42L, "PCP-2026-0042", null,
            "Martin Dübois", null, null, null, new BigDecimal("-243.90"), null, "2026-0001", null));
    JournalRecettesService.Vue vue = new JournalRecettesService.Vue("Octobre 2026", LocalDate.of(2026, 10, 1),
        LocalDate.of(2026, 10, 31), jours, total, factures, new BigDecimal("399.00"));
    ecrire("journal-recettes.pdf", renderer.rendre("journal", "Journal des recettes", Map.of("j", vue)));
  }
}

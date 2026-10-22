package pcpro.atelier.features.bondepot;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import pcpro.atelier.core.config.AtelierProperties;
import pcpro.atelier.core.util.Libelles;
import pcpro.atelier.features.client.domain.Client;
import pcpro.atelier.features.dossier.app.DossierService;
import pcpro.atelier.features.dossier.domain.Dossier;

/**
 * Bon de dépôt remis au client avec son appareil : récapitulatif du dépôt, QR code vers le suivi en ligne
 * et emplacement pour les signatures. Une page A4, en noir et blanc.
 */
@Service
@Transactional(readOnly = true)
public class BonDepotService {

  private static final Font TITRE = new Font(Font.HELVETICA, 18, Font.BOLD);
  private static final Font SOUS_TITRE = new Font(Font.HELVETICA, 11, Font.BOLD);
  private static final Font NORMAL = new Font(Font.HELVETICA, 10);
  private static final Font PETIT = new Font(Font.HELVETICA, 8);
  private static final Font LIBELLE = new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(90, 90, 90));
  private static final Font REFERENCE = new Font(Font.HELVETICA, 16, Font.BOLD);

  private final DossierService dossierService;
  private final AtelierProperties atelier;

  public BonDepotService(DossierService dossierService, AtelierProperties atelier) {
    this.dossierService = dossierService;
    this.atelier = atelier;
  }

  public record BonDepot(String nomFichier, byte[] pdf) {
  }

  public BonDepot generer(Long dossierId) {
    Dossier dossier = dossierService.getOrThrow(dossierId);
    ByteArrayOutputStream sortie = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 40, 40, 40, 40);
    try {
      PdfWriter.getInstance(document, sortie);
      document.addTitle("Bon de dépôt " + dossier.getReference());
      document.addCreator(atelier.nom());
      document.open();
      entete(document, dossier);
      sections(document, dossier);
      suivi(document, dossier);
      if (!atelier.conditionsDepot().isBlank()) {
        Paragraph conditions = new Paragraph(atelier.conditionsDepot(), PETIT);
        conditions.setSpacingBefore(14);
        document.add(conditions);
      }
      signatures(document);
      document.close();
    } catch (DocumentException | IOException e) {
      throw new IllegalStateException("Impossible de générer le bon de dépôt", e);
    }
    return new BonDepot("bon-depot-" + dossier.getReference() + ".pdf", sortie.toByteArray());
  }

  private void entete(Document document, Dossier dossier) throws DocumentException {
    PdfPTable table = new PdfPTable(new float[] {3, 2});
    table.setWidthPercentage(100);

    PdfPCell gauche = cellule();
    gauche.addElement(new Paragraph(atelier.nom(), TITRE));
    for (String ligne : new String[] {atelier.adresse(), atelier.telephone(), atelier.email(), atelier.horaires()}) {
      if (!ligne.isBlank()) {
        gauche.addElement(new Paragraph(ligne, NORMAL));
      }
    }
    table.addCell(gauche);

    PdfPCell droite = cellule();
    droite.setHorizontalAlignment(Element.ALIGN_RIGHT);
    droite.addElement(aDroite(new Paragraph("BON DE DÉPÔT", SOUS_TITRE)));
    droite.addElement(aDroite(new Paragraph(dossier.getReference(), REFERENCE)));
    droite.addElement(aDroite(new Paragraph("Déposé le " + Libelles.DATE.format(dossier.getDateEntree()), NORMAL)));
    table.addCell(droite);

    table.setSpacingAfter(16);
    document.add(table);
  }

  private void sections(Document document, Dossier dossier) throws DocumentException {
    Client client = dossier.getClient();
    PdfPTable table = new PdfPTable(new float[] {1, 3});
    table.setWidthPercentage(100);

    titre(table, "Client");
    ligne(table, "Nom", client.getNom());
    ligne(table, "Téléphone", client.getTelephone());
    ligne(table, "Email", client.getEmail());

    titre(table, "Appareil");
    String appareil = Libelles.appareil(dossier.getTypeAppareil(), dossier.getMarque(), dossier.getModele());
    ligne(table, "Appareil", Character.toUpperCase(appareil.charAt(0)) + appareil.substring(1));
    ligne(table, "N° de série", dossier.getNumeroSerie());
    ligne(table, "Accessoires", dossier.getAccessoires() != null ? dossier.getAccessoires() : "Aucun");

    titre(table, "Réparation");
    ligne(table, "Problème", dossier.getDescriptionProbleme());
    ligne(table, "Date prévue", dossier.getDateEstimee() != null ? Libelles.DATE.format(dossier.getDateEstimee()) : null);

    document.add(table);
  }

  private void suivi(Document document, Dossier dossier) throws DocumentException, IOException {
    String lien = atelier.lienSuivi(dossier.getReference(), dossier.getCodeSuivi());
    PdfPTable table = new PdfPTable(lien != null ? new float[] {1, 4} : new float[] {1});
    table.setWidthPercentage(100);
    table.setSpacingBefore(18);

    if (lien != null) {
      Image qr = Image.getInstance(qrCode(lien), null);
      qr.scaleToFit(90, 90);
      PdfPCell cellule = new PdfPCell(qr, false);
      cellule.setBorder(Rectangle.NO_BORDER);
      table.addCell(cellule);
    }

    PdfPCell texte = cellule();
    texte.setVerticalAlignment(Element.ALIGN_MIDDLE);
    texte.addElement(new Paragraph("Suivre votre réparation", SOUS_TITRE));
    Paragraph explication = new Paragraph(lien != null
        ? "Scannez ce QR code avec votre téléphone, ou rendez-vous sur " + atelier.urlPublique()
            + "/suivi avec votre référence et ce code :"
        : "Donnez votre référence et ce code à l'atelier pour suivre votre réparation :", NORMAL);
    texte.addElement(explication);
    Paragraph code = new Paragraph();
    code.add(new Chunk("Code de suivi : ", NORMAL));
    code.add(new Chunk(dossier.getCodeSuivi(), REFERENCE));
    texte.addElement(code);
    table.addCell(texte);

    document.add(table);
  }

  private void signatures(Document document) throws DocumentException {
    PdfPTable table = new PdfPTable(2);
    table.setWidthPercentage(100);
    table.setSpacingBefore(24);
    for (String libelle : new String[] {"Signature du client", "Pour l'atelier"}) {
      PdfPCell cellule = new PdfPCell(new Phrase(libelle, LIBELLE));
      cellule.setFixedHeight(60);
      cellule.setBorderColor(new Color(170, 170, 170));
      cellule.setPadding(6);
      table.addCell(cellule);
    }
    document.add(table);
  }

  private static void titre(PdfPTable table, String texte) {
    PdfPCell cellule = new PdfPCell(new Phrase(texte, SOUS_TITRE));
    cellule.setColspan(2);
    cellule.setBorder(Rectangle.BOTTOM);
    cellule.setBorderColor(new Color(170, 170, 170));
    cellule.setPaddingTop(10);
    cellule.setPaddingBottom(4);
    table.addCell(cellule);
  }

  private static void ligne(PdfPTable table, String libelle, String valeur) {
    PdfPCell gauche = new PdfPCell(new Phrase(libelle, LIBELLE));
    gauche.setBorder(Rectangle.NO_BORDER);
    gauche.setPaddingTop(4);
    table.addCell(gauche);
    PdfPCell droite = new PdfPCell(new Phrase(valeur == null || valeur.isBlank() ? "-" : valeur, NORMAL));
    droite.setBorder(Rectangle.NO_BORDER);
    droite.setPaddingTop(4);
    table.addCell(droite);
  }

  private static PdfPCell cellule() {
    PdfPCell cellule = new PdfPCell();
    cellule.setBorder(Rectangle.NO_BORDER);
    return cellule;
  }

  private static Paragraph aDroite(Paragraph paragraphe) {
    paragraphe.setAlignment(Element.ALIGN_RIGHT);
    return paragraphe;
  }

  private static BufferedImage qrCode(String contenu) {
    try {
      BitMatrix matrice = new QRCodeWriter().encode(contenu, BarcodeFormat.QR_CODE, 300, 300,
          Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
      BufferedImage image = new BufferedImage(matrice.getWidth(), matrice.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
      for (int x = 0; x < matrice.getWidth(); x++) {
        for (int y = 0; y < matrice.getHeight(); y++) {
          image.setRGB(x, y, matrice.get(x, y) ? 0x000000 : 0xFFFFFF);
        }
      }
      return image;
    } catch (WriterException e) {
      throw new IllegalStateException("QR code impossible à générer", e);
    }
  }
}

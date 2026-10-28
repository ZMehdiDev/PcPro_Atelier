package pcpro.atelier.features.document;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.Map;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * QR codes imprimés (suivi en ligne, fiche du dossier, paiement par virement) et communication structurée belge.
 */
public final class QrCodes {

  /** Pixels par module : assez pour rester net à l'impression sans lissage du lecteur PDF. */
  private static final int PIXELS_PAR_MODULE = 10;

  private QrCodes() {
  }

  /** QR code en PNG, prêt pour un attribut src (data URI). */
  public static String dataUri(String contenu) {
    return dataUri(contenu, ErrorCorrectionLevel.M);
  }

  private static String dataUri(String contenu, ErrorCorrectionLevel correction) {
    try {
      BitMatrix matrice = new QRCodeWriter().encode(contenu, BarcodeFormat.QR_CODE, 0, 0,
          Map.of(EncodeHintType.ERROR_CORRECTION, correction, EncodeHintType.MARGIN, 0,
              EncodeHintType.CHARACTER_SET, "UTF-8"));
      int taille = matrice.getWidth() * PIXELS_PAR_MODULE;
      BufferedImage image = new BufferedImage(taille, taille, BufferedImage.TYPE_BYTE_BINARY);
      for (int x = 0; x < taille; x++) {
        for (int y = 0; y < taille; y++) {
          image.setRGB(x, y, matrice.get(x / PIXELS_PAR_MODULE, y / PIXELS_PAR_MODULE) ? 0x000000 : 0xFFFFFF);
        }
      }
      ByteArrayOutputStream png = new ByteArrayOutputStream();
      ImageIO.write(image, "png", png);
      return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
    } catch (WriterException e) {
      throw new IllegalStateException("QR code impossible à générer", e);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * QR code de virement SEPA (norme EPC069-12), reconnu par les applis bancaires belges (Belfius, KBC, ING, BNP...) :
   * le client le scanne et le virement est prérempli. Null si l'IBAN n'est pas configuré ou le montant nul.
   */
  public static String virement(String beneficiaire, String iban, String bic, BigDecimal montant, String communication) {
    if (iban == null || iban.isBlank() || montant == null || montant.signum() <= 0) {
      return null;
    }
    String contenu = String.join("\n",
        "BCD",
        "002",
        "1",
        "SCT",
        bic == null ? "" : bic,
        beneficiaire.length() > 70 ? beneficiaire.substring(0, 70) : beneficiaire,
        iban,
        "EUR" + montant.setScale(2, RoundingMode.HALF_UP).toPlainString(),
        "",
        "",
        communication == null ? "" : communication);
    return dataUri(contenu, ErrorCorrectionLevel.M);
  }

  /**
   * Communication structurée belge (+++123/4567/89002+++) : 10 chiffres puis un contrôle modulo 97.
   * Permet de rapprocher un virement de sa facture sans erreur de recopie.
   */
  public static String communicationStructuree(long nombre) {
    String base = String.format("%010d", Math.floorMod(nombre, 10_000_000_000L));
    int controle = new BigInteger(base).mod(BigInteger.valueOf(97)).intValue();
    if (controle == 0) {
      controle = 97;
    }
    String chiffres = base + String.format("%02d", controle);
    return "+++" + chiffres.substring(0, 3) + "/" + chiffres.substring(3, 7) + "/" + chiffres.substring(7) + "+++";
  }
}

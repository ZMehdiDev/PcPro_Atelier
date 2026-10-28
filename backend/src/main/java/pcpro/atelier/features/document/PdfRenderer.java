package pcpro.atelier.features.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import pcpro.atelier.core.config.AtelierProperties;

/**
 * Transforme un gabarit HTML (resources/documents) en PDF A4.
 *
 * Les gabarits sont du HTML bien formé (XHTML) rendu par Thymeleaf, puis mis en page par openhtmltopdf.
 * Polices embarquées, comme dans l'appli : "Inter" pour le texte, "Inter Chiffres" (chiffres à largeur fixe) pour que
 * les montants s'alignent en colonne, et "Newsreader" (serif) pour les titres.
 * Chaque gabarit reçoit "atelier" (identité de l'entreprise), "fmt" (formats) et "logo" (image ou null).
 */
@Component
public class PdfRenderer {

  private static final Logger log = LoggerFactory.getLogger(PdfRenderer.class);

  private static final Map<String, Integer> GRAISSES = Map.of(
      "Regular", 400, "Medium", 500, "SemiBold", 600, "Bold", 700);

  private final TemplateEngine moteur;
  private final AtelierProperties atelier;
  /** Famille CSS → graisse → fichier de police. */
  private final Map<String, Map<Integer, byte[]>> polices = new LinkedHashMap<>();
  private final String logo;

  public PdfRenderer(AtelierProperties atelier) {
    this.atelier = atelier;
    ClassLoaderTemplateResolver resolveur = new ClassLoaderTemplateResolver();
    resolveur.setPrefix("documents/");
    resolveur.setSuffix(".html");
    resolveur.setTemplateMode(TemplateMode.HTML);
    resolveur.setCharacterEncoding("UTF-8");
    resolveur.setCacheable(true);
    moteur = new TemplateEngine();
    moteur.setTemplateResolver(resolveur);
    Map.of("Inter", "Inter", "Inter Chiffres", "InterChiffres").forEach((famille, fichier) -> {
      Map<Integer, byte[]> parGraisse = new LinkedHashMap<>();
      GRAISSES.forEach((nom, graisse) -> parGraisse.put(graisse, ressource("documents/fonts/" + fichier + "-" + nom + ".ttf")));
      polices.put(famille, parGraisse);
    });
    polices.put("Newsreader", Map.of(
        500, ressource("documents/fonts/Newsreader-Medium.ttf"),
        600, ressource("documents/fonts/Newsreader-SemiBold.ttf")));
    logo = chargerLogo(atelier.logo());
  }

  /**
   * @param gabarit   nom du fichier dans resources/documents, sans .html
   * @param titre     titre du PDF (affiché par le lecteur PDF)
   * @param variables données du gabarit
   */
  public byte[] rendre(String gabarit, String titre, Map<String, Object> variables) {
    Context contexte = new Context(Locale.FRENCH);
    contexte.setVariable("atelier", atelier);
    contexte.setVariable("fmt", Formats.INSTANCE);
    contexte.setVariable("logo", logo);
    contexte.setVariable("titre", titre);
    contexte.setVariables(variables);
    String html = moteur.process(gabarit, contexte);

    ByteArrayOutputStream sortie = new ByteArrayOutputStream();
    try {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      polices.forEach((famille, parGraisse) -> parGraisse.forEach((graisse, octets) ->
          builder.useFont(() -> new ByteArrayInputStream(octets), famille, graisse, FontStyle.NORMAL, true)));
      builder.withHtmlContent(html, "classpath:/documents/");
      builder.withProducer(atelier.nom());
      builder.toStream(sortie);
      builder.run();
    } catch (IOException e) {
      throw new UncheckedIOException("Impossible de générer le document " + gabarit, e);
    }
    return sortie.toByteArray();
  }

  private static byte[] ressource(String chemin) {
    try (InputStream flux = PdfRenderer.class.getClassLoader().getResourceAsStream(chemin)) {
      if (flux == null) {
        throw new IllegalStateException("Ressource introuvable : " + chemin);
      }
      return flux.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Logo configuré, en data URI ; null (logo dessiné) s'il n'y en a pas ou s'il est illisible. */
  private static String chargerLogo(String chemin) {
    if (chemin == null || chemin.isBlank()) {
      return null;
    }
    Path fichier = Path.of(chemin);
    String nom = fichier.getFileName().toString().toLowerCase(Locale.ROOT);
    String type = nom.endsWith(".png") ? "image/png" : nom.endsWith(".jpg") || nom.endsWith(".jpeg") ? "image/jpeg" : null;
    if (type == null) {
      log.warn("Logo ignoré ({}) : seuls les fichiers PNG et JPG sont acceptés", chemin);
      return null;
    }
    try {
      return "data:" + type + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(fichier));
    } catch (IOException e) {
      log.warn("Logo ignoré, fichier illisible : {}", chemin);
      return null;
    }
  }
}

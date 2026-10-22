package pcpro.atelier.features.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import pcpro.atelier.core.config.AtelierProperties;
import pcpro.atelier.core.config.MailProperties;
import pcpro.atelier.core.exception.ServiceIndisponibleException;

/**
 * Envoi SMTP (Brevo ou autre). JavaMailSender n'existe que si spring.mail.host est configuré.
 */
@Service
public class EmailService {

  private static final Logger log = LoggerFactory.getLogger(EmailService.class);

  private final ObjectProvider<JavaMailSender> mailSender;
  private final MailProperties mailProperties;
  private final AtelierProperties atelier;
  private final String hote;

  public EmailService(
      ObjectProvider<JavaMailSender> mailSender,
      MailProperties mailProperties,
      AtelierProperties atelier,
      @Value("${spring.mail.host:}") String hote
  ) {
    this.mailSender = mailSender;
    this.hote = hote;
    this.mailProperties = mailProperties;
    this.atelier = atelier;
  }

  public boolean estConfigure() {
    return !hote.isBlank() && mailSender.getIfAvailable() != null && !mailProperties.expediteur().isBlank();
  }

  /**
   * @throws ServiceIndisponibleException si l'envoi n'est pas configuré ou échoue
   */
  public void envoyer(Email email) {
    JavaMailSender sender = mailSender.getIfAvailable();
    if (!estConfigure()) {
      throw new ServiceIndisponibleException("L'envoi d'emails n'est pas configuré");
    }
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(mailProperties.expediteur());
      if (!atelier.email().isBlank()) {
        helper.setReplyTo(atelier.email());
      }
      helper.setTo(email.destinataire());
      helper.setSubject(email.sujet());
      helper.setText(email.texte(), email.html());
      sender.send(message);
    } catch (MessagingException | MailException e) {
      log.warn("Échec de l'envoi de l'email \"{}\" : {}", email.sujet(), e.getMessage());
      throw new ServiceIndisponibleException("L'email n'a pas pu être envoyé, réessayez plus tard");
    }
  }
}

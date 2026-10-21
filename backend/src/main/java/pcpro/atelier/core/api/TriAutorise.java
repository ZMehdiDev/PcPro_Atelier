package pcpro.atelier.core.api;

import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import pcpro.atelier.core.exception.BadRequestException;

/**
 * Limite les propriétés de tri acceptées depuis l'URL (?sort=...). Sans ça, un tri sur une propriété
 * d'une entité liée (ex : technicienAssigne.passwordHash) serait possible.
 */
public final class TriAutorise {

  private TriAutorise() {
  }

  public static Pageable verifier(Pageable pageable, Set<String> proprietesAutorisees) {
    for (Sort.Order order : pageable.getSort()) {
      if (!proprietesAutorisees.contains(order.getProperty())) {
        throw new BadRequestException("Tri non autorisé sur '" + order.getProperty()
            + "'. Valeurs possibles : " + proprietesAutorisees);
      }
    }
    return pageable;
  }
}

# Plan de validation du fix Maven/Spring Boot Test

## Diagnostic identifié

**Cause racine** : Java 21 + Maven Compiler Plugin 3.14.1 active le module-path (JPMS) par défaut, ce qui empêche le compilateur de trouver les classes de test Spring Boot (`@WebMvcTest`, `@MockBean`) même si les dépendances sont présentes.

**Solution appliquée** : Configuration explicite du `maven-compiler-plugin` avec `<useModulePath>false</useModulePath>` pour désactiver le module-path et utiliser le classpath classique.

## Commandes de validation (PowerShell)

### Étape 1 : Nettoyer le cache Maven (optionnel mais recommandé)

```powershell
# Purger les dépendances Spring Boot test du cache local
Remove-Item -Recurse -Force "$env:USERPROFILE\.m2\repository\org\springframework\boot\spring-boot-test*" -ErrorAction SilentlyContinue

# Alternative avec Maven
Set-Location backend
mvn dependency:purge-local-repository -DmanualInclude="org.springframework.boot:spring-boot-test-autoconfigure,org.springframework.boot:spring-boot-test,org.springframework.boot:spring-boot-starter-test" -DreResolve=true
```

### Étape 2 : Nettoyer le projet

```powershell
Set-Location backend
mvn clean
```

### Étape 3 : Compiler les tests

```powershell
Set-Location backend
mvn test-compile
```

**Résultat attendu** : 
- ✅ Pas d'erreur `package org.springframework.boot.test.autoconfigure.web.servlet does not exist`
- ✅ Pas d'erreur `cannot find symbol WebMvcTest`
- ✅ Pas d'erreur `cannot find symbol MockBean`
- ✅ Compilation réussie avec `BUILD SUCCESS`

### Étape 4 : Exécuter les tests

```powershell
Set-Location backend
mvn test
```

**Résultat attendu** :
- ✅ Les tests `DossierControllerTest` et `DossierControllerChangeStatutTest` compilent
- ✅ Les tests s'exécutent (peuvent échouer sur la logique, mais pas sur la compilation)

### Étape 5 : Validation complète avec mise à jour

```powershell
Set-Location backend
mvn -U clean test
```

## Vérification du fix dans pom.xml

Le `pom.xml` doit maintenant contenir :

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>21</source>
        <target>21</target>
        <useModulePath>false</useModulePath>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

## Explication technique

- **`useModulePath=false`** : Force Maven à utiliser le classpath classique au lieu du module-path (JPMS), ce qui permet au compilateur de trouver toutes les classes des dépendances test, y compris celles dans `spring-boot-test-autoconfigure`.

- **Pourquoi ça marche** : Avec Java 21, Maven Compiler Plugin active le module-path par défaut. Le module-path nécessite des `module-info.java` et une configuration explicite des modules. Les dépendances Spring Boot test ne sont pas configurées pour JPMS, donc elles ne sont pas accessibles via le module-path. En désactivant le module-path, on revient au classpath classique où toutes les dépendances sont accessibles.

## Notes

- Les dépendances `spring-boot-test` et `spring-boot-test-autoconfigure` sont redondantes (déjà incluses dans `spring-boot-starter-test`), mais leur présence explicite ne cause pas de problème.
- Ce fix est compatible avec Spring Boot 4.0.2 et Java 21.
- Le fix est durable et ne nécessite pas de modification des dépendances.

---
name: testing
description: Regles d'ecriture et d'execution des tests SmartShop (JUnit 5, Mockito, Testcontainers, Karma). A lire avant d'ecrire ou de lancer des tests.
---

# Tests SmartShop

## Backend
- Services metier : tests unitaires JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`), sans contexte Spring.
- Controllers : `@WebMvcTest` + `@MockitoBean` pour le service ; verifier codes HTTP, JSON, validation et securite.
- Integration / repositories : `@SpringBootTest` ou `@DataJpaTest` + `@Import(TestcontainersConfiguration.class)` + `@ActiveProfiles("test")`. PostgreSQL reel, pas de H2.
- Nommage : `methodeOuScenario_resultatAttendu` ou phrase descriptive ; un comportement par test.
- Cas obligatoires : chemin nominal, validation, ressource introuvable, regle metier violee (stock insuffisant, paiement > reste...), permissions.
- Montants : comparer les `BigDecimal` avec `isEqualByComparingTo`.
- Lancer : `./mvnw test` ; couverture : `./mvnw verify` -> `target/site/jacoco/index.html`.

## Frontend
- Specs Jasmine a cote du fichier (`*.spec.ts`) ; `provideHttpClientTesting()` pour les services HTTP.
- Lancer : `npm run test:ci` (Chrome headless).

## Regle
- Une tache n'est terminee que si les tests passent. Ne jamais desactiver/supprimer un test pour le faire passer.

# Hibernate Inheritance

A small Java project that shows the three JPA inheritance mapping strategies with Hibernate: `SINGLE_TABLE`, `JOINED` and `TABLE_PER_CLASS`. Each strategy has its own small class hierarchy, and a generic `CrudService` handles create, read, update, delete and polymorphic search on all of them.

## Technologies

- Java (8 or newer)
- Maven
- Hibernate Core 5.4.30.Final (JPA 2.2 implementation)
- JPA API 2.2 (`javax.persistence`)
- H2 Database 1.4.200 (in-memory, runtime scope)

## Project structure

```
hibernate-inheritance
├── pom.xml
└── src
    ├── main
    │   ├── java/com/example
    │   │   ├── App.java                         # Demo covering the three strategies
    │   │   ├── model
    │   │   │   ├── singletable
    │   │   │   │   ├── Vehicule.java            # Abstract root, SINGLE_TABLE
    │   │   │   │   ├── Voiture.java
    │   │   │   │   └── Moto.java
    │   │   │   ├── joined
    │   │   │   │   ├── Employe.java             # Abstract root, JOINED
    │   │   │   │   ├── Developpeur.java
    │   │   │   │   └── Manager.java
    │   │   │   └── tableperclass
    │   │   │       ├── Produit.java             # Abstract root, TABLE_PER_CLASS
    │   │   │       ├── Livre.java
    │   │   │       └── Electronique.java
    │   │   └── service
    │   │       └── CrudService.java             # Generic CRUD and search service
    │   └── resources/META-INF
    │       └── persistence.xml                  # Persistence unit "hibernate-inheritance"
    └── test/java/com/example
        └── AppTest.java                         # Placeholder test
```

## The three strategies

Every root class is abstract and declares an abstract `decrire()` method, which each subclass implements. This is what makes the polymorphic queries in the demo visible.

| | `SINGLE_TABLE` | `JOINED` | `TABLE_PER_CLASS` |
|---|---|---|---|
| Package | `model.singletable` | `model.joined` | `model.tableperclass` |
| Root class | `Vehicule` (`marque`) | `Employe` (`nom`) | `Produit` (`nom`) |
| Subclasses | `Voiture` (`nombrePortes`), `Moto` (`sidecar`) | `Developpeur` (`langage`), `Manager` (`nombreEquipes`) | `Livre` (`auteur`), `Electronique` (`garantie`) |
| Tables | One table `Vehicule` for the whole hierarchy | `Employe`, `Developpeur` and `Manager`, linked by the `id` | `Livre` and `Electronique`, each with all the columns; none for the abstract `Produit` |
| Discriminator | Column `vehicule_type`: `VOITURE` or `MOTO` | None | None |
| Id generation | `IDENTITY` | `IDENTITY` | `AUTO` (a sequence) |

### How they compare

- **`SINGLE_TABLE`** is the simplest and the fastest to query, since no join is needed. The columns of a subclass stay empty for rows of the other subclasses.
- **`JOINED`** keeps the data normalized: shared fields live in the parent table and specific fields in the child table. Reading an entity needs a join.
- **`TABLE_PER_CLASS`** gives each concrete class a complete table. Querying the root class combines the tables with a `UNION`. This strategy cannot use `IDENTITY` for the id, which is why `Produit` uses `GenerationType.AUTO`.

## CrudService

`CrudService<T>` is a generic service built from an `EntityManagerFactory` and an entity class. The demo creates one for each root class (`Vehicule`, `Employe`, `Produit`).

| Method | Description |
|---|---|
| `creer(entite)` | Persists an entity in a transaction |
| `trouver(id)` | Finds an entity by id, or `null` |
| `lister()` | Lists all entities of the type, including subclasses, ordered by id |
| `rechercher(attribut, prefixe, sousType)` | Case-insensitive prefix search on an attribute, restricted to one subclass (Criteria API with `root.type()`) |
| `modifier(id, modification)` | Loads an entity and applies a change in a transaction; returns `false` if it does not exist |
| `supprimer(id)` | Deletes an entity in a transaction; returns `false` if it does not exist |
| `rechercherParMarqueEtType(debutMarque, sousType)` | Same as `rechercher` on the `marque` attribute, so it only makes sense for `Vehicule` |

Writes run in a transaction that is rolled back if an exception is thrown. Each call opens and closes its own `EntityManager`.

## What the demo does

`App.main` creates the `EntityManagerFactory` for the unit `hibernate-inheritance`, then:

1. **Create:** saves a `Voiture` and a `Moto` (single table), a `Developpeur` and a `Manager` (joined), a `Livre` and an `Electronique` (table per class)
2. **Read:** lists each hierarchy through its root class, calling `decrire()` on every element
3. **Search:** counts the `Moto` whose brand starts with "Har", the `Developpeur` whose name starts with "Ali", and the `Livre` whose name starts with "Clean"
4. **Update:** changes the brand of the `Voiture` and reads it back
5. **Delete:** removes the `Electronique` and counts the remaining products

The console output, apart from the SQL logged by Hibernate, should be:

```
--- Véhicules ---
Voiture Toyota à 4 portes
Moto Harley avec sidecar
--- Employés ---
Développeur : Alice
Manager : Bob
--- Produits ---
Livre : Clean Code
Produit électronique : Smartphone
Motos Harley : 1
Développeurs Alice : 1
Livres Clean Code : 1
Voiture modifiée : Voiture Toyota modifiée à 4 portes
Produits après suppression : 1
```

## Configuration

The persistence unit is defined in `src/main/resources/META-INF/persistence.xml`:

- H2 in-memory database `jdbc:h2:mem:testdb`, user `sa`, empty password
- `hibernate.hbm2ddl.auto=create-drop`: tables are created at startup and dropped at shutdown
- `hibernate.show_sql=true` and `hibernate.format_sql=true`: generated SQL is printed in the console, which lets you see the differences between the strategies (a single table, joins, or a `UNION`)
- The nine entity classes are listed explicitly

The database lives in memory, so all data is lost when the program ends.

## Build and run

```bash
mvn compile exec:java -Dexec.mainClass=com.example.App
```
<img width="842" height="830" alt="Screenshot 2026-10-10 194733" src="https://github.com/user-attachments/assets/96becd84-d3d8-4aad-8356-56b8347758f0" />

## Notes

- **Tests:** `AppTest.java` uses JUnit, which is not declared in `pom.xml`. Add the `junit` dependency (test scope) before running `mvn test` or `mvn package`.
- **Java version:** `pom.xml` does not set a compiler level, and the code uses lambdas. Add `maven.compiler.source` and `maven.compiler.target` (for example `11`) to `<properties>` to be sure it builds on every Maven setup.
- **`rechercherParMarqueEtType`:** it duplicates `rechercher("marque", ...)` and is not called by the demo. It can be removed.

## Possible next steps

- Add a fourth level to a hierarchy (for example `Camion` under `Vehicule`)
- Try `@MappedSuperclass` and compare it with the three strategies
- Add real JUnit tests, one per strategy, checking the generated tables and the polymorphic queries

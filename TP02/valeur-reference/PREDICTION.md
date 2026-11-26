# Expérience valeur / référence (livrable B)

Code : `Box` (Serializable), `Probe` (Remote), `ProbeImpl`, `ExperimentClient`.
Lancement : `mvn -q compile exec:java` (tout-en-un, `Experiment`), ou deux JVM :
`java -cp target/classes tn.rnu.eniso.fwk.rmi.experiment.ProbeServer 1099` puis
`java -cp target/classes tn.rnu.eniso.fwk.rmi.experiment.ExperimentClient rmi://localhost:1099/probe`.

## 1. Prédiction (écrite avant l'exécution)

- `r + " " + b.n` affichera **`99 1`** : `Box` n'est pas `Remote`, donc RMI envoie une
  copie sérialisée ; le serveur met `n = 99` sur sa copie et renvoie 99, mon `b` reste à 1.
- `api.last() == api.last()` affichera **`false`** : chaque appel désérialise un nouvel
  objet dans ma JVM. `.equals` donne aussi **`false`** car `Box` ne redéfinit pas
  `equals` (comparaison d'identité héritée d'`Object`).

## 2. Résultat observé

```
r + " " + b.n                 -> 99 1
api.last() == api.last()      -> false
api.last().equals(api.last()) -> false (equals not overridden)
api.last().n                  -> 99
```

Prédiction confirmée (aussi vérifiée par `ExperimentTest`).

## 3. Explication (une phrase)

Un argument dont la classe n'implémente pas `java.rmi.Remote` est sérialisé puis
recréé de l'autre côté, donc le serveur modifie sa propre copie et chaque valeur
de retour est un nouvel objet : on compare par identifiant, jamais par `==`.

## 4. Mesure rapide (Part 2, « The price of a snapshot »)

`SnapshotCostTest` dans `../game-sousse-kombla` (snapshot réel `DynamicGameModel` :
2 joueurs, 3 sprites ; 1000 sérialisations après échauffement) :

| Encodage | Octets / snapshot | Coût | Débit à 25 Hz / joueur |
|----------|------------------|------|------------------------|
| `ObjectOutputStream` (RMI) | 1 353 | ≈ 16 µs | ≈ 34 Ko/s |
| Codec manuel TP01 (même snapshot) | 382 | — | ≈ 9,5 Ko/s |

Hands-on 2.4 (« casser exprès ») : Atom 1.5.4.2 le fait déjà — `ModelPoint`
n'est pas `Serializable`, et `writeObject(sprite)` lève
`java.io.NotSerializableException: net.thevpc.gaming.atom.model.ModelPoint`.

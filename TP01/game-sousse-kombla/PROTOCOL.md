# Protocole Kombla sur TCP — version 1

Une connexion TCP par joueur, deux flux indépendants : commandes client → serveur,
snapshots serveur → client. Pas de longueur explicite : chaque trame est une
**séquence typée** `DataOutputStream` (big-endian) ; **l'ordre d'écriture est le
protocole**. Codes : `shared/dal/ProtocolConstants.java`, codec :
`shared/dal/ProtocolCodec.java` (chaque `writeXxx` a son `readXxx` juste en dessous).

Notation : `INT` = 4 o, `LONG`/`DOUBLE` = 8 o, `BOOL` = 1 o,
`UTF` = 2 o de longueur + octets UTF-8 modifié, `NSTRING` = `BOOL` présent + `UTF` si vrai.

## 1. Session (une fois, dans `connect()`)

| # | Sens | Champ | Type Java | Valeur |
|---|------|-------|-----------|--------|
| 1 | c → s | code | `int` | `CONNECT = 1` |
| 2 | c → s | version | `int` | `PROTOCOL_VERSION = 1` |
| 3 | c → s | playerName | `String` (UTF) | ≤ 16 caractères |
| 4a | s → c | code | `int` | `OK = 7` |
| 5a | s → c | StartGameInfo | voir § 4 | id attribué par le moteur |
| 4b | s → c | code | `int` | `KO = 8` (version, nom invalide, partie pleine) |
| 5b | s → c | raison | `String` (UTF) | puis le serveur ferme |

Le thread `kombla-reader` du client démarre **après** 4a/5a ; la session n'entre
dans la table du serveur qu'après l'envoi de OK : aucun snapshot ne peut se
mélanger au handshake.

## 2. Commandes (c → s, fire-and-forget, pas d'ACK)

| Code | Valeur | Méthode client | Méthode moteur appelée |
|------|--------|----------------|------------------------|
| `LEFT` | 2 | `sendMoveLeft()` | `onReceiveMoveLeft(playerId)` |
| `RIGHT` | 3 | `sendMoveRight()` | `onReceiveMoveRight(playerId)` |
| `UP` | 4 | `sendMoveUp()` | `onReceiveMoveUp(playerId)` |
| `DOWN` | 5 | `sendMoveDown()` | `onReceiveMoveDown(playerId)` |
| `FIRE` | 6 | `sendFire()` | `onReceiveReleaseBomb(playerId)` |

Une trame = un `int` (4 octets), `flush()` immédiat, `TCP_NODELAY` actif.
Le `playerId` ne voyage pas : le serveur le connaît par la session.

## 3. Snapshot (s → c, à chaque frame du moteur : 25 Hz)

| Champ | Type |
|-------|------|
| code | `int` `OK = 7` |
| frame | `long` |
| players.size | `int` |
| players[i] | Player (§ 4) |
| sprites.size | `int` |
| sprites[i] | Sprite (§ 4) |

Le snapshot est encodé **une seule fois** puis écrit sur chaque session sous son
verrou, suivi d'un seul `flush()`.

## 4. Structures

- **StartGameInfo** : `playerId INT`, `rows INT`, `columns INT`, puis `rows × columns` cellules `INT` (0 = libre). 12×12 → 588 octets, envoyé une seule fois.
- **Player** : `id INT`, `name NSTRING`, `hasColor BOOL` [+ `rgba INT`], `properties MAP`.
- **Sprite** : `id INT`, `kind UTF` (`Person`/`Bomb`/`Explosion`), `name NSTRING`, `x DOUBLE`, `y DOUBLE`, `width DOUBLE`, `height DOUBLE`, `direction DOUBLE`, `speed DOUBLE`, `life INT`, `maxLife INT`, `playerId INT`, `layer INT`, `crossable BOOL`, **`movementStyle INT`**, `style INT`, **`properties MAP`**.
- **MAP** (`Map<String,String>`) : `size INT` puis `size ×` (`key UTF`, `value UTF`). Les valeurs `Object` d'Atom voyagent en `String` (`explosionTime` = "3", relu par `ExplosionView`).

`width/height` sont envoyés car `Explosion` (1×1) n'est pas re-dimensionnée par
`MainClientEngine` (seuls `Person` et `Bomb` le sont) ; `layer` fixe l'ordre de dessin.

## 5. Mesures (2 joueurs, 2 `Person`, `-Dkombla.debug` désactivé)

- Snapshot primitives ≈ **360 octets** (122 166 o / 336 trames mesurés) → 25 Hz ≈ 9 Ko/s par client.
- Variante object streams ≈ **1 130 octets** (303 696 o / 268 trames) → ≈ 28 Ko/s par client.
- Commande : 4 octets.

## 6. Primitives (`DataOutputStream`) vs object streams (`ObjectOutputStream`)

| | Primitives + codec | Object streams (`-Dkombla.transport=obj`) |
|---|---|---|
| Code à maintenir | `ProtocolCodec` (write/read symétriques) | aucun codec, `writeObject` |
| Taille | ~360 o / snapshot, exacte et prévisible | ~3× plus (descripteurs de classe, noms de champs) |
| Évolution | un champ oublié d'un côté désynchronise le flux (bug `movementStyle`) ; ajout en fin de trame possible | champ ajouté transmis automatiquement, mais même `serialVersionUID` et mêmes classes exigés des deux côtés |
| Interopérabilité | n'importe quel langage peut lire ce document | Java uniquement |
| Piège | ordre des champs | sans `reset()` après chaque écriture, l'objet déjà envoyé repart comme simple référence (client figé) |
| Sécurité | rien n'est exécuté à la lecture | désérialiser un flux non fiable = exécuter du code : jamais sur un port public |

Particularité d'Atom 1.5.4.2 : `ModelPoint` n'est pas `Serializable`, donc un
`Sprite` ne passe pas tel quel dans un `ObjectOutputStream`
(`NotSerializableException`). `DynamicGameModel` (DTO) déclare ses sprites
`transient` et les transporte sous forme de `SpriteState` sérialisable
(`writeObject`/`readObject`).

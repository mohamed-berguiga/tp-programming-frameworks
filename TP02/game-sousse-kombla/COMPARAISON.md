# Sockets (TP01) vs RMI (TP02) — pour Kombla

**Ce qui ne change pas.** Les quatre interfaces DAL, `MainClientEngine`,
`MainServerEngine` (une ligne d'instanciation), le découpage « maze une fois /
snapshot complet à 25 Hz », et la règle « seul le serveur décide ».

| Critère | Sockets + codec (TP01) | RMI (TP02) |
|---|---|---|
| Plomberie écrite | codec + constantes + thread lecteur, acceptor, threads de session, verrous d'écriture : ≈ 800 lignes (sans le bonus object streams) | 2 interfaces + servant + 2 DAO : ≈ 520 lignes (commentaires et validations compris), aucun thread de lecture, aucun codec |
| Qui fait le framing | nous : séquence typée, l'ordre d'écriture *est* le protocole | le JDK (sérialisation Java + protocole JRMP) |
| Taille d'un snapshot (2 joueurs, 3 sprites) | **382 o**, exacte | **1 353 o** (+ en-tête d'appel), descripteurs de classe à chaque appel |
| Débit à 25 Hz par joueur | ≈ 9,5 Ko/s | ≈ 34 Ko/s + allers-retours |
| Cadence d'envoi | écriture dans un buffer TCP : quasi instantanée, ne bloque pas la frame | appel synchrone : attend la réponse du client → isolé sur le thread `kombla-push` avec coalescence (le snapshot le plus récent gagne) |
| Commandes | 4 octets, fire-and-forget, pas d'ACK | un appel distant bloquant par touche (aller-retour), at-most-once |
| Joueur qui disparaît | `IOException` en écriture/lecture → session fermée | `RemoteException` au push → stub retiré ; DGC/lease libère l'export |
| Évolution du modèle | un champ = modifier `writeSprite` **et** `readSprite` (bug `movementStyle`) | champ transmis automatiquement… si tout le graphe est `Serializable` (piège Atom : `ModelPoint`) |
| Débogage | visible dans `tcpdump`/Wireshark, on pointe chaque octet | invisible ; `logCalls`, `jstack` (« RMI TCP Connection »), traces `sun.rmi` |
| Réseau / pare-feu | un seul port (1234) | registre + ports anonymes des objets exportés + `java.rmi.server.hostname` |
| Interopérabilité | n'importe quel langage avec `PROTOCOL.md` | Java uniquement, mêmes classes des deux côtés |
| Sécurité | rien n'est exécuté à la lecture | désérialisation de données distantes, aucune authentification : réseau du labo seulement |

**Pour ce jeu précis.** Le trafic dominant est un *push* de petit état complet,
25 fois par seconde, vers peu de joueurs. Les sockets y sont plus efficaces
(3,5× moins d'octets, pas d'aller-retour par frame, un seul port) et plus faciles
à observer ; RMI est plus court à écrire, typé par le compilateur, et rend le
handshake trivial (`join` atomique avec le callback). Le coût caché de RMI est le
caractère bloquant du callback : sans le thread `kombla-push`, le client le plus
lent fixerait la cadence du host. Choix : RMI pour un prototype Java en réseau
local ; sockets (ou un broker) dès qu'on vise la latence, des clients non Java ou
Internet.

**Limite commune.** Le contrat n'a pas de `onReceivePlayerLeft` : dans les deux
versions, le DAL arrête d'envoyer au joueur parti, mais son personnage reste
dans le modèle du moteur (ajouter la méthode modifierait la couche métier).

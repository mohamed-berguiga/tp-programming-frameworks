# Contrat RMI de Kombla (TP02)

Le protocole n'est plus une suite d'octets : ce sont **deux interfaces Java**
(`shared/dal`) compilées dans les deux JVM, plus un **contrat de nommage**.

## 1. Nommage

| Élément | Valeur | Où |
|---------|--------|----|
| Registre | embarqué dans la JVM host : `LocateRegistry.createRegistry(AppConfig.serverPort)` (défaut 1234) | `RmiMainServerDAO.start` |
| Nom lié | `"kombla-main"` → servant `GameServerImpl` | `RmiConstants.BINDING_NAME` |
| URL client | `rmi://<serverAddress>:<serverPort>/kombla-main` | `RmiMainClientDAO.connect` |
| Ports | registre = `serverPort` ; objets exportés (servant, callbacks) = port anonyme choisi par la JVM | — |

Le registre ne transporte aucune donnée de jeu : il sert seulement à obtenir le
stub de `GameServer`.

## 2. Interface de requêtes (client → serveur)

```java
public interface GameServer extends Remote {
    StartGameInfo join(String name, GameClientCallback cb) throws RemoteException;
    void moveLeft(int playerId)  throws RemoteException;
    void moveRight(int playerId) throws RemoteException;
    void moveUp(int playerId)    throws RemoteException;
    void moveDown(int playerId)  throws RemoteException;
    void fire(int playerId)      throws RemoteException;  // non idempotent : jamais rejoué
    void leave(int playerId)     throws RemoteException;  // sortie propre, idempotent
}
```

## 3. Interface de callback (serveur → client)

```java
public interface GameClientCallback extends Remote {
    void onModelChanged(DynamicGameModel m) throws RemoteException;
}
```

## 4. Ce qui traverse, et comment

| Paramètre / retour | Passage | Raison |
|---|---|---|
| `String name`, `int playerId` | valeur | type primitif / Serializable |
| `GameClientCallback cb` | **référence (stub)** | implémente `Remote` : le serveur peut rappeler le client |
| `StartGameInfo` (retour de `join`) | **valeur (copie)** | `Serializable`, envoyé une seule fois (maze 12×12) |
| `DynamicGameModel` (push) | **valeur (copie)** | `Serializable`, à chaque frame (25 Hz) |

`DynamicGameModel` : les sprites Atom contiennent un `ModelPoint` non sérialisable ;
ils voyagent donc sous forme de `SpriteState` (`writeObject`/`readObject` du DTO).

## 5. Séquence

1. `start(listener, cfg)` : le client exporte `CallbackImpl` (référence forte dans un champ).
2. `connect()` : `getRegistry(host, port).lookup("kombla-main")` puis `join(name, callback)` — seul appel bloquant ; la session (stub du callback) et l'id sont créés atomiquement sous le même verrou.
3. Touche → `sendMoveXxx()` → `server.moveXxx(playerId)` (thread pool RMI côté serveur → `engineLock` → moteur).
4. Frame serveur → `sendModelChanged(m)` → `broadcast` dépose le snapshot (le plus récent gagne) → thread `kombla-push` appelle `cb.onModelChanged(m)` pour chaque joueur → `invokeLater` côté moteur client.
5. Échec d'un push (`RemoteException`) → joueur retiré de la table, une ligne de log, les autres continuent.

## 6. Refus et erreurs

- `join` lève `RemoteException("game is full ...")` ou `("bad name")` ; le client la transforme en `RuntimeException` (pas d'autre canal d'erreur dans `MainClientDAO`).
- Commande d'un `playerId` inconnu → `RemoteException("unknown player")`.
- Côté client, une `RemoteException` sur une commande = résultat inconnu (at-most-once) : session marquée morte, **aucun rejeu**.
- `NotBoundException` au JOIN → « no game bound as 'kombla-main' ».

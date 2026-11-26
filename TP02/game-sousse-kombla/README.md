# Kombla — DAL Java RMI (TP02)

1. JDK 8 (`source/target 1.8`), Maven ; `atom-game-engine:1.5.4.2` dans `~/.m2`. Build + tests : `mvn clean package`.
2. HOST : `mvn exec:java` → « Host Game » ; le champ `serverPort` (défaut **1234**) est le **port du registre RMI** (registre embarqué, nom `kombla-main`).
3. JOIN : `mvn exec:java` → « Join Game », `serverAddress` = IP du host, même port. Sans Maven : `java -cp target/classes:target/lib/* net.thevpc.gaming.atom.examples.kombla.KomblaGame`.
4. Deux machines : lancer chaque JVM avec `-Djava.rmi.server.hostname=<son IP routable>` (ex. `mvn exec:java -Djava.rmi.server.hostname=192.168.1.20`) ; ouvrir le port du registre **et** les ports anonymes des objets exportés (ou couper le pare-feu le temps de la démo).
5. Options : `-Dkombla.debug=true`, `-Dkombla.playerName=ada`, `-Djava.rmi.server.logCalls=true`, `-Dsun.rmi.transport.tcp.responseTimeout=5000` (posé par défaut côté host).

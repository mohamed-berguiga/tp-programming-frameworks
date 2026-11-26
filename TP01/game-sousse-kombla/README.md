# Kombla — DAL sockets TCP (TP01)

1. JDK 8 (`source/target 1.8`), Maven ; `atom-game-engine:1.5.4.2` dans `~/.m2`. Build : `mvn clean package` (tests JUnit inclus).
2. HOST (terminal 1) : `mvn exec:java` → choisir « Host Game », port **1234** par défaut (champ `serverPort`), ESPACE/ENTRÉE.
3. JOIN (terminal 2) : `mvn exec:java` → « Join Game », `serverAddress` = IP du host (`localhost` en local), même port, ESPACE.
4. Sans Maven : `java -cp target/classes:target/lib/* net.thevpc.gaming.atom.examples.kombla.KomblaGame`.
5. Options : `-Dkombla.transport=obj` (variante ObjectStreams, sur les deux JVM), `-Dkombla.debug=true` (trace par trame), `-Dkombla.playerName=ada`.

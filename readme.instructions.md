Generiere eine Datei mit dem Namen README.md

Beschreibe die notwendigen Schritte um die Anwendung zu starten.

Zunächst muss eine MongoDB Instanz bereitgestellt werden. Das geschieht am einfachsten mit Docker oder Podman.

Im Rootverzeichnis liegt eine podman-compose.yml Datei, die eine MongoDB Instanz bereitstellt. Diese kann mit dem Befehl `podman-compose up -d` gestartet werden.

Wenn Podman / Docker nicht unter localhost läuft, muss in der [application.yml](./src/main/resources/application.yml) der uri zur MongoDb angepasst werden

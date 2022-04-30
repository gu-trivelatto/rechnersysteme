
RS2 Cgra Runner
======================================

Dieses Projekt dient als Vorlage und Testumgebung für die gpsAcuqisition aus dem Versuch 2 von RS2.

GpsAcquisition Vorlage
--------------------------------------

Im Unterordner `gpsAcquisition` befindet sich eine Vorlage samt Testdaten für eine C Implementierung der GPS-Acqisition. Es handelt sich um ein CMake Projekt, das standardmäßig für RISC-V Embedded Systeme compiliert wird (RV32IMFC Newlib embedded libc von der riscv-gnu-toolchain).

### Implementierung
Die einzige Datei die für eine Implementierung geändert werden sollte, ist die `acquisition.c`.
Hier müssen alle Funktionen implementiert werden. Das Struct kann erweitert werden: die ersten Einträge sind schon Teil des Headers und dürfen nicht verschoben oder geändert werden, es können aber beliebiger weitere Felder an dieses Struct angehängt werden.

### Compilieren für RISC-V

Zum Bauen sollte folgende Befehle im gpsAcquisition-Ordner durchgeführt werden:
```
mkdir build
cd build
cmake ..
make
```
Das Programm wird dann in die `acquisition.rv32imfc.O3.elf` compiliert.
Für die meisten darauf folgenden Änderungen am Projekt genügt es nochmals `make` im build-Ordner auszuführen. Sollte dies aus irgendeinem Grund fehlschlagen, kann das Projekt durch Löschen des build-Ordners und Ausführen der oben genannten Befehle vollständig neu gebaut werden.

### Testen des Programms mit Qemu

Der Emulator `qemu-riscv32` ist in der Lage ein passendes RISC-V System zu emulieren, um das Programm schnell auszuführen und so auf Korrektheit zu prüfen. Der Befehl
```
qemu-riscv32 acquisition.rv32imfc.O3.elf
```
genügt. Zusätzlich kann das acquisition-Programm mit einem optionalen Kommandozeilenargument gestartet werden, das erlaubt andere Testdaten zu nutzen. Die Vorlage beinhaltet bereits 3 Testdatensätze und nutzt standardmäßig Testdatensatz 0. Beispiel für Test 1:
```
qemu-riscv32 acquisition.rv32imfc.O3.elf 1
```

### Debuggen & C-Compiler Optimierungsstufe

Das Programm kann von der Kommandozeile mit GDB gedebugged werden. Der Befehl
```
qemu-riscv32 -g 2040 acquisition.rv32imfc.O3.elf
```
started das Programm im Debugmodus. Qemu erwartet dann auf dem Netzwerkport 2040 die Verbindung von GDB um die Ausführung zu kontrollieren. Mit
```
riscv32-unknown-elf-gdb acquisition.rv32imfc.O3.elf
```
kann GDB gestartet werden. Die Debugdaten werden aus der ELF-Datei ausgelesen.
Auf der GDB-Kommandozeile kann die Verbindung dann mit
```
(gdb) target remote :2040
```
hergestellt werden. Ab hier kann das Programm entweder debugged werden, oder mit dem Befehl `continue` wie regulär ausgeführt werden.

Die Befehle
```
(gdb) kill
```
oder
```
(gdb) disconnect
```
können das Programm entweder beenden oder ohne Debugger weiterlaufen lassen. GDB lässt sich anschließend erneut mit einem anderen Programm verbinden.

Standardmäßig ist die Vorlage bereits für das Level `-O3` und damit das höchste standardmäßige Optimierungslevel von GCC konfiguriert. Debuginfos für die Verwendung von Debuggern wie GDB sind dauerhaft aktiviert. Falls dieses hohe Optimierungslevel das Debuggen zu sehr erschwert, weil viele Codezeilen wegoptimiert und damit im Debugger nicht mehr nachvollzogen werden können, kann auch eine explizite Debugversion des Programms gebaut werden.

Dazu sollte aus dem Vorlageverzeichnis heraus folgendes ausgeführt werden:
```
mkdir build-debug
cd build-debug
cmake .. -DBUILD_TYPE=debug
make
```
Das Programm heißt dann entsprechend dem verwendeten Optimierungslevel `acquisition.rv32imfc.Og.elf`. Es läuft typischerweise deutlich langsamer, das Programm hält sich aber wesentlich dichter an den C-Code.

### Entwickeln mit IDE

Das CMake Projekt eignet sich um mit verbreiteten C-IDEs die CMake verstehen geöffnet zu werden.
Jetbrains CLion Beispielsweise ist für Studierende kostenlos erhältlich und kann das Projekt öffnen. (Compiler etc muss evtl zusätzlich in der IDE konfiguriert werden).



rs2runner
-------------------------------------

Hier befindet sich der Code um den Simulator des Fachgebiets RS zu nutzen, der RISC-V Prozessoren ähnlich wie Qemu simulieren kann, allerdings realistische Latenzen, Timings etc. berücksichtigt und so Taktanzahlen und verbrauchte Energie angeben kann. Auch ist die Beschleunigung mittels CGRA in diesen Simulator integriert. 

### Programm-Ausführung mit IDE

Das Gradle Projekt im Hauptordner (cgra-runner) ist eine Gradle Projekt, das von populären Java IDEs verstanden wird. Wir empfehlen die Opensource IDE IntelliJ IDEA Community oder die für Studierende kostenlos erhältliche IntelliJ IDEA Ultimate.

Nach dem Öffnen des Projekts in IntelliJ kann die Main-Funktion in `rs2runner/src/main/kotlin/rs/rs2/cgra/app/main.kt` mit der IDE ausgeführt werden. Diese bietet viele Kommandozeilen Optionen um vielseitig nutzbar zu sein.

### Programm-Ausführung auf der Kommandozeile

Der rs2Runner kann auch ohen IDE von der Kommandozeile ausgeführt werden. Entweder mit dem Befehl
```
./gradlew rs2runner:run --args "[kommandozeilen argumente]"
```
oder durch Installation des Runners mit
```
./gradlew install
r2runner/build/install/rs2Runner/bin/rs2Runner [kommandozeilen argumente]
```
Hierbei muss berücksichtigt werden, dass `./gradlew install` den Runner nur einmal baut. Bei jeder Änderungen im Quellcode muss diese Installation erneut durchgeführt werden um auf den aktuellen Stand gebracht zu werden. Die Ausführung durch Gradle baut alle benötigten Bestandteile neu wenn erforderlich, ist aber durch die notwendigen Anführungszeichen evtl unpraktischer. Auch führt Gradle den rs2Runner immer im rs2Runner Unterverzeichnis aus und lässt sich nur aus dem Hauptordner heraus starten. Die Installierte Variante ist hier flexibler.

### Kommandozeilen-Optionen von rs2Runner

Der rs2Runner verfügt über 2 Modi mit vielen einzelnen Optionen. Alle Optionen können mit dem Argument *-h* oder *--help* auf der Kommandozeile gelistet werden.

#### Speedup-Modus

```
rs2Runner speedup [pfad/zu/acquisition.elf]
```
Führt das angegebene Programm 2 mal aus. Einmal ohne CGRA um Schleifen mit hoher Rechenlast und die Referenzzahlen für Taktzahl und Energieverbrauch zu messen und anschließend mit automatischer Beschleunigung mittels CGRA. Abschließend wird der Geschwindigkeitszuwachs angegeben, sowie Statistiken zur CGRA-Nutzung. Standardmäißg der der `PerformanceFocused`-CGRA mit dem Namen `performance` verwendet. Mit dem Argument `--cgra [name]` können aber beliebige andere Konfiguration gewählt werden.

Kommandozeilenargumente für das Acquisition-Programm können wie mit Qemu direkt hinter dem ELF-Dateinamen angehängt werden.

#### Simulate-Modus
Der Runner verfügt noch über einen einfacheren Modus, der das Programm ähnlich wie Qemu nur einmal ausführt. Wenn man experimentieren will, wie sich Änderungen in C auf die Geschwindigkeit auswirken, oder ob sie unterstützt werden kann hiermit nur den gewünschten Teil manuell simulieren.

Eine Simulation ohne CGRA ist beispielsweise mit 
```
rs2runner simulate [pfad/zu/acquisition.elf]
```
möglich.

Eine Simulation mit CGRA geht auch. Hierfür müssen allerdings die mit CGRA zu beschleunigenden Kernel manuell angegeben werden, da bei einer einzelnen Ausführung keine Messungen für die automatische Beschleunigung existieren. 'speedup' gibt alle Kernel an, die genutzt werden, die Namen sind auch für 'simulate' gültig. Es handelt sich hierbei typischerweise um die die Funktionsnamen aus dem C-Code. Aber Byte-Adressen aus dem Code werden auch akzeptiert. Eine einzelne Adresse muss dabei einem Funktionseinstiegspunkt entsprechen, Bereiche der Form `name:0x1200-0x1400` Beschreiben explizit Code-Abschnitte, die flexibler sind, aber schlechter unterstützt werden, als ganze C-Funktionen.
```
rs2runner simulate --cgra performance --kernel startAcquisition --aot [pfad/zu/acquisition.elf].
```
Es können beliebig viele Kernel jeweils mit `--kernel [kernelName]` genannt werden. `--aot` weist den Simulator an diese Kernel auch sofort zu nutzen, ohne zuvor Analysedaten zur Rechenzeit erhalten zu haben.

Für Programme die im Praktikum erwartet werden, sollte `rs2runner simulate` ähnlich wie Qemu funktionieren. Das Debuggen mit GDB wird beispielsweise genauso wie mit Qemu mit der Option `-g [portNummer]` unterstützt. Weitere Optionen wie Beispielsweise der detaillierte `--energy-report` können der eingebauten Hilfe des runners entnommen werden.

### Tests ausführen
In `rs2runner/src/test/kotlin/rs/rs2/cgra/GpsAcquisitionTests.kt` befinden sich tests die eine Speedup-Simulation ähnlich wie auf der Kommandozeile ausführen. Hier ist der das auszuführende Programm mit `gpsAcquisition/build/acquisition.rv32imfc.O3.elf` fest eingestellt und auch die CGRA-Konfigurationen sind fest auf `EnergyFocused` und `PerformanceFocused` eingestellt, wie  auch in der Abgabe verlangt. Diese Tests sollten also für eine gültige Abgabe ohne Änderungen an der Testklasse erfolgreich durchlaufen.


### CGRA-Konfigurationen

Im Ordner `rs2runner/src/main/kotlin/rs/rs2/cgraConfigurations` befinden sich die verschiedenen CGRA-Konfigurationen. *EnergyFocused* und *PerformanceFocused* sind für sie zum Ändern vorgesehen und enthalten jeweils Kommentare, die erklären welche Optionen es gibt. Die weiteren Konfigurationen enthalten andere Standardvorlagen oder zeigen Beispielhaft, wie man spezifische Effekte wie irreguläre Interconnects nutzen kann. 

Jeder CGRA-Konfiguration enthält einen eindeutigen Namen in `name`, der dem Namen auf der Kommandozeile entspricht. Sollten sie neue Konfigurationen hinzufügen, anstatt existierende zu ändern, müssen diese in `rs2runner/src/main/resources/META-INF/services/de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider` hinzugefügt werden. Nur hier erwähnte Konfigurationen werden auf der Kommandozeile erkannt.

Für die Abgabe dürfen aber **ausschließlich** EnergyFocused und PerformanceFocused mit unverändertem Namen verwendet werden, damit sich alle Abgaben auf die gleiche weise öffnen lassen.


### Optimierungskonfigurationen

In der Datei `rs2runner/src/main/kotlin/rs/rs2/optConfig/cfgOptConfig.kt` sind alle für die Optimnierung relevanten Einstellungen zusammengefasst. Hier können einzelne C-Funktionen von der Beschleunigung ausgeschlossen werden, oder das *Unrolling*, das zu einem großen Teil für die Beschleunigung verantwortlich ist konfiguriert werden. Die vorhandenen Optionen sind in Javadoc dokumentiert (In IntelliJ standardmäßig mit `Ctrl+Q` erreichbar).


Abgabe
-----------------------------

Die Abgabe wird im Hauptordner mit `./gradlew abgabe` erzeugt. Die Abgabe enthält keine Dateien, die sie nicht für eine gültige Abgabe ändern dürfen.

Im C-Code darf nur die acquisition.c und CMakeLists.txt für etwaige Compiler-Optionen und zusätzliche Dateien geändert werden. Wenn sie zusätzliche Dateien schreiben werden diese auch mit abgegeben.

Im rs2Runner sind nur die beiden CGRA-Konfigurationen EnergyFocused.kt und PerformanceFocuses.kt und die Optimierungskonfiguration (cfgOptConfig.kt) enthalten, da nur diese Dateien geändert werden sollen. Die Klassennamen und Namen der CGRA-Konfigurationen dürfen nicht geändert werden, damit die ausgelieferten Tests weiterhin funktionieren.
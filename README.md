
RS2 Cgra Runner
======================================

Dieses Projekt dient als Vorlage und Testumgebung für die gpsAcuqisition aus dem Versuch 2 von RS2.

Voraussetzungen
--------------------------------------

Um den C-Code zu kompilieren wird Make, CMake und die RISC-V GNU Toolchain mit Newlib und RV32IMFC Support benötigt. Die ist auf allen RS-Rechnern bereits vorinstalliert (Der Server per X2Go. Die Rechner in Raum 350 sind leider zu alt für diese Software) und geladen und kann auf sonstigen Rechnersysteme Accounts mit dem Befehl `module load gcc/rv32imfc/2022.04.12` geladen werden.

Für das Gradle Projekt wird mindestens ein JDK ab Java 8 benötigt um Gradle auszuführen. Der eigentliche Code wird aber mit Java 17 kompiliert und ausgeführt. 
Ein solches kann auf unseres PCs mit dem Befehl `module load jdk/OpenJDK17` geladen werden. Wenn Gradle kein JDK 17 auf dem PC findet, wird es selbst eines herunterladen. Alle anderen notwendigen Libraries werden auch von Gradle verwaltet.


GpsAcquisition Vorlage
--------------------------------------

Im Unterordner `gpsAcquisition` befindet sich eine Vorlage samt Testdaten für eine C Implementierung der GPS-Acqisition. Es handelt sich um ein CMake Projekt, das standardmäßig für RISC-V Embedded Systeme compiliert wird (RV32IMFC Newlib embedded libc von der riscv-gnu-toolchain).

### Implementierung
Die einzige Datei die für eine Implementierung geändert werden sollte, ist die `acquisition.c`.
Hier müssen alle Funktionen implementiert werden. Das Struct kann erweitert werden: die ersten Einträge sind schon Teil des Headers und dürfen nicht verschoben oder geändert werden, es können aber beliebiger weitere Felder an dieses Struct angehängt werden.

### Compilieren für RISC-V

Zum Bauen sollte folgende Befehle im gpsAcquisition-Ordner durchgeführt werden:
```
cmake -B build
cmake --build build
```
Das Programm wird dann in die `acquisition.rv32imfc.O3.elf` compiliert.
Für die meisten darauf folgenden Änderungen am Projekt genügt es `cmake --build build` auszuführen, oder innerhalb des erstellten build-Ordners `make`. Sollte dies aus irgendeinem Grund fehlschlagen, kann das Projekt durch Löschen des build-Ordners und Ausführen der oben genannten Befehle vollständig neu gebaut werden.

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
r2runner/build/install/rs2Runner/bin/rs2runner [kommandozeilen argumente]
```
Hierbei muss berücksichtigt werden, dass `./gradlew install` den Runner nur einmal baut. Bei jeder Änderungen an der CGRA- oder Optimierungskonfiguration im rs2runner-Ordner muss diese Installation erneut durchgeführt werden, um auf den aktuellen Stand gebracht zu werden. Die Ausführung durch Gradle baut alle benötigten Bestandteile neu wenn erforderlich, ist aber durch die notwendigen Anführungszeichen evtl unpraktischer. Auch führt Gradle den rs2Runner immer im rs2Runner Unterverzeichnis aus und lässt sich nur aus dem Hauptordner heraus starten. Die Installierte Variante ist hier flexibler.

### Kommandozeilen-Optionen von rs2Runner

Der rs2runner verfügt über 2 Modi mit vielen einzelnen Optionen. Alle Optionen können mit dem Argument *-h* oder *--help* auf der Kommandozeile gelistet werden.

#### Speedup-Modus

```
rs2runner speedup [pfad/zu/acquisition.elf]
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

Eine Simulation mit CGRA geht auch. Hierfür müssen allerdings die mit CGRA zu beschleunigenden Kernel manuell angegeben werden, da bei einer einzelnen Ausführung keine Messungen für die automatische Beschleunigung existieren. 'speedup' gibt alle Kernel an, die genutzt werden, die Namen sind auch für 'simulate' gültig. Es handelt sich hierbei typischerweise um die Funktionsnamen aus dem C-Code. Aber Byte-Adressen aus dem Code werden auch akzeptiert. Eine einzelne Adresse muss dabei einem Funktionseinstiegspunkt entsprechen, Bereiche der Form `name:0x1200-0x1400` Beschreiben explizit Code-Abschnitte, die flexibler sind, aber schlechter unterstützt werden, als ganze C-Funktionen.
```
rs2runner simulate --cgra performance --kernel startAcquisition --aot [pfad/zu/acquisition.elf].
```
Es können beliebig viele Kernel jeweils mit `--kernel [kernelName]` genannt werden. `--aot` weist den Simulator an diese Kernel auch sofort zu nutzen, ohne zuvor Analysedaten zur Rechenzeit erhalten zu haben.

Für Programme die im Praktikum erwartet werden, sollte `rs2runner simulate` ähnlich wie Qemu funktionieren, nur langsamer. Das Debuggen mit GDB wird beispielsweise genauso wie mit Qemu mit der Option `-g [portNummer]` unterstützt. Weitere Optionen wie Beispielsweise der detaillierte `--energy-report` können der eingebauten Hilfe des runners entnommen werden.

### Tests ausführen
In `rs2runner/src/test/kotlin/rs/rs2/cgra/GpsAcquisitionTests.kt` befinden sich tests die eine Speedup-Simulation ähnlich wie auf der Kommandozeile ausführen. Hier ist der das auszuführende Programm mit `gpsAcquisition/build/acquisition.rv32imfc.O3.elf` fest eingestellt und auch die CGRA-Konfigurationen sind fest auf `EnergyFocused` und `PerformanceFocused` eingestellt, wie  auch in der Abgabe verlangt. Diese Tests sollten also für eine gültige Abgabe ohne Änderungen an der Testklasse erfolgreich durchlaufen.


### CGRA-Konfigurationen

Im Ordner `rs2runner/src/main/kotlin/rs/rs2/cgraConfigurations` befinden sich die verschiedenen CGRA-Konfigurationen. *EnergyFocused* und *PerformanceFocused* sind für sie zum Ändern vorgesehen und enthalten jeweils Kommentare, die erklären welche Optionen es gibt. Die weiteren Konfigurationen enthalten andere Standardvorlagen oder zeigen beispielhaft, wie man spezifische Effekte wie irreguläre Interconnects nutzen kann. 

Jeder CGRA-Konfiguration enthält einen eindeutigen Namen in `name`, der dem Namen auf der Kommandozeile entspricht. Sollten sie neue Konfigurationen hinzufügen, anstatt existierende zu ändern, müssen diese in `rs2runner/src/main/resources/META-INF/services/de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider` hinzugefügt werden. Nur hier erwähnte Konfigurationen werden auf der Kommandozeile erkannt.

Für die Abgabe dürfen aber **ausschließlich** EnergyFocused und PerformanceFocused mit unverändertem Namen verwendet werden, damit sich alle Abgaben auf die gleiche weise öffnen lassen.

### Limits der CGRA-Konfiguration
Im Grunde können eigentlich alle Optionen genutzt werden, die geboten werden. Limitiert sind:

 * ContextMemory: Wenn sie diese erhöhen müssen, begründen Sie in Ihrer Ausarbeitung, dass die BlockRAM Speicheraustattung eines Xilinx Zynq Z-7045 aureichen würde für ihre Konfiguration. Für die Schätzung: Der Kontext für ein PE passt etwa in
   ein long - also 8 Byte. Macht bei dem von uns Vorgegebenen CGRA mit 16 PEs und 4096
   Kontexteinträgen 8*16*4096 = 0,5 MB. Der Z-7045 hat 545 BRAMs mit je 36Kb. Wenn man davon die BRAMS für Caches, Prozessor, etc. abzieht bleiben noch 509 BRAMs übrig. Das macht in Summe ca 2MB.
 * Speicherports: Für den CGRA sind maximal 4 PEs mit Speicherzugriff erlaubt. Mehr wäre auf einem FPGA nicht realistisch umzusetzen mit der verwendeten Cache-Kohärenten Anbindung.
 * Cache-Größen und Prozessorkonfiguration. Diese dürfen nicht verändert werden.
 * Konfiguration außerhalb von `cfgOptConfig.kt`, `EnergyFocused.kt` und `PerformanceFocues.kt` sollte allgemein nicht geändert werden, da nur diese 3 Dateien abgegeben werden. Änderen Sie andere Sachen, ist ihr Ergebniss nicht reproduzierbar und wird nicht akzeptiert.


### Optimierungskonfigurationen

In der Datei `rs2runner/src/main/kotlin/rs/rs2/optConfig/cfgOptConfig.kt` sind alle für die Optimnierung relevanten Einstellungen zusammengefasst. Hier können einzelne C-Funktionen von der Beschleunigung ausgeschlossen werden, oder das *Unrolling*, das zu einem großen Teil für die Beschleunigung verantwortlich ist konfiguriert werden. Die vorhandenen Optionen sind in Javadoc dokumentiert (In IntelliJ standardmäßig mit `Ctrl+Q` erreichbar).


Abgabe
-----------------------------

Die Abgabe wird im Hauptordner mit `./gradlew abgabe` erzeugt. Die Abgabe enthält keine Dateien, die sie nicht für eine gültige Abgabe ändern dürfen.

Im C-Code darf nur die acquisition.c und CMakeLists.txt für etwaige Compiler-Optionen und zusätzliche Dateien geändert werden. Wenn sie zusätzliche Dateien schreiben werden diese auch mit abgegeben.

Im rs2runner sind nur die beiden CGRA-Konfigurationen EnergyFocused.kt und PerformanceFocuses.kt und die Optimierungskonfiguration (cfgOptConfig.kt) enthalten, da nur diese Dateien geändert werden sollen. Die Klassennamen und Namen der CGRA-Konfigurationen dürfen nicht geändert werden, damit die ausgelieferten Tests weiterhin funktionieren.


Eigene Compiler Toolchain bauen
------------------------------
Um den Compiler auf anderen Rechnern verwenden zu können. Auf RS Rechnern nicht nötig.

Um die Toolchain aus dem Praktikum selbst zu bauen:
https://github.com/riscv-collab/riscv-gnu-toolchain auschecken auf Tag '2022.04.12'.
`git submodule update --init --recursive` ausführen.

Im submodule riscv-gdb manuell branch 'riscv-binutils-2.38' auschecken, ältere Versionen sind nicht kompatibel.

Prerequisites nach Anleitung im Git installieren.

```
./configure --prefix=/opt/rv32imfc --with-arch=rv32imfc --with-abi=ilp32f
```
konfiguriert die Toolchain für die im Praktikum mindestens notwendige Architektur. Unsere Toolchain ist allerdings noch für weitere Architekturen gebaut, der Befehl

```
./configure --prefix=/opt/rv32imfc --with-arch=rv32imfc --with-abi=ilp32f --with-multilib-generator="rv32im-ilp32--;rv32imc-ilp32--;rv32imf-ilp32f--;rv32imfc-ilp32f--"
```
konfiguriert die Toolchain exakt wie im Praktikum.

Mit
```
make newlib -j [thread-anzahl]
```
wird dann anschließend alles kompiliert und in den Zielpfad `/opt/rv32imfc` installiert.
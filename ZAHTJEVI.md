# Matrica zahtjeva — specifikacija → implementacija

Izvor: `PJ2 - projektni zadatak - maj 2026.pdf` + `dodatna_pojasnjenja.txt`
Stanje: 5. avgust 2026, poslije R0 + R2 + R1 + R5 + čišćenja S1–S4/S6 + C6 (`PrikazTerminala`) + C2 (`GeneratorPlovila`).
Test paket: 109 ukupno, 1 pad (`sudarUkljucujeDvaPlovila`, čeka R4), 0 ignorisano (F2 riješen).
Poznata povremena nestabilnost (nevezano za današnji rad): `BrodThreadTest.ploviloPodRotacijomZavrsavaSimulaciju`
je vremenski osjetljiv integracioni test (pravе niti + `Thread.sleep`) i rijetko (~1 od 5 pokretanja
u lokalnom mjerenju) ne stigne da priveže svih 6 plovila u 40s. Nije popravljeno danas — van obima.

Legenda: **DONE** gotovo i pokriveno testom · **PART** djelimično · **TODO** nije započeto · **RISK** namjerno odstupanje, mora se vratiti

---

## Model plovila

| # | Zahtjev | Status | Gdje |
|---|---|---|---|
| M1 | Naziv, IMO, broj motora, fotografija, registarski broj | DONE | `Plovilo` |
| M2 | Kontejnerski (TEU), kruzer (putnici), tanker (bareli) | DONE | 3 podklase |
| M3 | Kombinacije: kont→OS; kruzer→OS,carina; tanker→OS,carina,vatrogasci | DONE | 6 podklasa |
| M4 | Rotacija na državnim plovilima | DONE | `SluzbenoPlovilo` (**R1**), `ObalskaStraza`/`Carina`/`Vatrogasci` ga nasljeđuju |
| M5 | Prioritet vatrogasci > obalska straža > carina | DONE | `getPrioritet()` se čita u `BrodThread.ploviIstocno()` (**R5**) |
| M6 | Obalska straža nosi fajl sa IMO brojevima za potjernicom | PART | polje postoji, sadržaj se ne čita |
| M7 | Jedinstvena slučajna brzina | DONE | `Plovilo`, test |

## Terminal i kretanje

| # | Zahtjev | Status | Gdje |
|---|---|---|---|
| T1 | Broj terminala iz properties fajla | TODO | `luka.properties` postoji, niko ga ne čita |
| T2 | Oblik terminala 4×17, 30 dokova | DONE | `Terminal`, test |
| T3 | Plovidba desnom stranom kanala | DONE | R0, red 2 istočno / red 1 zapadno |
| T4 | Preticanje preko jednog polja lijevo, ako nema suprotnog smjera | DONE | `smijePreticati()` |
| T5 | Nikad dva plovila na istom polju | DONE | `pomjeriNaPolje()` pod ključem, test |
| T6 | Evidencija slobodnih vezova po terminalu | DONE | `getBrojSlobodnihVezova()` |
| T7 | Ako su svi terminali puni — nema ulaza u luku | DONE | petlja u `run()` |
| T8 | Pun terminal — plovilo ide pravo na naredni | DONE | rezervacija prije ulaska |
| T9 | Slobodan dok — ulazak i kružni prolazak | DONE | ruta kanal → dok |
| T10 | Svaki brod je nit | DONE | `BrodThread implements Runnable` |
| T11 | Simulacija ni prebrza ni prespora | DONE | `trajanjeKoraka()` 20–400ms |

## Administratorska aplikacija

| # | Zahtjev | Status |
|---|---|---|
| A1 | GUI administratorska aplikacija | TODO |
| A2 | Padajući meni sa svim tipovima plovila | TODO |
| A3 | Dugme za dodavanje plovila | TODO |
| A4 | Tabelarni prikaz plovila po terminalu | TODO |
| A5 | Dugme za pokretanje korisničke aplikacije | TODO |
| A6 | Prazna tabela pri prvom pokretanju | TODO |
| A7 | **Jedna** forma, polja se kreiraju dinamički po tipu | TODO |
| A8 | FileDialog / JavaFX ekvivalent za fajlove | TODO |
| A9 | Automatsko osvježavanje tabele nakon dodavanja | TODO |
| A10 | Kombo boks za izbor terminala mijenja tabelu | TODO |
| A11 | Izmjena i brisanje plovila | TODO |
| A12 | Serijalizacija u `luka.ser` prije pokretanja klijenta | PART — `SerializationUtil` radi, nije pozvan iz GUI-ja |
| A13 | Deserijalizacija pri pokretanju admin dijela | PART — isto |

## Korisnička aplikacija

| # | Zahtjev | Status |
|---|---|---|
| C1 | Korisnik zadaje minimalan broj plovila **po terminalu** | TODO |
| C2 | Slučajan tip, 90% komercijalna | DONE — `util.GeneratorPlovila.generisiSlucajno()`/`(Random)`, testovi |
| C3 | Prvo se postavljaju plovila iz `luka.ser` na slučajne dokove | TODO |
| C4 | Dopuna slučajnim plovilima do minimuma | TODO |
| C5 | Prikaz terminala, izbor kombo boksom | TODO |
| C6 | Prazan dok `*`, slovo po tipu, `R` za rotaciju | DONE — `view.PrikazTerminala.render()`/`renderAsText()`, testovi |
| C7 | 15% plovila po terminalu odlazi iz luke | TODO |
| C8 | Dodavanje plovila tokom simulacije ako luka nije puna | TODO |
| C9 | Novo plovilo kreće od ulaza ka prvom slobodnom doku | PART — ruta postoji u `BrodThread` |

## Incidenti

| # | Zahtjev | Status |
|---|---|---|
| I1 | 2% sudara pri mimoilaženju | **RISK** — `SUDARI_OMOGUCENI = false`, mora se vratiti u R4 |
| I2 | Najbliža obalska straža, carina i vatrogasci pod rotacijom | TODO |
| I3 | Blokada saobraćaja na terminalu, uviđaj 3–10s | TODO |
| I4 | Ostali terminali rade normalno | TODO |
| I5 | Potjernica: pratnja ka izlazu, uviđaj 3–5s, saobraćaj radi | TODO |
| I6 | Evidencija: učesnici, vrijeme, fotografije | TODO |
| I7 | Binarni fajl po slučaju, u `user.home` | TODO |
| I8 | Učesnici napuštaju terminal poslije uviđaja | TODO |

## Takse i završetak

| # | Zahtjev | Status |
|---|---|---|
| F1 | Evidencija ulaska po IMO broju i vremenu | DONE — `Luka`, poziv iz `BrodThread` |
| F2 | 100 KM/sat, do 12h 1000 KM, do 24h 2000 KM | DONE — `PokretacIzvjestaja`, testovi |
| F3 | Državna plovila ne plaćaju | DONE — test |
| F4 | CSV izvoz | PART — piše, nije pozvan pri izlasku broda |
| E1 | Kraj kad odabrana plovila izađu i klijentska se privežu | TODO |
| E2 | Ponovna serijalizacija u `luka.ser` | TODO |
| E3 | Svi izuzeci u `error.log` preko `Logger` | DONE |

---

## Otvorena odstupanja koja se moraju zatvoriti

1. **I1 — sudari isključeni.** `BrodThread.SUDARI_OMOGUCENI = false`. Vratiti na `true` u R4.
2. ~~**F2 — zaokruživanje.** `Duration.toHours()` reže naniže, pa 90 min = 100 KM.~~ Riješeno, korištena Math.ceil() metoda i princip "plafona" za računanje tarife.
3. **M6 — spisak potjernica se ne čita.** Fajl se čuva, sadržaj se nikad ne parsira.
4. **T1 — properties se ne čita.** Broj terminala je trenutno hardkodovan kroz `TestFactory`.

## Redoslijed preostalog rada

~~R1 + R5 (interfejs + prioritet)~~ **gotovo 4. avgusta** → T1 (properties) → A* (admin GUI) →
C* (klijent GUI + prikaz) → C7/E1/E2 (odlazak i kraj) → F4 (CSV na izlazu) → **R4 (incidenti)**

R4 je najveći pojedinačni blok i ima najviše nezatvorenih zahtjeva (I1–I8).

## Riješeno 4. avgusta: čišćenje preostalih padova (S1–S4, S6)

Prema `CISCENJE_I_R4_PRIPREMA.md`: dodati `equals`/`hashCode` u `Plovilo` (IMO ključ),
imenovane konstante `PRIORITET_POD_ROTACIJOM` u šest službenih klasa (razrješava
kontradikciju iz prošle sesije — vidi "Riješeno" u `PRONALASCI.md`), `Luka.evidencijaUlaska`
sada `ConcurrentHashMap`, obrisana mrtva mapa `brojSlobodnihVezova`, `serialVersionUID`
dodat u `Luka` i `Polje`, CSV izvoz sada RFC 4180 escape-uje IMO/naziv/tip i piše iznos sa
`Locale.US`. Test paket pao sa 94 na 93 (obrisan test za mrtvu mapu), sa 7 padova na 1
(`sudarUkljucujeDvaPlovila`, čeka R4).

Preostalo van ove runde: **S5/R3** (hardkodovane putanje `luka.ser`/`takse.csv`) — nije
bio dio `CISCENJE_I_R4_PRIPREMA.md`, i dalje otvoren.

## Riješeno 4. avgusta: C6 (`PrikazTerminala`)

Novi paket `view` (van `simulation` — nema ulogu u nitima): `PrikazTerminala.render(Terminal)`
vraća snimak matrice 4x17 kao `String[][]` (zaključan sa `synchronized (terminal)` da ne
uhvati polovičan potez), `renderAsText(Terminal)` isto formatira za konzolu. Identitet službe
pobjeđuje tip trupa — provjera ide `Vatrogasci` → `ObalskaStraza` → `Carina` (kroz
`SluzbenoPlovilo`/markerske interfejse iz R1) prije pada na tip trupa (`K`/`P`/`T`), pa
`TankerVatrogasci` pod rotacijom ispisuje `VR`, ne `T`. 11 novih determinističkih testova
bez niti/tajmauta u `PrikazTerminalaTest`. `C5` (prikaz terminala u GUI-ju, izbor kombo
boksom) ostaje TODO — ovo je samo model→tekst transformacija koju GUI (C5) tek treba pozvati.

## Riješeno 5. avgusta: C2 (`GeneratorPlovila`)

`util.GeneratorPlovila.generisiSlucajno()` / `generisiSlucajno(Random)` (C2). Komercijalno/
državno se baca **prvo i nezavisno** (90/10 tačno po konstrukciji), tek onda se unutar državne
grane bira služba pa dozvoljeni trup za tu službu (M3: vatrogasci samo tanker; obalska straža
kontejnerski/kruzer/tanker; carina kruzer/tanker) — suprotan redoslijed bi mogao proizvesti
nepostojeću kombinaciju (npr. vatrogasni kruzer). IMO brojevi jedinstveni preko `AtomicInteger`
brojača (7 cifara), nezavisno od predatog `Random`-a — ponovljivost seed-a pokriva samo izbor
tipa/naziva/brojčanih atributa, ne i IMO (koji po prirodi mora biti jedinstven, ne reproduktivan).

**Odluka o raspodjeli unutar državnih 10%** (spec ne propisuje omjer): obalska straža 50%,
carina 25%, vatrogasci 25% — namjerno naklonjeno obalskoj straži jer jedino ona nosi spisak
potjernica (M6), pa veći udio povećava šansu da se scenario potjere (I5) stvarno pojavi tokom
demonstracije. Sve tri konstante (`UDIO_KOMERCIJALNIH`, `UDIO_OBALSKA_STRAZA`, `UDIO_CARINA`)
su javne radi transparentnosti i eventualne izmjene.

**Bug uhvaćen prije commit-a:** prva verzija je čitala IMO brojač DRUGI PUT (za motor/registarski
broj) nakon što ga je `sledeciImo()` već inkrementirao — motor/registarski bi time referencirali
sljedeći, ne trenutni IMO. Ispravljeno hvatanjem `imo` u lokalnu varijablu jednom po plovilu i
izvođenjem motora/registarskog iz nje, umjesto ponovnog čitanja dijeljenog brojača.

5 novih determinističkih testova (`GeneratorPlovilaTest`, fiksni seed, 10.000 uzoraka gdje je
bitna statistika): udio komercijalnih u [0.88, 0.92], svaka državna kombinacija dozvoljena,
nema dupliranih IMO na 10.000, IMO je sedmocifren, isti seed daje identičnu flotu (tip/IMO/
naziv/prioritet) pri dva odvojena poziva (uz reset test-only brojača preko package-private
`resetujImoBrojacZaTest`).

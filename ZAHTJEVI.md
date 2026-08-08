# Matrica zahtjeva — specifikacija → implementacija

Izvor: `PJ2 - projektni zadatak - maj 2026.pdf` + `dodatna_pojasnjenja.txt`
Stanje: 8. avgust 2026, poslije R0 + R2 + R1 + R5 + čišćenja S1–S4/S6 + C6 (`PrikazTerminala`) + C2 (`GeneratorPlovila`) + code review ispravke na C2 + T1/C1/C3/C4 (`PokretacSimulacije`) + `Zadatak`/parkiranje + O1 + D5 (determinizam sudara, priprema za R4) + R4a (infrastruktura za sistem incidenata — `Incident`, blokada saobraćaja na terminalu, `PretragaPatrole`).
Test paket: 165 ukupno, 1 pad (`sudarUkljucujeDvaPlovila`, čeka R4b), 0 ignorisano (F2 riješen).
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
| T1 | Broj terminala iz properties fajla | DONE | `PokretacSimulacije.pripremiPocetnoStanje(int)` čita `PropertiesUtil.getBrojTerminala()` |
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

| # | Zahtjev                                                                                                                                         | Status |
|---|-------------------------------------------------------------------------------------------------------------------------------------------------|---|
| C1 | Korisnik zadaje minimalan broj plovila **po terminalu**                                                                                         | DONE — `PokretacSimulacije.pripremiPocetnoStanje(int minimumPoTerminalu)` uzima taj broj kao parametar; poziv iz GUI-ja (unos vrijednosti) je dio C5, još TODO |
| C2 | Slučajan tip, 90% komercijalna                                                                                                                  | DONE — `util.GeneratorPlovila.generisiSlucajno()`/`(Random)`, testovi |
| C3 | Prvo se postavljaju plovila iz `luka.ser` na slučajne dokove                                                                                    | DONE — `simulation.PokretacSimulacije`, testovi |
| C4 | Dopuna slučajnim plovilima do minimuma                                                                                                          | DONE — isto, testovi |
| C5 | Prikaz terminala, izbor kombo boksom                                                                                                            | TODO |
| C6 | Prazan dok `*`, slovo po tipu, `R` za rotaciju                                                                                                  | DONE — `view.PrikazTerminala.render()`/`renderAsText()`, testovi |
| C7 | 15% plovila po terminalu odlazi iz luke                                                                                                         | TODO |
| C8 | Dodavanje plovila tokom simulacije ako luka nije puna, MORA KORISTITI TERMINAL.REZERVISISLOBODANDOK() I DRZATI TERMINAL LOCK, NE SETUP HELPERE! | TODO |
| C9 | Novo plovilo kreće od ulaza ka prvom slobodnom doku                                                                                             | PART — ruta postoji u `BrodThread` |

## Incidenti

| # | Zahtjev | Status |
|---|---|---|
| I1 | 2% sudara pri mimoilaženju | **RISK** — `SUDARI_OMOGUCENI = false`, mora se vratiti u R4b |
| I2 | Najbliža obalska straža, carina i vatrogasci pod rotacijom | PART — `simulation.PretragaPatrole.najblizaPatrola()` (D2) postoji i testirana, port-wide pretraga preko `Luka.getAktivnaPlovila()`; još je niko ne poziva iz dispečovanja (R4b) |
| I3 | Blokada saobraćaja na terminalu, uviđaj 3–10s | PART — `Terminal.blokirajSaobracaj()`/`odblokirajSaobracaj()`/`smijeProci()` (D4) i provjera u `BrodThread.pomjeriNaPolje()` postoje i testirani; još niko ne poziva blokadu niti mjeri trajanje uviđaja (R4b) |
| I4 | Ostali terminali rade normalno | PART — blokada je po instanci `Terminal`-a (I3 infra), pa je ovo strukturno zagarantovano čim se I3 poveže; nema još stvarnog incidenta koji bi to demonstrirao |
| I5 | Potjernica: pratnja ka izlazu, uviđaj 3–5s, saobraćaj radi | TODO |
| I6 | Evidencija: učesnici, vrijeme, fotografije | PART — `simulation.Incident` (učesnici sudara, odazvana službena plovila, vrijeme, apsolutne putanje fotografija, trajanje uviđaja, terminal) postoji i testirana; još je niko ne konstruiše iz stvarnog sudara (R4b) |
| I7 | Binarni fajl po slučaju, u `user.home` | PART — `Incident.sacuvaj()`/`sacuvaj(File)`/`ucitaj(File)` implementirani i testirani; još se ne poziva iz toka sudara (R4b) |
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
4. ~~**T1 — properties se ne čita.**~~ Riješeno 5. avgusta — vidi ispod. `TestFactory.luka(n)` i dalje hardkoduje broj terminala, ali to je namjerno (test fabrika), ne proizvodni kod.

## Redoslijed preostalog rada

~~R1 + R5 (interfejs + prioritet)~~ **gotovo 4. avgusta** → ~~T1 (properties) → C1/C3/C4 (harnes)~~
**gotovo 5. avgusta** → ~~R4a (infrastruktura: `Incident`, blokada terminala, `PretragaPatrole`)~~
**gotovo 8. avgusta** → **R4b (detekcija sudara, dispečovanje, prelasci `Zadatak`-a)** → A* (admin
GUI) → C5/C8 (klijent GUI + prikaz + dodavanje tokom rada) → C7/E1/E2 (odlazak i kraj) → F4 (CSV na
izlazu)

R4 (sada R4a+R4b) je najveći pojedinačni blok i ima najviše nezatvorenih zahtjeva (I1–I8).
`Zadatak`/parkiranje i `Luka.aktivnaPlovila` su urađeni unaprijed 5. avgusta baš zbog R4, a R4a
(8. avgust) je dodao preostalu infrastrukturu (`Incident`, blokada terminala, `PretragaPatrole`) —
R4b sutra treba samo da poveže te komade u stvarnu logiku uviđaja, bez mijenjanja životnog ciklusa
`BrodThread`-a usred posla.

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

## Riješeno 5. avgusta: code review ispravke na `GeneratorPlovila` (C2)

Tri nalaza iz code review-a, sva tri stvarni bagovi:

1. **`fotografija` je uvijek bila `null`.** M1 zahtijeva fotografiju na svakom plovilu, a R4
   treba da je čuva u zapisu incidenta. Dodat `resources/placeholder-fotografija.txt`,
   generator sada uvijek postavlja tu putanju umjesto `null`-a. Admin GUI (A8) je kasnije
   zamjenjuje pravom fotografijom preko `FileDialog`-a.
2. **`spisakPotjera` je uvijek bila `null` za obalsku stražu.** Tiho poništava cijelu poentu
   naginjanja ka obalskoj straži (50% državnih) — I5 (potjernica) nema šta da čita. Dodat
   `resources/spisak-potjera-default.txt` (placeholder sadržaj — stvarno parsiranje i
   poređenje je I5/R4, još nije implementirano).
3. **IMO kolizija sa `luka.ser`.** `SLEDECI_IMO` je statičko polje koje kreće od `1_000_000`
   pri svakom pokretanju JVM-a — isto od čega je kretalo i u prethodnoj sesiji čiji su brodovi
   sada u `luka.ser`. C3/C4 eksplicitno miješaju deserijalizovana i novogenerisana plovila, pa
   bi kolizija bila izvjesna, a `equals()`/`hashCode()` (IMO ključ, S6) bi dva različita broda
   pretvorili u jedan unos u evidenciji ili `HashSet`-u. Dodata `GeneratorPlovila.
   obezbijediJedinstvenostImoZa(Luka)` — skenira cijelu matricu svakog terminala, pomjera
   brojač iznad najvišeg pronađenog IMO broja. Deterministički pristup (ne probabilistički
   pečat vremena) jer jedino deterministički garantuje da kolizije nema. **C3/C4 moraju
   pozvati ovu metodu odmah nakon deserijalizacije, prije prvog `generisiSlucajno()`** — sama
   metoda ne radi ništa dok se ne pozove.

Test `obezbjeduJedinstvenostImoIzbjegavaKolizijuSaPostojecomLukom` simulira upravo taj
scenario: postojeća plovila na dokovima sa IMO brojevima u opsegu koji bi brojač inače
dodijelio, pa 100 novih generacija provjerenih protiv istog skupa.

Usput uhvaćen i compile-time bug u ispravci: `AtomicInteger.updateAndGet()` lambda je
zahtijevala efektivno final varijablu — `maxPostojeci` (loop-akumulator) nije bio, popravljeno
izdvajanjem `minimalniSledeci` kao zasebne final vrijednosti prije poziva.

**Otvorena odluka za R4** (nije bag, treba odlučiti prije R4): pri 10% državnih × 25%
vatrogasci = 2.5% vatrogasnih plovila ukupno. Sa npr. 5 plovila po terminalu na 3 terminala
(15 ukupno), očekivano ~0.4 vatrogasna broda — većina pokretanja simulacije neće imati
nijednog. R4 zahtijeva slanje vatrogasaca na incident. Kad se stigne do C3/C4 (postavljanje
minimalnog broja plovila po terminalu), treba odlučiti: (a) garantovati bar jedno plovilo
svake službe po terminalu pri inicijalnom postavljanju, ili (b) prihvatiti da demonstracije
često neće moći pokazati puni dispatch. Nije riješeno danas.

## Riješeno 5. avgusta: T1/C1/C3/C4 (simulacioni harnes) + priprema za R4

Novi paket-lokalni pandan za pokretanje simulacije: `simulation.PokretacSimulacije`. Wire-uje
`PropertiesUtil.getBrojTerminala()` (T1, do sada niko nije čitao), zatim izvlači zatečenu flotu
iz deserijalizovanog `luka.ser` i raspoređuje je na slučajne dokove nove strukture terminala
(C3), pa dopunjava svaki terminal do korisnički zadatog minimuma slučajno generisanim plovilima
preko `GeneratorPlovila.generisiSlucajno()` (C1/C4). `GeneratorPlovila.obezbijediJedinstvenostImoZa()`
se poziva odmah nakon deserijalizacije, prije prve generacije — bez ovog redoslijeda bi C3/C4
gotovo izvjesno proizveli IMO koliziju (isti opseg brojača kao zatečena flota).

Podijeljeno na dvije javne metode radi testabilnosti: `pripremiPocetnoStanje(int)` je pravi
ulaz (čita `luka.properties` i `luka.ser` sa diska), a `pripremiPocetnoStanje(Luka, int, int, Random)`
je čista varijanta bez I/O-a koju testovi pozivaju direktno — isti obrazac kao
`GeneratorPlovila.generisiSlucajno()` / `generisiSlucajno(Random)`.

**Bitna napomena o rasporedu (C3):** ako se broj terminala između sesija promijenio (T1 se
izmijeni u `luka.properties`), zatečena flota se prenosi na *novu* strukturu terminala, ne na
staru — otud raspoređivanje na slučajan dok umjesto vraćanja na tačno onaj dok na kojem je
plovilo bilo prije. Ako nova struktura ima manje ukupnog kapaciteta od broja zatečenih plovila,
višak se tiho izostavlja uz upozorenje u `error.log` (nema specifikacije šta raditi u tom rubnom
slučaju — kapacitet 30×broj_terminala je u praksi uvijek dovoljan za realne vrijednosti C1).

### Zadatak / PRIVEZAN — BrodThread se više ne gasi kad se plovilo priveže

Ovo je preduslov koji je zahtijevao R4 (D3 iz `PRONALASCI.md`), urađen sada da se ne mora
naknadno mijenjati životni ciklus niti usred pisanja logike uviđaja:

- Novi enum `simulation.Zadatak { KA_DOKU, PRIVEZAN, KA_INCIDENTU, NA_INCIDENTU, NAPUSTA }`.
- `BrodThread.run()` više se ne završava kad `doploviDoDoka()` uspije. Umjesto toga nit uđe u
  `Zadatak.PRIVEZAN` i parkira se u `wait()` na **posebnom `parkLock` objektu, nikad na
  `synchronized(terminal)`** — to je kritično svojstvo iz D4: `PrikazTerminala.render()` uzima
  isti ključ terminala, pa bi `wait()` unutar te sinhronizacije zamrznuo GUI za trajanje
  čekanja. Regresioni test `parkiranoCekanjeNeBlokiraRenderTerminala` direktno provjerava da
  `render()` vrati rezultat u razumnom vremenu dok je brod parkiran.
- `zatraziNapustanje()` budi parkiranu nit (`moraNapustiti = true` + `notifyAll()` na
  `parkLock`), nakon čega nit prelazi u `Zadatak.NAPUSTA` i poziva postojeći `napustiTerminal()`.
  Ovo je kuka koju će R4 (uviđaj) i C7/C8 (odlazak/dopuna) koristiti da reaktiviraju privezano
  plovilo — do sada ništa u kodu to ne poziva osim testova i `PokretacSimulacije` internih tokova.
- Novi konstruktor `BrodThread(Plovilo, Luka, Terminal, Dok)` za plovilo koje je **već fizički**
  postavljeno na dok (C3/C4 seeding) — nit odmah kreće u `PRIVEZAN`, bez ponovnog prolaska kroz
  ulazni kanal. Koristi ga `PokretacSimulacije.pokreniPrivezanaPlovila(Luka)`.
- `getX()`/`getY()`/`getTrenutniTerminal()` sada javni — priprema za D2 (R4: pretraga najbliže
  patrole na nivou luke, filtrirana po terminalu preko `getTrenutniTerminal()`).

**Posljedica po postojeće testove:** `BrodThreadTest.pokreniIsacekaj()` je ranije čekao
`ExecutorService.awaitTermination()` kao signal da je "simulacija gotova" — sa parkiranjem to
više ne funkcioniše (nit koja se uspješno privezala se više nikad sama ne gasi). Zamijenjeno
anketiranjem: čeka se da svako plovilo ili bude privezano (`isPrivezan()`) ili je njegova nit
već završila bez privezivanja (`Future.isDone()`). Ovo ne mijenja semantiku nijednog postojećeg
testa (svi provjeravaju stanje matrice terminala, ne završetak niti), samo tačku na kojoj se
smatra da je test spreman za asertacije. `exec.shutdownNow()` na kraju i dalje prekida sve
parkirane niti (InterruptedException iz `wait()` se hvata u `run()` i nit se uredno gasi) — bez
toga bi 100+ testova ostavljalo zombi niti blokirane zauvijek.

### Luka.aktivnaPlovila — registar živih niti (priprema za D2)

`Luka` sada nosi `transient Set<BrodThread> aktivnaPlovila` (`ConcurrentHashMap.newKeySet()`).
`BrodThread.run()` se registruje na početku i uklanja u `finally` bloku, bez obzira da li se
plovilo privezalo, odustalo, ili je parkiranje prekinuto. Osnova za R4 pretragu najbliže patrole
na nivou cijele luke (D2 iz `PRONALASCI.md`) — sa ~2.5% vatrogasnih plovila, pretraga samo unutar
jednog terminala vrlo često neće naći nijedno.

**Namjerno nije `final`**, iako bi konceptualno trebalo biti: inline inicijalizator transient
polja se nikad ne izvršava pri deserijalizaciji (samo pri običnoj konstrukciji), pa bi `final`
polje ostalo trajno `null` nakon učitavanja `luka.ser`. Riješeno inicijalizacijom u konstruktoru
i ponovo u novom `readObject()`. Test `aktivnaPlovilaPrezivljavaKaoPrazanSkup` u
`SerializationUtilTest` direktno provjerava da registar nakon deserijalizacije bude prazan skup,
ne `null` — žive niti iz prethodne sesije očigledno ne postoje više nakon ponovnog pokretanja JVM-a.

Napomena: `Luka` (u `model.classes`) sada uvozi `BrodThread` (iz `simulation`), što stvara
kružnu zavisnost paketa (`simulation` već zavisi od `model.classes` za `Terminal`/`Plovilo`/itd.).
Legalno u Javi, ali vrijedi zabilježiti — direktno je zahtijevano ovom odlukom (živi registar niti
mora biti tipiziran kao `Set<BrodThread>`, ne generički `Set<Object>` ili slično).

### Test paket: 112 → 135 (23 nova, 0 novih padova)

Novi testovi: 7 u `BrodThreadTest` (parkiranje, `zatraziNapustanje`, ne-blokiranje rendera,
predokovani konstruktor, registracija/deregistracija u `aktivnaPlovila` — i za uspješno i za
neuspješno privezivanje), 1 u `LukaTest`, 1 u `SerializationUtilTest`, 2 u `GeneratorPlovilaTest`
(O1 fix + integracioni test da harnes zaista poziva `obezbijediJedinstvenostImoZa` prije dopune),
12 u novom `PokretacSimulacijeTest` (T1, C1/C4 dopuna, C3 prenos i preraspodjela pri promjeni
broja terminala, C3+C4 zajedno — zatečena flota se računa u minimum, ne dodaje preko njega,
pokretanje niti za privezana plovila, validacija granica). Puni paket ponovljen tri puta zaredom
bez varijacije (135/135, 1 očekivani pad) radi provjere da nova konkurentnost (parkiranje,
anketiranje u `pokreniIsacekaj`) nije unijela nestabilnost pored postojeće (O3, nepromijenjeno).

## Riješeno 8. avgusta: R4a (infrastruktura za sistem incidenata)

Prema zadatku "R4a only — infrastructure for the incident system": tri komada infrastrukture koje
R4b (sudar, dispečovanje, prelasci `Zadatak`-a) treba da poveže sutra. Namjerno **ne** uključuje
detekciju sudara (i dalje placeholder iz D5), biranje/slanje patrole na incident, niti ijedan novi
prelaz stanja `Zadatak`-a — samo model podataka i primitivi koje ta logika treba da pozove.

**1. `simulation.Incident implements Serializable`** (I6/I7, D6): učesnici sudara i odazvana
službena plovila (odvojene liste `List<Plovilo>`), vrijeme incidenta, trajanje uviđaja (ms), id
terminala, i apsolutne putanje do fotografija svih učesnika. Putanje se izvode automatski u
konstruktoru preko `File.getAbsolutePath()` na `Plovilo.getFotografija()` svakog učesnika —
odabrano eksplicitno apsolutno (ne relativno kao ostatak projekta, vidi O2) jer je zapis incidenta
"case file" koji mora ostati čitljiv bez obzira iz kojeg je radnog direktorijuma simulacija
pokrenuta u trenutku nastanka incidenta, za razliku od `luka.ser`/`takse.csv` koji uvijek žive uz
istu instancu aplikacije. `sacuvaj()` piše jedan binarni fajl (`incident-<uuid>.ser`) direktno u
`System.getProperty("user.home")` (I7); `sacuvaj(File direktorijum)` je preopterećenje istog
principa kao S5/R3 (metoda koja prima putanju) — bez njega bi svaki test morao pisati u stvarni
home direktorijum korisnika. `Incident.ucitaj(File)` čita nazad, greška pri I/O-u se loguje i vraća
`null` (isti obrazac kao `SerializationUtil`), ne baca izuzetak. 9 novih testova u `IncidentTest`,
uključujući round-trip (sačuvaj → učitaj → uporedi polja) i eksplicitnu provjeru da podrazumijevani
`sacuvaj()` zaista piše u stvarni `user.home` (test sam čisti za sobom u `finally`).

**2. `Terminal` — blokada saobraćaja (I3/I4, D4).** Novo `transient volatile boolean
saobracajBlokiran` polje (transient jer je prolazno stanje trajanja simulacije, ne dio
`luka.ser`-a — blokada zatečena pri gašenju JVM-a gubi smisao jer je uviđaj koji ju je izazvao
nestao zajedno sa svim živim nitima), sa `blokirajSaobracaj()`/`odblokirajSaobracaj()`/
`isSaobracajBlokiran()`. Nova `Terminal.smijeProci(Plovilo)`: propušta sve dok terminal nije
blokiran, a dok jeste, propušta samo plovilo pod aktivnom rotacijom (`instanceof SluzbenoPlovilo &&
isRotacija()`) — obična pripadnost državnoj službi bez upaljene rotacije nije dovoljna, testirano
eksplicitno (`pomjeriNaPoljeZaustavljaSluzbenoPloviloBezRotacije`). Poziva se iz
`BrodThread.pomjeriNaPolje()`, jedine fizičke primitive kretanja kroz koju prolaze sve metode
kretanja (silazak do kanala, plovidba, napuštanje terminala, dolazak do doka) — jedna izmjena na
jednom mjestu automatski blokira sav budući saobraćaj bez obzira koji je poziv u pitanju. Provjera
je čisto čitanje `volatile` polja **van** `synchronized(t)` bloka, bez ikakvog `wait()`/`sleep()` —
ne krši D4 (`PrikazTerminala.render()` uzima isti `synchronized(terminal)` ključ, pa bi čekanje
unutar njega zamrznulo GUI). `pomjeriNaPolje()` promijenjena iz `private` u paket-privatnu
vidljivost (isti obrazac kao `ustupaProlaz`/`provjeriSudar`) da bi testovi mogli pozvati direktno.
4 nova testa u `TerminalTest` (BUCKET C) + 4 u `BrodThreadTest` (blokada zaustavlja obično plovilo,
propušta plovilo pod rotacijom, zaustavlja službeno plovilo bez rotacije, odblokada vraća normalno
kretanje).

**3. `simulation.PretragaPatrole.najblizaPatrola(Luka, Terminal, x, y)`** (I2, D2): port-wide
pretraga preko `Luka.getAktivnaPlovila()`, ne ograničena na terminal incidenta — namjerno, jer sa
~2.5% vatrogasnih plovila u tipičnoj floti (vidi napomenu iz C2 sesije) terminal na kojem se
incident desio vrlo često neće imati nijedno vatrogasno plovilo. Vraća `BrodThread` (ne `Plovilo`)
jer pozivalac (R4b) treba i poziciju i mogućnost kasnijeg upravljanja nađenom niti, isti obrazac kao
`Luka.getAktivnaPlovila()`.

Rastojanje preko granice terminala nije trivijalno: svaki `Terminal` ima nezavisnu matricu 4×17, pa
koordinate (x,y) u različitim terminalima nisu u istom koordinatnom sistemu — čist Menhetn
(Manhattan) proračun ima smisla samo unutar istog terminala. Pošto je profesor potvrdio da se
prelazak plovila između terminala modeluje logički, ne kao kontinuirano kretanje kroz fizički
prostor između njih (pitanje 1, `dodatna_pojasnjenja.txt`), rastojanje između terminala je takođe
diskretno: broj terminala koje treba preći, otežan konstantom `TEZINA_PRELASKA_TERMINALA = 17`
(širina matrice terminala) tako da terminalska razlika uvijek dominira nad lokalnim rastojanjem
unutar terminala — patrola u ciljnom terminalu se uvijek bira ispred patrole u bilo kom drugom
terminalu, bez obzira na lokalne koordinate. Ovo je namjerno pojednostavljenje (nije u zahtjevu
razrađen tačan algoritam), dokumentovano direktno u JavaDoc-u klase; test
`terminalskaRazlikaDominiraNadLokalnimRastojanjem` pinuje tačno to ponašanje. 7 novih testova u
`PretragaPatroleTest`: prazan registar, samo komercijalna plovila (vraća `null`), jedina patrola u
terminalu, biranje bliže od dvije patrole u istom terminalu po Menhetn rastojanju, terminalska
razlika dominira nad lokalnim rastojanjem, sve tri patrolne službe se prepoznaju (vatrogasci/
obalska straža/carina pojedinačno), nepozicionirano plovilo (`x/y == -1`, još u kanalu prije
ulaska) se preskače bez pada. Test-only pomoćna metoda konstruiše `BrodThread` preko postojećeg
predokovanog konstruktora sa fiktivnim `Dok`-om (proizvoljne koordinate, van stvarne liste vezova
terminala) i ručno ga registruje u `Luka.getAktivnaPlovila()` — bez pokretanja stvarne niti.

### Test paket: 141 → 165 (24 nova, 0 novih padova)

Puni paket ponovljen nakon svake od tri izmjene, isti jedan očekivani pad
(`sudarUkljucujeDvaPlovila`, i dalje čeka R4b) na 165/165 ostalih.

### Šta ostaje za R4b (namjerno van obima R4a)

Detekcija sudara (`provjeriSudar()` je i dalje placeholder), biranje učesnika i konstrukcija
`Incident`-a iz stvarnog sudara, pozivanje `Terminal.blokirajSaobracaj()`/`odblokirajSaobracaj()` i
mjerenje trajanja uviđaja (`MIN/MAX_TRAJANJE_UVIDJAJA_*` iz D5 se i dalje nigdje ne koriste), poziv
`PretragaPatrole.najblizaPatrola()` iz stvarnog dispečovanja, novi prelazi `Zadatak.KA_INCIDENTU`/
`NA_INCIDENTU` (enum vrijednosti postoje od D3, ništa ih još ne postavlja), i `SUDARI_OMOGUCENI`
nazad na `true` (I1) tek kad sve gore navedeno postoji — prerano ga uključiti bi značilo da sudari
počnu da se dešavaju bez ijednog mehanizma koji na njih reaguje.

## Otvoreni nalazi (nisu bagovi danas, postaju bagovi kasnije)

### O1 — `obezbijediJedinstvenostImoZa()` ne skenira evidenciju ulaska — ✅ RIJEŠENO (5. avgust)

Metoda je skenirala samo matricu svakog terminala (privezana plovila i ona u kanalu), ne i
`Luka.getEvidencijaUlaska()`, gdje ostaju IMO brojevi plovila koja su **već napustila luku** —
ta mapa je osnov za obračun lučkih taksi (F1). Postalo bi stvaran problem čim C7/E1 počnu
uklanjati plovila iz luke: novogenerisano plovilo bi dobilo IMO otišlog plovila, `putIfAbsent`
u `addToEvidencija` bi zadržao **stari** vremenski pečat, i novi brod bi bio naplaćen za tuđe
zadržavanje. Ispravljeno dodavanjem petlje po `evidencijaUlaska.keySet()` u `obezbijediJedinstvenostImoZa()`.
Test `obezbjeduJedinstvenostImoSkeniraIEvidencijuUlaska` u `GeneratorPlovilaTest`.

### O2 — `resources/` nije na classpath-u

`pom.xml` nadjačava `<sourceDirectory>src</sourceDirectory>` i nema `<resources>` blok, pa se `resources/placeholder-fotografija.txt` i `resources/spisak-potjera-default.txt` **ne kopiraju u `target/`**.

Trenutno radi jer se putanje razrješavaju kao obične relativne putanje u odnosu na radni direktorijum, a to je korijen projekta i u IntelliJ-u i pri pokretanju testova.

Puca ako se projekat ikada spakuje u jar ili pokrene iz drugog radnog direktorijuma — dakle potencijalno pri predaji ili na tuđoj mašini. Riješiti prije predaje, dodavanjem u `pom.xml`:

```xml
<resources>
    <resource>
        <directory>resources</directory>
    </resource>
</resources>
```

i čitanjem preko `getResourceAsStream()` umjesto `new File()`.

### O3 — Nestabilan test `ploviloPodRotacijomZavrsavaSimulaciju`

Zabilježen **jedan** pad tokom punog paketa nakon C2. Nije reprodukovan u 20+ uzastopnih izolovanih pokretanja (4.7s–6.3s, sva prošla). Poruka greške izgubljena zbog isteka sesije — nije poznato koja od dvije tvrdnje je pala:

- `"Simulacija se nije završila u zadatom vremenu"` → niti se nisu okončale u 40s (zastoj),
- `expected 24 but was 25` → plovilo je odustalo na ulazu (`udjiUTerminal`, granica 100 × 100ms = 10s), otkazalo rezervaciju i napustilo luku neusidreno.

**Hipoteza (nije potvrđena):** pravilo ustupanja iz R5 zamrzne komercijalno plovilo dok je plovilo pod rotacijom neposredno iza njega. Ako je istovremeno suprotni kanal (red 1) zauzet drugim plovilom u preticanju, plovilo pod rotacijom ne može proći, a ono ispred stoji upravo zato što je ono iza njega. Oba čekaju do granice `ukupnoPokusaja` (400 × ~100ms ≈ 40s), što je tačno na pragu timeout-a testa.

Napomena: pun paket je drugačije okruženje od izolovanog pokretanja — više niti u gašenju iz prethodnih testova, veće opterećenje procesora. Reprodukovati pod tim uslovima (pun paket, 10 ponavljanja), ne pojedinačno.

Relevantno za R4: tri službena plovila koja se istovremeno probijaju ka jednom incidentu je najgori mogući slučaj za ovu klasu zastoja.

### O4 — R5 nije potvrđen u pokretu

Tri testa `ustupaProlaz` su deterministična i prolaze, ali testiraju metodu izolovano. U dosadašnjim integracionim pokretanjima log nikada nije prikazao preticanje po prioritetu (`Započinje preticanje` se pojavljivao samo iz grane `neuspjesi >= PRAG_PRETICANJA`, dakle kod običnih plovila). Pravilo je dokazano jedinično, nedokazano u simulaciji.

---

## Odluke koje treba donijeti prije R4

Specifikacija zadaje cilj, ali ne i mehanizam. Ovih sedam odluka oblikuju strukturu koda i treba ih donijeti **prije** pisanja logike uviđaja, a ne tokom.

### D1 — Ko posjeduje incident?

Sudar uključuje dva plovila, svako u svojoj niti. Nijedno ne može prirodno „posjedovati" incident jer bi tada gašenje te niti ugasilo i uviđaj. Opcije:

- koordinator po terminalu (`KoordinatorUvidjaja`),
- statički registar na nivou luke,
- sam `Terminal` dobija stanje incidenta.

Ova odluka određuje strukturu svega ostalog u R4.

### D2 — Gdje se traži najbliža patrola?

Za „najbližu patrolu" treba registar živih `BrodThread`-ova sa čitljivim pozicijama — ne postoji. Po terminalu ili na nivou cijele luke?

Pri 2.5% vatrogasnih plovila terminal često neće imati nijedno, pa pretraga vjerovatno mora biti na nivou luke. To povlači i pitanje kako službeno plovilo prelazi između terminala (profesor je dozvolio logički prelaz — vidi `dodatna_pojasnjenja.txt`, pitanje 1).

Udaljenost: Manhattan rastojanje do ćelije incidenta je dovoljno.

### D3 — Kako plovilo mijenja cilj usred rute?

`BrodThread.run()` je trenutno pravolinijski: rezerviši → uđi → doplovi → gotovo. Usidreno službeno plovilo mora da se odveže i krene ka incidentu. Treba mu stanje, npr. `enum Zadatak { KA_DOKU, KA_INCIDENTU, NA_INCIDENTU, NAPUSTA }` i provjera tog stanja unutar petlji kretanja.

Najveća pojedinačna izmjena u R4 i glavni razlog procjene od 12–16 sati.

### D4 — Kako se blokira saobraćaj?

Blokira se **samo terminal na kojem je incident** (I3/I4). `wait()`/`notifyAll()` na terminalu je pravi primitiv — aktivno čekanje bi trošilo procesor tokom uviđaja od 3–10s.

**Kritično:** `PrikazTerminala.render()` uzima isti ključ. Ako se ikad uđe u `wait()` ili `Thread.sleep()` **držeći** `synchronized (terminal)`, GUI se zamrzava. Trenutno nijedan `synchronized (terminal)` blok u `BrodThread`-u ne spava — to svojstvo mora ostati.

### D5 — Determinizam sudara u testovima — ✅ RIJEŠENO (5. avgust, priprema — logika uviđaja i dalje čeka R4)

Sudar ima vjerovatnoću 2%, pa bi svaki test koji ga čeka bio nasumičan. Tačka ubrizgavanja
uvedena prije pisanja logike, tačno kako je predloženo:

```java
public static volatile double VJEROVATNOCA_SUDARA = 0.02;
```

plus izvor slučajnosti po niti, injektabilan preko `BrodThread.setGeneratorSudara(Random)`
(podrazumijevano `ThreadLocalRandom.current()`, nepredvidiv po dizajnu dok se eksplicitno ne
zamijeni). `provjeriSudar()` je promijenjen iz `void` u `boolean` — i dalje je čist placeholder
(nema `Incident`-a, dispečovanja ni blokade terminala, to ostaje za R4), sada samo vraća ishod:
`SUDARI_OMOGUCENI == false` ⇒ uvijek `false` (I1 i dalje na snazi), inače
`generatorSudara.nextDouble() < VJEROVATNOCA_SUDARA`. Vidljivost paket-privatna (bez modifikatora),
po uzoru na već postojeći `ustupaProlaz()` — testovi u istom paketu (`simulation`) pozivaju
direktno, bez pokretanja cijele niti.

Trajanje uviđaja dobilo četiri imenovane konstante umjesto inline literala, sve `public static
volatile` (ne `final` — testovi ih moraju moći spustiti na ~50ms):

```java
public static volatile long MIN_TRAJANJE_UVIDJAJA_MS = 3000L;           // I3, opšti incident
public static volatile long MAX_TRAJANJE_UVIDJAJA_MS = 10000L;
public static volatile long MIN_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 3000L; // I5, uže od opšteg
public static volatile long MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS = 5000L;
```

Nijedna od ove četiri konstante se još nigdje ne koristi za stvarno uspavljivanje niti tokom
uviđaja — to je R4. Ova runda je samo definisala mjesto gdje ta vrijednost živi, da se ne mora
naknadno tražiti po kodu i pretvarati inline `3000`/`10000` literale u konstante usred pisanja
logike uviđaja.

6 novih testova u `BrodThreadTest` (D5 sekcija): vjerovatnoća `1.0` garantuje sudar na 100
uzastopnih provjera, `0.0` nikad ne prijavljuje sudar, `SUDARI_OMOGUCENI == false` nadjačava čak
i vjerovatnoću `1.0` (I1 test), isti seed (`new Random(42)`) ubrizgan u dva različita `BrodThread`
daje identičan niz od 200 ishoda, podrazumijevane vrijednosti svih pet konstanti, i da se sve
četiri trajanja uviđaja mogu privremeno spustiti na 50ms (svaki test čuva staru vrijednost i vraća
je u `finally`, jer su ovo dijeljena statička polja preko cijelog test paketa).

Bez ovoga bi R4 testovi bili i nestabilni i spori.

### D6 — Šta ide u binarni fajl incidenta?

`Incident implements Serializable`, jedan fajl po slučaju u `user.home` (I6/I7). Sadržaj: učesnici sudara, službena plovila, vrijeme, fotografije.

Odluka: čuvati **putanje** do fotografija, ne bajtove. Putanje su manje i jednostavnije, bajtovi su samodovoljni ali fajl raste. Povezano sa O2 (putanje relativne na radni direktorijum).

### D7 — Šta poslije uviđaja?

Profesor je oba rješenja odobrio, uz preferencu:

- **Službena plovila:** napuste terminal _ili_ se privežu na prvi slobodan dok. Preferira se privezivanje, „kako bi se mogla službena vozila ponovo aktivirati" — što je direktno relevantno zbog O-nalaza o oskudici vatrogasaca.
- **Učesnici sudara:** „Neka napuste terminal" — ovdje je odgovor jednoznačan.

### Podsjetnik — vratiti sudare

`BrodThread.SUDARI_OMOGUCENI` je trenutno `false` (privremeno isključeno radi determinizma testova poslije R0). Mora nazad na `true` kad R4 bude gotov. To je jedino namjerno odstupanje od specifikacije koje trenutno postoji u projektu (I1).

### NOVA OTVORENA ODLUKA

Ulazni timestampovi se prenose kroz sesije iz "luka.ser" (omogucava perzistenciju simulacije), tako da plovilo koje je juce dokovano sada placa stvarne protekle sate, a ne striktno sate unutar simulacije. Da li resetovati brojac pri ponovnom pokretanju, ili koristiti neku vrstu vremenskog skaliranja unutar simulacije koje ce garantovati realne cijene i realne sate naplacene, a ne ekstremno velike vrijednosti samo zato sto je laptop ostao ugasen par dana?

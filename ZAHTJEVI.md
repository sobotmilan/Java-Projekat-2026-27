# Matrica zahtjeva — specifikacija → implementacija

Izvor: `PJ2 - projektni zadatak - maj 2026.pdf` + `dodatna_pojasnjenja.txt`
Stanje: 13. avgust 2026, poslije R0 + R2 + R1 + R5 + čišćenja S1–S4/S6 + C6 (`PrikazTerminala`) + C2 (`GeneratorPlovila`) + code review ispravke na C2 + T1/C1/C3/C4 (`PokretacSimulacije`) + `Zadatak`/parkiranje + O1 + D5 (determinizam sudara, priprema za R4) + R4a (infrastruktura za sistem incidenata — `Incident`, blokada saobraćaja na terminalu, `PretragaPatrole`) + R4b (logika incidenta — detekcija sudara, dispečovanje, prelasci `Zadatak`-a, raspetljavanje; I1–I8 zatvoreni) + naknadne ispravke iz code review-a (`R4B_GRESKE.md`, G1–G8, vidi `PRONALASCI.md`) + I5/M6 (potjernica — vidi "Riješeno 13. avgusta" ispod).
Test paket: 208 ukupno, 0 pada, 0 ignorisano.
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
| M6 | Obalska straža nosi fajl sa IMO brojevima za potjernicom | DONE | `util.SpisakPotjeraUtil.ucitaj(File)` čita i kešira sadržaj (I5, Korak 1) |
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
| I1 | 2% sudara pri mimoilaženju | DONE — `BrodThread.SUDARI_OMOGUCENI = true` (podrazumijevano), `provjeriSudar()` detektuje oba učesnika u grani preticanja (Korak 1) |
| I2 | Najbliža obalska straža, carina i vatrogasci pod rotacijom | DONE — `PretragaPatrole.najblizaPatrola(..., Class)` (Korak 2), poziva ga `KoordinatorUvidjaja.pozoviPatrole()` za svaku od tri službe pojedinačno |
| I3 | Blokada saobraćaja na terminalu, uviđaj 3–10s | DONE — `KoordinatorUvidjaja` (Korak 3) zove `blokirajSaobracaj()`/`odblokirajSaobracaj()` i uspavljuje se na slučajno trajanje iz `MIN/MAX_TRAJANJE_UVIDJAJA_MS` |
| I4 | Ostali terminali rade normalno | DONE — blokada je po instanci `Terminal`-a, demonstrirano testom (`KoordinatorUvidjajaTest`) da susjedni terminal ostaje neblokiran |
| I5 | Potjernica: pratnja ka izlazu, uviđaj 3–5s, saobraćaj radi | DONE — `BrodThread.provjeriPotjernicu()`/`pokreniPotjernicu()` (vidi "Riješeno 13. avgusta" i "Ispravke 13. avgusta (code review)" ispod); terminal se nikad ne blokira, za razliku od I3. **Svjesna odluka o "pratnji":** oba plovila (obalska straža i traženo) napuštaju terminal nezavisno, svako svojom putanjom kroz postojeći `napustiTerminal()` — traženo plovilo ne prati obalsku stražu ćeliju po ćeliju. Specifikacija doslovno traži da meta "prati brod obalske straže ka izlazu", ali doslovno praćenje pozicije druge, nezavisne niti korak-po-korak je netrivijalno (zahtijevalo bi novu rutu kretanja koja prati poziciju druge niti u realnom vremenu) i ne mijenja nijedan mjerljiv ishod — oba plovila i dalje napuštaju terminal, obalska straža je pod rotacijom cijelo vrijeme, evidencija sadrži oba. Odluka je da se to ne implementira sada; vrijeme je bolje uloženo u preostale GUI zahtjeve (A*/C5/C7/C8). |
| I6 | Evidencija: učesnici, vrijeme, fotografije | DONE — `KoordinatorUvidjaja` konstruiše `Incident` sa stvarnim učesnicima sudara i odazvanim patrolama nakon svakog uviđaja |
| I7 | Binarni fajl po slučaju, u `user.home` | DONE — `KoordinatorUvidjaja` poziva `incident.sacuvaj()` na kraju uviđaja; integracioni test provjerava stvaran fajl u `user.home` |
| I8 | Učesnici napuštaju terminal poslije uviđaja | DONE — Korak 5: `BrodThread.udjiULuku()` presreće učesnike sudara na tačci uspješnog privezivanja i preusmjerava ih na `napustiTerminal()` umjesto privezivanja |

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

1. ~~**I1 — sudari isključeni.** `BrodThread.SUDARI_OMOGUCENI = false`. Vratiti na `true` u R4.~~
   Riješeno 9. avgusta (R4b, "Na kraju") — vidi sekciju ispod. Ovo je bilo jedino namjerno
   odstupanje od specifikacije koje je postojalo u projektu; više ga nema.
2. ~~**F2 — zaokruživanje.** `Duration.toHours()` reže naniže, pa 90 min = 100 KM.~~ Riješeno, korištena Math.ceil() metoda i princip "plafona" za računanje tarife.
3. ~~**M6 — spisak potjernica se ne čita.** Fajl se čuva, sadržaj se nikad ne parsira.~~ Riješeno
   13. avgusta (I5, Korak 1) — vidi ispod.
4. ~~**T1 — properties se ne čita.**~~ Riješeno 5. avgusta — vidi ispod. `TestFactory.luka(n)` i dalje hardkoduje broj terminala, ali to je namjerno (test fabrika), ne proizvodni kod.

## Redoslijed preostalog rada

~~R1 + R5 (interfejs + prioritet)~~ **gotovo 4. avgusta** → ~~T1 (properties) → C1/C3/C4 (harnes)~~
**gotovo 5. avgusta** → ~~R4a (infrastruktura: `Incident`, blokada terminala, `PretragaPatrole`)~~
**gotovo 8. avgusta** → ~~R4b (detekcija sudara, dispečovanje, prelasci `Zadatak`-a, raspetljavanje)~~
**gotovo 9. avgusta** → **A\* (admin GUI)** → C5/C8 (klijent GUI + prikaz + dodavanje tokom rada) →
C7/E1/E2 (odlazak i kraj) → F4 (CSV na izlazu)

R4 (R4a+R4b) je bio najveći pojedinačni blok i imao najviše nezatvorenih zahtjeva — svi I1–I8 su
sada zatvoreni (I5, potjernica, je bio izuzetak — namjerno van obima R4b, zatvoren zasebno
13. avgusta, vidi "Riješeno 13. avgusta" ispod). `Zadatak`/parkiranje i
`Luka.aktivnaPlovila` su urađeni unaprijed 5. avgusta baš zbog R4, R4a (8. avgust) je dodao
preostalu infrastrukturu (`Incident`, blokada terminala, `PretragaPatrole`), a R4b (9. avgust) je
povezao te komade u stvarnu logiku uviđaja u pet koraka — vidi "Riješeno 9. avgusta" ispod za
detalje.

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

**Bug uhvaćen prije commit-a (code review):** `pomjeriSaCekanjem()` (koristi je `doploviDoDoka`,
`sidjiDoKanala`, `napustiTerminal`) broji neuspjehe do `MAX_POKUSAJA` (100) × `CEKANJE_MS` (100ms)
= tačno 10000ms — identično podrazumijevanoj `MAX_TRAJANJE_UVIDJAJA_MS`. Prvobitna implementacija
blokade brojala je pokušaj blokiran od strane `Terminal.smijeProci()` isto kao pokušaj blokiran
zauzetom ćelijom: plovilo koje bi čekalo baš na posljednjem koraku ulaska u dok tokom najdužeg
mogućeg uviđaja bi otkazalo legitimno dobijenu rezervaciju veza (`otkaziRezervaciju`) i produžilo ka
narednom terminalu — samo zbog podudarnosti dva vremenska budžeta, ne zato što je ciljna ćelija
ikad bila stvarno trajno zauzeta. Netačan ishod zavisi od tajminga (da li je plovilo baš na
posljednjem koraku kad blokada počne), pa je najgora vrsta baga za reprodukciju — otkriven kroz
analizu koda, ne kroz pad testa. Ispravljeno: `pomjeriSaCekanjem()` ne inkrementira brojač pokušaja
dok je neuspjeh izazvan blokadom (nova `cekaZbogBlokade()`), samo dok je izazvan zauzetom ćelijom —
plovilo nastavlja da čeka i pokušava svaki `CEKANJE_MS`, koliko god blokada trajala, umjesto da
otkaže rezervaciju. Terminal koji nije postavljen se i dalje tretira kao normalan neuspjeh (brojač
se inkrementira), da nit ne bi čekala unedogled bez ijednog terminala. `pomjeriSaCekanjem()`
promijenjena u paket-privatnu vidljivost radi direktnog testa. Regresioni test
(`pomjeriSaCekanjemNeOdustajeZbogBlokadeIakoTrajeDuzeOdBudzetaPokusaja`) blokira terminal, čeka
10.5s (duže od starog budžeta) i provjerava da plovilo još nije odustalo, pa odblokira i provjerava
uspješan završetak — namjerno spor test (~11s) jer je to tačno prozor u kojem se bag ranije
manifestovao.

### Test paket: 141 → 166 (25 novih, 0 novih padova)

Puni paket ponovljen nakon svake izmjene, isti jedan očekivani pad (`sudarUkljucujeDvaPlovila`, i
dalje čeka R4b) na 166/166 ostalih.

### Šta ostaje za R4b (namjerno van obima R4a)

Detekcija sudara (`provjeriSudar()` je i dalje placeholder), biranje učesnika i konstrukcija
`Incident`-a iz stvarnog sudara, pozivanje `Terminal.blokirajSaobracaj()`/`odblokirajSaobracaj()` i
mjerenje trajanja uviđaja (`MIN/MAX_TRAJANJE_UVIDJAJA_*` iz D5 se i dalje nigdje ne koriste), poziv
`PretragaPatrole.najblizaPatrola()` iz stvarnog dispečovanja, novi prelazi `Zadatak.KA_INCIDENTU`/
`NA_INCIDENTU` (enum vrijednosti postoje od D3, ništa ih još ne postavlja), i `SUDARI_OMOGUCENI`
nazad na `true` (I1) tek kad sve gore navedeno postoji — prerano ga uključiti bi značilo da sudari
počnu da se dešavaju bez ijednog mehanizma koji na njih reaguje.

**Sve ovo je urađeno 9. avgusta — vidi "Riješeno 9. avgusta: R4b" ispod.**

## Riješeno 9. avgusta: R4b (logika incidenta)

Pet koraka, ovim redom, sa punim paketom pokrenutim prije i poslije svakog. Detalji dizajna i
odluke "šta i zašto" po koraku — testovi za svaki korak nabrojani na kraju odgovarajućeg pasusa.

**Determinizam (preduslov za sve testove):** nijedan test ne smije zavisiti od stvarne
vjerovatnoće (2%) ili stvarnog trajanja uviđaja (3–10s). Svaki test koji pokreće stvarnu
navigaciju postavlja `SUDARI_OMOGUCENI`/`VJEROVATNOCA_SUDARA`/`MIN`/`MAX_TRAJANJE_UVIDJAJA_MS`
lokalno (try/finally ili `@BeforeEach`/`@AfterEach`) i vraća ih na kraju — ovi su `static
volatile` i cure između testova u istom JVM procesu. Pošto je `SUDARI_OMOGUCENI` sada
podrazumijevano `true` (vidi "Na kraju" ispod), `BrodThreadTest` dobio je klasni `@BeforeEach`
koji spušta `VJEROVATNOCA_SUDARA` na `0.0` prije svakog testa — bez ovoga bi svaki od desetak
postojećih testova koji pokreće stvarnu, višebrodsku navigaciju (iz R0–R5 ere, prije R4b) postao
probabilistički nedeterministički, sa realnom šansom sudara kod plovila koja to ne očekuju.

**Korak 1 — detekcija sudara sa dva učesnika.** `BrodThread.provjeriSudar()` promijenjen iz
`boolean` u `Plovilo[]` (dva učesnika ili `null`). Mjesto ostaje jedino gdje ima smisla —
grana preticanja u `ploviIstocno()` — ali sada se poziva samo kad je pomjeraj bio dio stvarnog
preticanja (novi lokalni `preticanje` flag), ne na svaki uspješan korak; poziv iz
`sidjiDoKanala()` (koji nikad nije mimoilaženje) uklonjen. Drugi učesnik se čita iz suprotnog
traka kanala na istoj koloni (`KANAL_IZLAZ` ↔ `KANAL_ULAZ`), unutar `synchronized(terminal)`;
kockica se baca van tog bloka. Ako je `SUDARI_OMOGUCENI == false`, metoda uopšte ne pristupa
terminalu (kratko-spoj prije bilo kakvog čitanja). Postojeći D5 testovi koji su direktno pozivali
`provjeriSudar()` bez ijednog susjeda (nikad nisu mogli vratiti "dva učesnika" jer terminal nije
bio postavljen) prepravljeni da postave stvarno mimoilaženje preko package-private
`pomjeriNaPolje()`, isti obrazac kao ostali R5 testovi.

**Korak 2 — `PretragaPatrole` po tipu službe i po dostupnosti.** Novo preopterećenje
`najblizaPatrola(Luka, Terminal, x, y, Class<T> tip)` — deli internu logiku sa postojećom
verzijom preko `Predicate<Plovilo>`, ne duplira pretragu. Dodat filter dostupnosti: patrola već u
`Zadatak.KA_INCIDENTU`/`NA_INCIDENTU`/`NAPUSTA` se preskače (I2 — inače bi dva istovremena
incidenta poslala isto vatrogasno plovilo na oba). Izjednačeno rastojanje razrješeno biranjem
manjeg IMO broja (leksikografsko poređenje stringa, radi i za obične i za test-specifične IMO
vrijednosti). Dodat paket-privatni `BrodThread.setZadatak(Zadatak)` — potreban testovima da
fabrikuju "zauzetu" patrolu bez pokretanja stvarne niti, isti obrazac kao `pomjeriNaPolje`.

**Korak 3 — `KoordinatorUvidjaja`.** Nova klasa, posjeduje incident (D1) — konstruisana sa lukom,
terminalom, dvoje učesnika sudara i koordinatama incidenta; radi u sopstvenoj niti (`Runnable`)
koju `BrodThread` pokreće preko novog privatnog `pokreniUvidjaj()` čim `provjeriSudar()` vrati
par (detektujuća nit se ne blokira — samo pokreće koordinatora i nastavlja svoju petlju, koja se
prirodno zaustavi na sljedećem pokušaju pomjeranja jer terminal postaje blokiran). Redoslijed:
blokiraj → dispečuj sve tri službe (upali rotaciju, `pozoviNaIncident`) → sačekaj dolazak
(ograničeno `MAX_CEKANJE_DOLASKA_MS`, podrazumijevano 15s — patrola koja ne stigne ne blokira
uviđaj zauvijek) → uspavaj se na slučajno trajanje (`MIN/MAX_TRAJANJE_UVIDJAJA_MS`) → sačuvaj
`Incident` → u `finally`: obilježi učesnike sudara za napuštanje, odblokiraj, ugasi rotacije,
raspetljaj patrole (Korak 5). Nikad `synchronized(terminal)` unutar koordinatora (D4) — blokada
je čisto čitanje/pisanje `volatile` zastavice na `Terminal`-u. Nedostatak neke službe u luci samo
loguje upozorenje i nastavlja sa preostalima.

**Korak 4 — prelasci stanja i buđenje privezanih patrola.** Novi `BrodThread.pozoviNaIncident(
Terminal, x, y)` — isti obrazac kao `zatraziNapustanje()` (park-ključ, nikad
`synchronized(terminal)`), ali djeluje samo ako je plovilo trenutno `PRIVEZAN` (idempotentan
guard umjesto posebne zastavice — sprečava da patrola koja je već krenula ili već na incidentu
bude "probuđena" dvaput, i uz to čini `cekajNapustanje()`-ov uslov jednostavnim: budi se na
`moraNapustiti` ILI na `zadatak == KA_INCIDENTU`). `otidjiNaIncident()` oslobađa rezervaciju veza
koji napušta (vidi K6 u `PRONALASCI.md`), logički prelazi u drugi terminal ako je dispečovana
patrola bila usidrena negdje drugo (D2 — isti obrazac kao standardni ulazak novog plovila), pa se
kreće ka polju pored incidenta koristeći isključivo postojeće primitive kretanja
(`pomjeriSaCekanjem`) — bez nove logike preticanja, jer profesor dozvoljava da službeno plovilo
samo sačeka zauzeta polja. Kad stigne, `Zadatak.NA_INCIDENTU`, čeka signal kraja uviđaja —
ograničeno `MAX_CEKANJE_KRAJA_UVIDJAJA_MS` (20s podrazumijevano, isti razlog kao
`MAX_CEKANJE_DOLASKA_MS`: ako koordinator ugine prije nego stigne da razriješi patrolu, ona ne
smije čekati zauvijek — podrazumijeva se napuštanje).

**Korak 5 — raspetljavanje nakon uviđaja.** Dvije nezavisne putanje:
- *Službena plovila:* `KoordinatorUvidjaja` pokušava `Terminal.rezervisiSlobodanDok()`; novi
  `BrodThread.zavrsiUvidjaj(Dok)` postavlja `Zadatak.KA_DOKU` (dok nađen) ili `Zadatak.NAPUSTA`
  (nije), budi patrolu preko park-ključa. `run()` je preoblikovan iz linearnog toka u petlju
  (`PRIVEZAN → [incident → KA_DOKU] → PRIVEZAN → ...`) umjesto ugniježđenih pozivа, da patrola
  može odgovoriti na proizvoljno mnogo uzastopnih incidenata bez rasta steka niti.
- *Učesnici sudara:* "Neka napuste terminal" je jednoznačno (profesor), ali oba učesnika su u
  tom trenutku duboko unutar `ploviIstocno()`/`doploviDoDoka()`, ne u parkiranom stanju — nema
  gdje ih "probuditi". Umjesto dodirivanja R0/R5 logike kretanja, `KoordinatorUvidjaja` samo
  postavlja novu `sudarMoraNapustiti` zastavicu (preko `BrodThread.oznaciKaoUcesnikaSudara()`,
  pretragom `Luka.getAktivnaPlovila()` po identitetu `Plovilo`-a) — a `udjiULuku()` je presječen
  na TAČNO jednom mjestu: čim `doploviDoDoka()` uspije, ako je zastavica postavljena, rezervacija
  se otkazuje i plovilo ide na `napustiTerminal()` umjesto da se ikad označi privezanim. Plovilo
  fizički stigne do svog veza (blokada terminala ga do tog trenutka i tako zadržava), pa odmah
  krene dalje — nikad se ne vidi kao privezano, ne duplira evidenciju, ne ostaje "trajno
  rezervisan a prazan" (K6 fix se ovdje direktno koristi).

**Redoslijed odblokiraj → ugasi rotaciju → raspetljaj (namjerno, provjereno):** `smijeProci()`
propušta kroz blokirani terminal samo plovila pod aktivnom rotacijom, pa dispečovana patrola
zavisi od te rotacije dok terminal *jeste* blokiran (put ka incidentu, Korak 4). Kad
`KoordinatorUvidjaja` u `finally` bloku prvo odblokira terminal, PA TEK ONDA gasi rotaciju i
poziva raspetljavanje — put NAZAD (ka novom doku ili izlazu) se odvija kroz već otvoren terminal,
gdje rotacija više nije potrebna za prolaz. Obrnut redoslijed (gašenje rotacije prije
odblokiranja, ili raspetljavanje prije oba) bi patrolu na povratku zamrznuo usred terminala.

**Na kraju:**
- `BrodThreadTest.sudarUkljucujeDvaPlovila` zamijenjen stvarnim integracionim testom umjesto
  `fail()`: realna, ali determinizovana navigacija (statička "prepreka" — plovilo bez sopstvene
  niti na fiksnoj poziciji u kanalu, isti princip kao Korak 1 testovi — koju stvarno, samostalno
  sustiže i pretiče jedno realno plovilo kroz stvarnu R5 preticanje logiku) garantuje sudar bez
  ikakve probabilističke trke; test čeka stvarni fajl incidenta u `user.home`, provjerava dva
  različita učesnika i bar jednu odazvanu patrolu, pa čisti fajl za sobom.
  Dvije ranije verzije ovog testa (dva stvarna plovila različitih brzina, sa i bez zaleta) su se
  pokazale nedeterministički nepouzdane pod punim paketom — otkriveno upravo ponavljanjem testa
  10× izolovano, tačno razlog zašto "jedan zeleni prolaz ne znači ništa" kod konkurentnosti.
- `BrodThread.SUDARI_OMOGUCENI` vraćeno na `true` kao podrazumijevana vrijednost (I1) — jedino
  namjerno odstupanje od specifikacije u projektu je zatvoreno.
- Test paket: **166 → 187** (21 novih, 0 novih padova; `sudarUkljucujeDvaPlovila` prešao iz
  jedinog očekivanog pada u zeleno). Puni paket pokrenut tri puta zaredom nakon svih pet koraka
  i "Na kraju" izmjena — bez varijacije.

## Riješeno 13. avgusta: I5 (potjernica) + M6 (čitanje spiska potjera)

Četiri koraka, ovim redom, sa punim paketom pokrenutim prije i poslije svakog (isti obrazac kao
R4b). Ključna razlika u odnosu na I3/R4b: **terminal se ovdje nikad ne blokira** — potjernica ne
prekida saobraćaj ostalih plovila, pa cijeli tok radi bez `KoordinatorUvidjaja` (ta klasa nije
dirana — pratnja ne treba njenu glavnu odgovornost, blokadu i orkestraciju dolaska tri službe).

**Korak 1 — `SpisakPotjeraUtil` (zatvara M6).** Nova klasa u `util`, po uzoru na
`PropertiesUtil`, ali keširana po putanji fajla (`Map<String, Set<String>>`), ne globalno kao
`PropertiesUtil` — svaka obalska straža nosi svoj `spisakPotjera`, pa više različitih fajlova
mora moći koegzistirati u istom test-JVM-u. Čita jedan IMO broj po liniji, preskače prazne linije
i linije koje počinju sa `#` (komentar), a nedostajući ili nečitljiv fajl ne baca izuzetak —
loguje upozorenje i vraća prazan skup, isti "tih neuspjeh" obrazac kao ostatak projekta (npr.
`GeneratorPlovila` kad fotografija ne postoji). `resetujKes()` za testove, isti obrazac kao
`PropertiesUtil`.

**Korak 2 — detekcija.** `BrodThread.provjeriPotjernicu()` (paket-privatno, isti obrazac
vidljivosti kao `provjeriSudar()`/`ustupaProlaz()`) radi samo ako plovilo implementira
`ObalskaStraza` (obična plovila nikad ne ulaze u granu, čak i kad je IMO tražene meta slučajno na
susjednom polju — provjereno testom). Čita četiri susjedna polja (gore/dolje/lijevo/desno) unutar
`synchronized(terminal)`, kao i sva ostala čitanja matrice u ovoj klasi (T5). Poziva se iz
`ploviIstocno()` nakon **svakog** uspješnog koraka (ne samo tokom preticanja, za razliku od
`provjeriSudar()`) — potjernica mora biti otkrivena čim obalska straža fizički prođe pored
traženog plovila, bilo u kanalu bilo pored doka, ne samo kad se dvije nesluzbene niti mimoilaze.

**Korak 3 — pratnja ka izlazu.** Dvije nove `Zadatak` vrijednosti: `PRACENJE` (obalska straža) i
`POD_PRATNJOM` (traženo plovilo). Kad `provjeriPotjernicu()` pronađe metu,
`pokreniPotjernicu()` pali rotaciju na obalskoj straži, postavlja joj `naPratnji`/`zadatak`, i —
ako je traženo plovilo trenutno privezano — budi ga preko novog `pozoviNaPratnju()` (isti
park-ključ obrazac kao `pozoviNaIncident()`, idempotentan istim `PRIVEZAN`-guardom). Nijedna
strana ne prolazi kroz koordinatora niti drži `synchronized(terminal)` preko čekanja — obalska
straža jednostavno prekida sopstveni `ploviIstocno()` (`return false`), što je isti mehanizam
kojim se metoda inače vraća kad joj ponestane pokušaja, samo namjerno izazvan. Traženo plovilo
ne dobija posebnu "idi ka izlazu" logiku — `napustiZbogPratnje()` samo otkazuje rezervaciju
njegovog veza, a ostatak (fizički izlazak) radi se ponovnom upotrebom postojećeg
`napustiTerminal()`, potpuno isto kao svako drugo plovilo koje napušta luku; specifikacija traži
da meta ode ka izlazu, ne da vizuelno prati baš obalsku stražu ćeliju po ćeliju. Trajanje uviđaja
(3–5s) čita se iz već postojećih `MIN/MAX_TRAJANJE_UVIDJAJA_POTJERNICE_MS` konstanti (dodate 5.
avgusta, D5, neiskorištene do sada) unutar `zavrsiPotjernicu()` — obično `Thread.sleep()`, van
bilo kog ključa, pa ne blokira nikog drugog dok traje.

**Korak 4 — evidencija (I6/I7 za potjernicu).** Ponovo upotrijebljen postojeći `Incident` — nova
`TipIncidenta` enumeracija (`SUDAR`/`POTJERNICA`) i preopterećen konstruktor sa tim parametrom;
stari petoargumentni konstruktor i dalje radi, sada samo delegira sa `TipIncidenta.SUDAR`, pa
`KoordinatorUvidjaja` nije morao biti dirnut. `zavrsiPotjernicu()` konstruiše `Incident` sa
traženim plovilom kao "učesnikom" i obalskom stražom kao "odazvanim službenim plovilom", ista
podjela uloga kao kod sudara. Novo `BrodThread.DIREKTORIJUM_INCIDENTA_POTJERNICE` (`static
volatile File`, podrazumijevano `null` ⇒ `user.home`) — isti princip injektovanja kao ostale D5
statike, jer `BrodThread` (za razliku od `KoordinatorUvidjaja`) nema konstruktorski parametar za
direktorijum, a nit se pravi mnogo prije nego što se zna hoće li do potjernice uopšte doći.

**Na kraju:**
- Test paket: **192 → 207** (15 novih kroz sva četiri koraka: 7 `SpisakPotjeraUtilTest` + 4
  detekcija + 3 pratnja/blokada + 1 evidencija, 0 novih padova). Puni paket pokrenut tri puta
  zaredom nakon svih koraka — bez varijacije.

## Ispravke 13. avgusta (code review, `I5_PREGLED.md`)

Code review nakon prve verzije I5 (commit `c794897`) je otkrio jedan nalaz koji blokira merge
(P1) i jedan manji nalaz o dupliranom stanju (P2); P3 (doslovno praćenje pozicije) je gore
dokumentovan kao svjesna odluka, ne popravka.

**P1 — trajanje uviđaja i cjelina potjernice zavisile su od poziva koji je slučajno tačan.**
`pokreniPotjernicu()` je samo postavljao stanje i vraćao se; stvarni `Thread.sleep()`/upis
evidencije/napuštanje terminala su bili u zasebnoj `zavrsiPotjernicu()`, pozvanoj isključivo iz
`udjiULuku()`-ove grane "ne mogu doći do doka". To je (a) mjerilo trajanje uviđaja od pogrešne
tačke (par metoda kasnije nego trenutak detekcije) i (b) učinilo cijeli mehanizam krhkim — kad
bi ikad zatrebalo pozvati detekciju iz drugog konteksta (npr. plovilo koje je već privezano pa
probuđeno), evidencija se nikad ne bi upisala niti rotacija ugasila. **Popravka:**
`pokreniPotjernicu()` je sad samodovoljna — radi buđenje mete, spavanje 3–5s, upis evidencije i
napuštanje terminala u jednom mjestu, bez oslanjanja na pozivaoca; `zavrsiPotjernicu()` obrisana.
Jedna namjerna razlika od prijedloga iz `I5_PREGLED.md`: grana `if (this.naPratnji)` u
`udjiULuku()` je zadržana (samo bez poziva `zavrsiPotjernicu()`) jer bi njeno potpuno brisanje
pustilo petlju da nastavi na `idx++` i pokuša naredni terminal kao da je ovaj bio privremeno pun
— plovilo koje je `pokreniPotjernicu()` već fizički izvela iz luke bi se pokušalo ponovo uvesti
kroz sljedeći terminal. Test `pokreniPotjernicuRadiSamostalnoBezObziraOdakleJePozvana`
(`BrodThreadPotjernicaTest`) poziva `pokreniPotjernicu()` direktno na već privezanoj,
"probuđenoj" obalskoj straži (izvan `udjiULuku()` konteksta) i provjerava da evidencija i dalje
nastaje i rotacija se gasi.

**P2 — `Zadatak.PRACENJE` se postavljao ali nigdje nije čitan (isti obrazac kao K8).** Dva
polja su opisivala "obalska straža je u potjeri": novi `Zadatak.PRACENJE` i postojeći `boolean
naPratnji`. Za razliku od K8 (gdje su oba polja zaista opisivala isto trenutno stanje i njihovo
neatomarno ažuriranje je bilo prava trka), ovdje polja imaju različit vijek trajanja:
`pokreniPotjernicu()` na kraju postavlja `zadatak = NAPUSTA`, dok se `naPratnji` ne gasi nikad
(namjerno — koristi ga `run()`-ov završni log koji se izvršava tek pošto se cijela potjernica,
uključujući napuštanje terminala, već desila, dakle **poslije** te posljednje promjene
`zadatak`-a). Zbog toga zamjena `naPratnji` sa `zadatak == PRACENJE` svuda (prva opcija
predložena u `I5_PREGLED.md`) kvari baš tu log poruku — u trenutku provjere `zadatak` je već
`NAPUSTA`, ne `PRACENJE`. Odabrana je druga ponuđena opcija: `Zadatak.PRACENJE` obrisan iz enuma,
`naPratnji` ostaje jedini izvor istine za "da li je ovo plovilo bilo u potjeri" (obalska straža
tokom same potjere nema poseban `zadatak`, samo `KA_DOKU` do trenutka kad `pokreniPotjernicu()`
eksplicitno postavi `NAPUSTA`). Ne dodaje se JavaDoc na `POD_PRATNJOM` (jedina preostala nova
vrijednost enuma) — stoji izričita instrukcija projekta da se novi kod ne dokumentuje JavaDoc-om.

- Test paket: **207 → 208** (1 novi test za P1, 0 novih padova). Puni paket pokrenut tri puta
  zaredom nakon ovih ispravki — bez varijacije.

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

### D1 — Ko posjeduje incident? — ✅ RIJEŠENO (9. avgust)

Sudar uključuje dva plovila, svako u svojoj niti. Nijedno ne može prirodno „posjedovati" incident jer bi tada gašenje te niti ugasilo i uviđaj. Opcije:

- koordinator po terminalu (`KoordinatorUvidjaja`),
- statički registar na nivou luke,
- sam `Terminal` dobija stanje incidenta.

Ova odluka određuje strukturu svega ostalog u R4.

**Odabrano:** `KoordinatorUvidjaja` (posljednja opcija) — nova klasa u paketu `simulation`,
radi u sopstvenoj niti, konstruisana sa `Luka`/`Terminal`/učesnicima/koordinatama. Vidi "Riješeno
9. avgusta: R4b", Korak 3.

### D2 — Gdje se traži najbliža patrola? — ✅ RIJEŠENO (8. avgust infrastruktura, 9. avgust povezano u dispečovanje)

Za „najbližu patrolu" treba registar živih `BrodThread`-ova sa čitljivim pozicijama — ne postoji. Po terminalu ili na nivou cijele luke?

Pri 2.5% vatrogasnih plovila terminal često neće imati nijedno, pa pretraga vjerovatno mora biti na nivou luke. To povlači i pitanje kako službeno plovilo prelazi između terminala (profesor je dozvolio logički prelaz — vidi `dodatna_pojasnjenja.txt`, pitanje 1).

Udaljenost: Manhattan rastojanje do ćelije incidenta je dovoljno.

**Odabrano:** port-wide pretraga preko `Luka.getAktivnaPlovila()` (`PretragaPatrole`), logički
prelaz između terminala implementiran u Koraku 4 (`BrodThread.predjiLogickiUTerminal()`) — isti
obrazac kao standardni ulazak novog plovila.

### D3 — Kako plovilo mijenja cilj usred rute? — ✅ RIJEŠENO (9. avgust)

`BrodThread.run()` je trenutno pravolinijski: rezerviši → uđi → doplovi → gotovo. Usidreno službeno plovilo mora da se odveže i krene ka incidentu. Treba mu stanje, npr. `enum Zadatak { KA_DOKU, KA_INCIDENTU, NA_INCIDENTU, NAPUSTA }` i provjera tog stanja unutar petlji kretanja.

Najveća pojedinačna izmjena u R4 i glavni razlog procjene od 12–16 sati.

**Odabrano:** enum `Zadatak` (peta vrijednost `PRIVEZAN` dodata 5. avgusta, prije R4). `run()` je
od Koraka 5 petlja (`PRIVEZAN → [incident] → PRIVEZAN → ...`), ne linearan tok — patrola može
odgovoriti na proizvoljno mnogo uzastopnih incidenata. Buđenje uvijek preko posebnog park-ključa
(`pozoviNaIncident()`/`zavrsiUvidjaj()`, isti obrazac kao postojeći `zatraziNapustanje()`).

### D4 — Kako se blokira saobraćaj? — ✅ RIJEŠENO (8. avgust infrastruktura, 9. avgust povezano)

Blokira se **samo terminal na kojem je incident** (I3/I4). `wait()`/`notifyAll()` na terminalu je pravi primitiv — aktivno čekanje bi trošilo procesor tokom uviđaja od 3–10s.

**Kritično:** `PrikazTerminala.render()` uzima isti ključ. Ako se ikad uđe u `wait()` ili `Thread.sleep()` **držeći** `synchronized (terminal)`, GUI se zamrzava. Trenutno nijedan `synchronized (terminal)` blok u `BrodThread`-u ne spava — to svojstvo mora ostati.

**Odabrano:** `Terminal.blokirajSaobracaj()`/`odblokirajSaobracaj()` su čisto čitanje/pisanje
`volatile` zastavice, ne `wait()`/`notifyAll()` kako je ovdje prvobitno predloženo — jednostavnije
i dovoljno, jer niko ne čeka DA se terminal odblokira preko ovog mehanizma, samo se svaki pokušaj
pomjeranja provjerava. `KoordinatorUvidjaja` nikad ne uzima `synchronized(terminal)`; svoje
sopstveno čekanje (dolazak patrola, trajanje uviđaja) radi na `Thread.sleep()`/park-ključu, van
bilo kog ključa terminala. Provjereno i redoslijedom odblokiraj → ugasi rotaciju → raspetljaj
(vidi "Riješeno 9. avgusta", pasus o redoslijedu) — obrnut redoslijed bi patrolu na povratku
zamrznuo usred terminala.

### D5 — Determinizam sudara u testovima — ✅ RIJEŠENO (5. avgust, priprema — logika uviđaja povezana 9. avgusta)

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

### D6 — Šta ide u binarni fajl incidenta? — ✅ RIJEŠENO (8. avgust infrastruktura, 9. avgust povezano)

`Incident implements Serializable`, jedan fajl po slučaju u `user.home` (I6/I7). Sadržaj: učesnici sudara, službena plovila, vrijeme, fotografije.

Odluka: čuvati **putanje** do fotografija, ne bajtove. Putanje su manje i jednostavnije, bajtovi su samodovoljni ali fajl raste. Povezano sa O2 (putanje relativne na radni direktorijum).

**Povezano:** `KoordinatorUvidjaja` konstruiše `Incident` sa stvarnim učesnicima sudara i
odazvanim patrolama i poziva `sacuvaj()` na kraju svakog uviđaja (Korak 3).

### D7 — Šta poslije uviđaja? — ✅ RIJEŠENO (9. avgust)

Profesor je oba rješenja odobrio, uz preferencu:

- **Službena plovila:** napuste terminal _ili_ se privežu na prvi slobodan dok. Preferira se privezivanje, „kako bi se mogla službena vozila ponovo aktivirati" — što je direktno relevantno zbog O-nalaza o oskudici vatrogasaca.
- **Učesnici sudara:** „Neka napuste terminal" — ovdje je odgovor jednoznačan.

**Odabrano:** tačno preferirana opcija za oba slučaja — službena plovila pokušavaju
`Terminal.rezervisiSlobodanDok()` i vraćaju se u `PRIVEZAN` ako uspije, inače napuštaju; učesnici
sudara uvijek napuštaju, presretnuti u `BrodThread.udjiULuku()` na tačci uspješnog privezivanja
(vidi Korak 5).

### Podsjetnik — vratiti sudare — ✅ RIJEŠENO (9. avgust)

`BrodThread.SUDARI_OMOGUCENI` je trenutno `false` (privremeno isključeno radi determinizma testova poslije R0). Mora nazad na `true` kad R4 bude gotov. To je jedino namjerno odstupanje od specifikacije koje trenutno postoji u projektu (I1).

Vraćeno na `true` kao podrazumijevanu vrijednost u "Na kraju" R4b-a. Testovi iz R0–R5 ere koji
pokreću stvarnu navigaciju a ne tiču ih se sudari zaštićeni su klasnim `@BeforeEach` u
`BrodThreadTest` koji spušta `VJEROVATNOCA_SUDARA` na `0.0` (vidi "Riješeno 9. avgusta: R4b").

### NOVA OTVORENA ODLUKA

Ulazni timestampovi se prenose kroz sesije iz "luka.ser" (omogucava perzistenciju simulacije), tako da plovilo koje je juce dokovano sada placa stvarne protekle sate, a ne striktno sate unutar simulacije. Da li resetovati brojac pri ponovnom pokretanju, ili koristiti neku vrstu vremenskog skaliranja unutar simulacije koje ce garantovati realne cijene i realne sate naplacene, a ne ekstremno velike vrijednosti samo zato sto je laptop ostao ugasen par dana?

## Riješeno 15. avgusta: Administratorski GUI (A1–A13)

Novi paket `gui`, izgrađen na postojećem `model`/`util` sloju bez ijedne izmjene u `simulation`,
`model` ili `util`.

### Odluka o toolkit-u (blokirala je početak)

Odgovor profesora o JavaFX-u nije stigao. Odabran **Swing/AWT**: `FileDialog` je AWT komponenta
koju specifikacija imenuje direktno, nulta Maven zavisnost (JavaFX je od Java 11 odvojen modul,
rizik po tuđem laptopu), a `PrikazTerminala.render()` je već toolkit-agnostičan
(`String[][]`) pa izbor toolkit-a ne diktira arhitekturu modelnog sloja.

### Arhitektura — model/logika odvojeni od Swing komponenti

Pet klasa bez ijednog Swing/AWT importa, sve pod `test/.../gui/` pokrivene testovima
(ukupno 31 nov test):

- **`TipPlovila`** — enum sa svih devet postojećih konkretnih kombinacija trup×služba.
  `odObjekta(Plovilo)` mapira nazad na tip preko direktnog `instanceof` (sve klase su listovi
  hijerarhije, redoslijed provjera nije bitan).
- **`PlovilaFabrika`** — jedina tačka koja poziva konstruktore plovila iz sirovih vrijednosti
  forme; drži svih devet poziva konstruktora na jednom mjestu umjesto razbacano po dijalogu.
- **`PlovilaValidator`** — IMO ne smije biti prazan ni duplikat *u cijeloj luci* (ne samo na
  odabranom terminalu) — sken cijele matrice svih terminala plus `Luka.getEvidencijaUlaska()`,
  po uzoru na `GeneratorPlovila.obezbijediJedinstvenostImoZa()` (plovilo može biti serijalizovano
  usred kanala, ne samo na doku). Parametar `izuzetiImo` isključuje sopstveni raniji IMO plovila
  pri izmjeni bez promjene IMO-a — bez ovoga bi svaka izmjena postojećeg plovila lažno prijavila
  duplikat sama sa sobom.
- **`UredjivanjePlovilaService`** — dodavanje/izmjena/brisanje isključivo kroz `Terminal`,
  nikad direktnim upisom u `Polje` van `synchronized(terminal)` bloka. Dodavanje ponavlja isti
  obrazac kao `BrodThread.udjiULuku()`: `rezervisiSlobodanDok()` → fizičko postavljanje →
  `otkaziRezervaciju()` — isti K6 razlog (rezervacija se mora osloboditi i na uspješnoj grani, ne
  samo na neuspješnoj). Izmjena zamjenjuje referencu na plovilo na *istom* doku (nema nove
  rezervacije), pa broj slobodnih vezova ostaje netaknut.
- **`PregledTerminalaService`** — tabelarni prikaz (A4) i pretraga po IMO za predpopunjavanje
  forme pri izmjeni.

### Odluka: A5 (Pokreni klijentsku aplikaciju) nema šta pravo da pokrene još

`Main.java` je i dalje prazan skelet — klijentska GUI aplikacija (C5/C7/C8) je eksplicitno budući
zadatak, poslije admin GUI-ja. Dugme ipak radi tačno ono što traži A12/A13 redoslijed
(`serijalizujStanjeLuke()` pa tek onda otvaranje prozora), a otvoreni prozor
(`KlijentskiProzor`) je namjerno **samo statički snimak** — kombo za terminal +
`PrikazTerminala.renderAsText()` u `JTextArea`-i, bez ijedne niti i bez auto-osvježavanja. Ovo
daje dugmetu iskreno, radno ponašanje uz postojeću klasu građenu tačno za ovu svrhu
(`PrikazTerminala`-in vlastiti komentar je već govorio "za prikaz u GUI-ju, C5, još TODO"), a da se
ne zadire u C5-ov opseg (živi prikaz, odlazak plovila, dinamičko dodavanje tokom simulacije).

### Ostale manje odluke

- Admin aplikacija se pokreće preko `AdminProzor.main()` (entry point unutar `gui` paketa) — `Main.java`
  nije diran, njegov postojeći komentar već kaže da je wireovanje admin/klijent GUI-ja budući posao.
- Učitavanje (`SerializationUtil.ucitajStanjeLuke()`) i serijalizacija idu kroz `SwingWorker`, ne
  direktno na EDT-u — eksplicitan zahtjev zadatka ("ne blokiraj EDT... čitanje fajlova i
  serijalizacija mogu trajati").
- Nema `PokretacSimulacije.rasporediNaSlucajneDokove`/`dopuniDoMinimuma` poziva iz GUI-ja — obje su
  već označene kao setup-only (ne smiju se pozivati dok postoje žive korisničke niti; admin
  aplikacija ionako nikad ne pokreće `BrodThread`-ove).
- Tip plovila se bira u glavnom prozoru (padajući meni, A2) prije otvaranja forme — forma sama
  (A7) gradi polja jednom, za taj fiksni tip; izmjena ne dozvoljava promjenu tipa (to bi značilo
  brisanje + novo dodavanje, dosljedno tome što je tip ugrađen u hijerarhiju klasa, ne u jedno
  polje).

### Popravke nakon code review-a (`GUI_KORAK1_PREGLED.md`, 15. avgust)

Tri nalaza, sva popravljena isti dan.

**G1 (blokirao nastavak).** `izmijeniPlovilo` je zamjenjivao cijeli objekat plovila novom
instancom iz `PlovilaFabrika` — a `Plovilo`-in konstruktor generiše `brzina` slučajno pri svakom
pozivu. Administrator koji samo ispravi slovnu grešku u nazivu je nesvjesno mijenjao brzinu
plovila, kršeći invarijantu koju `SerializationUtilTest.plovilaPrezivljavajuRoundTrip` izričito
tvrdi (brzina se čuva, ne regeneriše). Popravka: `izmijeniPlovilo` prenosi `brzina` (i, ako je
plovilo `SluzbenoPlovilo`, stanje `rotacija`) sa starog objekta na novi prije zamjene na doku —
oboje je stanje koje admin forma namjerno ne izlaže (rotacija se uključuje/isključuje isključivo
kroz `KoordinatorUvidjaja`/`BrodThread`, ne kroz admin formu), pa mora preživjeti izmjenu
nepromijenjeno. Novi testovi: `izmjenaNazivaNeMijenjaBrzinu`, `izmjenaCuvaRotaciju`.

**G2.** `dodajPlovilo` je pozivao `Terminal.rezervisiSlobodanDok()`/`otkaziRezervaciju()` —
mehanizam namijenjen simulaciji, gdje plovilo putuje ka vezu kroz više koraka i rezervacija
sprečava trku dva broda za isti vez (R2/K5). Admin plovilo se postavlja odmah, u jednom koraku,
pa je rezervacija bila suvišan trostepeni put (rezerviši → postavi → otkaži) koji otvara prozor
u kojem je vez lažno rezervisan, i ostavlja curenje ako bi izuzetak pukao između koraka. Popravka:
jedan `synchronized(terminal)` blok, direktan upis u `Polje`. Bezbjedno je **samo** zato što admin
aplikacija radi dok simulacija ne radi (nema `BrodThread`-ova koji bi se takmičili za isti vez) —
komentar iznad metode to eksplicitno kaže, isto upozorenje kao nad
`PokretacSimulacije`-ovim setup-only metodama.

**G3 (srednje).** `PlovilaValidator` je odbijao IMO broj ako je postojao u
`Luka.getEvidencijaUlaska()`, bez obzira da li je plovilo još fizički u luci. Evidencija je zapis
za naplatu taksi i nikad se ne prazni (K-nalazi iz `PRONALASCI.md`), pa je IMO broj plovila koje je
davno napustilo luku ostajao trajno zabranjen — administrator poslije nekoliko simulacija ne bi
mogao ponovo upotrijebiti taj broj. Popravka: `PlovilaValidator` provjerava jedinstvenost samo
kroz fizičku matricu (stvarno prisustvo), ne kroz evidenciju; `dodajPlovilo` sada briše zaostali
zapis evidencije za taj IMO neposredno prije postavljanja na dok, tako da novo plovilo (ili
ponovo iskorišten broj) ne naslijedi tuđe vrijeme ulaska preko `putIfAbsent`-a u
`Luka.addToEvidencija()`. Namjerno **ne** i u `izmijeniPlovilo` — brisanje evidencije pri izmjeni
bi resetovalo vrijeme ulaska (i time taksu) plovilu koje je stvarno stiglo kroz simulaciju i sada
se samo uređuje kroz GUI, što bi bio novi bug, ne popravka. Novi testovi:
`imoSeMozePonovoIskoristitiNakonBrisanja`, `dodavanjeBrisePreostaluEvidenciju`,
`PlovilaValidatorTest.imoUEvidencijiBezFizickogPrisustvaNeBlokira` (zamjenjuje raniji
`duplikatImoUEvidenciji`, koji je tvrdio suprotno ponašanje).

**Sitnije, takođe primijenjeno:** `PregledTerminalaService.redovi()`/`pronadjiPlovilo()` sada
čitaju matricu terminala unutar `synchronized(terminal)`, po uzoru na
`PrikazTerminala.render()` — dok simulacija ne radi razlika je kozmetička, ali dosljednost
("svako čitanje matrice ide pod istim ključem") je jeftina i vrijedna zadržati. Provjereno da
`AdminProzor` već traži plovilo po IMO broju (kolona 0), ne po indeksu reda tabele — nalaz o
tome da `redovi()` preskače prazne vezove (pa indeks reda ≠ oznaka veza) nije zahtijevao izmjenu,
samo potvrdu.

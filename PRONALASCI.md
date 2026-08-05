# Retroaktivni testovi — nalazi i uputstvo

Testovi su pisani prema **specifikaciji**, ne prema trenutnom kodu. Dio njih namjerno pada.

## Podešavanje (IntelliJ, bez Mavena/Gradlea)

1. Raspakuj `test/` u korijen projekta, pored `src/`.
2. Desni klik na folder `test` → **Mark Directory as → Test Sources Root**.
3. `File → Project Structure → Libraries → + → From Maven…` i dodaj:
   - `org.junit.jupiter:junit-jupiter:5.10.2`
   - `org.junit.platform:junit-platform-launcher:1.10.2`
4. Pokretanje: desni klik na `test` → **Run 'All Tests'**.

Testovi označeni sa `@Tag("bug")` mogu se izolovati kroz Run Configuration → Tags → `bug`.

> **Preporuka:** pređi na Maven prije nego što nastaviš. Ručno kačenje jarova će te
> koštati vremena na svakoj mašini na kojoj ovo pokreneš, uključujući i onu na odbrani.

## Tri kategorije testova

| Oznaka | Značenje | Šta raditi |
|---|---|---|
| bez oznake (A) | Prolazi na trenutnom kodu | Zaštitna mreža — ne diraj |
| `@Tag("bug")` (B) | **Pada** i ukazuje na stvarni nedostatak | Popravi kod, ne test |
| `@Disabled` (C) | Čeka odluku ili refaktor | Odluči, pa uključi |

## Očekivani rezultat prvog pokretanja

Približno **48 testova prolazi**, **13 pada**. Padovi nisu greška u testovima.

## Nalazi, po ozbiljnosti

### K1 — Brodovi plove kroz dokove (kritično)

`BrodThread` vodi brod niz kolonu 0, pa ga na `pomjeriNaPolje(3, 1)` prebacuje u **red 3**
i dalje ga kreće uzduž tog reda. Red 3 je red dokova. Redovi 1 i 2 — jedini stvarni
horizontalni plovni kanal iz šeme — ne koriste se nikada.

Posljedice se granaju: brod u prolazu obara `getBrojSlobodnihVezova()`, `Dok.isSlobodan()`
laže, i dva broda se "sudaraju" na tuđem vezu.

Testovi: `brodNeSmijeProlazitiKrozDokove`, `horizontalniKanalSeKoristi`,
`horizontalniKanalMoraBitiOznacenCijelomDuzinom`.

**R0 (prvo što treba uraditi):** kretanje po redu 2 udesno, po redu 1 ulijevo,
ulazak u dok kao pomjeraj za jedan red gore/dolje sa kanala.

### K2 — Nema modela incidenta

`proveriRizikOdUdesa()` baca kockicu na jednom brodu, uspava sopstvenu nit i završi.
Nema drugog učesnika, nema slanja službenih plovila, nema blokade terminala,
nema binarnog fajla. Specifikacija traži sve četiri stvari.

**R4:** klasa `Incident` (učesnici, vrijeme, fotografije, `Serializable`) +
koordinator koji bira najbliža službena plovila i drži blokadu na nivou terminala.

### K3 — Rotacija se ne može uključiti polimorfno — ✅ RIJEŠENO (4. avgust)

`setRotacija()`/`isRotacija()` su bili duplirani u šest klasa i nisu postojali u nadtipu.

**R1 urađeno:** dodat `SluzbenoPlovilo` (`model/interfaces/SluzbenoPlovilo.java`) sa
`isRotacija()`/`setRotacija(boolean)`. `ObalskaStraza`, `Carina` i `Vatrogasci` sada
`extends SluzbenoPlovilo` — sve šest konkretnih klasa već su imale tačno te metode, pa
im nije trebala nikakva izmjena da bi zadovoljile interfejs. Šest kopija polja `rotacija`
**ostaje** (Java nema višestruko nasljeđivanje stanja, a klase već nasljeđuju tri različita
konkretna tipa — `KontejnerskiBrod`/`PutnickiKruzer`/`Tanker`); dobitak je što pozivalac sada
može da radi `if (plovilo instanceof SluzbenoPlovilo sp) sp.setRotacija(true);` bez
`instanceof` lanca po sve tri kombinacije. Test `TipoviPlovilaTest.rotacijaSeMozeUkljucitiPolimorfno`
otključan i prolazi.

### K4 — Prioritet je mrtav kod — ✅ RIJEŠENO (4. avgust)

`getPrioritet()` je ispravno implementiran svuda, ali se do danas nigdje nije pozivao.

**R5 urađeno:** `BrodThread.ploviIstocno()` sada čita `plovilo.getPrioritet()`:
- plovilo pod aktivnom rotacijom (`getPrioritet() < 10`) preskače prag `PRAG_PRETICANJA`
  i pokušava preticanje čim je blokirano;
- obično plovilo (`getPrioritet() == 10`) provjerava novi statički helper
  `BrodThread.ustupaProlaz(terminal, x, y, trenutni)` — ako je plovilo pod rotacijom
  neposredno iza njega u istoj traci, ono se ne pomjera taj korak (ostaje na postojećem polju).
- Testovi `BrodThreadTest.obicnoPloviloUstupaProlazPloviluPodRotacijom`,
  `ploviloPodRotacijomNeUstupaProlazObicnom`, `obicnoPloviloNeUstupaProlazObicnom` i
  `ploviloPodRotacijomZavrsavaSimulaciju` zamjenjuju stari placeholder test
  (koji je bio hardkodovan da uvijek padne — `assertTrue(false, ...)` — i nije mogao
  proći ni nakon ispravke koda).

**Ispravka (4. avgust, poslije code review-a):** prva verzija `ustupaProlaz()` je
poredila `iza.getPrioritet()` protiv konstante `10` umjesto protiv `trenutni.getPrioritet()`,
pa je svako plovilo pod rotacijom (uključujući carinu, prioritet 3) rano izlazilo iz
metode i **nikada** nije ustupalo prolaz — ni vatrogascima (prioritet 1) iza sebe.
Ranija verzija je i keširala `imamPrioritet` van `while` petlje u `ploviIstocno()`, pa
plovilo kojem bi R4 upalio rotaciju usred tranzita ne bi dobilo prioritet do sljedećeg
poziva metode. Oba su ispravljena: `ustupaProlaz()` sada poredi `iza.getPrioritet() <
trenutni.getPrioritet()` (redoslijed vatrogasci > OS > carina > komercijalno ispada
prirodno iz poređenja, bez posebnog slučaja), a `imamPrioritet` se čita svaki korak
petlje. Dodati testovi `carinaUstupaProlazVatrogascimaPodRotacijom` i
`vatrogasciNeUstupajuProlazCariniPodRotacijom` pokrivaju redoslijed među samim
službenim plovilima — direktno relevantno za R4, gdje će se vatrogasci, obalska
straža i carina istovremeno slati na isti incident i takmičiti za iste ćelije kanala.

Preostalo, van obima za danas: šest kopija polja `rotacija` i dalje postoje (interfejs
je riješio polimorfni pristup, ne i deduplikaciju — vidi napomenu u R1 gore).

### K5 — Trka pri rezervaciji doka

Traženje slobodnog doka i njegovo zauzimanje su dva odvojena `synchronized` bloka.
Između njih drugi brod može uzeti isti dok.

**R2:** `public synchronized Dok rezervisiSlobodanDok(Plovilo p)` na `Terminal`-u —
pronađi i zauzmi u jednoj atomarnoj operaciji.

### S1 — Duplirano knjigovodstvo vezova — ✅ RIJEŠENO (4. avgust)

`Luka.brojSlobodnihVezova` je bila `Map<Terminal, AtomicInteger>` popunjena nulama
koja se nikada ne ažurira, dok `Terminal.getBrojSlobodnihVezova()` računa tačno.
Polje i njegovo punjenje u konstruktoru obrisani; test `mapaSlobodnihVezovaJeMrtvaIliTacna`
(dolazio je do polja refleksijom) takođe obrisan jer ne postoji šta da provjeri. Jedini
izvor istine ostaje `Terminal.getBrojSlobodnihVezova()`/`getBrojRaspolozivihVezova()`.

### S2 — `Luka` i `Polje` nemaju `serialVersionUID` — ✅ RIJEŠENO (4. avgust)

Dodat `serialVersionUID = 1L` u obje klase. Postojeći `luka.ser` (ako je nastao prije
ove izmjene) postaje nečitljiv — to je očekivano, aplikacija tretira `null` kao "prvo
pokretanje".

### S3 — `addToEvidencija()` nije sinhronizovana — ✅ RIJEŠENO (4. avgust)

`Luka.evidencijaUlaska` je sada `ConcurrentHashMap`, `addToEvidencija()` koristi
`putIfAbsent()` (atomarna provjera+upis). `BrodThread.evidentirajUlazak()` više ne radi
ručnu `synchronized`/`containsKey`/`put` sekvencu — samo poziva `luka.addToEvidencija(...)`.

### S4 — CSV se lomi na zarezu u nazivu — ✅ RIJEŠENO (4. avgust)

Naziv tipa `Luka, Kraljica Mora` je proizvodio sedam kolona umjesto šest.
`PokretacIzvjestaja.escapeCsv()` sada citira polje po RFC 4180 (navodnici kad sadrži
zarez/navodnik/novi red, unutrašnji navodnici udvojeni) i primjenjuje se na IMO, naziv i tip.
Iznos se formatira sa `Locale.US` da decimalna tačka ne postane zarez na lokalizovanim mašinama.

**Zamka otkrivena pri ovom radu:** naivni `String.split(",")` u testu ne poznaje RFC 4180
navodnike, pa bi i dalje izbrojao 7 "kolona" za red sa citiranim poljem (zarez unutar
navodnika ostaje u tekstu). Test `nazivSaZarezomNeRazbijaCsv` je dobio pomoćnu
`brojKolonaCsv()` koja poštuje navodnike; ista je primijenjena i u `csvImaIspravanBrojKolona`
radi konzistentnosti.

### S6 — `Plovilo` nema `equals`/`hashCode` — ✅ RIJEŠENO (4. avgust)

Poređenje je padalo na referentni identitet: isto plovilo učitano iz `luka.ser` u dvije
sesije nije bilo "jednako" samo sebi. IMO broj je jedini prirodan ključ identiteta (M1),
pa su `equals()`/`hashCode()` dodati u `Plovilo`, računati isključivo iz `imoBroj`.

**Zamka:** `setImoBroj()` postoji i mijenja polje koje sada određuje `hashCode()` — plovilo
već ubačeno u `HashMap`/`HashSet` bi se "izgubilo" u starom bucket-u nakon izmjene IMO broja.
Dokumentaciona odluka umjesto brisanja settera: postojeći protective-net test
`PloviloTest.setteriRade` direktno poziva `setImoBroj()` i ne smije se dirati, pa je setter
zadržan uz upozorenje u JavaDoc-u (opcija "B" iz `CISCENJE_I_R4_PRIPREMA.md`, ne
preporučena opcija koja briše setter — ovdje preporučena opcija nije bila primjenjiva).
`BrodThread.pomjeriNaPolje()` i dalje namjerno koristi `==` (referentni identitet) pri
provjeri `staro.getTrenutnoPlovilo() == this.plovilo` — to ostaje ispravno i ne smije se
mijenjati u `equals()`, jer bi dva različita plovila sa istim IMO brojem inače mogla da se
pomiješaju u matrici terminala.

### S5 — Hardkodovane putanje

`"luka.ser"` i `"takse.csv"` su relativne na radni direktorijum.
**R3:** preopterećenja metoda koja primaju putanju — usput čini testove čistijim.

## Redoslijed popravki

R0 (kanal) → R2 (rezervacija doka) → R1 (interfejs) → R5 (prioritet) → R4 (incident)

R0 je preduslov za sve ostalo: dok brodovi plove kroz dokove, svaki test kapaciteta
i svaki sudar mjeri pogrešnu stvar.

**Status (4. avgust):** R0, R2, R1, R5 i S1–S4 gotovi (vidi `CISCENJE_I_R4_PRIPREMA.md`
za detalje čišćenja). Test paket: **93 ukupno, 1 pad** (`sudarUkljucujeDvaPlovila`, čeka R4),
1 ignorisan (F2 zaokruživanje, otvoreno pitanje ispod). Preostaje: S5 (hardkodovane putanje,
R3 — nije bio dio ove runde čišćenja), zatim T1/A*/C*/F4, pa R4 (najveći pojedinačni blok).

**Status (5. avgust):** dodato C2 (`GeneratorPlovila`), C6 (`PrikazTerminala`), T1/C1/C3/C4
(`PokretacSimulacije`) i temeljni dio D3 iz K2 (enum `Zadatak`, `BrodThread` se više ne gasi pri
privezivanju nego parkira u `PRIVEZAN`, `Luka.aktivnaPlovila` kao registar za D2) — detalji u
`ZAHTJEVI.md`, sekcije "Riješeno 5. avgusta". Test paket: **135 ukupno, 1 pad** (isti,
`sudarUkljucujeDvaPlovila`, čeka R4). K2/R4 ostaje najveći preostali blok, ali D3 (kako plovilo
mijenja cilj usred rute) i D2 (gdje se traži najbliža patrola) sada imaju infrastrukturu koju
uviđaj treba samo da iskoristi (`Zadatak.KA_INCIDENTU`/`NA_INCIDENTU` postoje kao vrijednosti
enuma ali ih još ništa ne postavlja; `getX()`/`getY()`/`getTrenutniTerminal()` na `BrodThread`-u
i `Luka.getAktivnaPlovila()` su spremni za pretragu najbliže patrole).

## Otvoreno pitanje za tebe

`Duration.toHours()` reže naniže, pa 90 minuta = 100 KM. Ako profesor očekuje
zaokruživanje naviše, to je 200 KM. Test postoji kao `@Disabled` — odluči i dokumentuj.

## Riješeno (4. avgust): kontradikcija u `TipoviPlovilaTest`

Otkriveno 4. avgusta pri radu na R1/R5: `bezRotacijeNemaPrioriteta` (protective net) i
`poljePrioritetJeMrtvoUSluzbenimKlasama` (`@Tag("bug")`) su tražili suprotne vrijednosti
za `getPrioritet()` na istom, netaknutom objektu — 10 naspram 1.

Razriješeno po analizi iz `CISCENJE_I_R4_PRIPREMA.md`: `bezRotacijeNemaPrioriteta` je bio
u pravu (M5 — prioritet važi samo *pod rotacijom*), a pravi problem je bio što su
konstruktori šest službenih klasa prosljeđivali `super(..., N)` vrijednost koju override
`getPrioritet()` nikad nije čitao — mrtav parametar, ne mrtvo pravilo. Ispravka: svaka
službena klasa sada ima imenovanu `public static final int PRIORITET_POD_ROTACIJOM`, a
override glasi `isRotacija() ? PRIORITET_POD_ROTACIJOM : super.getPrioritet()` —
konstruktor službene klase više uopšte ne prosljeđuje prioritet; poziva se
šestoargumentski `super(...)` osnovnog tipa (`KontejnerskiBrod`/`PutnickiKruzer`/`Tanker`),
koji već postavlja 10 kao podrazumijevani prioritet u `Plovilo`. Test `poljePrioritetJeMrtvoUSluzbenimKlasama`
zamijenjen sa `prioritetPodRotacijomJeImenovanaKonstanta`, koji provjerava i slučaj bez
rotacije (10) i slučaj sa rotacijom (poređenje protiv imenovane konstante, ne magičnog broja).

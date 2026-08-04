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

### S1 — Duplirano knjigovodstvo vezova

`Luka.brojSlobodnihVezova` je `Map<Terminal, AtomicInteger>` popunjena nulama
koja se nikada ne ažurira, dok `Terminal.getBrojSlobodnihVezova()` računa tačno.
Obriši mapu.

### S2 — `Luka` i `Polje` nemaju `serialVersionUID`

Prva izmjena bilo koje od tih klasa učiniće postojeći `luka.ser` nečitljivim.

### S3 — `addToEvidencija()` nije sinhronizovana

`HashMap` + više niti = tihi gubitak upisa. `ConcurrentHashMap` rješava.

### S4 — CSV se lomi na zarezu u nazivu

Naziv tipa `Luka, Kraljica Mora` proizvodi sedam kolona umjesto šest.

### S5 — Hardkodovane putanje

`"luka.ser"` i `"takse.csv"` su relativne na radni direktorijum.
**R3:** preopterećenja metoda koja primaju putanju — usput čini testove čistijim.

## Redoslijed popravki

R0 (kanal) → R2 (rezervacija doka) → R1 (interfejs) → R5 (prioritet) → R4 (incident)

R0 je preduslov za sve ostalo: dok brodovi plove kroz dokove, svaki test kapaciteta
i svaki sudar mjeri pogrešnu stvar.

**Status (4. avgust):** R0, R2, R1 i R5 gotovi. Preostaje R4 (najveći pojedinačni blok).

## Otvoreno pitanje za tebe

`Duration.toHours()` reže naniže, pa 90 minuta = 100 KM. Ako profesor očekuje
zaokruživanje naviše, to je 200 KM. Test postoji kao `@Disabled` — odluči i dokumentuj.

## Novo otvoreno pitanje (otkriveno 4. avgusta, pri radu na R1/R5)

`TipoviPlovilaTest` ima **dva testa koja se međusobno isključuju** i ne mogu oba proći:

- `bezRotacijeNemaPrioriteta` (bez oznake, trenutno prolazi — zaštitna mreža) očekuje da
  novokreiran `TankerVatrogasci` (rotacija ugašena, podrazumijevano) ima `getPrioritet() == 10`.
- `poljePrioritetJeMrtvoUSluzbenimKlasama` (`@Tag("bug")`, trenutno pada) očekuje da **isto**
  novokreirano plovilo, i dalje bez rotacije, ima `getPrioritet() == 1` — tvrdeći da je
  vrijednost proslijeđena konstruktoru (`super(..., 1)`) "mrtav kod" jer je override
  `isRotacija() ? 1 : 10` ignoriše.

Ovo nije samo duplikacija magičnog broja — test doslovno traži da prioritet važi i **bez**
upaljene rotacije, što je suprotno M5 iz specifikacije ("ukoliko je upaljena rotacija, ta
plovila imaju prioritet") i suprotno `bezRotacijeNemaPrioriteta`. Nisam dirao ni kod ni
test za ovo jer nije bio dio R1/R5 obima i jer bi "popravka" u bilo kom smjeru pokvarila
jedan od druga dva testa. Moguća čista popravka (van obima za danas): promijeniti
override u `isRotacija() ? super.getPrioritet() : 10` da konstruktorska vrijednost
prestane biti mrtvi kod *kada je rotacija upaljena*, ali to i dalje ne bi zadovoljilo
`poljePrioritetJeMrtvoUSluzbenimKlasama` u trenutnom obliku — taj test bi tada trebalo
prepisati ili izbrisati. Odluči i dokumentuj, kao i za F2 iznad.

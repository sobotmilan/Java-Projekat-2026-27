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

### K6 — Rezervacija veza se nikad nije oslobađala na uspješnom privezivanju — ✅ RIJEŠENO (9. avgust)

Nezavisna od K5 (koja se odnosi na trku u traženju), i predhodi R4 potpuno — nema veze sa
incidentima ili patrolama. `Terminal.rezervisiSlobodanDok(Plovilo)` upisuje redni broj veza u
`rezervisaniVezovi`, a `otkaziRezervaciju(Dok)` ga uklanja — ali `BrodThread.udjiULuku()` je
pozivala `otkaziRezervaciju` samo na **neuspješnoj** granici (`doploviDoDoka()` vrati `false`).
Na **uspješnoj** granici (plovilo stvarno stigne do veza i postane privezano), rezervacija je
ostajala upisana zauvijek — nikad oslobođena, ni pri samom privezivanju, ni kasnije pri
napuštanju terminala (`napustiTerminal()` fizički oslobađa ćeliju matrice preko
`oslobodiTrenutnoPolje()`, ali ne dodiruje `rezervisaniVezovi`).

Posljedica: `Terminal.getBrojSlobodnihVezova()` (čisto fizička provjera) je uvijek bila tačna,
ali `getBrojRaspolozivihVezova()` (fizički slobodan **i** nerezervisan — to je vrijednost koju
`udjiULuku()`/T7-T8 stvarno koriste za odluku "ima li mjesta") drifta naniže sa svakim uspješnim
privezivanjem tokom trajanja JVM procesa, bez obzira koliko plovila kasnije napusti luku. Na
dovoljno dugoj simulaciji (ili dovoljno dugom demo sešnu bez ponovnog pokretanja) bi terminal
počeo izgledati trajno pun i odbijati nova plovila iako je fizički prazan — čist "vessels stop
entering after a while" simptom, klasičan za otkriti tek na demonstraciji, ne u kratkim testovima.

Otkriveno usput tokom R4b (Korak 4/5), gradeći `otidjiNaIncident()`/`vratiSeNaDok()` za patrole
koje se vraćaju na incident i ponovo traže dok preko `rezervisiSlobodanDok()` u istoj sesiji —
tamo bi se ista stara rezervacija odmah pokazala (patrola bi izgubila sopstveni upravo napušteni
vez kao kandidata), što je i navelo na trag. Popravka je opšta, ne samo za incident-tok: jedan
novi `t.otkaziRezervaciju(rezervisan)` pozvan u `BrodThread.udjiULuku()` odmah nakon uspješnog
`doploviDoDoka()`, prije postavljanja `isPrivezan = true`.

Regresioni test `BrodThreadTest.vezPostajeRaspolozivIPoRezervacijiNakonNormalnogNapustanja`:
normalno privezivanje i `zatraziNapustanje()` (bez incidenta), provjerava da
`getBrojRaspolozivihVezova()` poslije napuštanja odmah opet iznosi 30, ne 29.

### K7 — Svjesna odluka: redoslijed gašenja rotacije naspram povratka patrole (9. avgust, revizija)

Nalaz iz konsolidovane revizije (`R4B_GRESKE.md`, G7). `KoordinatorUvidjaja.run()`-ov `finally`
blok je gasio rotaciju odazvanih patrola **prije** `raspetljajPatrole()` (poziva koji budi patrolu
preko `zavrsiUvidjaj()` da krene ka novom doku ili napusti). Redoslijed je promijenjen —
`raspetljajPatrole()` sada ide prije gašenja rotacije.

Bitno je reći šta ova izmjena **ne** rješava potpuno: `raspetljajPatrole()` samo *budi* patrolinu
nit (sinhrono, brzo) — stvarni fizički povratak do novog doka (`BrodThread.vratiSeNaDok()`) se
odvija asinhrono, u patrolinoj sopstvenoj niti, **nakon** što `finally` blok (uključujući i
gašenje rotacije) već završi. To znači da patrola i dalje provede najveći dio povratnog puta bez
rotacije, bez obzira na redoslijed unutar `finally`-ja.

Zašto je ovo i dalje prihvatljivo, ne prava popravka: u trenutku kad povratak počne, blokada NA
TOM terminalu je već skinuta (`odblokirajSaobracaj()` je pozvan ranije u istom bloku) — rotacija
tokom povratka nije potrebna kao izuzeće od blokade, samo bi dala prioritet u odnosu na obično
plovilo (R5), što specifikacija ne traži za povratni put. Rizik koji ostaje, i koji ova izmjena
ne uklanja: ako se **novi** sudar na **istom** terminalu desi baš u tom prozoru (patrola još hoda
ka doku, novi `KoordinatorUvidjaja` odmah zove `blokirajSaobracaj()`), patrola se zaustavi kao
obično plovilo i ostaje zaglavljena do isteka `maxBlokadaPokusaja()` budžeta u
`pomjeriSaCekanjem()` (ne zauvijek — taj budžet postoji upravo za ovakve slučajeve — ali odustaje
od povratka na dok umjesto da priđe s prioritetom). Uzastopni sudari na istom terminalu u tako
kratkom prozoru su rijetki; svjesno se ne rješava dodatnim mehanizmom (npr. čekanje da patrola
stvarno stigne pre gašenja rotacije bi zahtijevalo da koordinator blokira na tuđoj niti, kršeći
D4) dok se ne pokaže da je stvarni problem u praksi.

### K8 — Trka između "stigla" (pozicija) i "spremna" (zadatak) je nečujno gutala signal kraja dolaska — ✅ RIJEŠENO (9. avgust)

Otkriveno empirijski, ne čitanjem koda: nakon G7 popravke, `KoordinatorUvidjajaTest.
sluzbenoPloviloSeVracaNaSlobodanDokNakonUvidjajaIRotacijaSeGasi` je u ponovljenim izolovanim
pokretanjima (15 uzastopnih) pao **5 od 15 puta** (33%) sa `expected: <PRIVEZAN> but was:
<NA_INCIDENTU>` — patrola bi zauvijek ostala zaglavljena na incidentu. Ovo je konkretno ono na
šta upozorava "Redoslijed" u `R4B_GRESKE.md": jedan zeleni prolaz ne isključuje ovakvu grešku, a
33% stopa pada je previsoka da bi bila slučajnost — potvrđeno ponovnim pokretanjem istog testa
20× nakon popravke, 0 padova.

Uzrok — klasična trka između dva različita signala "da li je patrola stigla":
`KoordinatorUvidjaja.sacekajDolazakPatrola()`/`stiglaPored()` je provjeravala samo **fizičku
poziciju** (`getX()`/`getY()`), dok `BrodThread.zavrsiUvidjaj()` prihvata poziv samo ako je
`zadatak == Zadatak.NA_INCIDENTU`. U `otidjiNaIncident()`, pozicija se ažurira (kroz
`napredujKaPolju()`) **prije** nego što se `this.zadatak = Zadatak.NA_INCIDENTU;` izvrši — dvije
odvojene linije, ne atomarna operacija. Ako koordinatorova provjera stigne baš u tom procjepu
(pozicija već tačna, zadatak još nije), `sacekajDolazakPatrola()` zaključi da je patrola stigla i
odmah nastavi (uviđaj je kratak u testovima, ~50ms), stigne do `raspetljajPatrole()` i pozove
`zavrsiUvidjaj()` — čiji guard u tom trenutku još vidi stari zadatak (`KA_INCIDENTU`), odbija
poziv kao no-op. Tek nakon toga patrolina nit konačno upiše `NA_INCIDENTU` i uđe u
`cekajKrajUvidjaja()` — čekajući signal koji se **već desio i bio odbačen**. Koordinator je
gotov, niko drugi neće ponovo pozvati `zavrsiUvidjaj()`; patrola čeka do sopstvenog
`maxCekanjeKrajaUvidjaja()` budžeta (G1: `MAX_CEKANJE_DOLASKA_MS + MAX_TRAJANJE_UVIDJAJA_MS +
5000`, sa podrazumijevanim/neizmijenjenim `MAX_CEKANJE_DOLASKA_MS` to je 20+ sekundi) — mnogo
duže nego što ijedan test čeka.

Popravka: `stiglaPored()` sada **prvo** provjerava `patrola.getZadatak() ==
Zadatak.NA_INCIDENTU`, tek onda poziciju. Ovim se garantuje da koordinator nikad ne pređe u fazu
raspetljavanja dok patrolina nit stvarno ne upiše `NA_INCIDENTU` — u trenutku kad `zavrsiUvidjaj()`
konačno bude pozvan, guard ga sigurno prihvata (ništa drugo ne mijenja `zadatak` u međuvremenu).
Bezbjedno je i ako patrolina nit u međuvremenu i sama uđe u `cekajKrajUvidjaja()` prije poziva —
`while (zadatak == NA_INCIDENTU)` provjerava uslov pri ulasku u sinhronizovani blok, nema
propuštenog signala bez obzira na redoslijed (klasičan "provjeri-pa-čekaj" umjesto "čekaj-pa-nadaj
se notify-ju").

Ovaj tip greške (provjera stanja preko dva različita, nezavisno ažurirana polja) je vrijedan
opšti podsjetnik za ostatak `KoordinatorUvidjaja`/`BrodThread` interakcije — bilo gdje gdje jedna
strana čita poziciju a druga zadatak kao uslov, isti obrazac trke je moguć.

### K9 — Svjesne odluke pri implementaciji I5 (potjernica) + M6 (13. avgust)

I5 je stigao kao zaseban zahtjev (`R4_ZAVRSEN_I5_ZAHTJEV.md`), sa eksplicitnim ograničenjima
("ne blokiraj terminal", "ne diraj `KoordinatorUvidjaja` osim ako mora dijeliti kod", "bez
JavaDoc-a na novom kodu"). Nekoliko mjesta je zahtijevalo odluku koju specifikacija ne propisuje:

1. **Nema posebnog koordinatora/niti za potjernicu.** R4b je uveo `KoordinatorUvidjaja` kao
   posebnu nit baš zato što je trebalo blokirati terminal i koordinisati dolazak tri službe dok
   se druga plovila zaustavljaju iza blokade — detektujuća nit se nije smjela sama uspavati na
   3–10s jer bi to zamrznulo i njenu sopstvenu petlju. Kod potjernice terminal se **nikad** ne
   blokira (glavna razlika prema I3, po specifikaciji), pa taj razlog za posebnu nit ne postoji:
   `pokreniPotjernicu()`/`zavrsiPotjernicu()` rade sinhrono unutar niti obalske straže koja je
   detekciju i izvršila, bez ijednog `synchronized(terminal)` bloka koji bi obuhvatio čekanje.
   Ovo je direktna primjena uputstva "ne diraj `KoordinatorUvidjaja` osim ako mora dijeliti kod"
   — ovdje zaista ne mora.

2. **Traženo plovilo ne dobija posebnu "idi ka izlazu" rutu.** Specifikacija kaže da meta mora
   pratiti obalsku stražu ka izlazu iz luke, ali ne zahtijeva vizuelno praćenje ćelija po ćeliju.
   `napustiZbogPratnje()` samo oslobađa rezervaciju veza; sam izlazak koristi postojeći
   `napustiTerminal()` — identičnu putanju kojom bilo koje plovilo napušta terminal. Alternativa
   (nova ruta koja doslovno prati poziciju obalske straže) bi udvostručila logiku kretanja bez
   ikakvog dodatnog ispunjenja zahtjeva — oba plovila i dalje na kraju napuštaju terminal, što je
   ono što testovi (i specifikacija) stvarno provjeravaju.

3. **Detekcija provjerava sva četiri susjedna polja, na svakom uspješnom koraku — ne samo tokom
   preticanja.** `provjeriSudar()` (I1) se namjerno poziva samo u grani preticanja jer sudar
   pretpostavlja mimoilaženje dva plovila u suprotnim trakama. Potjernica nema taj preduslov —
   obalska straža mora prepoznati traženo plovilo i dok prolazi pored doka, ne samo dok pretiče
   u kanalu — pa `provjeriPotjernicu()` visi direktno u `ploviIstocno()`-ovoj glavnoj grani
   uspjeha, bezuslovno.

4. **`TipIncidenta` kao nova enumeracija + preopterećen konstruktor, ne novo polje na
   `KoordinatorUvidjaja`.** I5-prompt je izričito predložio marker tipa ako zatreba. Umjesto
   dodavanja zastavice `boolean jePotjernica` (lakše zaboraviti postaviti), nova `TipIncidenta{
   SUDAR, POTJERNICA}` enumeracija plus šestoargumentni `Incident` konstruktor — stari
   petoargumentni i dalje postoji i samo delegira sa `TipIncidenta.SUDAR`, pa ni jedan postojeći
   pozivalac (`KoordinatorUvidjaja`) nije morao biti izmijenjen.

5. **Novo `BrodThread.DIREKTORIJUM_INCIDENTA_POTJERNICE` (`static volatile File`), ne
   konstruktorski parametar.** `KoordinatorUvidjaja` prima direktorijum kroz konstruktor jer se
   pravi tačno u trenutku kad je incident već izvjestan. `BrodThread` se, nasuprot tome, pravi na
   samom početku simulacije za svako plovilo — davno prije nego što je poznato hoće li ono ikad
   učestvovati u potjernici. Isti obrazac injektovanja kao ostale D5 statike
   (`VJEROVATNOCA_SUDARA`, `MIN/MAX_TRAJANJE_UVIDJAJA_MS`): `null` znači podrazumijevano
   ponašanje (`incident.sacuvaj()` → `user.home`), testovi ga postave na privremeni direktorijum.

6. **Deterministička dodjela dokova u testovima preko `Terminal.rezervisiSlobodanDok()` na
   plovilu koje se nikad fizički ne pojavljuje, umjesto direktnog upisa u matricu.**
   `rezervisiSlobodanDok()` isključuje dok iz budućih dodjela sve dok se rezervacija eksplicitno
   ne otkaže (`otkaziRezervaciju()`) — pozivanjem te metode sa plovilom koje nikad ne dobija
   sopstvenu nit i nikad se ne otkazuje, testovi "rezervišu unaprijed" prvih N vezova i time
   tačno kontrolišu koji vez stvarna nit dobija (`Terminal.getDokovi()` je deterministički
   poredak — red 0 pa red 3, po koloni), a da pritom ne diraju `Polje`/matricu direktno. Isti
   trik omogućava da se detekcija u Koraku 3/4 testovima dogodi na tačno predvidljivoj koloni
   (obalska straža prolazi pored doka mete dok plovi ka sopstvenom, dalje dodijeljenom doku) —
   bez ijedne trke između stvarnih niti.

7. **Podjela uloga u `Incident`-u za potjernicu: traženo plovilo je "učesnik", obalska straža je
   "odazvano službeno plovilo".** `Incident` je dizajniran za sudar (dva "prekršioca" + službena
   plovila koja se odazivaju), a specifikacija ne kaže eksplicitno kako se ta dva polja
   preslikavaju na potjernicu koja ima samo jednog "prekršioca" (metu) i jedno odazvano službeno
   plovilo (koje ju je i pronašlo, ne treće). Odabrano preslikavanje čuva semantiku oba polja
   (`ucesniciSudara` = ko je "kriv", `odazvanaSluzbenaPlovila` = ko se odazvao) umjesto da se
   izmišlja treće polje samo za ovaj slučaj.

### K10 — Potjernica: cjelina zavisila od poziva sa strane, `PRACENJE` bio mrtav duplikat — ✅ RIJEŠENO (13. avgust, code review `I5_PREGLED.md`)

Dva nalaza iz spoljnog pregleda (`I5_PREGLED.md`), oba u istoj oblasti koda kao K9, oba popravljena istog dana.

**P1 (blokirao merge).** `pokreniPotjernicu()` je samo postavljala stanje (rotacija, `naPratnji`,
buđenje mete) i odmah se vraćala; stvaran `Thread.sleep()` od 3–5s, upis `Incident`-a i fizičko
napuštanje terminala su bili u zasebnoj `zavrsiPotjernicu()`, pozvanoj **isključivo** iz jedne
grane `udjiULuku()`-a. Dvije posljedice: (a) trajanje uviđaja se mjerilo od pogrešnog trenutka —
nekoliko poziva metoda i `otkaziRezervaciju()` kasnije nego stvarni trenutak detekcije; (b) da je
ikad postojao poziv `provjeriPotjernicu()`/`pokreniPotjernicu()` izvan te jedne grane (npr. za
plovilo koje je već privezano pa probuđeno), `zavrsiPotjernicu()` se nikad ne bi pozvala —
rotacija bi ostala trajno upaljena, `Incident` se nikad ne bi upisao. Provjereno da ta konkretna
putanja danas nije dostižna (`ploviIstocno()`, jedino mjesto koje poziva `provjeriPotjernicu()`,
ima tačno jednog pozivaoca — `doploviDoDoka()` — koji se poziva tačno jednom, iz `udjiULuku()`;
`otidjiNaIncident()`/`napredujKaPolju()`, put kojim ide već privezano pa probuđeno plovilo, ne
prolazi kroz `ploviIstocno()`), ali samodovoljnost je i dalje vrijedna popravka — dvije metode
koje moraju biti pozvane u tačno određenom redoslijedu, od strane tačno određenog pozivaoca, da bi
se izbjegla tiha greška, jesu upravo obrazac krhkosti koji je uzrokovao K8.

Popravka: `pokreniPotjernicu()` sad radi kompletan slučaj u jednom mjestu — budi metu, spava
3–5s, upisuje evidenciju, napušta terminal, gasi rotaciju — bez oslanjanja na to ko je poziva ili
odakle. `zavrsiPotjernicu()` obrisana. Metoda je promijenjena iz `private` u paket-privatnu da bi
test mogao direktno provjeriti samodovoljnost, izvan konteksta `udjiULuku()`-a (isti obrazac
vidljivosti kao `provjeriSudar()`/`provjeriPotjernicu()`). Jedno namjerno odstupanje od
konkretnog predloga u `I5_PREGLED.md`: predlog je tražio da se grana `if (this.naPratnji)` u
`udjiULuku()` obriše u cjelini; zadržana je (samo bez poziva `zavrsiPotjernicu()`, jer ta metoda
više ne postoji), jer bi njeno potpuno brisanje ostavilo petlju da nastavi na `idx++` i pokuša
naredni terminal — plovilo koje je `pokreniPotjernicu()` već fizički izvela iz luke bi pokušalo
ponovo ući, ovaj put kroz sljedeći terminal, kao da je prvobitni neuspjeh bio običan "terminal
privremeno pun". Provjereno testom (treći scenario ispod).

**P2.** `Zadatak.PRACENJE` je postavljan u `pokreniPotjernicu()` ali nigdje čitan — dva polja
(`zadatak == PRACENJE` i `boolean naPratnji`) opisivala su isto "obalska straža je u potjeri",
isti obrazac kao K8. Za razliku od K8, ovdje polja nemaju isti vijek trajanja: `naPratnji` mora
preživjeti do `run()`-ovog završnog logovanja, koje se izvršava **poslije** što `pokreniPotjernicu()`
već postavi `zadatak = NAPUSTA` na svom kraju — pa direktna zamjena `naPratnji` sa
`zadatak == PRACENJE` (prva, "dosljednija" opcija iz `I5_PREGLED.md`) kvari baš tu poruku (uvijek
bi vidjela `NAPUSTA`, ne `PRACENJE`, u trenutku provjere). Odabrana druga ponuđena opcija:
`Zadatak.PRACENJE` obrisan iz enuma, `naPratnji` ostaje jedini izvor istine za taj log. `POD_PRATNJOM`
(vrijednost za traženo plovilo, ne za obalsku stražu) ostaje — ona se stvarno čita, u
`cekajNapustanje()`-ovom uslovu i u `run()`-ovoj grani koja poziva `napustiZbogPratnje()`.

Nov test, `BrodThreadPotjernicaTest.pokreniPotjernicuRadiSamostalnoBezObziraOdakleJePozvana`:
poziva `pokreniPotjernicu()` direktno na već privezanoj (predokovani konstruktor + ručno
postavljen `Zadatak.PRIVEZAN`) obalskoj straži, van bilo kakve stvarne navigacije, i provjerava da
evidencija ipak nastaje i rotacija se ipak gasi — scenario koji je prije popravke tiho propadao
(kad bi bio dostižan).

**Sitnije iz istog pregleda, namjerno ostavljeno kako jeste:** `I5_PREGLED.md` predlaže spajanje
`BrodThread.DIREKTORIJUM_INCIDENTA_POTJERNICE` sa hipotetičkim ekvivalentom za obični uviđaj u
jedno polje, uz napomenu da nije hitno. Nema šta da se spoji — `KoordinatorUvidjaja` prima
direktorijum kroz konstruktorski parametar (pravi se tek kad je incident već izvjestan), dok
`BrodThread` nema tu privilegiju (pravi se za svako plovilo na početku simulacije, davno prije
nego što je poznato hoće li ono ikad učestvovati u potjernici) — vidi K9, odluku 5. Dva različita
mehanizma injektovanja za dvije stvarno različite okolnosti konstrukcije; primoravanje na
zajedničko ime bi zamaglilo tu razliku, ne pojasnilo je.

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

**Status (8. avgust):** R4a — infrastruktura za sistem incidenata (`Incident`, blokada
saobraćaja na terminalu preko `Terminal.smijeProci()`, `PretragaPatrole.najblizaPatrola()`
port-wide). Namjerno bez detekcije sudara, dispečovanja ili prelaza `Zadatak`-a — to je R4b.
Detalji u `ZAHTJEVI.md`, "Riješeno 8. avgusta: R4a". Test paket: **166 ukupno, 1 pad** (isti).

**Status (9. avgust):** K2/R4 zatvoreno — R4b (logika incidenta) urađen u pet koraka:
1) `provjeriSudar()` vraća oba učesnika iz grane preticanja, ne samo `boolean`; 2)
`PretragaPatrole` po konkretnoj službi i po dostupnosti; 3) `KoordinatorUvidjaja` (D1 — koordinator
posjeduje incident, ne plovila ni terminal) orkestrira blokadu/dispečovanje/uviđaj/upis; 4)
`BrodThread.pozoviNaIncident()` budi privezanu patrolu preko park-ključa (isti obrazac kao
`zatraziNapustanje()`); 5) raspetljavanje — službena plovila se vraćaju na prvi slobodan dok ili
napuštaju, učesnici sudara presretnuti na tačci uspješnog privezivanja u `udjiULuku()` i uvijek
napuštaju. Usput otkriven i ispravljen **K6** (rezervacija veza se nikad nije oslobađala na
uspješnom privezivanju — predhodi R4 potpuno, vidi K6 iznad). `BrodThread.SUDARI_OMOGUCENI`
vraćeno na `true` — jedino namjerno odstupanje od specifikacije u projektu je zatvoreno (I1).
Detalji i sve odluke (D1–D7, sada sve ✅) u `ZAHTJEVI.md`, "Riješeno 9. avgusta: R4b". Test
paket: **187 ukupno, 0 padova** — puni paket pokrenut tri puta zaredom bez varijacije.
K2/R4 (najveći preostali blok od početka retroaktivnog audita) je zatvoren. Preostaje: A*
(admin GUI) → C5/C8 (klijent GUI) → C7/E1/E2 (odlazak i kraj) → F4 (CSV na izlazu), i M6/I5
(spisak potjera, potjernica) kao samostalan zahtjev van obima R4b.

**Status (15. avgust):** Administratorski GUI (A1–A13) završen — Swing/AWT (odluka i obrazloženje
u `ZAHTJEVI.md`, "Riješeno 15. avgusta"), novi paket `gui`, `simulation`/`model`/`util` netaknuti.
Model/logika sloj (`TipPlovila`, `PlovilaFabrika`, `PlovilaValidator`,
`UredjivanjePlovilaService`, `PregledTerminalaService`) pokriven sa 31 novim testom; Swing
komponente (`PlovilaFormaDijalog`, `AdminProzor`, `KlijentskiProzor`) provjerene kompajliranjem i
smoke-testom pokretanja (nema izuzetaka pri startu), ali ne i ručnim klikanjem kroz UI u ovoj
sesiji — nije bio dostupan alat za snimanje ekrana/kontrolu miša da se to uradi vizuelno, pa ta
provjera ostaje otvorena za sljedeće pokretanje iz IDE-a.

Vanjski code review istog dana (`GUI_KORAK1_PREGLED.md`) je našao tri nalaza u modelnom sloju,
sva popravljena — detalji u `ZAHTJEVI.md`, "Popravke nakon code review-a": G1 (izmjena kroz formu
je tiho regenerisala brzinu plovila, kršeći invarijantu koju `SerializationUtilTest` tvrdi), G2
(nepotreban rezerviši→postavi→otkaži put u `dodajPlovilo`, zamijenjen jednim atomarnim upisom), G3
(provjera jedinstvenosti IMO-a je gledala i `evidencijaUlaska`, pa je broj davno otišlog plovila
ostajao trajno zabranjen — sužena na fizičko prisustvo, uz čišćenje zaostale evidencije pri
dodavanju). Test paket: **243 ukupno, 0 padova** (208 + 35 novih), pokrenut tri puta zaredom bez
varijacije. Sljedeće: C5/C7/C8 (klijent GUI, prikaz terminala, odlazak, dinamičko dodavanje) —
vidi `R4_POTVRDA_I_GUI_ZADATAK.md`.

**Status (15. avgust, kasnije istog dana):** Konačno poređenje cijele specifikacije protiv
`ZAHTJEVI.md` (`PROPUSTENI_ZAHTJEVI_V2.md`) je našlo šest nalaza (N1–N6), sva zatvorena isti dan.

Najveći: **F6 (skaliranje vremena)** — profesorov eksplicitan zahtjev, dotad vođen samo kao
"otvoreno pitanje" bez ijedne linije koda. Prvi predlog iz pregled-dokumenta (množenje stvarnog
proteklog vremena faktorom skaliranja) je provjerom matematike ispao **pogrešnog smjera** — bilo
koji faktor > 1 primijenjen na sirovu kalendarsku razliku pravi period-dok-je-aplikacija-ugašena
problem *gorim*, ne boljim (nema načina da skalar razlikuje "živo" vrijeme od "ugašeno" vrijeme).
Usvojeno rješenje razdvaja dvije stvari koje su u prvom predlogu bile pomiješane: `Luka` sad pamti
`vrijemeZadnjegCuvanja` i pri učitavanju pomjera cijelu evidenciju ulaska unaprijed za tačno
onoliko koliko je aplikacija bila zatvorena (`SerializationUtil.primijeniPauzu()`) — to je stvarna
ispravka; `PokretacIzvjestaja.FAKTOR_SKALIRANJA_VREMENA` (60×) je odvojen, kozmetički tempo koji
se primjenjuje tek na već "očišćeno" živo vrijeme, radi bržeg odigravanja tarifne ljestvice tokom
demonstracije. Puna analiza (uključujući zašto je prvi predlog odbačen) u `ZAHTJEVI.md`, "Riješeno
15. avgusta: F4/F5/F6".

**F4 (naplata pri izlasku)** je bio djelimično urađen mjesecima — `PokretacIzvjestaja` je ispravno
računala i pisala CSV, ali niko je nije pozivao kad plovilo stvarno napusti luku. Mapirana su tačno
tri fizička mjesta konačnog izlaska u `BrodThread` (normalan odlazak, prisilan izlazak učesnika
sudara, izlazak obalske straže poslije potjernice) — `napustiTerminal()` sâm nije bio bezbjedna
kuka jer se poziva i pri internom prelasku sa terminala na terminal (T7/T8), ne samo pri stvarnom
napuštanju luke.

**F5 (preuzimanje CSV-a)** i **N3/N4/N6** (login forma — svjesno preskočena; audit svih `catch`
blokova u `src/` za E3 — nijedan tiho ne guta grešku; T8/T9 dokumentacija — implementacija
"rezervacija prije ulaska" umjesto doslovnog "kružnog prolaska" kroz terminal) zatvoreni u istom
prolazu. Puna matrica zahtjeva u `ZAHTJEVI.md` ažurirana u cijelosti (A1–A14 su bili ostali na
TODO/PART otkad je admin GUI završen ranije istog dana — matrica nije bila sinhronizovana sa
stvarnim stanjem koda dok ovaj pregled to nije primijetio).

Test paket: **262 ukupno, 0 padova** (243 prije ovog kruga + 19 novih: 4 `LukaTest`, 4
`SerializationUtilTest`, 4 `PokretacIzvjestajaTest`, 3 `BrodThreadTest` za F4/F6, i 4 nova
`IzvjestajServiceTest` za F5).

**Status (15. avgust, treći prolaz istog dana):** Code review F5/F6 (`F5_F6_PREGLED.md`) je našao
dvije stvari, obje popravljene.

**F1 (blokirao) — pogrešno kalibrisan faktor skaliranja.** `FAKTOR_SKALIRANJA_VREMENA = 60L` ("1
stvarni minut = 1 simulacioni sat") je zvučalo razumno u izolaciji, ali stvarno trajanje boravka
plovila u luci tokom jedne simulacije je reda **sekundi**, ne desetina minuta (`trajanjeKoraka()`
20–400ms po polju, T11). Sa faktorom 60, prvi prag tarifne ljestvice (12h) bi zahtijevao 12
stvarnih *minuta* neprekidnog boravka — u praksi bi svako plovilo platilo tačno minimum od 100 KM,
i cijela ljestvica iz specifikacije se nikad ne bi vidjela na demonstraciji. Ispravljeno na
`3600L` (1 stvarna sekunda = 1 simulacioni sat) — ista greška klase kao "testovi prolaze ali
funkcionalnost nikad ne okine u stvarnoj upotrebi", vrijedan opšti podsjetnik: testovi koji
pozivaju logiku direktno sa izmišljenim vremenima (kao ovdje) ne hvataju kalibracijske greške u
odnosu na stvarno trajanje operacija koje ta logika mjeri — potreban je bar jedan test protiv
podrazumijevane vrijednosti i realističnog opsega ulaza, ne samo protiv proizvoljno odabranih
testnih vrijednosti koje uvijek "rade" bez obzira na kalibraciju.

**F2 (nisko) — `IzvjestajService` se oslanjao na spoljnu provjeru.** `AdminProzor` je već
ispravno pozivao `izvjestajPostoji()` prije `FileDialog`-a, ali sâma servisna metoda nije bila
samodovoljna — pozvana direktno bez te provjere, propustila bi sirov `NoSuchFileException`.
Dodata eksplicitna provjera sa čitljivom porukom (K10 obrazac). Reviewerov prateći predlog da se
doda JavaDoc na `IzvjestajService` kao "jedinu klasu bez njega" u `gui` paketu je provjeren i
odbačen — nijedna klasa u `gui` paketu nema JavaDoc, dosljedno eksplicitnoj instrukciji iz
`R4_POTVRDA_I_GUI_ZADATAK.md`; reviewerova premisa o trenutnom stanju koda nije bila tačna.

Detalji u `ZAHTJEVI.md`, "F1 ispravka"/"F2 ispravka" (unutar "Riješeno 15. avgusta: F4/F5/F6").
Test paket: **265 ukupno, 0 padova** (262 + 3 nova: 2 `PokretacIzvjestajaTest` za F1-regresiju i
faktor 3600, 1 `IzvjestajServiceTest` za F2), pokrenut tri puta zaredom bez varijacije.

**Status (15. avgust, četvrti prolaz):** `PlovilaFormaDijalog.pokusajSacuvaj()` je zaostajao za
međuvremenskim proširenjem `PlovilaValidator`-a (naziv/broj motora/registarski broj/spisak
potjernica) — na neuspjelom parsiranju specifičnog brojčanog polja i dalje je bezuslovno pozivao
`dodajPlovilo()`/`izmijeniPlovilo()`, čiji interni poziv validatora je proizvodio drugu,
redundantnu poruku o istom polju (npr. "TEU ne smije biti prazno" + "Kapacitet mora biti
pozitivna vrijednost" istovremeno). Popravljeno: greške se sada sakupljaju, `PlovilaValidator.
validiraj()` se poziva direktno i spaja sa specifičnom porukom, a `dodajPlovilo()`/
`izmijeniPlovilo()` se pozivaju samo ako je spojena lista prazna. Detalji u `ZAHTJEVI.md`,
"Ispravka nakon code review-a — `PlovilaFormaDijalog.pokusajSacuvaj()`". Test paket: **271
ukupno, 0 padova** (265 + 6 novih u `PlovilaValidatorTest`), pokrenut tri puta zaredom bez
varijacije.

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

### OTVORENO PITANJE

Package cycle — `model.classes.Luka` imports `simulation.BrodThread`, and `BrodThread` imports `Luka`. Caused by putting `aktivnaPlovila` on `Luka`. Compiles fine, but it's simulation state on a model class. Cleaner alternative if time permits: move the registry to `PokretacSimulacije`. Noted as a known design compromise, not a bug.
Postoji ciklus uvozenja - Luka uvozi BrodThread, BrodThread uvozi Luku. Donijeti odluku, nije nužno "bug"...
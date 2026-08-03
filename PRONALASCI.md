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

### K3 — Rotacija se ne može uključiti polimorfno

`setRotacija()`/`isRotacija()` su duplirani u šest klasa i nema ih u nadtipu.
Zato je u `BrodThread`-u linija `this.plovilo.setRotacija(true)` **zakomentarisana** —
kod nije mogao da je pozove.

**R1:** `public interface SluzbenoPlovilo { boolean isRotacija(); void setRotacija(boolean); }`,
pa neka `ObalskaStraza`, `Carina` i `Vatrogasci` naslijede taj interfejs.
Time nestaje i šest kopija istog polja.

### K4 — Prioritet je mrtav kod

`getPrioritet()` je ispravno implementiran svuda i **nigdje se ne poziva**.
Pravilo da plovilo pod rotacijom pretiče, a ostali staju, ne postoji.

**R5:** čitanje prioriteta u logici preticanja.

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

## Otvoreno pitanje za tebe

`Duration.toHours()` reže naniže, pa 90 minuta = 100 KM. Ako profesor očekuje
zaokruživanje naviše, to je 200 KM. Test postoji kao `@Disabled` — odluči i dokumentuj.

# Automatic native accessor discovery

## Plan and implementation

The v0.3.24 implementation follows this sequence:

1. Isolate accessor selection from quote/trade capture. Preserve historical neutrality, frozen quotes, view isolation and subscription behavior.
2. Inspect candidate bytecode before invocation. Resolve existing record getters by a pure field read; use known mappings and inspected instruction fingerprints for more complex accessors.
3. Resolve top-record fields through their declared book type. Require exactly one candidate; never use reflection order or live bid/ask values to break ties.
4. Resolve feed-hook method names by unique descriptor and staticness. Keep target-class anchors; reject duplicate loaded target classes or mismatched class loaders.
5. Cache both successful and failed resolution by Class identity, report failures in status, and preserve readable neutral trade rows on quote-path failure.
6. Test renamed members, ambiguous candidates, impure getters, unreadable bytecode, cache isolation and existing behavior. Inspect installed archives without executing their classes. Package a separately versioned agent.

## Matching rules

| Role | Rule |
| --- | --- |
| Existing market record | Public non-static zero-argument method returning `jclient.record.o`; executable body must be exactly `aload_0`, reference `getfield`, `areturn`. Require a unique candidate. Allocation, calls, branches and writes are rejected. |
| Instrument from a native event | Public zero-argument getter with a known instruction fingerprint; returned native type must itself have a resolvable existing-record getter. |
| Top record | Unique non-static declared field whose native type declares a verified book getter; subclasses that merely inherit it are excluded. Field name is irrelevant. The field is read only after resolution. |
| Book | First use checked class/name/return-type mappings for inspected builds. Otherwise require a known instruction fingerprint and the expected quote accessor signatures. Multiple verified candidates fail. |
| Native event row | Keep checked `e(int,int): Object` mapping; if missing, require the inspected row-access instruction fingerprint. |
| Quote notification | Unique concrete non-static method with the known scope/observer/changes descriptor. |
| Live event factory | Unique concrete static method with the known live-event input and output descriptor. |

Fingerprints are SHA-256 hashes of normalized instructions. They preserve opcodes, local slots, control-flow labels, exception handlers, constants, reference aliasing, descriptor structure and Java/Swing member names. Obfuscated symbol spelling, debug metadata and stack-map frames are excluded. They are compatibility patterns, not a security proof or a general semantic equivalence test. An implementation change can require a new inspected profile.

The profiles cover installed TWS 10.51, 10.52 and 10.53 code. No proprietary class bytes are included in the source or distribution. Class-resource reads are bounded to 8 MiB per class. Missing or mismatched resources fail closed. Resources describe installed class definitions, not necessarily modifications made by another agent in the live JVM.

`ClassValue` caches include failures, so painting does not repeatedly rescan an unsupported class. Different class loaders get independent resolutions. Native candidate resolution never invokes a method, initializes a class intentionally, requests a subscription or writes a TWS field. Only the selected getters are called during ordinary capture.

## Failure behavior and diagnostics

`status` includes `resolverScans`, `resolverFailures` and `resolverLast`, alongside `dataErrors` and `lastDataError`. Rejected exploratory candidates do not increment resolverFailures; requesting an unusable cached resolution reports it once. Repeated capture failures can still increment dataErrors.

When trade price and size are readable but its quote/record path is unsupported, the row remains visible and neutral. No quote is guessed or borrowed from another instrument. An unsupported row accessor declines the native adapter; the existing generic adapter remains available for recognized ordinary table columns. An ambiguous feed-hook match fails installation and uses the existing rollback path.

## Deliberate boundaries

This release does **not** automatically recover from arbitrary TWS redesigns. These anchors remain explicit:

- Tape table, history/Current model, record, factory and renderer target class names.
- Feed-hook parameter and return types, notification ordering, and factory semantics.
- Price/size roles (`D`, `F`) and quote roles (`G`, `F`, `aa`, `Z`), including their meanings and units.
- The Current-event class used for its special capture lookup.

A numeric getter alone cannot establish whether it means Last, bid, ask or another price. Never choose those roles by method order, observed values, spread ordering or a best-score guess. Same-signature semantic changes can still require manual inspection. The automatic logic is intended to survive renames within the checked structure, not to claim universal compatibility.

## Verification and operations

```bash
./test.sh
python3 -m unittest discover -s tests -p 'test_*.py'
bash tools/check-tws.sh /absolute/path/to/jts4launch-1053-linux-x64_TIMESTAMP.dat
```

The archive checker never loads or invokes TWS classes. It verifies record, top-field, book, instrument and row patterns and both hook signatures. It also verifies book fingerprints after renaming methods and member references in memory. It uses known baseline class/field anchors to locate these checks; it is not a substitute for live verification or a scanner for arbitrary unknown TWS layouts.

The optional AccuracyAudit uses production accessor discovery, then independently reads values and performs its decimal and pixel comparisons. It therefore checks capture correctness, not independent correctness of accessor identification.

Restart TWS before switching from an older loaded agent. Build with `./build`, run `./twstapecolor start`, then inspect `./twstapecolor status`. The user ran v0.3.24 against live TWS 10.53.1a on November 27, 2026. Saved status showed zero resolver failures/data errors and advancing green/red paint counters. A snapshot of the active trace passed verification for 49 quotes and 77 trades, with zero direction or preceding-quote mismatches and no missing/duplicate events. Direct attachment from this environment, independent pixel/restoration audits and Windows operation remain unverified.

## v0.3.24 live-report fix

The November 27 v0.3.23 live status showed zero quotes and an ambiguous top
resolution: `jclient.record.o.p` is `jclient.oQ`, while `q` is a `jclient.record.cZ`
wrapper extending `jclient.oQ`. Reflection includes inherited methods, so both
fields appeared to have a verified book getter. The original archive-only
checker inspected declared methods and missed this runtime ambiguity.

v0.3.24 requires the field type to declare the verified getter. It retains
ambiguity rejection for multiple primary candidates. The archive checker now
also runs the production resolver against installed class metadata in isolated
class loaders, checking the entire path and excluding the inherited wrapper.
No TWS constructors or application methods are invoked by that check.

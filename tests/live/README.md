# Opt-in live verification

These diagnostic agents are separate from `./test.sh`. They inspect the existing Time & Sales tables and native market-data callbacks; they do not send orders or change subscriptions. Generated classes, jars, CSV measurements and images belong under ignored `work/` or `logs/`.

From the project root:

```bash
bash tests/live/build.sh
./twstapecolor start --pid PID
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/AccuracyAudit.jar" "$PWD/work/logs/my-audit|V0320|45"
```

The accuracy audit runs for 45 seconds, samples displayed rows twice a second and removes its own transformers afterward. It records:

- `events.csv`: an independent read of native bid/ask before record observers, compared with the snapshot frozen by the production live-trade hook; raw native trade price and size are compared separately. Bid/ask sizes are checked when present, and the frozen quote is also compared with a fresh book read at trade arrival to expose possible notification lag.
- `pixels.csv`: a decimal price-comparison oracle, applied row direction, independently calculated RGB/intensity and actual painted background pixels for every eligible visible column.
- `summary.txt`: checked counts and mismatches, including component-state restoration after painting. Selected cells and pending model rebuilds are counted separately; pending rebuilds are still checked for incorrect direction and pixels. Distinct trade identities are counted so repeated paints are not mistaken for independent executions.
- `screen-*.png`: actual on-screen tape crops; `sample-*.png`: offscreen renders of those TWS components. The tape window is brought forward for visual checks.

The decimal oracle normalizes binary encoding noise to 12 significant digits. This is appropriate for the stock prices observed in this audit; it is not a universal precision policy for every instrument. The audit uses the configured default green/red RGB colors. Change its constants before checking custom CLI colors.

For restoration, stop the agent and save a clean baseline:

```bash
./twstapecolor stop --pid PID
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/StateAudit.jar" "$PWD/work/logs/native|baseline"
./twstapecolor start --pid PID
# Exercise the tape, then:
./twstapecolor stop --pid PID
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/StateAudit.jar" "$PWD/work/logs/cycle1|check"
```

Use the same baseline for repeated cycles. `check.txt` compares stable table/model identities, sorter, selection settings, column order/widths, row height, colors, listener identities and normalized instruction fingerprints of all three instrumented methods. Market prices and row counts are intentionally excluded because the tape continues updating. `agentCalls` must be zero in a genuinely native baseline and after successful stop. Captured proprietary bytecode is never written by this audit; only hashes and counts are saved.

`LegacyCleanup` is a one-off recovery tool for the disabled v0.3.11–v0.3.13 renderer transformers left by the old instrumentation-owner bug. It refuses active legacy hooks and makes only those exact tool transformer classes inert before retransformation. It is not run automatically. Restarting TWS also clears those old agents. v0.3.14 retains the registering instrumentation owner and does not need this cleanup across ordinary stop/start cycles.

Zero mismatches establish consistency with TWS data and local callback order during the sample. They do not independently validate the provider's NBBO, exchange sequencing, feed latency or aggressor identity.

## Update-gap flicker regression

To sample paints after a native model event but before the deferred filter refresh:

```bash
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/EventGapAudit.jar" "$PWD/work/logs/update-gap|V0320|30"
```

This bounded diagnostic installs a temporary model listener, checks each directional row against a decimal quote comparison, and saves the first three table renders. Its listener is removed after 30 seconds. `neutralFlashes`, `wrongSides` and `errors` must all be zero; a run with no directional checks is inconclusive. It reproduced the cache gap in v0.3.14. The accuracy audit also checks actual pixels while filter refreshes are pending rather than skipping those observations.

## Level 2 isolation and resource usage

Use a freshly restarted TWS when upgrading from v0.3.15 or earlier: the old renderer can leave native opacity flags damaged after stop. Save the clean native baseline before starting v0.3.16.

```bash
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/ResourceAudit.jar" "$PWD/work/logs/native-resources|45|native"
# Wait for complete.txt, then start coloring.
./twstapecolor start --pid PID
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/Level2IsolationAudit.jar" "$PWD/work/logs/level2|30"
# After correctness audits finish, measure ordinary live operation:
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/ResourceAudit.jar" "$PWD/work/logs/live-resources|300|V0320"
```

The isolation audit compares two full Level 2 renders around repeated tape paints within the same EDT turn. It reports pixel differences, unexpected tape wrappers, sorter changes and colored depth rows. A changing live book can affect pixels; inspect any differences. The resource audit samples process CPU time, heap, GC, EDT queue delay and cache sizes once a second, then shuts down its worker. It never forces GC. Compare CPU time deltas per elapsed second; 1.0 means one CPU core. These are whole-TWS measurements and include market activity and unrelated panels. Native and enabled intervals cannot establish causal CPU differences by themselves.

For a headless renderer allocation comparison, first run `./test.sh` and build the live tools. `RendererBenchmark VERSION ITERATIONS` needs both the production classes/JAR and synthetic fixtures from `build/test-classes` on its classpath. Run versions in separate JVMs. `OpacityRepro IsolatedTintRenderer` with an old JAR reproduces the native opacity corruption; `OpacityRepro TintRendererV0320` with the current build preserves it. All generated profiles and captures belong under ignored `work/` or `logs/`.

## Symbol-switch verification

Start from native TWS, save a `StateAudit` baseline, then start the current tool. Select a symbol through the normal UI and observe at least one genuinely new history arrival, rather than counting Current-only updates or backfilled rows.

For the authorized 1810 / 9988 SEHK or XLAB / WETO / TCRT US symbols in a layout with exactly two visible native tape/depth selectors:

```bash
# This optional driver changes the selected symbol using an existing native history item.
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/SymbolChoice.jar" "1810|depth"
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/SymbolSwitchAudit.jar" "$PWD/work/logs/switch-1810|1810|V0320|8"
```

`SymbolChoice` allows only 1810 / 9988 SEHK and XLAB / WETO / TCRT US history items, and refuses other symbols/exchanges, missing history entries, or ambiguous layouts. It selects the same native combo history item used by TWS's UI. It does not send orders or change linking preferences. `SymbolSwitchAudit` only observes: it records the asynchronous native transfer separately, then requires both selectors, Current and history to agree with the requested instrument. After settlement, every retained history event must belong to that instrument, Current must have no private sorter, and native history identities must advance with freshly captured events. The duration is an upper bound: the audit can finish early after two ready seconds, at least two captured events and a new history identity. A run with zero ready samples or no new history arrivals is inconclusive. It releases table/event references at completion.

Repeat in both directions from tape and depth, reselect the same symbol, try a rapid round trip, then check stop/start restoration. Run the independent accuracy audit after switching to each instrument. A market-depth subscription is needed to verify actual depth-book rows; contract header and quote checks alone do not prove depth data delivery.

## Saved debug-data check

`start --pid PID --debug` enables the production bounded live-event logger. After stopping it, run `python3 tools/verify_debug.py 'logs/<one-session-prefix>-0.csv'` (supply all existing segments from that same session). The verifier checks event continuity, preceding per-record quote prices/sizes/sequence, and an independent decimal direction oracle. It refuses to call a truncated/rotated or incomplete sample a complete pass. Do not mix sessions. Six Python regression tests exercise correct data, corrupted direction, incorrect quote size, missing events, instrument isolation and decimal/invalid prices: `python3 -m unittest discover -s tests -p 'test_*.py'`.

Run correctness audits sequentially per settled symbol and wait for their summary before switching again. The Level 2 audit reacquires current native tables on every sample; symbol switches can retire zero-height tables. Empty depth tables are counted separately and cannot prove native color isolation. A sample with no new trades is inconclusive for trade accuracy. Diagnostic agents loaded in the same JVM can be cached by class name; use unique helper names after changing them, or restart TWS.

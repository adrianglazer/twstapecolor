# Opt-in live verification

These diagnostic agents are separate from `./test.sh`. They inspect the existing Time & Sales tables and native market-data callbacks; they do not send orders or change subscriptions. Generated classes, jars, CSV measurements and images belong under ignored `work/` or `logs/`.

From the project root:

```bash
bash tests/live/build.sh
./twstapecolor start --pid PID
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/AccuracyAudit.jar" "$PWD/work/logs/my-audit|V0315|45"
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
java --add-modules jdk.attach -cp work/audit-classes AttachAudit PID "$PWD/work/audits/EventGapAudit.jar" "$PWD/work/logs/update-gap|V0315|30"
```

This bounded diagnostic installs a temporary model listener, checks each directional row against a decimal quote comparison, and saves the first three table renders. Its listener is removed after 30 seconds. `neutralFlashes`, `wrongSides` and `errors` must all be zero; a run with no directional checks is inconclusive. It reproduced the cache gap in v0.3.14. The accuracy audit also checks actual pixels while filter refreshes are pending rather than skipping those observations.

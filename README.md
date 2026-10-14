# twstapecolor v0.3.10

Unofficial, open-source-ready experimental **Time & Sales row-background coloring** for **Interactive Brokers Trader Workstation (TWS)** on Linux. This project is not affiliated with, supported by, or endorsed by Interactive Brokers. It does **not** place trades, transmit orders, or use account credentials.

## What it does

While TWS is running, `twstapecolor` attaches a Java agent using the JDK Attach API and **retransforms one already-loaded TWS class in the JVM's memory**: `jtable.basic.by`, method `getCellRenderer(int,int)`. The hook wraps a Swing table-cell renderer only for known Time & Sales tables (`feature.tape.ui.aN` with `feature.tape.ui.aT` or `feature.tape.data.k` models).

For any visible price-renderer column, it reads the renderer's `PROPERTY_NAME_COLOR_TAG` client property (`com.ib.chart.timesales.columns.ColoredColumn$ColorTag`). It then applies the same classification to **all visible columns of that row** (including Last, Size, Time, NBBO and Midpoint when visible):

| Native TWS tag | Meaning in this tool | Row background |
|---|---|---|
| `UP` | Ask-side / up-arrow print | Dark green `#184F31` |
| `DOWN` | Bid-side / down-arrow print | Dark red `#66262A` |
| `NO_CHANGE` or absent/unknown | Neutral/unknown | Unchanged, original TWS renderer colors |

This **follows TWS's own indicator**, not an independent determination of trade aggressor or a bid/ask quote calculation. Whether UP/DOWN means a true executed-at-ask/bid trade depends on TWS's native semantics and the data feed. Selected cells are left unchanged. No colors are applied to unrecognized TWS tables.

**Implementation detail:** Swing renderers are reused. The hook checks native color tags on visible price renderer cells for the same row, without depending on a fixed column name or order. If conflicting directional tags are present, the row stays unchanged. If all direction-bearing columns are hidden, rows stay unchanged rather than guessing. The hook is defensive and defaults to the original appearance on missing or unexpected state. It adds some per-cell overhead.

## Requirements

- Linux with a running desktop TWS instance (validated by a user on TWS 10.51.1c; other builds untested).
- Java **JDK 21+** with `java`, `javac`, `jar` and `jcmd` available; attach support and same-user permission for the TWS process.
- Bash and `unzip`; ASM 9.8 is bundled in `lib/asm-9.8.jar`.
- A paper-trading TWS session for initial testing, with a visible Time & Sales window.

## Build and run

Extract the ZIP and enter its `twstapecolor-v0.3.10` directory. Nothing is installed into your TWS directory.

```bash
chmod +x twstapecolor build.sh
./build.sh
./twstapecolor --version
PID=$(jcmd -l | awk '/install4j.jclient.Launcher/ {print $1; exit}')
echo "TWS PID: $PID"   # check this identifies the right instance
./twstapecolor start --pid "$PID"
./twstapecolor status --pid "$PID"
```

**Custom colors** use independent command-line flags. Quote hex colors in the shell so `#` is not interpreted as a comment:

```bash
./twstapecolor start --pid "$PID" --ask-color '#006B3C'
./twstapecolor start --pid "$PID" --bid-color '#8B2025'
./twstapecolor start --pid "$PID" --ask-color '#006B3C' --bid-color '#8B2025'
```

Each `start` sets the effective colors (unspecified colors reset to their built-in defaults). Colors must be six-digit hex RGB (`#RRGGBB`). The colors can be updated with another `start` command without restarting TWS. Use `status` to confirm active colors and counters.

```bash
./twstapecolor stop --pid "$PID"
```

`stop` removes the agent transformer and requests a retransformation to restore the original `getCellRenderer` implementation. If the process becomes unstable or restoration fails, restart TWS. Starting TWS anew also clears runtime agent changes.

## Other commands

- `status --pid PID`: enabled state, installation and coloring counters, configured colors.
- `test-paint --pid PID`: alternating green/red **diagnostic pattern** unrelated to trade classification. Use only to test the paint hook.
- `probe --pid PID`, then `report --pid PID`: bounded diagnostic captures renderer metadata and values in the home-directory report. Inspect reports before sharing them: they can include market prices, symbol-related information or other UI data.
- `dump --pid PID`: saves in-memory class bytecode for debugging.

**Clean by default:** `start`, `status`, `stop`, and other commands do not leave report/log files behind. They still print concise results/errors in the terminal. The launcher uses a short-lived temporary response file to communicate with the attached agent and removes it after completion. If the launcher is forcibly killed, an orphaned temp file may remain.

To save a persistent per-command diagnostic report, add `--debug` (works with every command, and can be combined with color flags):

```bash
./twstapecolor start --pid "$PID" --debug
./twstapecolor status --pid "$PID" --debug
./twstapecolor start --pid "$PID" --ask-color '#006B3C' --bid-color '#8B2025' --debug
```

`--debug` writes `~/twstapecolor-<command>-<pid>-<timestamp>.txt`. Note that `dump` deliberately writes a `.class` file even without `--debug` because producing that file is the purpose of the command; `report` prints its collected observations to the console by default. Building still creates build artifacts inside the project directory. The launcher rebuilds its agent JAR before each command (this does not alter TWS files).

## Safety, trust and limitations

**It does not patch, replace or overwrite TWS JARs, executables, installation files or configuration files.** Changes to the application code are **runtime-only in the currently running JVM memory**, and `stop` attempts to restore the original bytecode. This is different from saying the tool cannot affect TWS: a Java agent executes code **inside TWS's process** and modifies execution of a Swing rendering method. Bugs, TWS updates, extra rendering work, class-loader differences or other agents may cause incorrect colors, freezes, errors, or crashes. Restoration is best-effort; a TWS restart is the reliable way to discard instrumentation.

Treat this as **experimental software**, not a safety-certified or production-supported TWS extension. Test in paper trading; do not rely on the colors for order execution or as verified buy/sell signals. Close or restart TWS if anything behaves unexpectedly. The agent does not intentionally interact with orders or accounts, but it does have the technical privileges of the TWS process. **Read the source before running third-party instrumentation.**

The previous v0.3.6 version was confirmed in a live TWS session to recognize UP/DOWN tags and color price-cell backgrounds. **v0.3.10's full-row implementation has been compiled and locally checked but requires testing against the live TWS renderer**, especially for rapid updates, column reordering, scrolling, and changing symbols.

## Troubleshooting

- `jcmd` returns no launcher PID: ensure the TWS JVM is running and use `jcmd -l` to identify it explicitly.
- `VerifyError`: the TWS bytecode may have changed. Run `stop`, restart TWS if necessary, and report your TWS and Java versions and the terminal error output (use `--debug` to retain a report). Do not repeatedly inject on a production session.
- `injected=true` but no tint: open Time & Sales and inspect `status`. Native tags must be present; unknowns are intentionally unchanged.
- Only some columns tinted: verify renderer support and capture a screenshot and status report.
- `stop` reports restoration failure: restart TWS.

## License, free use and liability disclaimer

**twstapecolor is free and open source under the [MIT License](LICENSE).** You may use it personally or commercially, fork it, modify it, and redistribute copies, including modified versions, subject to retaining the copyright and license notice. There is no fee, no warranty, and no promise of support.

**USE AT YOUR OWN RISK.** This is an unofficial third-party tool that attaches to a running Trader Workstation (TWS) JVM and changes rendering behavior in memory. It is not affiliated with, endorsed by, or supported by Interactive Brokers. It does not alter the installed TWS executables or JARs, but injected code **does run inside the TWS process**. Defects or incompatibilities can cause incorrect visual signals, UI freezes, missed information, crashes, or other losses. `stop` makes a best-effort restoration; restarting TWS is the reliable fallback.

**No trading or financial guarantee.** Colors merely reflect the TWS renderer's UP/DOWN/NO_CHANGE direction tags; they are not verified executions, investment advice, or a recommendation to trade. Users are solely responsible for how they use the tool, for their trading decisions, for monitoring the application, and for any resulting gains or losses. To the maximum extent permitted by applicable law, the authors and copyright holders disclaim warranties and liability for **direct, indirect, incidental, consequential, special, or other damages, including trading losses, lost profits, data loss, trading interruptions, and missed trades**, arising from use, inability to use, or malfunction of the software. Nothing here excludes liability that applicable law does not allow to be excluded. See the full [MIT License](LICENSE) for the binding license terms.

**Third-party software:** This distribution bundles ASM 9.8, which is separately licensed under BSD-3-Clause; the MIT license above applies to the twstapecolor project code, **not** to ASM. Comply with ASM's copyright and license conditions when redistributing its JAR (see https://asm.ow2.io/license.html). No proprietary TWS class files are included.

Before publishing, the project maintainer should confirm or replace the copyright holder name in `LICENSE` and include any required third-party license notices in the release package.

## Restart lifecycle (v0.3.10)

This version adds a guard against recursively wrapping renderers while looking up
arrow direction in other columns. The former full-row detector called
`getCellRenderer()` from inside renderer execution, and this could lead to
nested wrappers or excessive renderer work across stop/start cycles.

**Before testing this version, restart TWS completely** to clear already-loaded
agent versions and any stale renderer instances. First try `start`, `stop`,
`start`, `status`, then repeatedly change symbols and scroll the tape.
If TWS freezes, terminate the test and restart TWS. This is experimental
in-process instrumentation; it cannot guarantee application safety.
Do not test on a live-trading account.

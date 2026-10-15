![Screenshot of the earlier native-tag version](https://github.com/adrianglazer/twstapecolor/blob/main/twstapecolor.png)

# twstapecolor v0.3.11

Experimental Time & Sales **row backgrounds** for Interactive Brokers Trader Workstation (TWS) on Linux. Unofficial; not affiliated with or supported by Interactive Brokers. It does not place orders or use account credentials.

## Behavior

- Show the newest **50 trades** available in the retained TWS event model, in model order (newest first). A trade requires both a positive numeric **Last** and **Last Size** (or **Size**). Empty, zero, nonnumeric and nonfinite values are hidden.
- Hide quote-only and incomplete rows **in the table view**. All events remain in the model. The tool cannot recover older trades that TWS has already dropped; increase TWS's event retention if fewer than 50 trades are retained.
- Classify each trade using the **nearest preceding NBBO-bearing event** in model event order. With newest-first rows, earlier events have larger model indices. Timestamps are not sorted or used to break ties. A newer quote, or a quote in the trade's own event, cannot classify that trade.
- At or above the preceding ask: green (`#184F31`). At or below the preceding bid: red (`#66262A`). Inside the spread, missing quotes, and invalid, partial, locked or crossed quotes: original TWS appearance. Invalid quotes replace earlier valid quotes rather than silently reusing stale data.
- Tint every visible column. Smaller trades have softer backgrounds; the largest of the visible trades receives the configured color. Intensity is `0.35 + 0.65 * log(1 + size) / log(1 + largest visible size)`, blended with each renderer's original background. The scale updates as trades enter or leave the visible set.
- Preserve selected-cell appearance. `test-paint` uses alternating full-intensity colors solely for diagnosis.

This version replaces the old UP/DOWN renderer-tag classifier with an event-based NBBO comparison. It is a price-relative classification, not a verified identification of the trade aggressor.

### Supported data and view layout

Only `feature.tape.ui.aN` tables with `feature.tape.ui.aT` or `feature.tape.data.k` models are targeted. Model column names are matched ignoring case, spaces and punctuation: `Last`, `Last Size`/`Size`, and either `Bid`/`Ask` (also `NBBO Bid`/`NBBO Ask`) or `NBBO`. Hidden or reordered view columns do not affect classification. Model events must be newest first.

Bid/ask cells accept positive numbers or decimal numeric text (commas are treated as thousands separators). A combined NBBO cell accepts plain decimal prices separated by `x`, `×`, `/`, or a spaced hyphen, for example `100.00 x 100.05`. Size-decorated strings, currency symbols, localized decimal commas and opaque TWS value objects are not decoded. Unrecognized NBBO text leaves trades neutral. Partial bid/ask updates invalidate the snapshot; this tool expects complete NBBO events and does not reconstruct independent bid/ask deltas.

The filter installs a private Swing row sorter only when the table has no existing sorter and its required trade columns are recognized. **An application-owned sorter is preserved; filtering and NBBO coloring are skipped for that table.** Unknown column names are also left unchanged. Use `probe`/`report` to inspect the actual TWS values if the tape remains unfiltered or neutral. Supporting additional TWS formats requires a captured example and a parser test.

### Renderer and instrumentation lifecycle

The agent retransforms only `jtable.basic.by.getCellRenderer(int,int)` in JVM memory. The exit hook loads the returned renderer and `this` only; it never reloads dead row/column arguments, preserving the fix for the original `VerifyError`. Existing stack-map frames are retained.

Renderer wrappers are unwrapped before reuse. Tinting occurs inside an isolated paint call; shared component backgrounds, opacity, parent and bounds are restored in `finally`, including when painting fails. Classification uses cached model data and does not recursively render other cells. Model changes invalidate the snapshot immediately and coalesce a rebuild on the Swing event thread. Quote lookup stops at the event preceding the oldest visible trade instead of scanning all retained history on every paint.

`stop` removes model listeners and the private view filter, then removes the transformer and requests restoration of the original bytecode. Model replacement discards the old view state. A later `start` installs fresh view state. Agent and hook classes have new versioned names to avoid reusing v0.3.10 classes already loaded in TWS.

## Build and run

Requirements: Linux, JDK **21+** (`java`, `javac`, `jar`, `jcmd`), Bash, and `unzip`. The build downloads ASM 9.8 from Maven Central if missing; subsequent builds can run offline.

```bash
./build.sh
./test.sh
./twstapecolor --version
jcmd -l
# Choose the correct TWS PID explicitly:
./twstapecolor start --pid 12345
./twstapecolor status --pid 12345
./twstapecolor stop --pid 12345
```

Before upgrading from an older running agent, **restart TWS completely** to clear stale hooks and renderers. Initially validate in paper trading. Change symbols, reorder/hide columns, scroll, and try `start`, `stop`, `start` while watching the tape. Confirm NBBO parsing and event order on your TWS build before trusting the display.

Custom colors (six-digit hex RGB):

```bash
./twstapecolor start --pid 12345 --ask-color '#006B3C' --bid-color '#8B2025'
```

Each `start` resets unspecified colors to the defaults. Run `start` again to change them without restarting TWS.

## Diagnostics and debug opt-in

- `status --pid PID`: installation state, renderer/color counters, and configured colors.
- `test-paint --pid PID`: alternating green/red diagnostic rows; unrelated to classification.
- `probe --pid PID`, then `report --pid PID`: bounded renderer metadata and values. Probe mode removes the trade filter and makes no color changes.
- `dump --pid PID`: deliberately saves in-memory target bytecode to a `.class` file.

Commands print to the console. Normal commands leave no persistent reports: a short-lived response file is removed after use (a forcibly killed launcher may leave it behind). Add `--debug` to any command to retain `~/twstapecolor-<command>-<pid>-<timestamp>.txt`. Inspect diagnostic data before sharing; it may contain market data and symbol information. `dump` writes its requested file even without debug. The launcher rebuilds the agent JAR before each command.

## Validation and limits

`./test.sh` compiles the agent and runs headless Swing checks with `-Xverify:all`. Tests cover event ordering, absent/invalid/partial/locked/crossed NBBO, pair parsing, hidden columns, the 50-trade limit, bounded history reads, adaptive intensity, full-cell painting, shared-component restoration (also on exceptions), model updates/replacement, filter removal/restart, selected cells, and non-tape isolation. A synthetic target reproduces the dead row/column argument stack-map frame and loads and executes transformed bytecode. An instrumentation test double verifies transformer installation/removal across stop/start cycles.

These tests do **not** establish live TWS compatibility. v0.3.11 has not been tested inside TWS. Obfuscated model names, column labels, values, event retention and ordering can change across TWS versions. The repository screenshot shows an earlier native-tag version, not validation of this version. Probe a paper session to confirm the assumptions above. Quote retention is controlled by TWS, so missing historical NBBO leaves older trades neutral.

For `VerifyError`, restoration failure, freezes or unexpected UI behavior, stop testing and restart TWS. Report TWS/Java versions and opt-in diagnostic output. If rows remain unfiltered, check required columns and the existing-sorter limitation. If rows are filtered but neutral, check NBBO format, available quote history and event ordering.

## License and liability

Project code is free and open source under the [MIT License](LICENSE). Retain its copyright and license notice when redistributing. The MIT license and its warranty/liability disclaimer are unchanged.

Use at your own risk. An attached Java agent runs inside TWS and changes rendering in memory. Bugs or TWS updates may cause incorrect colors, UI freezes, crashes, missed information or trading losses. Installed TWS JARs, executables and configuration files are not overwritten. `stop` restores the bytecode on a best-effort basis; restarting TWS reliably clears runtime changes. Read the source and test in paper trading. Colors are not investment advice, verified executions, or a guarantee of trade direction.

ASM 9.8 is third-party software under BSD-3-Clause, separately from this project's MIT license. Its redistribution conditions apply to the bundled dependency; see the [ASM license](https://asm.ow2.io/license.html). No proprietary TWS class files are distributed; tests use synthetic fixtures.

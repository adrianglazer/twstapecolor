# Troubleshooting and diagnostics

[Documentation index](README.md) · [Command reference](user-guide.md#command-and-option-reference) · [Tape behavior](tape-behavior.md)

## Diagnose in order

1. Confirm that the launcher and target are the expected versions: run `./twstapecolor --version`, check the TWS build and Java version, and confirm the target PID using `jcmd -l`. Keep using the same version and PID throughout the session.
2. Open the native tape first. Confirm that new executions arrive without the agent and, if testing depth, that the Level 2 book actually has rows. Then start the agent.
3. Compare two `status` readings during new quote and trade activity. Look for changes rather than judging a single cumulative number.
4. If normal coloring is missing, use `test-paint` briefly to isolate the paint path, then return to normal mode with `start`. If the paint path is unclear, use `probe` and `report`.
5. End with `stop`. If restoration reports a warning or the interface stays abnormal, fully restart TWS before trying again.

Examples use the Linux launcher; substitute the [direct JAR command](user-guide.md#windows-run-the-shared-jar) on Windows.

| Symptom | What to check |
| --- | --- |
| Launcher asks you to build | Run `./build` from the checkout. It creates `out/twstapecolor.jar`; a distribution JAR in `dist/` does not satisfy the Linux source launcher. |
| No running TWS instance found | Confirm TWS is open and visible to the same account. Check `jcmd -l`; provide `--pid` if the launcher name differs. |
| Multiple matches | Select the intended TWS explicitly with `--pid`. Do not choose a PID based on its order in the list. |
| Missing JDK or class version error | Run `java -version` and confirm JDK 21+. On Windows check the Java selected by the script and any older `JAVA_HOME`. If the failure is inside TWS while loading the agent, the target JVM also needs to support the agent's Java 21 classes. |
| Attachment fails or response is unavailable | Confirm the PID did not change, account/elevation matches, the JVM permits attachment, and the JAR directory and project log directory are writable as needed. For Windows use the original versioned filename. |
| Renderer class not loaded or data hooks absent | Open Time & Sales and allow the tape to initialize, then rerun `start`. Check compatibility if the expected classes or signatures still do not appear. |
| Tape remains neutral | Wait for fresh trades. An older row or an inside-spread trade may be correctly neutral. Check `feedHooks`, changes in `quoteEvents` and `tradeEvents`, and `lastDataError`. |
| Less than 50 prints | Check how much history TWS retained, how many trades had valid Last/Size, and how many rows fit in the panel. The tool does not create extra history. |
| No filtering | Confirm the panel is the supported native tape history, not Current or Level 2. An existing TWS sorter takes precedence. |
| Current updates after a switch but history does not | Wait for a fresh execution on the new symbol. Check the native behavior with the agent stopped. Report whether both linked headers settled, whether depth was populated, and which selector was used. |
| Wrong-looking color | Compare the trade to its **frozen preceding** quote, not the quote now on screen. Check selected state, visual test mode and the size scale before treating a shade change as a direction change. |
| `VerifyError`, UI freeze, or restoration warning | Stop if possible and restart TWS. Save the first error, version details, and the steps that triggered it. |

## Read status and probe output

| Fields | Interpretation |
| --- | --- |
| `enabled`, `visualTest`, `probeMode` | Whether normal/diagnostic rendering is enabled and which diagnostic mode is active. `enabled=true` alone does not establish that a particular table is tinted. |
| `injected`, `feedHooks` | Renderer hook and native quote/trade hooks respectively. If data hooks are absent, alternating test colors do not prove that live classification is available. |
| `quoteEvents`, `tradeEvents`, `dataErrors` | Changed available quote snapshots, captured positive Last/Size trade callbacks, and recorded data-access failures. Unchanged books are deduplicated; unavailable books can invalidate a snapshot without increasing the quote counter. |
| `lastQuote`, `lastTrade`, `lastDataError` | Last observations across watched records. They are not a per-symbol history or one atomic snapshot. `lastTrade` includes its own frozen price pair, quote sequence and direction. |
| `rendererSelections`, `rendererCalls`, `visualPaintCalls`, `green`, `red`, `neutral` | Renderer activity and cell-level decisions. A single trade can contribute several cells over repeated repaints. The counters do not count distinct trades or prove exact RGB values. |
| `askColor`, `bidColor` | Configured target colors before size-dependent blending. |
| `liveDebug`, `prefix`, `events`, `dropped`, `queued`, `error` | State and path of the CSV session, numbered events offered, queue overflows, pending items and writer errors. A stalled worker may also be reported as `writerDraining=true`. |

Most hook and rendering counters are cumulative for that agent class in the running JVM; `stop` clears captures but does not reset the counters. CSV session counters start over per capture. `probe` resets its own sample counters. The report retains at most 80 sample records even if the reported sample-slot counter grows beyond 80.

```bash
./twstapecolor test-paint
./twstapecolor probe
# Let the tape repaint before collecting the report.
./twstapecolor report --debug
./twstapecolor stop
```

Probe mode disables coloring and removes the private filter. It samples renderer/component types, values and text, colors, icons and selected properties. Native model cell values can be null even when renderer text appears: native renderers can fetch events themselves. A null probe value is not evidence of a missing trade feed. `dump` separately captures the renderer class in project logs and is useful only for compatibility work. Do not publish dumped TWS bytecode.

## Capture and verify a live session

Start from stopped or freshly restarted TWS to maximize the chance that the first quote for each record is included in the trace. Keep the capture short enough to avoid rotation.

```bash
./twstapecolor start --debug
# Wait for new quotes and trades.
./twstapecolor status --debug
./twstapecolor stop --debug
./twstapecolor status --debug
```

Find the exact `prefix=` reported by status. Pass all existing segments for **one** prefix to Python 3, replacing the sample date and PID:

```bash
python3 tools/verify_debug.py logs/twstapecolor-start-12345-20261016-120000-000.txt-live-*.csv
```

### What is written

- A normal command uses a temporary response beside the JAR and removes it on exit. `--debug` preserves a command response under project `logs/`; only `start --debug` starts a live trace. `dump` persists its class file even without debug.
- Continuous capture records changed book snapshots and valid live trade callbacks for watched records. It is not a full quote feed, depth archive, or export of history. Unchanged available books are deduplicated; unavailable snapshots can be emitted explicitly.
- The writer uses a 4,096-entry bounded queue and does disk work away from the feed callbacks. Four rotating segments of 4 MiB each hold up to about 16 MiB per session. Rotation overwrites old segments. A full queue can drop entries; previous sessions remain on disk until you remove them.
- `stop`, `probe`, or a new start closes the previous capture. A normal stop gives the writer up to two seconds to finish; check its status if the filesystem is slow.

### CSV schema

| Fields | Contents |
| --- | --- |
| `event` | Increasing sequence number for the debug session; gaps can indicate loss or rotation. |
| `epochMillis`, `monotonicNanos` | Local capture wall-clock milliseconds and a JVM monotonic time sample, respectively. Neither is exchange time. |
| `type`, `record`, `eventObject` | `Q` for quote or `T` for trade; local hexadecimal identity hashes for the market record and trade event. The event identity is blank for quotes. These are diagnostic identifiers, not symbols or permanent IDs. |
| `last`, `size` | Trade price and size; `NaN` on quote rows. |
| `bid`, `ask`, `bidSize`, `askSize`, `quoteSequence` | Snapshot fields on `Q`; the frozen snapshot on `T`. An absent book is written as four `NaN` values and sequence 0. Individual unavailable quote fields may also be `NaN`. |
| `direction` | `1` ask-side, `-1` bid-side, `0` neutral; always 0 on quote records. |

The offline verifier sorts by sequence, checks each trade against the latest preceding logged quote for its record, and recomputes direction with a separate 12-significant-digit decimal comparison. A pass needs trades and no detected gaps, duplicate numbers, missing initial books or mismatches. Check that writer status shows no drops, queued rows, errors, or draining, and compare the verifier's event count with the final writer count. The verifier alone cannot detect an omitted tail or certify that all market events reached TWS. Starting capture while an existing quote is already cached can leave a trade with no preceding `Q` in that session; repeat from stopped TWS if needed. Exact boundary arithmetic can also differ between the decimal verifier and the runtime ULP comparison on unusual price scales.

For actual rendered RGB values, native component restoration, or symbol-link behavior, use the [opt-in live audits](../tests/live/README.md). A short sample with no fresh executions is inconclusive.

## Report an issue

Include the tool version, TWS build, platform and Java versions, command used, and a short sequence that reproduces the issue. For rendering, state which tape columns were shown, whether the agent was started fresh, and whether fresh trades and populated depth were available. Include relevant status before and after activity and after stop, plus the first exception or warning. If you include a screenshot, crop it to the tape or depth needed to explain the issue. Review logs and images for account, symbol, and market information before sharing. Raw captures belong in ignored `logs/` or `work/`.

# v0.3.13 live verification

Tested 2026-10-15 against the user's running TWS 10.51.1c.

## Cause and correction

The previous version assumed ordinary cell values and a column called Last. Live probes showed that `getValueAt()` returns null, the price header is Last/Midpoint, and the actual rows are obtained through `jtable.basic.bB.e(row,-1)`. Native event getters D/F supply Last/Last Size. The original classifier therefore never acquired any trades or usable NBBO.

With only Time, Last and Size enabled, historical trade objects contain missing bid/ask sentinels. The current-row object reads live top-of-book values dynamically, so using it while repainting old trades would assign future quotes to past trades.

The fix watches the already-existing market record and captures the book before record observers run. TWS normally supplies an empty explicit change array; relevant changes are accumulated on the top record, so snapshots are compared by value rather than relying on that array. A hook at the completed live-event factory freezes the latest observed snapshot with the incoming event. Repaints never substitute a newer quote.

Verified getter mappings from the running bytecode:

| Data | Access |
| --- | --- |
| Native tape row | model.e(row, -1) |
| Last, trade size | event.D(), event.F() |
| Instrument / market record | event.ar().rk() |
| Existing live top record | marketRecord.p (read only) |
| Active top-of-book view | top.cO() |
| Bid / ask | book.G(), book.F() |
| Bid / ask size | book.aa(), book.Z() |

These signatures are specific to this TWS build. No proprietary bytecode is committed. Local probe sources, bytecode, raw images and logs are under ignored work/ and logs/. Two selected, unaltered tape-only captures are published under docs/screenshots/ for the README before/after comparison.

## Evidence

- Native values and real quote sizes were read without adding columns or altering TWS configuration.
- Screen captures of the actual tape show green and red backgrounds spanning Time, Last and Size, with intensity varying by trade size.
- The final v0.3.13 run observed 83 quote changes, 95 live trades, 807 green and 852 red cell paints during one verification interval, with zero data errors.
- Stopping removed tinting and restored native rendering and the unfiltered view. The final v0.3.13 build uses fresh versioned helpers; stop restored both data and renderer bytecode, and start reattached successfully.
- All 80 automated checks pass under -Xverify:all. Checks include the original dead-argument verifier regression, a TWS-shaped null-value model, future-quote isolation, empty notification arrays, multiple instruments, sentinel handling, decimal rounding, painting/restoration, filtering and restart lifecycle.

The tested native model retained 17 history events. The filter allows up to 50 trades when available; it cannot reconstruct history discarded by TWS. Trades seen before attach remain neutral because their preceding quotes were not captured. Classification reflects locally observed TWS quote/trade delivery order; separate feed delivery does not prove exchange order or trade aggressor identity.


## Deeper review: v0.3.14

The initial v0.3.13 stop check verified visible tint removal, but a later bytecode audit found that separate attach commands had replaced the instrumentation instance needed to remove the renderer transformer. Disabled hooks from earlier versions remained installed. v0.3.14 retains the registering owner and checks removal success. The old disabled transformer bodies in the tested JVM were neutralized once; a TWS restart also clears them.

The stronger verification now compares normalized instructions of all three target methods and 42 stable table properties with a clean native baseline. Three cycles matched exactly, including zero remaining agent-call instructions. See [the QA report](QA-REPORT-2026-10-15.md) and [live audit instructions](tests/live/README.md) for the independent trade/pixel measurements and regression coverage.

## Live-update stability: v0.3.15

A focused update-gap audit reproduced transient neutral colors in v0.3.14 (33/33 directional row checks). v0.3.15 refreshed classifications before painting and passed 18/18 such checks without flashes or wrong sides. A separate live audit matched 13 incoming trades, 13 quote-size snapshots and 918 colored cell paints with zero mismatches. Stop restored all 42 checked native properties and method fingerprints. See the [updated QA report](QA-REPORT-2026-10-15.md) for method, limits and retained evidence.

## Level 2 isolation and resources: v0.3.16

The persistent Level 2 color loss was traced to restoring `DefaultTableCellRenderer.isOpaque()` as a stored flag even though that getter depends on its table context. v0.3.16 never changes native renderer backgrounds, opacity or parent ownership. Starting from the user's freshly restarted TWS, 80 paired Level 2 renders had no pixel changes around tape paints; three stop cycles restored all 42 checked properties and method fingerprints. A separate live audit matched 40 incoming trades and 1,914 colored cells. Defaults and the size curve now make large trades much brighter than small trades.

A five-minute live CPU/heap/EDT sample showed bounded caches and no sustained monotonic heap growth in that interval. Significant CPU usage was already present with coloring disabled. One million synthetic paints completed under a 64 MiB heap limit. See the [resource review](QA-REPORT-2026-10-15.md) for exact measurements and limits.

## Symbol switching: v0.3.18

On 2026-10-16, v0.3.16 reproduced the reported split-symbol/empty-history failure during a live 9988-to-1810 SEHK switch. Swing sorter exceptions interrupted the native linked callback chain. v0.3.18 rebuilds native history mappings for reset notifications, guards stale repaint indices, leaves Current's row handling native, rotates bounded watched records and guards reused captured events.

The final build passed 11 live observation phases from both selectors, including same-symbol and rapid switches. Both headers and settled event instruments matched, and fresh history advanced after every phase. Independent post-switch audits matched 44 incoming trades and 3,858 colored-cell checks. Three stop/start cycles restored all 42 checked properties/fingerprints. The full suite passes 56,540 checks, including 1,000 seeded reset/selection cases.

The HK session had no depth subscription, so actual depth rows/colors could not be verified; selected-contract and quote linkage were checked. See the [2026-10-16 verification report](QA-REPORT-2026-10-16.md) for evidence, resource observations and limits.


## v0.3.19 — US premarket, 2026-10-16

Fresh TWS, XLAB and user-requested TCRT, with real depth rows on both sides. Independent live quote/size/direction and RGB checks, linked tape/depth switches, three exact stop/start restorations, full debug CSV replay and short native/enabled resource intervals are documented in [QA-US-2026-10-16.md](QA-US-2026-10-16.md). The README image now uses actual TCRT before/after tape-only captures with the current palette. The earlier HK report remains historical and did not verify subscribed depth rows.

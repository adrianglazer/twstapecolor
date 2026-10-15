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

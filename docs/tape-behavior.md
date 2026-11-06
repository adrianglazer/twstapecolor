# How the tape is colored

[Documentation index](README.md) · [Operation](user-guide.md) · [Internals](maintainer-guide.md)

## From a quote to a colored print

For watched instruments the agent observes changes to the best bid and ask in TWS's local top-of-book record, including displayed sizes. When a live trade event is created, the agent saves the latest observed quote for that same record with the event. This is the **preceding quote in local TWS processing order**. It may differ from the book at the exchange's execution time, particularly when trade and quote updates arrive on separate feeds. The agent neither independently validates a consolidated NBBO nor obtains order-side flags.

| Trade and frozen quote | Direction | Default appearance |
| --- | --- | --- |
| Trade at or above a valid ask | `1` | Green, `#16834A` |
| Trade at or below a valid bid | `-1` | Red, `#B83B4B` |
| Trade strictly inside a valid spread | `0` | Native background |
| Missing or invalid bid/ask price, or ask at/below bid | `0` | Native background |
| No captured quote for that trade | `0` | Native background |

A valid comparison requires positive finite prices, excluding TWS's missing-value sentinel. A locked book has equal bid and ask; a crossed book has bid above ask. Neither is used for a directional color. Missing quote **sizes** do not by themselves invalidate a usable price pair; quote sizes are diagnostics. A later invalid quote replaces the prior book for future trades instead of silently reusing an old valid pair.

For example, with a preceding bid of **2.90** and ask of **2.92**, a trade at **2.92** is green, **2.90** is red, and **2.91** stays native. A subsequent quote of **2.92 × 2.94** does not change those directions. The comparison tolerates a few floating-point representation steps (ULPs) at the boundary, not a whole price tick.

Existing history and backfills without a captured event quote remain neutral. Watching an instrument can seed a book for future trade callbacks. It does not supply a quote for rows already in the tape. If the saved event is evicted or no longer matches its native instrument, price, and size, the tool cannot use that saved classification.

## Which rows are shown

An eligible trade has a positive, finite Last and Last Size. The history view shows up to the first 50 eligible prints in TWS's newest-first model. Quote-only rows and incomplete rows remain in the native model but do not occupy space in that filtered view. A neutral trade can still be a valid visible print. The tool cannot add data that TWS did not retain.

The Current summary is a separate native row and is not filtered. It can be colored when a matching capture is available. Selected cells keep native selection styling. The supported native tape can use hidden or reordered columns, since the data comes from its events. The tool preserves an existing application-owned sorter; filtering and normal classification are skipped for that table.

When switching symbols, TWS may temporarily clear or repopulate history. The tool follows the existing link groups and subscriptions. Judge whether switching worked after both panels settle and fresh trades arrive; the Current summary changing on its own is not proof that tape history has resumed.

## Why shades vary

Each directional cell blends its native background toward the configured color:

```text
intensity = 0.18 + 0.82 × sqrt(trade size / largest included trade size)
channel   = round(native channel + (target channel − native channel) × intensity)
```

The maximum is computed for the eligible set in that tape view, including eligible neutral trades. It is not limited to rows that happen to fit in the current scroll viewport. With a maximum of 10,000 shares:

| Print size | Approximate intensity |
| --- | ---: |
| 100 | 26% |
| 1,000 | 44% |
| 10,000 | 100% |

Neutral and selected cells retain native styling regardless of size. A print can get lighter or darker as the maximum changes, even though its direction is frozen. Native alternating row backgrounds can also produce slightly different rendered shades. Compare actual Size values when comparing prints across different times or symbols.

## Reading the tape in context

Green and red describe where a trade landed relative to the saved book. Each trade still has a buyer and a seller. A green trade does not establish the initiator with certainty, and it can be followed by a lower price.

Repeated trades near the offer with little upward price progress can be consistent with passive sell liquidity absorbing demand. The opposite pattern can occur at the bid. Look at share volume at a price, the response of subsequent quotes and trades, and whether displayed liquidity persists or refills. Replenishment is not proof of an iceberg or a specific participant. Quiet activity can also reflect exhaustion rather than absorption. The [README discussion](../README.md#reading-the-prints-price-response-and-absorption) gives more context and sources. This tool does not detect absorption or provide entry and exit signals.

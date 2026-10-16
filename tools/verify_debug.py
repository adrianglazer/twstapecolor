#!/usr/bin/env python3
"""Check one debug session's rotated CSVs independently; never contacts TWS."""
import argparse
import csv
import json
import math
from decimal import Decimal, localcontext
from pathlib import Path


def side(last, bid, ask):
    if not all(math.isfinite(x) and 0 < x < float.fromhex('0x1.fffffffffffffp+1023') for x in (last, bid, ask)) or ask <= bid:
        return 0
    with localcontext() as ctx:
        ctx.prec = 12
        last, bid, ask = (+Decimal(str(x)) for x in (last, bid, ask))
        return 1 if last >= ask else -1 if last <= bid else 0


def verify(paths):
    rows = []
    for path in paths:
        with Path(path).open() as stream:
            rows.extend(csv.DictReader(stream))
    rows.sort(key=lambda r: int(r['event']))
    latest = {}
    counts = dict(events=len(rows), quotes=0, trades=0, green=0, red=0, neutral=0,
                  directionMismatches=0, precedingQuoteMismatches=0, quoteChecks=0,
                  initialQuotesUnavailable=0, missingEvents=0, duplicateEvents=0)
    previous = 0
    for row in rows:
        event = int(row['event'])
        counts['missingEvents'] += max(0, event - previous - 1)
        counts['duplicateEvents'] += event == previous
        previous = event
        book = tuple(row[k] for k in ('bid', 'ask', 'bidSize', 'askSize', 'quoteSequence'))
        if row['type'] == 'Q':
            counts['quotes'] += 1
            latest[row['record']] = book
        elif row['type'] == 'T':
            counts['trades'] += 1
            expected = side(*(float(row[k]) for k in ('last', 'bid', 'ask')))
            counts['green' if expected > 0 else 'red' if expected < 0 else 'neutral'] += 1
            counts['directionMismatches'] += expected != int(row['direction'])
            preceding = latest.get(row['record'])
            if preceding is None:
                counts['initialQuotesUnavailable'] += 1
            else:
                counts['quoteChecks'] += 1
                counts['precedingQuoteMismatches'] += book != preceding
        else:
            raise ValueError('Unknown event type: '+row['type'])
    counts['complete'] = bool(rows) and not any(counts[k] for k in ('missingEvents', 'duplicateEvents', 'initialQuotesUnavailable'))
    counts['passed'] = counts['trades'] > 0 and counts['complete'] and not counts['directionMismatches'] and not counts['precedingQuoteMismatches']
    return counts


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('files', nargs='+', help='All CSV segments from exactly ONE debug start session')
    args = parser.parse_args()
    result = verify(args.files)
    print(json.dumps(result, indent=2))
    raise SystemExit(0 if result['passed'] else 1)

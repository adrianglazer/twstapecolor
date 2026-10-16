import csv
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('verify', Path(__file__).resolve().parents[1] / 'tools/verify_debug.py')
verify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)
FIELDS = 'event,epochMillis,monotonicNanos,type,record,eventObject,last,size,bid,ask,bidSize,askSize,quoteSequence,direction'.split(',')


class DebugVerifierTest(unittest.TestCase):
    def result(self, changes=None):
        rows = [dict(zip(FIELDS, row.split(','))) for row in [
            '1,1,1,Q,a,,NaN,NaN,10,11,100,200,1,0',
            '2,2,2,T,a,b,11,50,10,11,100,200,1,1',
            '3,3,3,Q,a,,NaN,NaN,11,12,300,400,2,0',
            '4,4,4,T,a,c,11,100,11,12,300,400,2,-1']]
        if changes:
            changes(rows)
        Path('work').mkdir(exist_ok=True)
        with tempfile.TemporaryDirectory(dir='work') as root:
            path = Path(root)/'events.csv'
            with path.open('w') as out:
                writer = csv.DictWriter(out, fieldnames=FIELDS)
                writer.writeheader()
                writer.writerows(rows)
            return verify.verify([path])

    def test_valid(self):
        result = self.result()
        self.assertTrue(result['passed'])
        self.assertEqual((result['green'], result['red'], result['quoteChecks']), (1, 1, 2))

    def test_wrong_direction(self):
        self.assertEqual(self.result(lambda r: r[1].update(direction='-1'))['directionMismatches'], 1)

    def test_wrong_preceding_book_size(self):
        self.assertEqual(self.result(lambda r: r[3].update(askSize='999'))['precedingQuoteMismatches'], 1)

    def test_missing_event(self):
        self.assertFalse(self.result(lambda r: r.pop(0))['complete'])

    def test_wrong_instrument(self):
        self.assertFalse(self.result(lambda r: r[1].update(record='other'))['passed'])

    def test_rounding_and_invalid(self):
        self.assertEqual(verify.side(2.36, 2.35, 2.3600000000000003), 1)
        for book in [(10, 11, 11), (10, 12, 11), (10, float('nan'), 11), (float('inf'), 10, 11)]:
            self.assertEqual(verify.side(*book), 0)
        self.assertEqual(verify.side(10.5, 10, 11), 0)


if __name__ == '__main__':
    unittest.main()

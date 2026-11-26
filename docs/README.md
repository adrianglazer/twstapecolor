# twstapecolor documentation

This manual describes **v0.3.22**. twstapecolor is an experimental Java agent for Interactive Brokers Trader Workstation (TWS). It adds price-relative trade backgrounds to the Time & Sales tape, filters the history view to valid prints, and can record local quote and trade diagnostics. It is unofficial and does not place orders or request market-data subscriptions.

| Start here if you want to… | Read |
| --- | --- |
| Install, start, customize, or stop the tool | [Installation and operation](user-guide.md) |
| Understand why a row is green, red, neutral, hidden, or a different shade | [How the tape is colored](tape-behavior.md) |
| Diagnose a blank tape, attachment problem, or unexpected color | [Troubleshooting and diagnostics](diagnostics.md) |
| Understand the hooks, maintain the code, or package a release | [Internals and maintenance](maintainer-guide.md) |

The [project README](../README.md) includes the before/after capture and an introduction to interpreting prints. The [distribution guide](../dist/README.md) explains the shared JAR and Windows launchers. See the [verification record](../LIVE-VERIFICATION.md) for dated evidence and the [live audit instructions](../tests/live/README.md) for reproducing it. Older reports describe the versions and directory layouts used at the time.

The tested live path is Linux TWS; Windows packaging and commands are provided, but Windows attachment and rendering have not been verified. The agent relies on TWS internals, so compatibility can change when TWS changes. Try it first in paper trading. Read the [project license and liability notes](../README.md#license-and-liability) before use.

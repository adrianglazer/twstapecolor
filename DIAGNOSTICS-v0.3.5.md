# v0.3.5 live tape renderer probe

Diagnostic only. No color changes in `probe` mode. Target: current TWS `feature.tape.ui.aN` / supported tape models.

```
PID=$(jcmd -l | awk '/install4j.jclient.Launcher/ {print $1; exit}')
./twstapecolor probe --pid "$PID"
# Wait 10-20 seconds while tape is visible/active.
./twstapecolor report --pid "$PID"
./twstapecolor stop --pid "$PID"
```

Upload the `twstapecolor-report-*.txt` file. It includes sample renderer labels, icon classes, selected model values, cell colors, and related properties. Inspect/redact any potentially sensitive table content before sharing. Report stores at most 80 initial renderer calls, and this is diagnostic-only, not a trade classifier. Open the tape window before probing. `stop` restores the original bytecode. Fresh AgentV035/HookV035 names avoid older versions loaded in JVM.

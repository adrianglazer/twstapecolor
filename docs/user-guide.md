# Installation and operation

[Documentation index](README.md) · [Tape behavior](tape-behavior.md) · [Troubleshooting](diagnostics.md)

## Requirements and installation choices

You need a compatible TWS process, a Time & Sales window, and **JDK 21 or newer** on the machine running TWS. The Java launcher uses the `jdk.attach` module to load the agent into the running application. Use the same operating-system account as TWS. On Windows, use a 64-bit native Windows JDK and run both at the same elevation level.

| Method | What you need | When to choose it |
| --- | --- | --- |
| Linux source build | JDK, Bash, `unzip`; `curl` or `wget` if ASM must be downloaded | You are editing the code or want to build from the checkout |
| Shared distribution JAR | JDK and the [versioned JAR](../dist/twstapecolor-v0.3.20.jar) | You want to try the tool without compiling it |
| Windows double-click launchers | JDK, the same JAR, and both `.cmd` files in [dist](../dist/README.md) | You want to start and stop from Explorer |

The JAR bundles ASM 9.8 and both license notices. It does not bundle Java or TWS. The Windows instructions are provided for testing; there is no verified Windows compatibility claim.

## Linux: build and run

From the repository root:

```bash
./build
./twstapecolor --version
./twstapecolor start
./twstapecolor status
./twstapecolor stop
```

Open the TWS tape before starting. `./build` delegates to `build.sh` and writes `out/twstapecolor.jar`. The first build downloads ASM into `lib/` if absent. The launcher uses the existing JAR; rerun `./build` after editing sources.

To run the distribution JAR instead, keep its filename unchanged and put it in a writable `dist/` folder under the project directory:

```bash
(cd dist && sha256sum -c SHA256SUMS)
java --add-modules jdk.attach -jar dist/twstapecolor-v0.3.20.jar start
```

Replace `start` with `status` or `stop` for those actions. The checksum verifies the file against the repository's published checksum.

## Windows: run the shared JAR

Extract the repository ZIP. In `dist`, double-click **start-twstapecolor.cmd** to start or **stop-twstapecolor.cmd** to stop. Keep the JAR and both scripts together. The scripts pause so you can read results, including messages about a missing JAR or unsuitable JDK. They prefer an existing Java executable under `JAVA_HOME`; otherwise they look up Java on `PATH`. If the selected Java is too old, correct `JAVA_HOME` or `PATH` and reopen the launcher.

For direct use from PowerShell in the project directory:

```powershell
java -version
Get-FileHash .\dist\twstapecolor-v0.3.20.jar -Algorithm SHA256
java --add-modules jdk.attach -jar .\dist\twstapecolor-v0.3.20.jar start
java --add-modules jdk.attach -jar .\dist\twstapecolor-v0.3.20.jar status
java --add-modules jdk.attach -jar .\dist\twstapecolor-v0.3.20.jar stop
```

Compare the hash to [SHA256SUMS](../dist/SHA256SUMS). The versioned filename lets the launcher load that file directly, avoiding replacement of a JAR the Windows JVM may hold open. Use Windows Java for Windows TWS, rather than invoking Linux Java through WSL.

## Select a TWS instance

By default the tool looks for a visible Java process whose launcher name is `install4j.jclient.Launcher`, optionally followed by arguments. It attaches automatically if there is exactly one match. If none match, it prints a message and exits with code 1 before creating a command report. If several match, it lists their PIDs and exits with code 1.

Use `jcmd -l` to identify the intended process and provide its PID explicitly. This also works around discovery when a TWS installation uses a different launcher name. An explicit PID bypasses name matching, so verify the target. The PID can change when TWS restarts. Use the same target for every command in a diagnostic sequence.

```bash
jcmd -l
./twstapecolor start --pid 12345
./twstapecolor status --pid 12345
./twstapecolor stop --pid 12345
```

Replace `12345` with the real PID. Options work the same way when running the JAR directly.

## Command and option reference

| Command | Effect |
| --- | --- |
| `--version` or `version` | Prints the tool version without attaching. Use it by itself. |
| `start` | Enables normal trade classification, history filtering, and coloring. |
| `status` | Reports this version's hooks, observations, paint counters, and debug state. |
| `stop` | Disables the agent and attempts to restore native rendering and bytecode. |
| `test-paint` | Enables alternating full-strength colors for visual diagnosis. These colors are unrelated to trades. |
| `probe` | Switches to renderer sampling without colors, removes the private filter, and stops continuous debug capture. |
| `report` | Prints the bounded renderer samples collected in probe mode. |
| `dump` | Writes a snapshot of the in-memory renderer class to project logs. For compatibility work only. |

All attach commands accept `--pid PID` and `--debug`. `--debug` retains the command response; on `start` it also opens a live CSV recording. Only `start` accepts `--ask-color` and `--bid-color`. Both take a quoted six-digit `#RRGGBB` color. Each normal start resets unspecified colors to their defaults.

```bash
./twstapecolor start --ask-color '#006B3C' --bid-color '#8B2025'
```

Run `start` after a visual test to return to real classification. Use `stop` to restore native behavior. Closing the launcher terminal does not stop the loaded agent. If restoration warns or TWS behaves unexpectedly, fully restart TWS. Before upgrading from an older active version, restart TWS first; a new JAR does not unload another version's hooks.

## First-session checklist

1. In paper TWS, open Time & Sales on a symbol receiving quotes and trades. Time, Last, and Size are enough; you do not need to add Bid or Ask columns.
2. Start the agent and wait for new trades. Rows already present can remain neutral. Check that positive-size trades appear and that eligible rows tint across the visible columns.
3. If you use linked windows, switch symbols in TWS and allow the native view to settle. Confirm fresh history rows arrive for the new symbol and any populated Level 2 book keeps its native colors.
4. Run `status` during activity, then `stop`. Confirm native appearance returns. If a check fails, follow the [troubleshooting guide](diagnostics.md).

The 50-print cap applies to retained history, not a promise that 50 executions will be visible. TWS controls both retention and how many rows fit in the window.

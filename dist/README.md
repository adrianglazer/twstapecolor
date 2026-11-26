# Ready-to-run Java distribution

`twstapecolor-v0.3.22.jar` is the cross-platform Java application and TWS agent, compiled for Java 21. It includes ASM 9.8, the project's MIT license (`LICENSE` inside the JAR), and ASM's BSD-3-Clause license (`ASM-LICENSE.txt` inside the JAR). No TWS classes or native platform binaries are included.

For double-click use on Windows, open this folder and run **`start-twstapecolor.cmd`**. To restore native rendering, run **`stop-twstapecolor.cmd`**. Keep both scripts and the JAR together. They keep the console open so errors can be read. The start script prefers `JAVA_HOME`, otherwise searches `PATH`, and checks the attach module and Java 21 class compatibility. If a suitable JDK is missing, it explains how to install one and displays a [download link](https://learn.microsoft.com/java/openjdk/download). Update an outdated `JAVA_HOME` if necessary. From a command prompt, the start script also accepts normal tool arguments, for example `start-twstapecolor.cmd status` or `start-twstapecolor.cmd start --debug`.

The batch scripts have been reviewed here but not executed on Windows; the JAR's Linux packaging checks do not establish Windows attachment or rendering compatibility.

Windows users need a 64-bit Windows JDK 21+ and a compatible running TWS. Windows attachment and rendering have not been live-tested. The same file runs on Linux. From the project root:

```bash
java --add-modules jdk.attach -jar dist/twstapecolor-v0.3.22.jar start
java --add-modules jdk.attach -jar dist/twstapecolor-v0.3.22.jar status
java --add-modules jdk.attach -jar dist/twstapecolor-v0.3.22.jar stop
```

On Windows, use PowerShell from the project root:

```powershell
java --add-modules jdk.attach -jar .\dist\twstapecolor-v0.3.22.jar start
java --add-modules jdk.attach -jar .\dist\twstapecolor-v0.3.22.jar status
java --add-modules jdk.attach -jar .\dist\twstapecolor-v0.3.22.jar stop
```

Use the same Windows account and elevation level as TWS. One TWS instance is detected automatically; use `--pid PID` for multiple instances. Keep the filename unchanged and this directory writable. Temporary responses are created here; debug data goes in the parent project's ignored `logs/` directory. A single-file download can also be placed in a writable `twstapecolor/dist/` directory with that layout. Restart TWS before upgrading an older active agent.

Verify the download with `Get-FileHash .\dist\twstapecolor-v0.3.22.jar -Algorithm SHA256` and compare it with `SHA256SUMS`. Linux users can run `(cd dist && sha256sum -c SHA256SUMS)`.

To rebuild the distribution from the repository sources on Linux, run `bash tools/package.sh`. This refreshes the JAR and checksum; commit both together after testing. Ordinary `bash build.sh` and `bash test.sh` do not overwrite the committed distribution.

See the main [README](../README.md) for configuration, diagnostics and compatibility limits.

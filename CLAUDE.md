# Working in this repo

## Tests

Don't run the full gametest suite (`./gradlew runGameTestServer` with no filter): it takes about 45 minutes.
Run only the tests your change touches:

```
./gradlew runGameTestServer -Ptests='pipican:<test name or glob>'            # e.g. pipican:tamashigane, pipican:*_combat
./gradlew runGameTestServer -Ptests='pipican:<glob>' -PrepeatCount=10        # repeat, to check for flakiness
```

A single test takes about 30 seconds. Test names are the keys of the `TESTS` maps in `src/gametest/java/com/pipican/gametest/`
(`ClassSystemTests`, `BossTests`, ...). Compile first with `./gradlew compileJava compileGametestJava -q`.

Don't run two gametest servers at once - they share the run directory and fail.

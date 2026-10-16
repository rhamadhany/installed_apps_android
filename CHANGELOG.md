## 0.1.0

### Performance
- **Semaphore-bounded concurrency.** Each app is processed in its own
  coroutine with a `Semaphore(numCores)` guard, replacing the previous
  "chunk by `numCores * 2`, dispatch all at once" strategy. Prevents
  oversubscribing the CPU pool and eliminates GC storms on devices with
  200+ installed apps.
- **Downscale icons to 144 px** instead of using the drawable's intrinsic
  size (often 512×512 on adaptive-icon devices). Per-icon bitmap memory
  drops from ~1 MB to ~80 KB — a 12× reduction.
- **Encode icons as WEBP @ 85** instead of PNG @ 100. 5–10× faster to
  compress, smaller output, visually identical for icon artwork.
- **Split dispatchers per workload:** APK reads (`loadLabel`, `loadIcon`)
  run on `Dispatchers.IO`; the CPU-bound render/compress phase runs on
  `Dispatchers.Default`.

### Reliability
- Plugin owns a long-lived `CoroutineScope(SupervisorJob + Dispatchers.Default)`
  instead of spawning a new scope per call. No more leaked Jobs.
- Per-app failures are logged (`Log.w`) and skipped, instead of being
  silently swallowed.
- Icon bitmap is always recycled via `try/finally`.
- `getInstalledApps` reports method-channel errors on the Dart side via
  `result.error(...)` instead of silently dropping.

### Compatibility
- `WEBP_LOSSY` on API 30+, falls back to the deprecated lossy `WEBP` for
  API 21–29. No min-SDK bump required.

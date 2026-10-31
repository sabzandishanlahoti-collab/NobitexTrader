# NobitexTrader — Project Handoff & Engineering Contract

> This file is the durable source of truth for future contributors and AI assistants. Read it before changing code. Update it whenever a verified milestone or architectural decision changes.

## 1. Repository and branch safety

- Repository: `sabzandishanlahoti-collab/NobitexTrader`
- Active working branch: `afa-mobile-dashboard-review`
- Current integration target: pull request #1 into `main`
- **Never commit to, reset, force-push, or merge into `main` unless the owner explicitly requests it.**
- Work on the active branch only. Before every commit, verify the branch head and use a compare-and-swap ref update.
- Do not create a second competing implementation or revive the abandoned Trading AFA repository.

## 2. Product goal

Build a reliable Android market-analysis/trading application for the Nobitex ecosystem, with Persian and English UI and a path toward a trustworthy installable APK.

Long-term product direction includes:
- Public market data and market scanning.
- Explainable strategy/market analysis and risk controls.
- Historical trade/event records and measurable performance.
- Later, authorized private-account integration and order execution, only after secure implementation and explicit user approval.
- Research of large flows and traceable traders/wallets using legal, verifiable data sources.

Important distinction:
- Public blockchain addresses and their on-chain activity may be analyzed where data is available.
- Private exchange accounts, balances, orders, and trader identities are not publicly traceable without authorized access. Never claim otherwise or fabricate wallet/account data.

## 3. Non-negotiable engineering rules

1. **Preserve working behavior.** Do not change scanner, exchange, strategy, risk, or execution logic during a visual-only task.
2. **No fake/demo functionality presented as real.** No fabricated prices, trades, wallets, balances, signals, performance, or API success.
3. **Verify before claiming.** Inspect actual source, API contracts, test results, and build output. Mark unsupported integrations as not implemented.
4. **Audit dependencies before editing.** Trace callers, state flow, models, persistence, and tests for every affected module.
5. **Make coherent batches.** Group related files into one reviewable subsystem change. Avoid isolated cosmetic micro-commits and avoid mixing unrelated features.
6. **No speculative rewrites.** Do not replace libraries, architecture, naming, navigation, or established patterns just because a new contributor prefers another approach.
7. **Full-file correctness.** Keep imports, package names, Compose conventions, Gradle configuration, and existing APIs consistent. Do not leave duplicate entry points or duplicate source trees.
8. **Test the actual change.** Run/inspect Android build and tests through GitHub Actions. A commit is not complete until its exact SHA has a successful workflow.
9. **One active batch at a time.** Wait for Actions on the current branch head before starting another batch. If red, inspect failing jobs/logs and fix the cause before proceeding.
10. **No premature completion claims.** Distinguish implemented, build-verified, integration-tested, and production-verified features.

## 4. Current verified checkpoint

- Active branch head at this checkpoint: `17f0d88e18c0bcb12054304c4f957a374f874490`
- Latest verified workflow for that SHA: Android APK Build and Tests, run #325 — **success**.
- Workflow: https://github.com/sabzandishanlahoti-collab/NobitexTrader/actions/runs/41192831419
- Latest change at checkpoint: dashboard/scanner metric-card typography and settings subtitle layout polish.
- Files touched by that change:
  - `app/src/main/java/ir/nobitextrader/app/MainActivity.kt`
  - `app/src/main/java/ir/nobitextrader/app/scanner/MarketScannerScreen.kt`
  - `app/src/main/java/ir/nobitextrader/app/SettingsScreen.kt`
- That change was visual-only; it did not implement private account access, real order execution, or public-wallet/large-flow tracking.
- Existing app areas include Material 3 theme, dashboard/navigation, settings, and a Nobitex public-market scanner. Do not infer production readiness from a successful APK build alone.

## 5. Required development sequence

Work through complete, dependency-aware phases. Re-audit actual code before declaring a phase complete.

### Phase A — Baseline and architecture audit
- Inventory modules, screens, data sources, repositories, view models, persistence, permissions, and tests.
- Map data flow from Nobitex public API/WebSocket (only where implemented) to normalized models, scanner, UI, and logs.
- Identify duplicated code, dead code, missing error states, stale-data risks, and untested paths.
- Produce a concise gap list before large implementation changes.

### Phase B — Public market data foundation
- Verify supported Nobitex public endpoints, rate limits, response formats, symbol coverage, timestamps, and error handling against current source/API documentation.
- Normalize market data and explicitly represent missing/stale values.
- Add deterministic tests with fixtures; do not use synthetic values in production UI.
- Make scanner refresh, cancellation, pagination/batching, and state transitions reliable.

### Phase C — Scanner and analysis
- Verify symbol discovery and scan coverage against actual API responses.
- Implement explainable filters, sorting, spread/liquidity/volume metrics, and strategy signals only when inputs are real and timestamps are valid.
- Show data freshness, source, and failure state. Do not imply that a displayed candidate is a guaranteed trade.

### Phase D — Risk, history, and backtesting
- Define position sizing, exposure limits, fees, slippage, stop/target semantics, and daily loss limits.
- Persist every simulated or authorized real trade with source, timestamps, entry/exit, costs, and realized P/L.
- Separate backtest, paper, and live modes clearly. Test accounting and edge cases.

### Phase E — Account security and execution (later phase)
- Only add private account access using official supported authorization and least-privilege credentials.
- Keep secrets out of source code, logs, crash reports, and Git history.
- Require explicit user opt-in and clear live-trading safeguards. Never silently place orders.
- Do not claim execution support until exchange integration, error handling, reconciliation, and tests are verified.

### Phase F — Public wallet and large-flow research (later phase)
- Use only verifiable public-chain data and documented APIs/indexers.
- Clearly distinguish exchange hot wallets, known labels, unknown addresses, and attributed entities.
- Never infer a private exchange trader's identity or holdings from public market trades.
- Show provenance, chain, transaction hash, block/time, asset, amount, and confidence where available.

### Phase G — Release readiness
- Verify Persian/English switching, accessibility, lifecycle, offline/error states, permissions, security, and device behavior.
- Build a release APK/AAB through Actions; verify artifact and installation.
- Maintain versioned release notes and document known limitations.

## 6. Required status language

For each feature, use one of these states:
- **Not started**
- **In progress**
- **Implemented, not yet verified**
- **Build/test verified**
- **Integration verified**
- **Production verified**

A green GitHub Actions run proves only that the configured workflow passed for that commit. It does not by itself prove API correctness, exchange compatibility, trading profitability, or production readiness.

## 7. Instructions for the next assistant/session

1. Read this file first, then inspect the current branch head and PR #1.
2. Check the latest Actions result for the exact branch-head SHA.
3. Do not restart visual redesign or reformat stable files without a concrete defect.
4. Continue from the next incomplete phase above; first perform the baseline audit if it has not been documented.
5. Before editing, state the exact scope and files; then implement a coherent batch on `afa-mobile-dashboard-review`.
6. Run Actions, inspect failures, and update this file with the new verified SHA/status.
7. Never ask the owner to repeat decisions already recorded here. If a decision is not recorded, inspect code/history before asking.

---
Last checkpoint recorded: 2026-10-31.


## 8. Baseline audit — first source inspection (2026-10-31)

This is a source-level inventory, not a claim of production readiness. The latest verified checkpoint after adding this section must be recorded separately once Actions passes.

### Repository structure observations
- The Android Gradle module is `app`; its configured source namespace is `ir.nobitextrader.app`.
- The actual launcher declared in `app/src/main/AndroidManifest.xml` is `ir.nobitextrader.app.MainActivity`, implemented at `app/src/main/java/ir/nobitextrader/app/MainActivity.kt`.
- The recursive Git tree also contains legacy-looking or misplaced files outside the active Java source root (for example root-level `MainActivity.kt`, `app/src/main/MainActivity.kt`, and a nested `.github/workflows/.github/workflows/android.yml`). These are not the manifest-selected app entry point or the active workflow. Do not delete them until their history and intended use are understood.
- Some paths contain the invisible WORD JOINER character U+2060 (for example `app/src/main/java/ir/nobitextrader/app⁠/data/`). Preserve exact path spelling when fetching or editing; do not normalize it accidentally.

### Confirmed implemented source components
- Public market statistics client: `data/NobitexMarketApi.kt` — read-only HTTP GET and response validation.
- Public order-book client: `data/NobitexOrderBookApi.kt` — single-symbol and all-order-books parsing, validation of levels and crossed books.
- Public recent trades client: `data/NobitexTradesApi.kt` — read-only recent public trades; source comments explicitly state that trades do not identify an account/person.
- Public candle client: `data⁠/NobitexCandlesApi.kt` — read-only OHLCV history parser.
- Scanner: `scanner/MarketScanner.kt`, `MarketScannerViewModel.kt`, filtering/ranking/refresh policies and UI. The view model starts/stops auto-refresh, streams scan results, requests recent public trades, and calls the strategy analysis coordinator.
- Strategy research: multiple strategy implementations (FMC, FVG, liquidity sweep, order block, scalping, whale pressure), shared strategy orchestration/decision analysis, and a deterministic OHLC backtest engine.
- Risk research: `risk/RiskEngine.kt`, risk event models/serializer/store and persistent file-backed risk event history.
- Test tree includes unit tests for API parsers, scanner, strategy modules, backtest, and risk components. The configured workflow runs `gradle :app:testDebugUnitTest`, then `:app:assembleDebug`.

### Important integration boundaries / gaps established by source inspection
- Dashboard explicitly labels the app READ ONLY. Capital, position, net-profit, deposit, and withdrawal cards display em-dash placeholders; open positions and trade history are empty panels. The UI explicitly says these require verified authenticated exchange data.
- No authenticated account balance/deposit/withdrawal/order integration was found in the inspected app manifest, Gradle dependencies, dashboard, or public API clients. Treat private account integration and live order execution as **Not started / not verified**, not implemented.
- The public trades endpoint provides public market prints, not the identity, balance, or positions of the trader behind each print. Public buy/sell flow summaries must never be described as identified whale/account activity.
- The risk engine is a standalone decision component whose request requires account equity, free capital, open risk, stop, costs, spread, data-validity, liquidity, and kill-switch inputs. Source inspection has not established a production order pipeline that calls it before submitting orders. Treat risk-to-execution wiring as unimplemented until call sites and end-to-end tests prove otherwise.
- Backtest is explicitly research-only and does not submit orders. It uses a caller-supplied signal callback; a green unit test/build does not establish strategy profitability or exchange execution.
- `NobitexOrderBookApi.fetchAll` validates `maxMarkets` but documents that the parameter does not limit returned markets. `MarketScanner.scanAll` processes every valid order book returned by the all endpoint. UI settings named “markets per batch” therefore need an integration/semantics review before being represented as a cap on all-market scanning.
- Dashboard market snapshot currently loads a BTC/IRT public market summary separately from the scanner. The dashboard account cards are not connected to authenticated account data.
- The active workflow builds a **Debug APK** and uploads it as an artifact. It does not currently create a signed release APK/AAB or GitHub Release.

### Audit follow-up required before feature implementation
1. Trace the entire scanner-to-strategy path, candle-window selection, stale-data checks, and all relevant UI state transitions.
2. Verify exact current Nobitex endpoint contracts, symbol units/quoting conventions, timestamp units, all-order-book behavior, and rate limits against current official documentation/live fixtures.
3. Determine which root/legacy/misplaced files are historical artifacts and plan safe cleanup only if necessary.
4. Trace all references to `RiskEngine`, persistent risk history, and backtest entry points to prove whether they are reachable from app UI or only isolated/tested classes.
5. Audit all strategy formulas, thresholds, candle requirements, and tests before tuning or claiming signal quality.
6. Add an explicit feature matrix with status labels and integration evidence. Do not infer completion from filenames or class existence.

### Verified build checkpoint after the audit-document commit
- Commit: to be filled after this documentation commit is created and its exact-SHA Actions run succeeds.
- Workflow status: pending verification.

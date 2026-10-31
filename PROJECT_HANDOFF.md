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

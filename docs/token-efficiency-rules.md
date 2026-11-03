# Agent Operating Rules — Token Efficiency

Use this as the workspace rules file (or paste it as the first message of a session).
Keep it stable so it can be cached as a prefix; do not edit it per task.

---

You are a coding agent working in a Kotlin Multiplatform + Go repository.
Your goal is to complete each task with the **minimum tokens and tool calls** needed for a correct, verified result.

## Reading code
- Never read whole directories or dump entire files by default.
- Start from `docs/REPO_MAP.md` (module → key classes → signatures). Open a file only when you need its body.
- Prefer targeted search (symbol name, error string) over browsing.
- Read only the relevant range of a large file (function or class), not the whole file.
- Do not re-read a file you already read this session unless it changed.

## Planning
- Before editing, write a plan of at most 5 bullets: files to touch, the change, how you will verify.
- If the task is ambiguous, ask ONE question instead of exploring speculatively.
- Stay inside the task scope. No drive-by refactors, renames or formatting changes.

## Editing
- Make small, patch-style edits. Never rewrite a whole file to change a few lines.
- One logical change per step, then verify.
- Follow existing patterns in the module; do not introduce new libraries without asking.
- `scaffold.yaml` is a protected file: never change it without asking first.

## Running commands
- Dev machine is Windows; use PowerShell syntax and `./gradlew.bat`, not bash-only commands. iOS code is verified only on the macOS CI runner.
- Git is manual: only read-only git commands are allowed (`status`, `log`, `diff`, `branch --list`). Never add, commit, amend, push or switch branches.
- Run the narrowest command that verifies the change:
  - one module: `./gradlew :feature:x:allTests`, not `./gradlew build`
  - one test class when iterating, the module suite before finishing
- When reading build/test output, extract only: failing task, `file:line`, error message, and up to 5 lines of context. Ignore the rest.
- Do not paste full logs into your reasoning or replies.

## Loop limits
- If the same error appears twice after your fixes, stop and report: the error, what you tried, your best hypothesis.
- Maximum 3 fix attempts per failing check before escalating to the human.
- Never edit, delete, skip or weaken a test to make it pass unless the task explicitly says the test is wrong. If you believe a test is wrong, stop and say why.

## Replies
- Be terse. No restating the task, no narrating each step.
- Final report format:
  - **Changed:** files + one line each
  - **Verified:** commands run + result
  - **Open issues:** anything unresolved (or "none")

## Memory between sessions
- At the end of a task, append at most 3 lines to `docs/AGENT_NOTES.md` with durable facts learned (e.g., "iOS tests need `-Pkotlin.native.cacheKind=none`"). Do not log task narratives.

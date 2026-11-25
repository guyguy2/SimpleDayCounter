# Plan: yearly repeating events

Status: planned, not started. Written 2026-11-25 after the W7 merge (`1505b96`).
Implement on a new branch from `main` (`feat/yearly-repeat`), never directly on `main`:
every push to `main` runs `release-internal.yml` and publishes to the Play internal track.

## Instructions for the implementing session

Read this whole file and `AGENTS.md` first, then implement the plan.

Setup
- Branch `feat/yearly-repeat` from `main`. Never commit to `main` and never push `main`.
- Do not push any branch or open a PR until the owner says so.

Scope and conduct
- Yearly repeat only. The 7 decisions below are final; do not re-litigate them.
- "Open details" lists the owner's defaults. Use them unless you find a concrete problem, and say so
  if you deviate.
- Make minimal, focused changes. Do not touch unrelated code. The unused `Item`-based `filterItems`
  and `sortItems` helpers stay as they are.
- No emojis and no em dashes in code, comments, docs or commit messages.

Rules from `AGENTS.md` that matter here
- Room: explicit `MIGRATION_5_6`, registered in `AppModule`, with the committed `6.json` schema.
- Strings: all UI text in `res/values/strings.xml`; `StringLiteralGuardTest` must pass.
- Write the tests listed under "Tests" alongside each step.
- Update `AGENTS.md` at the end (database version, migrations, form and card notes).

Verification
- Run `./gradlew testDebugUnitTest lintDebug assembleDebug` and report the results.
- Instrumented tests run only on an emulator and always with an explicit device
  (`adb -s emulator-5554 ...`). Never a bare `adb`, never `connectedDebugAndroidTest`, never a
  personal device. Reset any locale or animation settings afterwards.

Commits
- Small commits with plain subjects, since they feed the Play release notes (for example "Add yearly
  repeat for birthdays and anniversaries"). End each commit message with:
  `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`

When done, summarize what changed per plan step, the test results, and any deviation from the plan.

## Context

SimpleDayCounter shows days remaining or elapsed for named date events. Today every event is a
one-off: once the date passes it counts up as "N days past". That is right for "days since I quit
smoking" but wrong for birthdays and anniversaries, where the user must edit the date every year.

The app has no notifications and no network layer, so general recurrence (daily, weekly, monthly,
custom) has almost no value here; those belong in calendar apps.

## Decisions (made with the owner, 2026-11-25)

| # | Decision | Choice |
|---|---|---|
| 1 | Scope | Yearly only, as an opt-in "Repeats every year" toggle |
| 2 | When an event passes | Roll to next year at midnight. It shows "today" on the day itself, then the next year's date from the next day (about 364 days left). A repeating event is never "past". |
| 3 | Feb 29 in non-leap years | Feb 28. Document in a code comment and cover with a test. |
| 4 | Card display | Add a years line (for example "turns 5" / "5 years") when the original date is in the past. Nothing extra when it is not. |
| 5 | Toggle availability | Always shown in Add and Edit, off by default, for any date (past, today or future) |
| 6 | Timing | After W7 merged (done). Own branch, own v5 -> v6 migration, own PR. |
| 7 | Reminders | Out of scope. No notification permission, no alarms, no settings. A separate, later decision. |

Rejected: monthly/weekly/custom intervals; a one-day "yesterday" grace state before rolling;
letting a repeating event count as past; a three-way event type picker (Countdown / Count-up /
Yearly); birthday reminders in this wave.

## Design

Store the original date and a flag. Derive the next occurrence at read time. Never rewrite
`target_date`, so edit, delete and undo keep working unchanged and there is no background job.

### Data

- `Item` (`data/database/Database.kt`): add
  `@ColumnInfo(name = "repeats_yearly", defaultValue = "0") val repeatsYearly: Boolean = false`.
  The `defaultValue` is required: Room validates the migrated table against the entity, and an
  `ADD COLUMN ... DEFAULT 0` without a matching `defaultValue` fails schema validation.
- `AppDatabase` version 5 -> 6. Add `MIGRATION_5_6` (new file or next to `migration4To5` in
  `data/database/Migration.kt`): `ALTER TABLE items ADD COLUMN repeats_yearly INTEGER NOT NULL DEFAULT 0`.
- `di/AppModule.kt`: `.addMigrations(MIGRATION_4_5, MIGRATION_5_6)`. Leave
  `fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3)` as is.
- Commit `app/schemas/com.happypuppy.daycounter.data.database.AppDatabase/6.json`.
- `data_extraction_rules.xml` and `proguard-rules.pro` need no change (same database file, no new
  ViewModel, converter or route).

### Date logic (`util/Utils.kt`, new pure function)

```kotlin
/** Next date on or after [today] that falls on the month and day of [date]. */
fun nextYearlyOccurrence(date: LocalDate, today: LocalDate): LocalDate {
    if (date >= today) return date          // not reached yet: ordinary countdown to the original date
    val thisYear = date.withYear(today.year)  // Feb 29 -> Feb 28 in non-leap years (java.time clamps)
    return if (thisYear >= today) thisYear else date.withYear(today.year + 1)
}
```

- `LocalDate.withYear` clamps Feb 29 to Feb 28 in non-leap years. That is decision 3, with no
  custom rule. Pin it with a test so a future change cannot silently move it to Mar 1.
- Years count: use `next.year - date.year`, not `ChronoUnit.YEARS.between`. `YEARS.between` of
  2000-02-29 and 2025-02-28 is 24, but the event is on its 25th occurrence.
- Show the years line only when that count is greater than 0 (original date in the past).

### Mapping and UI model (`ui/screens/EventListModels.kt`)

- `Item.toEventUi(today, ...)`: if `repeatsYearly`, compute `next = nextYearlyOccurrence(targetDate, today)`;
  `daysLeft = days between today and next`. Otherwise unchanged.
- `EventUi`: set `targetDate` to the effective (next) date so the card date, `daysLeft` and the
  `TARGET_DATE_*` sorts all agree. Add `repeatsYearly: Boolean = false` and `years: Int = 0`
  (0 means no years line). Defaults keep the `@Preview` constructions in `MainScreen.kt` compiling.
- `filterEvents` and `sortEvents` need no change: they work on `daysLeft` and `targetDate`, so
  repeating events fall under FUTURE or TODAY and never PAST.
- `filterItems` and `sortItems` (the `Item`-based versions) are not called from `main`, only from
  tests. Pre-existing and out of scope: leave them alone, but note they ignore the flag.
- `EventViewModel` already recomputes on `_today` (`midnightDateFlow`, `refreshToday()`), so the
  midnight roll needs no new code. Verify with a test that crossing midnight changes `daysLeft`.
- `saveItem(...)` and `updateItem(...)` take and persist `repeatsYearly`. Undo restores the same
  `Item` instance, so the flag survives delete and undo.

### Form (`ui/components/EventForm.kt`, `EditEventScreen.kt`, `BottomSheet.kt`, `MainScreen.kt`)

- `EventFormState`: add `repeatsYearly: Boolean = false`, `withRepeatsYearly(Boolean)`, and carry it
  in `fromItem`.
- `EventFormStateSaver` is a positional `listSaver`: append the flag as index 4 and restore it
  tolerantly (`as? Boolean ?: false`) so state saved by an older build still restores.
- `EventForm`: a `Switch` row labelled "Repeats every year" (with a short supporting line), placed
  near the date field. Stateless like the rest of the form, no Koin.
- Thread the value through `AddEventBottomSheet`, `EditEventScreen` (it copies the `Item` at
  `EditEventScreen.kt:37`) and the `MainScreen` save calls (around `MainScreen.kt:246`).

### Card (`ui/components/EventCard.kt`)

- Show the years line below or beside the date when `years > 0`.
- Accessibility: include "repeats every year" and the years in the card's TalkBack description, and
  make the switch row a single labelled toggle target.
- Colors: repeating events only ever use the future and today states. No new colors, so
  `EventCardColorsTest` is unaffected.

### Strings (`res/values/strings.xml`)

- `form_repeat_yearly` and `form_repeat_yearly_supporting`
- a `<plurals>` for the years line, for example `event_card_years` (decide wording below)
- a TalkBack string for "repeats every year"
- Follow the naming rules in AGENTS.md (`form_`, `event_card_`), XML entities for typographic
  characters, no spaced hyphens between digits. `StringLiteralGuardTest` must stay green.

## Open details (my defaults, not owner decisions)

1. Years wording: "turns N" suits birthdays only; "N years" works for anniversaries too. Default to
   the neutral "N years" unless the owner prefers "turns".
2. Card date for a repeating event: default to the next occurrence, so the date and the day count
   agree. The original date stays in the Edit form.
3. Past-today edge: an event whose original date is today shows "today" and `years = 0`.

## Tests

JVM (`app/src/test`):
- New `NextYearlyOccurrenceTest`: original in the future (returns the original), today (returns
  today), earlier this year (rolls to next year), later this year, previous years, Feb 29 in a
  leap and non-leap year (Feb 28), Dec 31 / Jan 1 year boundary.
- `EventViewModelTest`: repeating event shows `daysLeft` to the next occurrence; crossing midnight
  rolls from 0 to about 364; never matches the PAST filter; sorts by next occurrence; delete then
  undo keeps the flag; save and update persist the flag.
- `EventFormStateTest`: toggle state, `fromItem`, saver round trip, and restore of an old 4-element
  saved list defaulting to off.
- `FakeItemDao` and any `Item(...)` test helpers: pass the new field where needed.
- `StringLiteralGuardTest`: must pass with the new strings.

Instrumented (`app/src/androidTest`, emulator only, not run by CI):
- New `Migration5To6Test` in the style of `Migration4To5Test` (existing rows get `repeats_yearly = 0`,
  Room validates the schema, CRUD works afterwards).
- Extend `MainFlowInstrumentedTest`: create a repeating event, see it on the card, edit the toggle.

## Implementation order

1. Branch from `main`.
2. `nextYearlyOccurrence` plus its tests.
3. `Item` field, DB version 6, `MIGRATION_5_6`, `AppModule`, `6.json` schema, `Migration5To6Test`.
4. `EventUi` and `toEventUi`, then ViewModel save/update plumbing and tests.
5. `EventFormState` and saver, form switch, strings.
6. Card years line and accessibility text.
7. Update `AGENTS.md`: database version and migration list, "Persistence" and "Dates & utilities"
   sections, the form description, and the strings notes.
8. Run `./gradlew testDebugUnitTest lintDebug assembleDebug`, then the instrumented tests on a named
   emulator (`adb -s emulator-5554 ...`, never a bare `adb` and never on a personal device).
9. PR to `main`. Commit subjects feed the Play release notes: use a plain subject such as
   "Add yearly repeat for birthdays and anniversaries"; `docs`, `chore`, `ci`, `build` and `wip`
   subjects are dropped.

## Risks

- Migration: an `ADD COLUMN` without a matching `defaultValue` on the entity fails validation at
  startup. Covered by `Migration5To6Test` and the committed `6.json`.
- Feb 29 and year boundaries: easy to get wrong with `ChronoUnit.YEARS`; covered by the tests above.
- Do not push `main` to release this: the push publishes to the internal track. Release from a clean,
  merged `main` per the hub checklist in `AGENTS.md`, and only with the owner's explicit yes.

## Out of scope

Reminders and notifications, monthly/weekly/custom intervals, widgets, `Item.createdAt` changes,
and the unused `filterItems` / `sortItems` helpers.

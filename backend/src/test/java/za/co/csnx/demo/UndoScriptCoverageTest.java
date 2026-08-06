package za.co.csnx.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rollback guarantee: **every migration authored from {@value #UNDO_REQUIRED_FROM}
 * onwards ships a matching undo script.** Fleet-wide rule — every module carries this
 * test; see {@code platform/docs/migrations.md} §"Every migration ships an undo
 * script" for the canonical statement of it.
 *
 * <p>Flyway Community cannot run an undo — {@code flyway undo} is a Teams-edition
 * command — so nothing about this is automatic the way MyBatis Migrations'
 * {@code -- //@UNDO} section is. What the undo script buys is that the person rolling
 * back at 02:00 has the reverse statement written by the person who understood the
 * forward one, reviewed in the same merge request, instead of composing it from the
 * DDL under pressure. Restoring a backup stays the blunt instrument; this is the
 * scalpel, and for a seed-data or single-column migration it is usually the only
 * proportionate option.
 *
 * <pre>
 *   db/migration/V20260810101500__add_widget_table.sql        forward
 *   db/migration/undo/U20260810101500__add_widget_table.sql   undo, same name + U
 * </pre>
 *
 * <p><b>Not reversible?</b> The script still exists. A migration that destroys data
 * (a {@code DROP TABLE}, a lossy backfill) gets an undo that restores everything it
 * can — structure, constraints, seed rows — and states in its header, in one line,
 * exactly what cannot come back and that a restore is the only route to it. A
 * missing file says "nobody thought about it"; a two-line file that says "the
 * dropped rows are gone, restore from backup" says somebody did.
 *
 * <p>The {@code undo/} folder is excluded from the packaged resources in
 * {@code pom.xml}: Flyway scans a location recursively, and a {@code U}-file inside
 * {@code classpath:db/migration} would be part of the migration set.
 */
class UndoScriptCoverageTest {

    /**
     * Timestamped versions at or after this need an undo — 2026-08-08 UTC, the day the
     * rule landed across the fleet. Never move it forward: that silently forgives
     * scripts.
     */
    static final long UNDO_REQUIRED_FROM = 20260808000000L;

    /**
     * The frozen sequential band. New migrations are timestamped
     * ({@code V<yyyyMMddHHmmss>}), so this exists to catch the slip: someone reaches
     * for {@code V24} out of habit, and a cutoff expressed purely as a timestamp would
     * wave it through as "older than the rule".
     */
    static final long LEGACY_SEQUENTIAL_THROUGH = 23L;

    /** Below this a version is a sequential number; at or above it, a timestamp. */
    private static final long TIMESTAMP_FLOOR = 1_000_000L;

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
    private static final Path UNDO = MIGRATIONS.resolve("undo");

    /** {@code V<version>__<description>.sql} — the version may carry dots, ours don't. */
    private static final Pattern FORWARD = Pattern.compile("^V([\\d.]+)__(.+)\\.sql$");

    @Test
    @DisplayName("every migration from the cutoff onwards has an undo script")
    void everyNewMigrationHasAnUndo() throws IOException {
        List<String> missing = new ArrayList<>();
        for (Path script : scriptsIn(MIGRATIONS)) {
            String name = script.getFileName().toString();
            Matcher m = FORWARD.matcher(name);
            if (!m.matches() || !requiresUndo(m.group(1))) {
                continue;
            }
            if (!Files.exists(UNDO.resolve(undoNameFor(name)))) {
                missing.add("  " + name + "  ->  needs db/migration/undo/" + undoNameFor(name));
            }
        }

        assertThat(missing)
                .withFailMessage("""
                        %d migration(s) with no undo script.

                        Write db/migration/undo/U<same name as the forward script>.sql, reversing
                        the forward script statement for statement, in the opposite order.

                        If the change cannot be fully undone, the file is still required: restore
                        what you can and say in the header, in one line, what is gone for good.

                        %s""", missing.size(), String.join("\n", missing))
                .isEmpty();
    }

    @Test
    @DisplayName("no undo script is orphaned, misnamed or empty")
    void undoScriptsMatchRealMigrations() throws IOException {
        List<String> problems = new ArrayList<>();

        for (Path undo : scriptsIn(UNDO)) {
            String name = undo.getFileName().toString();
            if (!name.startsWith("U")) {
                // A V-file here is the dangerous one: it reads as a migration, and only
                // the pom's exclude is stopping Flyway from applying it as one.
                problems.add("  " + name + " - undo scripts are named U<version>__<description>.sql");
                continue;
            }
            if (!Files.exists(MIGRATIONS.resolve(forwardNameFor(name)))) {
                problems.add("  " + name + " - no forward script " + forwardNameFor(name));
            }
            if (Files.readString(undo, StandardCharsets.UTF_8).isBlank()) {
                problems.add("  " + name + " - empty. Even an irreversible change gets a header"
                        + " saying so");
            }
        }

        assertThat(problems)
                .withFailMessage("%d problem(s) in the undo scripts:%n%n%s",
                        problems.size(), String.join("\n", problems))
                .isEmpty();
    }

    /* ---------- helpers ---------- */

    private static boolean requiresUndo(String version) {
        long n;
        try {
            n = Long.parseLong(version.replace(".", ""));
        } catch (NumberFormatException e) {
            return false;   // unparseable version - not ours to judge
        }
        return n < TIMESTAMP_FLOOR ? n > LEGACY_SEQUENTIAL_THROUGH : n >= UNDO_REQUIRED_FROM;
    }

    private static String undoNameFor(String forwardName) {
        return "U" + forwardName.substring(1);
    }

    private static String forwardNameFor(String undoName) {
        return "V" + undoName.substring(1);
    }

    /** Empty for a folder that does not exist yet. */
    private static List<Path> scriptsIn(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".sql")).sorted().toList();
        }
    }
}

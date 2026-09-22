-- UNDO for V20260922090000. Run by hand from the repo, never by the app on boot
-- (see platform/docs/migrations.md).
--
-- Removes DEMO's own CDC capture row, putting the module back on the platform's
-- central registry over Channel 1. Safe while CSNX-14757 Phase 3 has not run: the
-- identical row still exists in platform.int_cdc_capture and the remote fetch is
-- still wired. AFTER Phase 3 deletes it there, running this undo leaves DEMO with no
-- capture config at all and its trader capture stops — quietly, because an empty
-- config set is a legitimate answer.
--
-- Deliberately does NOT touch demo.int_cdc_state. That is the backfill ledger, and
-- its 'demo-trader' marker means "the existing trader rows have already been emitted
-- once". Dropping the marker with the config would make the next capture tick
-- re-emit the entire trader table as upserts — harmless to a convergent mirror, but
-- a large pointless republish. Clear it deliberately if a re-sync is what you want.

DELETE FROM demo.int_cdc_capture WHERE config_name = 'demo-trader';

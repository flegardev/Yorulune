# Yorulune fresh package migration

The proposed Play package is `com.tiniflegar.yorulune`. The source previously built as `com.aistudio.orbitproductivity.vxqpmr`; its `com.example` Kotlin/resource namespace remains internal to the source and does not define the Play application ID. App-private Room and DataStore identifiers are preserved.

## Moving personal records

Android treats the two application IDs as separate apps. Yorulune cannot read the old installation's private database or preferences directly. Before removing or replacing the old installation, open **Backup & Restore**, export a full JSON backup through the document picker, and keep the file somewhere you control. Install the new Yorulune package, return to **Backup & Restore**, select the JSON file, review the schema/record preview, choose the intended duplicate strategy, and confirm the import. Keep the export until your tasks, projects, reminders and preferences look correct. CSV and ICS are exchange formats, not full workspace restores.

The Room schema, DataStore keys, JSON backup schema, import validation and transactional restore remain unchanged. The backup decoder does not require the Android application ID to match. Manual export is the user-controlled bridge; scheduled alarms, notification permission and launcher widget instances are installation-scoped and should be reviewed/reconfigured after installing the new package. Reminder and widget action identifiers now use the new application identity and remain wired consistently between the manifest and app code.

## Release gate

Play Console reported this package name available on 13 November 2026. Console app creation remains pending the owner review of the required policy and US export attestations. The live privacy URL needs owner review and publication of the matching package statement before a release. No claim is made that package creation, signing, closed testing or publication is complete.

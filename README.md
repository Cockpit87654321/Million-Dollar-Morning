# Million-Dollar Morning

An Android alarm app for Mel Robbins' morning routine (podcast episode 40), extended with water, protein breakfast, no caffeine for two hours and daylight.

When the alarm rings, the routine opens full screen, also over the lock screen, and guides through nine steps with visuals, timers and voice:

1. No snooze: 5-4-3-2-1, get up
2. Make your bed
3. Say your affirmation, high-five the mirror
4. Loaded water, protein breakfast, no caffeine for 2 hours
5. Move your body (10 minutes, adjustable)
6. Move your mind (1 minute)
7. Name one thing that matters
8. 5 minutes of real progress
9. Get into the sunlight

## Build

GitHub Actions builds the APK on every push (see the Actions tab, artifact `million-dollar-morning-apk`).
Locally: `gradle assembleDebug` with Gradle 8.7, JDK 17 and the Android SDK 34.

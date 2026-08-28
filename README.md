# TrackInn

TrackInn is an offline-first Android app that combines a task tracker, calorie counter, and meditation timer into a single interface. No accounts, no cloud sync — everything stays on your device.

## What it does

TrackInn is built around three independent modules that can be enabled or disabled individually. The app is fully bilingual (English and Russian) and supports light, dark, and system themes.

### Todo

A simple task list with manual ordering. Tasks can be reordered via drag-and-drop. Each task optionally has a deadline (date + time), and the text color gradually shifts from black to yellow to red as the deadline approaches. Tasks can be added to Google Calendar with one tap.

### Calorie tracker

Track meals across four categories: breakfast, lunch, snack, dinner. The app maintains a local product database with per-100g nutritional values (calories, protein, fat, carbs) and supports composite dishes — recipes made of multiple ingredients with automatic calorie calculation. The daily progress bar at the bottom shows how close you are to your calorie goal, with configurable colors for normal, approaching, and exceeding states.

### Meditation timer

A configurable meditation timer with a circular progress ring and checkpoint markers. Timers support a preparation countdown, multiple checkpoints with separate sounds, and five built-in sounds (plink, bell, chime, gong, drop). Sessions are recorded to a local history with completion status and can be filtered by date range. The history screen includes a dashboard with key metrics (total sessions, time, streak, completion rate), a weekly bar chart, and a GitHub-style heatmap calendar showing meditation intensity.

### Settings and customization

The app provides a palette of 20 Material Design colors that can be assigned to various UI elements — task deadlines, calorie progress, meditation rings. Modules can be toggled on or off. All data can be exported to and imported from JSON.

## Tech stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room |
| Preferences | DataStore |
| Navigation | Navigation Compose |
| Drag-and-drop | sh.calvin.reorderable |
| Charts | Vico |
| Annotation processing | KSP |

## License

This project is licensed under the [WTFPL](https://www.wtfpl.net/).

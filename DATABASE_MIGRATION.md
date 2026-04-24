# Database Migration: YouTube Video Gallery Feature

## Required Schema Change

To support the YouTube video gallery feature, add the following column to the `exercice` table:

```sql
ALTER TABLE exercice ADD COLUMN video_url VARCHAR(500) DEFAULT NULL;
```

### Details:
- **Column Name:** `video_url`
- **Data Type:** `VARCHAR(500)` (allows full YouTube URL)
- **Nullable:** YES (optional - exercises without videos will have NULL)
- **Default:** NULL

## SQL Migration Script

Execute in your MySQL database:

```sql
USE chroniccare;

ALTER TABLE exercice 
ADD COLUMN IF NOT EXISTS video_url VARCHAR(500) DEFAULT NULL;
```

## Verification

After migration, verify the column exists:

```sql
DESCRIBE exercice;
```

You should see `video_url` in the columns list.

## Features Enabled

This database change enables:
1. ✅ Coaches to add YouTube video URLs when creating/editing exercises
2. ✅ Patients to watch video tutorials directly in the event details screen
3. ✅ Validation of YouTube URLs (youtube.com or youtu.be)
4. ✅ Embedded iframe display in WebView

## Code Changes Already Applied

- **Exercise.java** - Added `videoUrl` field and getters/setters
- **ExerciseService.java** - Updated insert(), update(), and mapExercise() to handle video_url
- **EditExerciseController.java** - Added video URL field and validation
- **AddExerciseController.java** - Added video URL field and validation
- **edit-exercise.fxml** - Added video URL input field with test button
- **add-exercise.fxml** - Added video URL input field with test button
- **PatientEventDetailsController.java** - Added video display logic and YouTube embed conversion
- **patient-event-details.fxml** - Added video panel that appears when exercise is selected





# 🎵 Java Swing Music Player

A feature-rich, lightweight desktop music player built entirely with **Java Swing** and the standard **Java Sound API**. This project demonstrates advanced GUI manipulation, audio signal processing, and event handling without relying on external third-party libraries.

## ✨ Key Features

*   **Playlist Management:** Add individual songs or import entire folders. The playlist is visually managed via a list, allowing double-click playback.
*   **Smart Playback Controls:** Standard Play, Pause, Stop, Previous, and Next functionality.
*   **Playback Modes:**
    *   🔀 **Shuffle:** Randomizes the playlist order.
    *   🔁 **Repeat:** Loops the currently playing track.
*   **Interactive Seek Bar:** A dynamic slider lets you visually track song progress and drag the handle to jump to specific timestamps.
*   **Volume Control:** Real-time volume adjustment that converts linear slider input into logarithmic Decibels for natural sound fading.
*   **Auto-Advance:** Automatically plays the next song in the queue when the current track finishes.

---

## 🛠️ Technical Implementation

### Playlist Management
*   **Storage:** Songs are stored in an `ArrayList<File>`.
*   **UI Integration:** The visual list is handled by a `JList` component backed by a `DefaultListModel`.
*   **Interaction:** A MouseListener detects double-clicks on list items to immediately trigger the `playMusic()` method for that specific index.

### Audio Engine (`javax.sound.sampled`)
*   **Clip Class:** The core class used to play audio. It loads the entire audio file into memory (RAM) prior to playback, ensuring low latency.
*   **LineListener:** A listener is attached to the Clip to detect `STOP` events. This logic distinguishes between a user clicking "Stop" and a song finishing naturally, triggering the `playNext()` logic automatically in the latter case.

### Seek Bar & Progress Logic
*   **Implementation:** Replaced the standard static `JProgressBar` with an interactive `JSlider`.
*   **Timer:** A Swing `Timer` fires every 100ms. It calculates `(CurrentPosition / TotalLength) * 100` to update the visual slider.
*   **Dragging Logic:** When the user clicks/drags the slider handle, the auto-update timer is paused. On release, the audio clip's `microsecondPosition` is updated to match the slider.

### Logic Controls (Pause & Volume)
*   **Pause/Resume:** The `Clip` class lacks a native "Pause" method. We simulate this by calling `stop()`, saving the current `microsecondPosition` to a variable, and passing that position back to the clip before calling `start()` again.
*   **Volume:** Uses `FloatControl.Type.MASTER_GAIN`. The code converts the linear slider value (0-100) into Decibels to ensure the volume controls feel smooth and accurate to the human ear.

### Album Art
*   A placeholder `JLabel` is used for Album Art.
*   *Note:* Extracting real ID3 tags (images) from audio files requires complex byte parsing or external libraries (like `mp3agic`), which were omitted to keep this project contained in a single file.

---

## 🚀 How to Run This

### Prerequisites
*   **Java Development Kit (JDK)** installed (Version 8 or higher).

### Steps
1.  Save the source code as `SwingMusicPlayer.java`.
2.  Open your command prompt or terminal in the folder containing the file.
3.  **Compile the code:**
    ```bash
    javac SwingMusicPlayer.java
    ```
4.  **Run the application:**
    ```bash
    java SwingMusicPlayer
    ```

### ⚠️ Important Note on File Support
This player uses the standard Java Sound API.
*   **Supported Files:** `.WAV`, `.AIFF`, `.AU`.
*   **MP3 Files:** Standard Java **does not** support MP3s natively. If you try to load an MP3, it will throw an `UnsupportedAudioFileException`. To play MP3s, you would need to add a library like **JLayer/MP3SPI** to your classpath.
*   **Recommendation:** Please test this application using **.WAV** files.

---


import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class SwingMusicPlayer extends JFrame {

    // Logic Variables
    private ArrayList<File> playlist;
    private int currentSongIndex = 0;
    private Clip clip;
    private long clipTimePosition = 0;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean isShuffle = false;
    private boolean isRepeat = false;
    private boolean isDraggingTime = false;

    // UI Components
    private JList<String> songListGUI;
    private DefaultListModel<String> listModel;
    private JSlider progressSlider;
    private JLabel timeLabel;
    private JSlider volumeSlider;
    private JLabel songTitleLabel;
    private JLabel albumArtLabel;
    private JButton playButton, pauseButton, stopButton, btnShuffle, btnRepeat;
    private Timer progressTimer;

    public SwingMusicPlayer() {
        playlist = new ArrayList<>();
        
        // --- UI SETUP ---
        setTitle("Java Swing Music Player");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);

        // --- NEW: MENU BAR ---
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        
        JMenuItem addSongsItem = new JMenuItem("Add Songs...");
        addSongsItem.addActionListener(e -> addFiles());
        
        JMenuItem addFolderItem = new JMenuItem("Add Folder...");
        addFolderItem.addActionListener(e -> addFolder());
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));

        fileMenu.add(addSongsItem);
        fileMenu.add(addFolderItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // 1. Top Panel (Artwork and Title)
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.DARK_GRAY);
        
        albumArtLabel = new JLabel("Music Player", SwingConstants.CENTER);
        albumArtLabel.setPreferredSize(new Dimension(120, 120));
        albumArtLabel.setOpaque(true);
        albumArtLabel.setBackground(Color.GRAY);
        albumArtLabel.setForeground(Color.WHITE);
        albumArtLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        songTitleLabel = new JLabel("Use File > Add Songs to start", SwingConstants.CENTER);
        songTitleLabel.setForeground(Color.WHITE);
        songTitleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        songTitleLabel.setBorder(new EmptyBorder(10, 0, 10, 0));

        topPanel.add(albumArtLabel, BorderLayout.WEST);
        topPanel.add(songTitleLabel, BorderLayout.CENTER);

        // 2. Center Panel (Playlist)
        listModel = new DefaultListModel<>();
        songListGUI = new JList<>(listModel);
        songListGUI.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        songListGUI.setFont(new Font("Arial", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(songListGUI);
        
        // Double click to play
        songListGUI.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = songListGUI.locationToIndex(evt.getPoint());
                    if (index >= 0) {
                        stopMusic();
                        currentSongIndex = index;
                        playMusic(playlist.get(currentSongIndex));
                    }
                }
            }
        });

        // 3. Bottom Panel (Controls, Volume, Time)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Time Control
        JPanel timePanel = new JPanel(new BorderLayout());
        timeLabel = new JLabel("0:00 / 0:00");
        timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        progressSlider = new JSlider(0, 100, 0);
        progressSlider.setEnabled(false);
        progressSlider.setBackground(null);
        progressSlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) { isDraggingTime = true; }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (clip != null && clip.isOpen()) {
                    int value = progressSlider.getValue();
                    long newMicroSeconds = (long) (clip.getMicrosecondLength() * (value / 100.0));
                    clip.setMicrosecondPosition(newMicroSeconds);
                }
                isDraggingTime = false;
            }
        });

        timePanel.add(timeLabel, BorderLayout.NORTH);
        timePanel.add(progressSlider, BorderLayout.CENTER);

        // Buttons & Volume
        JPanel controlsAndVolumePanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        // Add Button (Shortcut to Add Files)
        JButton btnAddShortcut = new JButton("Add Songs");
        btnAddShortcut.addActionListener(e -> addFiles());

        JButton btnPrev = new JButton("<<");
        playButton = new JButton("Play");
        pauseButton = new JButton("Pause");
        stopButton = new JButton("Stop");
        JButton btnNext = new JButton(">>");
        btnShuffle = new JButton("S: Off");
        btnRepeat = new JButton("R: Off");

        pauseButton.setEnabled(false);
        stopButton.setEnabled(false);

        // Listeners
        playButton.addActionListener(e -> {
            if (playlist.isEmpty()) {
                addFiles(); // If list empty, prompt to add
                return;
            }
            if (isPaused) resumeMusic();
            else playMusic(playlist.get(currentSongIndex));
        });
        pauseButton.addActionListener(e -> pauseMusic());
        stopButton.addActionListener(e -> stopMusic());
        btnNext.addActionListener(e -> playNext());
        btnPrev.addActionListener(e -> playPrevious());
        
        btnShuffle.addActionListener(e -> {
            isShuffle = !isShuffle;
            btnShuffle.setText(isShuffle ? "S: ON" : "S: Off");
        });
        
        btnRepeat.addActionListener(e -> {
            isRepeat = !isRepeat;
            btnRepeat.setText(isRepeat ? "R: ON" : "R: Off");
        });

        buttonPanel.add(btnAddShortcut);
        buttonPanel.add(btnShuffle);
        buttonPanel.add(btnPrev);
        buttonPanel.add(playButton);
        buttonPanel.add(pauseButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(btnNext);
        buttonPanel.add(btnRepeat);

        // Volume
        JPanel volumePanel = new JPanel(new BorderLayout());
        JLabel volLabel = new JLabel("Vol");
        volumeSlider = new JSlider(0, 100, 70);
        volumeSlider.setPreferredSize(new Dimension(80, 20));
        volumeSlider.addChangeListener(e -> setVolume(volumeSlider.getValue()));

        volumePanel.add(volLabel, BorderLayout.WEST);
        volumePanel.add(volumeSlider, BorderLayout.CENTER);

        controlsAndVolumePanel.add(buttonPanel, BorderLayout.CENTER);
        controlsAndVolumePanel.add(volumePanel, BorderLayout.EAST);

        bottomPanel.add(timePanel, BorderLayout.NORTH);
        bottomPanel.add(controlsAndVolumePanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        progressTimer = new Timer(100, e -> updateProgress());
    }

    // --- ADD MUSIC FEATURES ---

    // 1. Add specific files
    private void addFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Select Audio Files");
        // Standard Java Sound supports WAV, AIFF, AU. 
        // Note: Standard Java DOES NOT support MP3 without external libraries (like JLayer).
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Audio (WAV, AU, AIFF)", "wav", "au", "aiff");
        chooser.setFileFilter(filter);

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = chooser.getSelectedFiles();
            for (File file : files) {
                playlist.add(file);
                listModel.addElement(file.getName());
            }
            if (!playlist.isEmpty() && !isPlaying) {
                songTitleLabel.setText("Ready to Play: " + playlist.get(0).getName());
            }
        }
    }

    // 2. Add entire folder
    private void addFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Folder containing Music");

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            File[] files = folder.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".wav") || lower.endsWith(".au") || lower.endsWith(".aiff");
            });

            if (files != null && files.length > 0) {
                for (File file : files) {
                    playlist.add(file);
                    listModel.addElement(file.getName());
                }
                JOptionPane.showMessageDialog(this, "Added " + files.length + " songs from folder.");
            } else {
                JOptionPane.showMessageDialog(this, "No supported audio files (wav, au, aiff) found in that folder.");
            }
        }
    }

    // --- MUSIC LOGIC ---

    private void playMusic(File file) {
        try {
            if (clip != null && clip.isOpen()) {
                clip.close();
            }

            AudioInputStream audioInput = AudioSystem.getAudioInputStream(file);
            clip = AudioSystem.getClip();
            clip.open(audioInput);
            
            setVolume(volumeSlider.getValue());
            
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    if (clip.getMicrosecondPosition() >= clip.getMicrosecondLength()) {
                        playNext();
                    }
                }
            });

            clip.start();
            isPlaying = true;
            isPaused = false;
            
            songTitleLabel.setText(file.getName());
            songListGUI.setSelectedIndex(currentSongIndex);
            songListGUI.ensureIndexIsVisible(currentSongIndex);
            
            playButton.setEnabled(false);
            pauseButton.setEnabled(true);
            stopButton.setEnabled(true);
            progressSlider.setEnabled(true);
            progressTimer.start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error playing file: " + e.getMessage());
        }
    }

    private void setVolume(int value) {
        if (clip != null && clip.isOpen()) {
            try {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float range = gainControl.getMaximum() - gainControl.getMinimum();
                float gain = (range * value / 100.0f) + gainControl.getMinimum();
                gainControl.setValue(gain);
            } catch (Exception ex) {
                System.err.println("Volume control not supported.");
            }
        }
    }

    private void resumeMusic() {
        if (clip != null && isPaused) {
            clip.setMicrosecondPosition(clipTimePosition);
            clip.start();
            isPlaying = true;
            isPaused = false;
            playButton.setEnabled(false);
            pauseButton.setEnabled(true);
        }
    }

    private void pauseMusic() {
        if (clip != null && isPlaying) {
            clipTimePosition = clip.getMicrosecondPosition();
            clip.stop();
            isPlaying = false;
            isPaused = true;
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
        }
    }

    private void stopMusic() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clipTimePosition = 0;
            isPlaying = false;
            isPaused = false;
            playButton.setEnabled(true);
            pauseButton.setEnabled(false);
            stopButton.setEnabled(false);
            progressSlider.setValue(0);
            timeLabel.setText("0:00 / 0:00");
            progressTimer.stop();
        }
    }

    private void playNext() {
        if (playlist.isEmpty()) return;
        if (!isRepeat) {
            if (isShuffle) currentSongIndex = new Random().nextInt(playlist.size());
            else currentSongIndex = (currentSongIndex + 1) % playlist.size();
        }
        stopMusic();
        playMusic(playlist.get(currentSongIndex));
    }

    private void playPrevious() {
        if (playlist.isEmpty()) return;
        if (currentSongIndex > 0) currentSongIndex--;
        else currentSongIndex = playlist.size() - 1;
        stopMusic();
        playMusic(playlist.get(currentSongIndex));
    }

    private void updateProgress() {
        if (clip != null && (isPlaying || isPaused) && !isDraggingTime) {
            long currentMicro = clip.getMicrosecondPosition();
            long totalMicro = clip.getMicrosecondLength();
            progressSlider.setValue((int) ((double) currentMicro / totalMicro * 100));
            String currentStr = formatTime(currentMicro / 1000000);
            String totalStr = formatTime(totalMicro / 1000000);
            timeLabel.setText(currentStr + " / " + totalStr);
        }
    }
    
    private String formatTime(long seconds) {
        long min = seconds / 60;
        long sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new SwingMusicPlayer().setVisible(true));
    }
}
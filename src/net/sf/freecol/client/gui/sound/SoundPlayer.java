/**
 *  Copyright (C) 2002-2007  The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */


package net.sf.freecol.client.gui.sound;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import net.sf.freecol.common.option.PercentageOption;


/**
* Class for playing sound. See the package description for {@link net.sf.freecol.client.gui.sound} for information on how to play sfx/music.
*/
public class SoundPlayer {

    private static final Logger logger = Logger.getLogger(SoundPlayer.class.getName());

    private static final int MAXIMUM_FADE_MS = 7000;
    private static final int FADE_UPDATE_MS = 5;
    
    /** The thread-group containing all of the <i>SoundPlayerThreads</i>. */
    private ThreadGroup soundPlayerThreads = new ThreadGroup("soundPlayerThreads");

    /** Is the sound paused? */
    private boolean soundPaused = false;

    /** Is the sound stopped? */
    private boolean soundStopped = true;

    /**
    * Should the <i>SoundPlayer</i> play multiple sounds at the same time, or only one?
    * If it does not allow multiple sounds, then using <i>play</i> will stop the sound
    * currently playing and play the new instead.
    */
    private boolean multipleSounds;
    
    /**
     * Used with <code>multipleSounds</code>.
     */
    private SoundPlayerThread currentSoundPlayerThread;

    /** Should the player continue playing after it it finished with a sound-clip? This is the default used with the <i>play(Playlist playlist)</i>. */
    private boolean defaultPlayContinues;

    /**
    * This is the default repeat-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
    *
    * @see Playlist
    */
    private final int defaultRepeatMode;

    /**
    * This is the default pick-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
    *
    * @see Playlist
    */
    private final int defaultPickMode;

    private Mixer mixer;
    
    private PercentageOption volume;

    
    /**
    * Use this constructor.
    *
    * @param multipleSounds Should the <i>SoundPlayer</i> play multiple sounds at the same time,
    *                       or only one? If it does not allow multiple sounds, then using <i>play</i> will
    *                       stop the sound currently playing and play the new instead.
    *
    * @param defaultPlayContinues Should the player continue playing after it it finished with a sound-clip? This is the default used with the <i>play(Playlist playlist)</i>.
    *
    */
    public SoundPlayer(PercentageOption volume, boolean multipleSounds, boolean defaultPlayContinues) {
        this(volume, multipleSounds, defaultPlayContinues, Playlist.REPEAT_ALL, Playlist.FORWARDS);
    }



    /**
    * Or this.
    *
    * @param multipleSounds Should the <i>SoundPlayer</i> play multiple sounds at the same time,
    *                       or only one? If it does not allow multiple sounds, then using <i>play</i> will
    *                       stop the sound currently playing and play the new instead.
    *
    * @param volume The volume to be used when playing audio.
    * @param defaultRepeatMode This is the default repeat-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
    * @param defaultPickMode This is the default pick-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
    * @param defaultPlayContinues Should the player continue playing after it it finished with a sound-clip? This is the default used with the <i>play(Playlist playlist)</i>.
    *
    */
    public SoundPlayer(PercentageOption volume, boolean multipleSounds, boolean defaultPlayContinues, int defaultRepeatMode, int defaultPickMode) {
        this.volume = volume;
        this.multipleSounds = multipleSounds;
        this.defaultPlayContinues = defaultPlayContinues;
        this.defaultRepeatMode = defaultRepeatMode;
        this.defaultPickMode = defaultPickMode;
        
        mixer = AudioSystem.getMixer(null);
    }



    /**
    * Plays a playlist using the default play-continues, repeat-mode and pick-mode for this <i>SoundPlayer</i>.
    * @param playlist The <code>Playlist</code> to be played.
    */
    public void play(Playlist playlist) {
        play(playlist, defaultPlayContinues, defaultRepeatMode, defaultPickMode);
    }



    /**
    * Plays a playlist.
    * @param playlist The <code>Playlist</code> to be played.
    * @param playContinues <code>true</code> if the
    *       <code>SoundPlayer</code> should continue playing
    *       after playing the first entry on the playlist.
    * @param repeatMode The method this <code>PlayList</code>
    *      should be repeated.
    * @param pickMode The method to be used for picking
    *      the songs.
    */
    public void play(Playlist playlist, boolean playContinues, int repeatMode, int pickMode) {
        if (playlist != null) {
            currentSoundPlayerThread = new SoundPlayerThread(playlist, playContinues, repeatMode, pickMode);
            currentSoundPlayerThread.start();
        } else {
            currentSoundPlayerThread = null;
        }
    }



    /**
    * Stop playing the sounds.
    */
    public void stop() {
        soundStopped = true;
        soundPaused = false;
    }



    /**
    * Are the sounds stopped?
    * @return <code>true</code> is the sounds are stopped.
    */
    public boolean isStopped() {
        return soundStopped;
    }



    /**
    * Pauses all the sounds.
    */
    public void pause() {
        soundPaused = true;
    }



    /**
    * Are the sounds paused?
    * @return <code>true</code> is the sounds are paused.
    */
    public boolean isPaused() {
        return soundPaused;
    }




    /** Thread for playing a <i>Playlist</i>. */
    class SoundPlayerThread extends Thread {

        /** An array containing the currently selected playlist. The numbers in the array is used as an index in the <i>soundFiles</i>-array. */
        private Playlist playlist;

        /** Should the <i>SoundPlayer</i> continue to play when it is finished with a sound-clip? */
        private boolean playContinues;

        /**
        * This is the default repeat-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
        *
        * @see Playlist
        */
        private int repeatMode;

        /**
        * This is the default pick-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
        *
        * @see Playlist
        */
        private int pickMode;

        /** Should the sound be played again when it is finished? */
        @SuppressWarnings("unused")
        private boolean repeatSound;


        /**
        * The constructor to use.
        *
        * @param playlist A <i>Playlist</i> containing sound-files.
        * @param playContinues Should the player continue playing after it it finished with a sound-clip?
        * @param repeatMode This is the default repeat-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
        * @param pickMode This is the default pick-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
        */
        public SoundPlayerThread(Playlist playlist, boolean playContinues, int repeatMode, int pickMode) {
            super(soundPlayerThreads, "soundPlayerThread");

            this.playlist = playlist;
            this.playContinues = playContinues;
            this.repeatMode = repeatMode;
            this.pickMode = pickMode;
        }


        private boolean shouldStopThread() {
            return !multipleSounds && currentSoundPlayerThread != this; 
        }

        /**
        * This thread loads and plays the sound.
        */
        public void run() {
            playlist.setRepeatMode(repeatMode);
            playlist.setPickMode(pickMode);

            soundPaused = false;
            soundStopped = false;

            do {
                playSound(playlist.next());

                // Take a little break between sounds
                try { Thread.sleep(222); } catch (Exception e) {break;}
            } while (playContinues && playlist.hasNext()
                    && !soundStopped && !shouldStopThread());
        }

        public void playSound(File file) {
            try {
                AudioInputStream in= AudioSystem.getAudioInputStream(file);
                AudioInputStream din = null;
                if (in != null) {
                    AudioFormat baseFormat = in.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.getSampleRate(),
                            16,
                            baseFormat.getChannels(),
                            baseFormat.getChannels() * (16 / 8),
                            baseFormat.getSampleRate(),
                            baseFormat.isBigEndian());
                    din = AudioSystem.getAudioInputStream(decodedFormat, in);
                    rawplay(decodedFormat, din);
                    in.close();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not play audio.", e);
            }
        }

        private void updateVolume(FloatControl c, int volume) {
            final float minGain = c.getMinimum();
            final float gain = (volume / 100f) * (0 - minGain) + minGain;
            c.setValue(gain);
        }
        
        private void rawplay(AudioFormat targetFormat,  AudioInputStream din) throws IOException, LineUnavailableException {
            byte[] data = new byte[8192];
            SourceDataLine line = getLine(targetFormat);
            if (line != null) {
                line.start();
                
                // Volume control:
                final FloatControl c = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                final PropertyChangeListener pcl = new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent e) {
                        int v = ((Integer) e.getNewValue()).intValue();
                        updateVolume(c, v);
                    }
                };
                volume.addPropertyChangeListener(pcl);
                updateVolume(c, volume.getValue());

                // Playing audio:
                int read = 0;
                int written = 0;
                try {
                    while (read != -1 && !soundStopped && !shouldStopThread()) {
                        try {
                            while (soundPaused) {
                                Thread.sleep(10);
                            }
                        } catch (InterruptedException e) {}
                        read = din.read(data, 0, data.length);
                        if (read != -1) {
                            written = line.write(data, 0, read);
                        }
                    }
                } finally {
                    volume.removePropertyChangeListener(pcl);
                }
                
                // Implements fading down:
                if (!soundStopped) {
                    long ms = System.currentTimeMillis() + FADE_UPDATE_MS;
                    long fadeStop = System.currentTimeMillis() + MAXIMUM_FADE_MS;
                    while (read != -1
                            && !soundStopped 
                            && System.currentTimeMillis() < fadeStop) {
                        read = din.read(data, 0, data.length);
                        if (read != -1) {
                            written = line.write(data, 0, read);
                        }
                        if (System.currentTimeMillis() > ms) {
                            c.setValue(c.getValue() - 1f);
                            ms = System.currentTimeMillis() + FADE_UPDATE_MS;
                        }
                    }
                }
                
                line.drain();
                line.stop();
                line.close();
                din.close();
            }             
        }

        private SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
            SourceDataLine sdl = null;
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            sdl = (SourceDataLine) mixer.getLine(info);
            sdl.open(audioFormat);
            return sdl;
        }
    }
}

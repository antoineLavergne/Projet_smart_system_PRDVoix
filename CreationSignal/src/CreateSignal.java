import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class CreateSignal {
    public static final int SAMPLE_RATE = 8000;

    private static final double MAX_16_BIT = 32768;

    private static final int MONO   = 1;
    private static final boolean LITTLE_ENDIAN = false;
    private static final boolean SIGNED        = true;




    /**
     * Sauvegarde la liste de double en tant que fichier .wav
     *
     * @param  filename le nom du fichier
     * @param  samples la liste de double
     * @throws IllegalArgumentException si on peut pas sauvegarder {@code filename}
     * @throws IllegalArgumentException si {@code samples} est {@code null}
     * @throws IllegalArgumentException si {@code filename} est {@code null}
     * @throws IllegalArgumentException si {@code filename} problème d'extension {@code .wav}
     */
    public static void save(String filename, double[] samples) {
        if (filename == null) {
            throw new IllegalArgumentException("filenameis null");
        }
        if (samples == null) {
            throw new IllegalArgumentException("samples[] is null");
        }

        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, MONO, SIGNED, LITTLE_ENDIAN);
        byte[] data = new byte[2 * samples.length];
        for (int i = 0; i < samples.length; i++) {
            int temp = (short) (samples[i] * MAX_16_BIT);
            if (samples[i] == 1.0) temp = Short.MAX_VALUE;
            data[2*i + 0] = (byte) temp;
            data[2*i + 1] = (byte) (temp >> 8);
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            AudioInputStream ais = new AudioInputStream(bais, format, samples.length);
            if (filename.endsWith(".wav") || filename.endsWith(".WAV")) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File("records"+File.separator+filename));
            }
            else {
                throw new IllegalArgumentException("file type for saving must be .wav or .au");
            }
        }
        catch (IOException ioe) {
            throw new IllegalArgumentException("unable to save file '" + filename + "'", ioe);
        }
    }


    /**
     * Crée une liste de double représentant un signal avec :
     * @param hz une fréquence
     * @param duration une durée
     * @param amplitude une amplitude
     * @return liste de double représentant le signal
     */
    public static double[] note(double hz, double duration, double amplitude) {
        int n = (int) (8000 * duration);
        double[] a = new double[n+1];
        for (int i = 0; i <= n; i++)
            a[i] = amplitude * Math.sin(2 * Math.PI * i * hz / SAMPLE_RATE);
        return a;
    }

    /**
     * Test de génération d'un son
     *
     */
    public static void main(String[] args) {

        double freq = 400;
        //Création du signal
        double a[] = note(freq,5,0.2);
        //Enregistrement du signal
        save(freq+".wav",a);
    }
}

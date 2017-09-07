package cn.yiiguxing.plugin.translate;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Text to speech util.
 */
public final class Speech {

    private static final String TTS_URL = "http://dict.youdao.com/dictvoice?audio=%s&type=%d";

    private static final Logger LOG = Logger.getInstance("#" + Speech.class.getCanonicalName());

    private Speech() {
    }

    public enum Phonetic {
        /**
         * 英式发音
         */
        UK(1),
        /**
         * 美式发音
         */
        US(2);

        final int value;

        Phonetic(int value) {
            this.value = value;
        }
    }

    /**
     * 转换为语音
     *
     * @param text     目标文本
     * @param phonetic 音标
     */
    public static void toSpeech(@NotNull final String text, @NotNull final Phonetic phonetic) {
        ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
                play(text, phonetic);
            }
        });
    }

    private static void play(@NotNull String text, @NotNull Phonetic phonetic) {
        try {
            URL url = new URL(String.format(TTS_URL, text, phonetic.value));
            BufferedInputStream inputStream = new BufferedInputStream(url.openStream());
            AudioInputStream in = new MpegAudioFileReader().getAudioInputStream(inputStream);
            AudioFormat baseFormat = in.getFormat();
            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false);
            AudioInputStream din = new MpegFormatConversionProvider().getAudioInputStream(decodedFormat, in);

            // Play now.
            rawPlay(decodedFormat, din);

            in.close();
        } catch (Exception e) {
            LOG.error("toSpeech", e);
        }
    }

    private static void rawPlay(AudioFormat targetFormat, AudioInputStream din)
            throws IOException, LineUnavailableException {
        SourceDataLine line = getLine(targetFormat);
        if (line != null) {
            // Start
            line.start();

            byte[] data = new byte[4096];
            int bytesRead;
            while ((bytesRead = din.read(data, 0, data.length)) != -1) {
                line.write(data, 0, bytesRead);
            }

            // Stop
            line.drain();
            line.stop();
            line.close();
            din.close();
        }
    }

    private static SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);

        return res;
    }

}

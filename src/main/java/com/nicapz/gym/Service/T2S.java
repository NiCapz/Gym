package com.nicapz.gym.Service;

import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.vosk.LogLevel;
import org.vosk.Recognizer;
import org.vosk.LibVosk;
import org.vosk.Model;

public class T2S {

    public static void main(String[] args) throws IOException, UnsupportedAudioFileException {
        try (Model model = new Model("Model")) {
            InputStream ais = AudioSystem.getAudioInputStream(new BufferedInputStream(new FileInputStream("C:/Users/Niko/OneDrive - HAW-HH/Desktop/untitled.wav")));
            Recognizer recognizer = new Recognizer(model, 44100);

            int nbytes;
            byte[] b = new byte[4096];
            while ((nbytes = ais.read(b)) >= 0) {
                if (recognizer.acceptWaveForm(b, nbytes)) {
                    System.out.println(recognizer.getResult());
                } else {
                    System.out.println(recognizer.getPartialResult());
                }
            }
            System.out.println("Final Result: " + recognizer.getFinalResult());
        }
    }
}

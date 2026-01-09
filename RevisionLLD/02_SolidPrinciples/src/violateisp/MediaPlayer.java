package violateisp;

interface MediaPlayer {
    void playAudio(String audioFile);
    void stopAudio();
    void adjustAudioVolume(int volume);

    void playVideo(String videoFile);
    void stopVideo();
    void adjustVideoBrightness(int brightness);
    void displaySubtitles(String subtitleFile);
}
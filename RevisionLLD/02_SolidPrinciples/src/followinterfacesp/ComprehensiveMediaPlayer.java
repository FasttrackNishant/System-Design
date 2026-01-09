package followinterfacesp;

class ComprehensiveMediaPlayer implements AudioPlayerControls, VideoPlayerControls {
    @Override
    public void playAudio(String audioFile) {
        System.out.println("ComprehensiveMediaPlayer: Playing audio - " + audioFile);
    }

    @Override
    public void stopAudio() {
        System.out.println("ComprehensiveMediaPlayer: Audio stopped.");
    }

    @Override
    public void adjustAudioVolume(int volume) {
        System.out.println("ComprehensiveMediaPlayer: Audio volume set to " + volume);
    }

    @Override
    public void playVideo(String videoFile) {
        System.out.println("ComprehensiveMediaPlayer: Playing video - " + videoFile);
    }

    @Override
    public void stopVideo() {
        System.out.println("ComprehensiveMediaPlayer: Video stopped.");
    }

    @Override
    public void adjustVideoBrightness(int brightness) {
        System.out.println("ComprehensiveMediaPlayer: Brightness set to " + brightness);
    }

    @Override
    public void displaySubtitles(String subtitleFile) {
        System.out.println("ComprehensiveMediaPlayer: Subtitles from " + subtitleFile);
    }
}
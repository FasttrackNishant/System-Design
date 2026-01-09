package followinterfacesp;

class SilentVideoPlayer implements VideoPlayerControls {
    @Override
    public void playVideo(String videoFile) {
        System.out.println("SilentVideoPlayer: Playing video - " + videoFile);
    }

    @Override
    public void stopVideo() {
        System.out.println("SilentVideoPlayer: Video stopped.");
    }

    @Override
    public void adjustVideoBrightness(int brightness) {
        System.out.println("SilentVideoPlayer: Brightness set to " + brightness);
    }

    @Override
    public void displaySubtitles(String subtitleFile) {
        System.out.println("SilentVideoPlayer: Subtitles from " + subtitleFile);
    }
}
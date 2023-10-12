package net.flamgop.util;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class YoutubeHelper {

    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public static CompletableFuture<String> downloadVideoAsync(String id) {
        System.out.println("Grabbing file for video " + id);
        CompletableFuture<String> future = new CompletableFuture<>();
        executor.submit(() -> {
            YoutubeDownloader downloader = new YoutubeDownloader();
            RequestVideoInfo infoRequest = new RequestVideoInfo(id);
            var response = downloader.getVideoInfo(infoRequest);

            File dotSlash = new File("");
            RequestVideoFileDownload downloadRequest = new RequestVideoFileDownload(response.data().bestAudioFormat())
                    .saveTo(dotSlash.getAbsoluteFile())
                    .renameTo("audio")
                    .overwriteIfExists(false)
                    .callback(new YoutubeCallback<>() {
                        @Override
                        public void onFinished(File data) {
                            future.complete("file:///" + data.getAbsolutePath().replace("\\", "/"));
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            //noinspection CallToPrintStackTrace
                            throwable.printStackTrace(); // cri
                        }
                    })
                    .async();
            downloader.downloadVideoFile(downloadRequest);
        });
        return future;
    }
}

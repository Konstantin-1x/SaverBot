package com.jackpotsaver.bot.service;

import java.nio.file.Path;

public record DownloadedVideo(Path path, long sizeBytes) {
}

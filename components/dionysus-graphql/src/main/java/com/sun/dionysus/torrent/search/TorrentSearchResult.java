package com.sun.dionysus.torrent.search;

/**
 * A single search result from Jackett.
 */
public class TorrentSearchResult {

    private String name;
    private int seeders;
    private int leechers;
    private String size;
    private long sizeBytes;
    private String publishDate;
    private String magnet;

    public TorrentSearchResult() {
    }

    /**
     * @param name        the torrent title.
     * @param seeders     active seeders.
     * @param leechers    active leechers.
     * @param size        human-readable size string.
     * @param sizeBytes   raw size in bytes.
     * @param publishDate ISO-8601 publish date.
     * @param magnet      magnet URI.
     */
    public TorrentSearchResult(String name, int seeders, int leechers, String size,
                               long sizeBytes, String publishDate, String magnet) {
        this.name = name;
        this.seeders = seeders;
        this.leechers = leechers;
        this.size = size;
        this.sizeBytes = sizeBytes;
        this.publishDate = publishDate;
        this.magnet = magnet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSeeders() {
        return seeders;
    }

    public void setSeeders(int seeders) {
        this.seeders = seeders;
    }

    public int getLeechers() {
        return leechers;
    }

    public void setLeechers(int leechers) {
        this.leechers = leechers;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public String getMagnet() {
        return magnet;
    }

    public void setMagnet(String magnet) {
        this.magnet = magnet;
    }
}

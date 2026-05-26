package com.lencode.paper.behavior.vo;

import java.util.Collections;
import java.util.List;

public class BehaviorStatsResponse {

    private Long eventTotal;
    private List<BehaviorEventTypeCountResponse> eventTypeCounts = Collections.emptyList();
    private Long favoriteTotal;
    private Long ratingUserTotal;
    private Double averageRating;
    private List<BehaviorTopPaperResponse> topDetailViewedPapers = Collections.emptyList();
    private List<BehaviorTopPaperResponse> topDownloadClickedPapers = Collections.emptyList();
    private boolean cached;
    private boolean rebuilding;

    public BehaviorStatsResponse() {
    }

    public static BehaviorStatsResponse ready(
            Long eventTotal,
            List<BehaviorEventTypeCountResponse> eventTypeCounts,
            Long favoriteTotal,
            Long ratingUserTotal,
            Double averageRating,
            List<BehaviorTopPaperResponse> topDetailViewedPapers,
            List<BehaviorTopPaperResponse> topDownloadClickedPapers) {
        BehaviorStatsResponse response = new BehaviorStatsResponse();
        response.setEventTotal(eventTotal);
        response.setEventTypeCounts(eventTypeCounts);
        response.setFavoriteTotal(favoriteTotal);
        response.setRatingUserTotal(ratingUserTotal);
        response.setAverageRating(averageRating);
        response.setTopDetailViewedPapers(topDetailViewedPapers);
        response.setTopDownloadClickedPapers(topDownloadClickedPapers);
        response.setCached(false);
        response.setRebuilding(false);
        return response;
    }

    public static BehaviorStatsResponse rebuilding() {
        BehaviorStatsResponse response = ready(
                0L,
                Collections.emptyList(),
                0L,
                0L,
                null,
                Collections.emptyList(),
                Collections.emptyList()
        );
        response.setRebuilding(true);
        return response;
    }

    public Long getEventTotal() {
        return eventTotal;
    }

    public void setEventTotal(Long eventTotal) {
        this.eventTotal = eventTotal == null ? 0L : eventTotal;
    }

    public List<BehaviorEventTypeCountResponse> getEventTypeCounts() {
        return eventTypeCounts;
    }

    public void setEventTypeCounts(List<BehaviorEventTypeCountResponse> eventTypeCounts) {
        this.eventTypeCounts = eventTypeCounts == null ? Collections.emptyList() : eventTypeCounts;
    }

    public Long getFavoriteTotal() {
        return favoriteTotal;
    }

    public void setFavoriteTotal(Long favoriteTotal) {
        this.favoriteTotal = favoriteTotal == null ? 0L : favoriteTotal;
    }

    public Long getRatingUserTotal() {
        return ratingUserTotal;
    }

    public void setRatingUserTotal(Long ratingUserTotal) {
        this.ratingUserTotal = ratingUserTotal == null ? 0L : ratingUserTotal;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public List<BehaviorTopPaperResponse> getTopDetailViewedPapers() {
        return topDetailViewedPapers;
    }

    public void setTopDetailViewedPapers(List<BehaviorTopPaperResponse> topDetailViewedPapers) {
        this.topDetailViewedPapers = topDetailViewedPapers == null ? Collections.emptyList() : topDetailViewedPapers;
    }

    public List<BehaviorTopPaperResponse> getTopDownloadClickedPapers() {
        return topDownloadClickedPapers;
    }

    public void setTopDownloadClickedPapers(List<BehaviorTopPaperResponse> topDownloadClickedPapers) {
        this.topDownloadClickedPapers = topDownloadClickedPapers == null ? Collections.emptyList() : topDownloadClickedPapers;
    }

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public boolean isRebuilding() {
        return rebuilding;
    }

    public void setRebuilding(boolean rebuilding) {
        this.rebuilding = rebuilding;
    }
}

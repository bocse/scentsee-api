package com.bocse.scentsee.utils;

import com.bocse.perfume.data.RecommendedPerfume;

import java.util.Comparator;

/**
 * Created by bocse on 02.04.2016.
 */
public enum RecommendedPerfumeComparators {
    MATCH_DESCENDING((o1, o2) -> -o1.getMatchRate().compareTo(o2.getMatchRate())),

    MATCH_POPULARITY_DESCENDING((o1, o2) -> -new Double(o1.getMatchRate() * o1.getPopularity()).compareTo(o2.getMatchRate() * o2.getPopularity())),

    POPULARITY_DESCENDING((o1, o2) -> -o1.getPopularity().compareTo(o2.getPopularity())),

    POPULARITY_STOCK_DESCENDING(new Comparator<RecommendedPerfume>() {
        @Override
        public int compare(RecommendedPerfume o1, RecommendedPerfume o2) {
            Double p1 = o1.getPopularity() * o1.getMatchRate();
            Double p2 = o2.getPopularity() * o1.getMatchRate();
//            if (o1.getMatchRate()!=null)
//            {
//                p1+=o1.getMatchRate();
//            }
//            if (o2.getMatchRate()!=null)
//            {
//                p2+=o2.getMatchRate();
//            }
            if (o1.getPictureURL() == null || o1.getPictureURL().isEmpty()) {
                p1 /= 2.0;
            }

            if (o2.getPictureURL() == null || o2.getPictureURL().isEmpty()) {
                p2 /= 2.0;
            }
            if (o1.getAffiliateProducts() != null) {
                p1 += o1.getAffiliateProducts().size();
            }
            if (o2.getAffiliateProducts() != null) {
                p2 += o2.getAffiliateProducts().size();
            }

            return -p1.compareTo(p2);
        }
    });

    private Comparator<RecommendedPerfume> comparator;

    RecommendedPerfumeComparators(Comparator<RecommendedPerfume> comparator) {
        this.comparator = comparator;
    }

    public Comparator<RecommendedPerfume> getComparator() {
        return comparator;
    }
}

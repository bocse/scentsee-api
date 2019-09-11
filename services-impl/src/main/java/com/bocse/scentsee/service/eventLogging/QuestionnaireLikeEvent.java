package com.bocse.scentsee.service.eventLogging;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;

import java.util.Map;

/**
 * Created by bocse on 10.02.2016.
 */
public class QuestionnaireLikeEvent extends BusinessEvent {

    private Long recommendedPerfumeId;
    private Long recommendedPerfumeIndex;
    private Map<String, String> answers;

    @Override
    public void commit(AerospikeClient aerospikeClient) {
        if (recommendedPerfumeId == null || answers == null)
            return;
        Key key = new Key(aerospikeNamespace, this.getClass().getSimpleName(), System.currentTimeMillis() + "_" + System.nanoTime());
        Bin binIP = new Bin("ip", this.getIp());
        Bin binUA = new Bin("ua", this.getUserAgent());
        Bin bin1 = (new Bin("ts", System.currentTimeMillis()));
        Bin bin2 = new Bin("answers", answers);
        Bin bin3 = new Bin("recId", recommendedPerfumeId);
        Bin bin4 = new Bin("recIx", recommendedPerfumeIndex);

        aerospikeClient.put(null, key, binIP, binUA, bin1, bin2, bin3, bin4);
    }

    public Long getRecommendedPerfumeId() {
        return recommendedPerfumeId;
    }

    public void setRecommendedPerfumeId(Long recommendedPerfumeId) {
        this.recommendedPerfumeId = recommendedPerfumeId;
    }


    public Map<String, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, String> answers) {
        this.answers = answers;
    }

    public Long getRecommendedPerfumeIndex() {
        return recommendedPerfumeIndex;
    }

    public void setRecommendedPerfumeIndex(Long recommendedPerfumeIndex) {
        this.recommendedPerfumeIndex = recommendedPerfumeIndex;
    }
}

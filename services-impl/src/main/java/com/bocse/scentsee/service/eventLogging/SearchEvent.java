package com.bocse.scentsee.service.eventLogging;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;

import java.util.List;

/**
 * Created by bocse on 20.12.2015.
 */
public class SearchEvent extends BusinessEvent {

    private List<Long> recommendedIds;
    private List<Long> perfumeIds;

    public List<Long> getPerfumeIds() {
        return perfumeIds;
    }

    public void setPerfumeIds(List<Long> perfumeIds) {
        this.perfumeIds = perfumeIds;
        //this.bins.put("perfumeIds", new Bin("perfumeIds", perfumeIds));
    }


    @Override
    public void commit(AerospikeClient aerospikeClient) {
        if (perfumeIds == null)
            return;
        Key key = new Key(aerospikeNamespace, this.getClass().getSimpleName(), System.currentTimeMillis() + "_" + System.nanoTime());
        Bin binIP = new Bin("ip", this.getIp());
        Bin binUA = new Bin("ua", this.getUserAgent());
        Bin bin1 = (new Bin("ts", System.currentTimeMillis()));
        Bin bin2 = new Bin("perfumeList", perfumeIds);
        Bin bin3 = new Bin("recList", recommendedIds);
        aerospikeClient.put(null, key, binIP, binUA, bin1, bin2, bin3);
    }

    public List<Long> getRecommendedIds() {
        return recommendedIds;
    }

    public void setRecommendedIds(List<Long> recommendedIds) {
        this.recommendedIds = recommendedIds;
    }
}

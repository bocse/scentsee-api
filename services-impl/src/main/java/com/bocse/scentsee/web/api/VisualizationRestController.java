package com.bocse.scentsee.web.api;

import com.bocse.perfume.data.NoteType;
import com.bocse.perfume.data.Perfume;
import com.bocse.perfume.iterator.PerfumeIterator;
import org.jsondoc.core.annotation.Api;
import org.jsondoc.core.annotation.ApiMethod;
import org.jsondoc.core.annotation.ApiPathParam;
import org.jsondoc.core.pojo.ApiStage;
import org.jsondoc.core.pojo.ApiVisibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/rest/visualization")
@Api(name = "Perfume Visualization", description = "Methods for visualizing perfume components", group = "Visualization", visibility = ApiVisibility.PRIVATE, stage = ApiStage.PRE_ALPHA)
public class VisualizationRestController {

    @Autowired
    PerfumeIterator perfumeIterator;


    @Value("${collections.securityKey}")
    private String securityOverride = "MMKKDv4KhhwtMVzfT0G7W7hzEq87ID3noZbQVJx3";


    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/retrieve", method = RequestMethod.GET)
    @ApiMethod(description = "Creates a visualization for a specified perfume")
    Map<String, Object> retrieve(
            @ApiPathParam(description = "ID of the perfume")
            @RequestParam("id") Long id,
            @RequestParam("volume") Integer volume) {
        volume = Math.max(10, Math.min(100, volume));
        Map<Long, Perfume> perfumeMap = perfumeIterator.getPerfumeMap();
        Perfume perfume = perfumeMap.get(id);
        if (perfume == null) {
            throw new IllegalStateException("Unknown perfume");
        }
        Map<String, Object> response = new HashMap<>();
        //response.put("classes", NoteType.values());
        if (perfume.isSubstandard()) {
            response.put("base", randomize(volume, perfume.getMixedSignature()));
            response.put("heart", randomize(volume, perfume.getMixedSignature()));
            response.put("top", randomize(volume, perfume.getMixedSignature()));
        } else {
            response.put("base", randomize(volume, perfume.getBaseSignature()));
            response.put("heart", randomize(volume, perfume.getHeartSignature()));
            response.put("top", randomize(volume, perfume.getTopSignature()));
        }

        return response;
    }

    private List<Integer> randomize(Integer volume, Map<NoteType, Double> map) {
        List<Integer> result = new ArrayList<>();
        TreeMap<Double, NoteType> statisticalMap = new TreeMap<>();
        double sum = 0.0;

        for (Map.Entry<NoteType, Double> mapEntry : map.entrySet()) {
            sum += mapEntry.getValue();
            statisticalMap.put(new Double(sum), mapEntry.getKey());
        }
        for (int i = 0; i < volume; i++) {
            Double random = Math.random() * statisticalMap.lastKey();
            NoteType chosen = statisticalMap.higherEntry(random).getValue();
            int chosenInteger = chosen.ordinal();
            result.add(chosenInteger);
        }
        return result;
    }

}
package com.bocse.scentsee.web.api;

import com.bocse.perfume.data.Gender;
import com.bocse.perfume.data.NoteType;
import com.bocse.perfume.data.Perfume;
import com.bocse.perfume.iterator.PerfumeIterator;
import com.bocse.perfume.utils.TextUtils;
import com.google.common.util.concurrent.AtomicDouble;
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
@RequestMapping("/rest/analytics")
@Api(name = "Perfume Analytics", description = "Methods for analyzing the body of perfumes", group = "Analytics", visibility = ApiVisibility.PUBLIC, stage = ApiStage.PRE_ALPHA)
public class AnalyticsRestController {

    @Autowired
    PerfumeIterator perfumeIterator;


    @Value("${collections.securityKey}")
    private String securityOverride = "MMKKDv4KhhwtMVzfT0G7W7hzEq87ID3noZbQVJx3";


    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/noteTypeFrequency", method = RequestMethod.GET)
    @ApiMethod(description = "Analyzes frequency of note types. Requires a ScentSee Premium account.")
    Map<String, Object> noteFrequency(
            @ApiPathParam(description = "Brands")
            @RequestParam(value = "brands", required = false) List<String> brands,
            @ApiPathParam(description = "From which year to run the search")
            @RequestParam(value = "startYear", required = false) Integer startYear,
            @ApiPathParam(description = "Up to which year to run the search")
            @RequestParam(value = "endYear", required = false) Integer endYear,
            @ApiPathParam(description = "For which gender to perfom the analysis")
            @RequestParam(value = "gender", required = false) List<Gender> gender,
            @ApiPathParam(description = "If set to true, only those perfumes which are still being produced will be analyzed")
            @RequestParam(value = "mustBeInProduction", required = false) Boolean mustBeInProduction,
            @ApiPathParam(description = "The function to be used - count only accounts for the presence of a note type, while sum also accounts for its intensity")
            @RequestParam(value = "function", required = false, defaultValue = "sum") String function,
            @ApiPathParam(description = "The function to be used - count only accounts for the presence of a note type, while sum also accounts for its intensity")
            @RequestParam(value = "normalization", required = false, defaultValue = "true") Boolean normalization,
            @RequestParam(value = "accessKey", required = true) String accessKey,
            @RequestParam(value = "secretKey", required = true) String secretKey
    )

    {
        //TODO: Objective: note types on top, heart, base, all
        //TODO: Function: count, sum

        List<String> brandsTrimmed = new ArrayList<>();
        if (brands != null)
            for (String brand : brands) {
                brandsTrimmed.add(TextUtils.cleanupAndFlatten(brand.trim()).toLowerCase());
            }
        Map<String, Object> response = new HashMap<>();
        Map<NoteType, AtomicDouble> topMap = new HashMap<>();
        Map<NoteType, AtomicDouble> heartMap = new HashMap<>();
        Map<NoteType, AtomicDouble> baseMap = new HashMap<>();
        Map<NoteType, AtomicDouble> mixed = new HashMap<>();

        for (Perfume perfume : perfumeIterator.getPerfumeList()) {
            if (perfume.isSubstandard()) {
                continue;
            }
            if (gender != null && !gender.contains(perfume.getGender())) {
                continue;
            }
            if (mustBeInProduction != null && mustBeInProduction && !perfume.getInProduction()) {
                continue;
            }
            if (startYear != null && (perfume.getYear() == null || perfume.getYear() < startYear)) {
                continue;
            }
            if (endYear != null && (perfume.getYear() == null || perfume.getYear() > endYear)) {
                continue;
            }
            if (!brandsTrimmed.isEmpty() && !brandsTrimmed.contains(TextUtils.cleanupAndFlatten(perfume.getBrand()).toLowerCase())) {
                continue;
            }
            if (function.equals("sum")) {
                accumulateSumMap(topMap, perfume.getTopSignature());
                accumulateSumMap(heartMap, perfume.getHeartSignature());
                accumulateSumMap(baseMap, perfume.getBaseSignature());
            } else if (function.equals("count")) {
                accumulateCountMap(topMap, perfume.getTopSignature());
                accumulateCountMap(heartMap, perfume.getHeartSignature());
                accumulateCountMap(baseMap, perfume.getBaseSignature());
            }
        }
        if (normalization != null && normalization) {
            normalize(topMap);
            normalize(heartMap);
            normalize(baseMap);
        }
        response.put("top", sort(topMap));
        response.put("heart", sort(heartMap));
        response.put("base", sort(baseMap));
        return response;
    }

    private Map<NoteType, AtomicDouble> sort(Map<NoteType, AtomicDouble> noteMap) {

        List<Map.Entry<NoteType, AtomicDouble>> list =
                new LinkedList<Map.Entry<NoteType, AtomicDouble>>(noteMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<NoteType, AtomicDouble>>() {
            public int compare(Map.Entry<NoteType, AtomicDouble> o1, Map.Entry<NoteType, AtomicDouble> o2) {
                return -(new Double(o1.getValue().get())).compareTo(new Double(o2.getValue().get()));
            }
        });
        Map<NoteType, AtomicDouble> result = new LinkedHashMap<NoteType, AtomicDouble>();

        int classIndex = 0;
        for (Map.Entry<NoteType, AtomicDouble> entry : list) {
            if (entry.getKey().equals(NoteType.UNKNOWN) || entry.getKey().equals(NoteType.NON_CLASSIFIED)) {
                continue;
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private void normalize(Map<NoteType, AtomicDouble> accumulator) {
        Double sum = 0.0;
        for (AtomicDouble value : accumulator.values()) {
            sum += value.get();
        }
        for (Map.Entry<NoteType, AtomicDouble> entry : accumulator.entrySet()) {
            entry.getValue().set(entry.getValue().doubleValue() / sum);
        }
    }

    private void accumulateSumMap(Map<NoteType, AtomicDouble> accumulator, Map<NoteType, Double> element) {
        for (Map.Entry<NoteType, Double> noteType : element.entrySet()) {
            accumulator.putIfAbsent(noteType.getKey(), new AtomicDouble(0));
            accumulator.get(noteType.getKey()).addAndGet(noteType.getValue().doubleValue());
        }
    }

    private void accumulateCountMap(Map<NoteType, AtomicDouble> accumulator, Map<NoteType, Double> element) {
        for (Map.Entry<NoteType, Double> noteType : element.entrySet()) {
            accumulator.putIfAbsent(noteType.getKey(), new AtomicDouble(0));
            accumulator.get(noteType.getKey()).addAndGet(1);
        }
    }


}
package com.bocse.scentsee.web.api;

import com.bocse.perfume.affiliate.AffiliateCollection;
import com.bocse.perfume.data.Gender;
import com.bocse.perfume.data.Perfume;
import com.bocse.perfume.data.RecommendationAlgorithm;
import com.bocse.perfume.data.RecommendedPerfume;
import com.bocse.perfume.iterator.PerfumeIterator;
import com.bocse.perfume.utils.SynonymManager;
import com.bocse.perfume.utils.TextUtils;
import org.jsondoc.core.annotation.Api;
import org.jsondoc.core.annotation.ApiMethod;
import org.jsondoc.core.annotation.ApiPathParam;
import org.jsondoc.core.annotation.ApiQueryParam;
import org.jsondoc.core.pojo.ApiStage;
import org.jsondoc.core.pojo.ApiVisibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
//@RequestMapping("/{userId}/bookmarks")
@RequestMapping("/rest/collection")
@Api(name = "Perfume Collection", description = "Methods for searching and retriving perfumes by name, brand or ID", group = "Collection", visibility = ApiVisibility.PUBLIC, stage = ApiStage.RC)
public class CollectionRestController {

    @Autowired
    PerfumeIterator perfumeIterator;

    @Autowired
    AffiliateCollection affiliateCollection;


    private int maxResults = 50;
    private int minLength = 3;

    @Value("${collections.securityKey}")
    private String securityOverride = "MMKKDv4KhhwtMVzfT0G7W7hzEq87ID3noZbQVJx3";


    //@RequestMapping(value = "/listAll", method = RequestMethod.GET)
    List<HashMap<String, Object>> listAll(@PathVariable String authorization) {
        if (!authorization.equals(securityOverride)) {
            throw new SecurityException("Unauthorized");
        }
        List<HashMap<String, Object>> shortList = new ArrayList<>();
        List<Perfume> perfumeList = perfumeIterator.getPerfumeList();
        for (Perfume perfume : perfumeList) {
            HashMap<String, Object> perfumeAttributes = new HashMap<>();
            perfumeAttributes.put("brand", perfume.getBrand());
            perfumeAttributes.put("name", perfume.getName());
            perfumeAttributes.put("id", new Long(perfume.getId()));
            shortList.add(perfumeAttributes);
        }
        return shortList;
    }

    //@PathVariable Gender gender

    //@RequestMapping(value = "/listAll/{gender}", method = RequestMethod.GET)
    //@RequestMapping(value = "/query/{nameFragment}", method = RequestMethod.GET)
    List<HashMap<String, Object>> query(@PathVariable String nameFragment, @PathVariable String authorization) {

        if (!authorization.equals(securityOverride)) {
            throw new SecurityException("Unauthorized");
        }
        List<HashMap<String, Object>> shortList = new ArrayList<>();
        if (nameFragment.length() < minLength) {
            throw new IllegalStateException("Query length is too short");
        }
        String[] nameFragments = nameFragment.split("[, ;.:]");
        List<Perfume> perfumeList = perfumeIterator.getPerfumeList();
        for (Perfume perfume : perfumeList) {
            int matchCount = 0;
            String searchableName = (perfume.getName() + " " + perfume.getBrand()).toLowerCase();
            for (String fragement : nameFragments) {
                if (searchableName.contains(fragement)) {
                    matchCount++;
                }
            }
            if (matchCount >= nameFragments.length) {
                HashMap<String, Object> perfumeAttributes = new HashMap<>();
                perfumeAttributes.put("id", new Long(perfume.getId()));
                perfumeAttributes.put("brand", perfume.getBrand());
                perfumeAttributes.put("name", perfume.getName());
                //perfumeAttributes.put("searchable", (perfume.getBrand()+" "+perfume.getName()));
                shortList.add(perfumeAttributes);
                if (shortList.size() > maxResults) {
                    return shortList;
                }
            }
        }
        return shortList;
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/queryBrands")
    @ApiMethod(description = "Perform search of perfume collection by full or partial brand or name.")
    @ResponseBody
    public List<Map<String, Object>> queryBrands(
            @ApiPathParam(description = "The full or partial name of the brand") @RequestParam("query") String brandFragment
    ) {
        brandFragment = TextUtils.cleanupAndFlatten(brandFragment).toLowerCase();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, List<Perfume>> brandMap = perfumeIterator.getBrandMap();
        for (Map.Entry<String, List<Perfume>> entry : brandMap.entrySet()) {
            if (entry.getKey().contains(brandFragment)) {
                if (entry.getValue().size() > 0) {
                    Map<String, Object> brand = new HashMap<>();
                    brand.put("name", entry.getValue().get(0).getBrand());
                    brand.put("items", entry.getValue().size());

                    result.add(brand);
                }
            }
        }
        result.sort(new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                return (Integer) o2.get("items") - (Integer) o1.get("items");
            }
        });
        return result;
    }


    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/queryFull")
    @ApiMethod(description = "Perform search of perfume collection by full or partial brand or name.")
    @ResponseBody
    public List<RecommendedPerfume> queryFullGet(
            @ApiPathParam(description = "The full or partial name of the perfume") @RequestParam("query") String nameFragment,
            @ApiPathParam(description = "If set to true, only displays those perfumes which are available with accepted commercial vendor") @RequestParam(value = "canBeSold", required = false, defaultValue = "false") Boolean canBeSold,
            //@RequestParam(value = "withAffiliates", required = false, defaultValue = "true") Boolean withAffiliates,
            @ApiPathParam(description = "If set to true, only displays those perfumes which are still in production.") @RequestParam(value = "inProduction", required = false, defaultValue = "false") Boolean inProductionCondition,
            @ApiPathParam(description = "Gender of the user who performs the search (MALE, FEMALE, UNI)") @RequestParam(value = "gender", required = false) Gender gender,
            @ApiPathParam(description = "If true, will enable a fuzzy search") @RequestParam(value = "fuzzySearch", required = false, defaultValue = "true") Boolean fuzzySearch
    ) {

        if (nameFragment.length() < minLength) {
            throw new IllegalStateException("Query length is too short");
        }
        Boolean withAffiliates = false;
        Boolean acceptSubstandard = true;
        if (canBeSold) {
            withAffiliates = true;
            acceptSubstandard = true;
        }

        nameFragment = TextUtils.cleanupAndFlatten(nameFragment).toLowerCase();
        List<RecommendedPerfume> shortList = search(nameFragment, gender, withAffiliates, inProductionCondition, acceptSubstandard, false);
        if (fuzzySearch) {
            List<RecommendedPerfume> fuzzyList = search(nameFragment, gender, withAffiliates, inProductionCondition, acceptSubstandard, true);
            shortList.addAll(fuzzyList);
        }
        //shortList.sort(RecommendedPerfumeComparators.POPULARITY_STOCK_DESCENDING.getComparator());
        Collections.sort(shortList, Collections.reverseOrder());
        if (shortList.size() > 1)
            shortList = shortList.subList(0, Math.min(maxResults, shortList.size() - 1));
        return shortList;
    }

    private List<RecommendedPerfume> search(String nameFragment, Gender gender, Boolean withAffiliates, Boolean inProductionCondition, Boolean acceptSubstandard, Boolean synonyms) {
        String originalNameFragment = nameFragment;
        if (synonyms) {
            SynonymManager synonymManager = new SynonymManager();
            synonymManager.defaultInit();
            nameFragment = synonymManager.transform(nameFragment);
        }
        String[] nameFragments = nameFragment.split("[, ;.:-_]");

        List<RecommendedPerfume> shortList = new ArrayList<>();
        List<Perfume> perfumeList = perfumeIterator.getPerfumeList();
        Map<Perfume, String> contractions = perfumeIterator.getNameLexicographicContraction();
        for (Perfume perfume : perfumeList) {
            int matchCount = 0;
            String searchableName; //TextUtils.cleanupAndFlatten((perfume.getName() + " " + perfume.getBrand()).toLowerCase());
            if (!synonyms) {
                searchableName = perfume.getSearchableName();
            } else {
                searchableName = contractions.get(perfume);
            }
            for (String fragment : nameFragments) {
                if (searchableName != null) {
                    if (searchableName.contains(fragment)) {
                        matchCount++;
                    }
                } else {
                    System.currentTimeMillis();
                }
            }
            if (matchCount < nameFragments.length)
                continue;
            if ((acceptSubstandard || !perfume.isSubstandard()) &&
                    (!withAffiliates || perfume.getAffiliateProducts().size() > 0) &&
                    (!inProductionCondition || perfume.getInProduction()) &&
                    (gender == null || perfume.getGender().equals(gender) || perfume.getGender().equals(Gender.UNI)) &&
                    (perfume.getMixedNotes().size() > 0)
                    ) {
                String[] searchableNameParts = searchableName.split("[, ;.:-_]");
                RecommendedPerfume recommendedPerfume = new RecommendedPerfume(perfume, affiliateCollection);

                recommendedPerfume.setMetadata(originalNameFragment);
                double matchRate = matchCount * 1.0 / searchableNameParts.length;
                matchRate = matchRate * recommendedPerfume.getPopularity() * (1.0 + recommendedPerfume.getAffiliateProducts().size());
                if (recommendedPerfume.getPictureURL() == null || recommendedPerfume.getPictureURL().isEmpty()) {
                    matchRate = matchRate / 2.0;
                }
                recommendedPerfume.setMatchRate(matchRate);
                recommendedPerfume.getMatchRates().put(RecommendationAlgorithm.textSearch, matchRate);
                shortList.add(recommendedPerfume);
            }
        }
        return shortList;
    }
    @RequestMapping(value = "/retrieve", method = RequestMethod.GET)
    @ApiMethod(description = "Retrives information about a perfume specified by numeric unique identifier (id). Please note that ID are not guaranteed to be immutable.")
    RecommendedPerfume retrieve(
            @ApiPathParam(description = "ID of the perfume")
            @RequestParam("id") Long id) {
        Map<Long, Perfume> perfumeMap = perfumeIterator.getPerfumeMap();
        Perfume perfume = perfumeMap.get(id);
        return new RecommendedPerfume(perfume, affiliateCollection);
    }

    /*
    @RequestMapping(value = "/retrieveSlow", method = RequestMethod.GET)
    Perfume retrieveSlow(@RequestParam("id") Long id) {
        for (Perfume perfume: perfumeIterator.getPerfumeList())
        {
            if (perfume.getId().equals(id))
                return perfume;
        }
        return null;
    }
    */

    @RequestMapping(value = "/retrieveMany", method = RequestMethod.GET)
    @ApiMethod(description = "Retrives information about a list of perfumes specified by numeric unique identifiers (ids). IDs are not guaranteed to be immutable.")
    List<RecommendedPerfume> retrieve(
            @ApiQueryParam(description = "IDs of the perfumes (comma separated). Example &ids[]=839945385571163,839945385571163") @RequestParam("ids[]") Long[] ids) {
        List<RecommendedPerfume> perfumes = new ArrayList<>();
        for (Long id : ids) {
            perfumes.add(new RecommendedPerfume(perfumeIterator.getPerfumeMap().get(id), affiliateCollection));
            if (perfumes.size() > 2 * maxResults) {
                return perfumes;
            }
        }
        return perfumes;
    }
}
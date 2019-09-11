package com.bocse.scentsee.web.api;

import com.bocse.perfume.data.Gender;
import com.bocse.perfume.data.NoteType;
import com.bocse.perfume.iterator.QuestionnaireIterator;
import org.jsondoc.core.annotation.Api;
import org.jsondoc.core.annotation.ApiMethod;
import org.jsondoc.core.annotation.ApiQueryParam;
import org.jsondoc.core.pojo.ApiStage;
import org.jsondoc.core.pojo.ApiVisibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/constants")
@Api(name = "Constants", description = "Constants", group = "Collection", visibility = ApiVisibility.PUBLIC, stage = ApiStage.RC)
public class ConstantsRestController {

    @Autowired
    QuestionnaireIterator questionnaireIterator;


    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/noteTypes")
    @ApiMethod(description = "Lists all note types")
    @ResponseBody
    public Object noteTypes(
    ) {
        return NoteType.values();
    }

    @CrossOrigin(origins = "*")
    @RequestMapping(value = "/questionnaire")
    @ApiMethod(description = "Lists all questions from quiz (questionnaire) with all possible answers. Questions may be different based on the gender of the user.")
    @ResponseBody
    public Object quiz(
            @ApiQueryParam(description = "The gender for which questions and answers are required.") @RequestParam(value = "gender", required = false) Gender gender
    ) {
        if (gender != null) {
            return questionnaireIterator.getPublicQuestionnaireMapping().get(gender);
        } else {
            return questionnaireIterator.getPublicQuestionnaireMapping();
        }
    }
}
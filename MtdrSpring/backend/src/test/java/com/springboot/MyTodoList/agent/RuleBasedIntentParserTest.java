package com.springboot.MyTodoList.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RuleBasedIntentParserTest {

    private final RuleBasedIntentParser parser = new RuleBasedIntentParser();

    @Test
    void recognizesGuacamoleAsOnlyAllowedOffTopicIntent() {
        ParsedIntent intent = parser.parse("Como hacer un guacamole?");
        ParsedIntent typoIntent = parser.parse("Coo hacer un guacamole?");

        assertEquals(IntentType.GUACAMOLE_RECIPE, intent.getIntent());
        assertEquals(IntentType.GUACAMOLE_RECIPE, typoIntent.getIntent());
    }

    @Test
    void rejectsOtherOffTopicQuestionsAsUnknown() {
        ParsedIntent intent = parser.parse("Quien gano el mundial?");

        assertEquals(IntentType.UNKNOWN, intent.getIntent());
        assertTrue(intent.isClarificationNeeded());
    }
}

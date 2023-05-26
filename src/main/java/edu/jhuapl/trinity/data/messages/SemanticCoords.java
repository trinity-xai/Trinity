package edu.jhuapl.trinity.data.messages;

/*-
 * #%L
 * trinity-1.0.0-SNAPSHOT
 * %%
 * Copyright (C) 2021 - 2023 The Johns Hopkins University Applied Physics Laboratory LLC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author Sean Phillips
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemanticCoords extends MessageData {

    public static final String TYPESTRING = "semantic_coords";
    //<editor-fold defaultstate="collapsed" desc="JSON Payload">
    /*
        "coords": {
            "noun": {
                "dims": ["noun"],
                "attrs": {},
                "data": ["bear", "cat", "cow", "dog", "horse", "arm", "eye", "foot", "hand", "leg", "apartment", "barn", "church", "house", "igloo", "arch", "chimney", "closet", "door", "window", "coat", "dress", "pants", "shirt", "skirt", "bed", "chair", "desk", "dresser", "table", "ant", "bee", "beetle", "butterfly", "fly", "bottle", "cup", "glass", "knife", "spoon", "bell", "key", "refrigerator", "telephone", "watch", "chisel", "hammer", "pliers", "saw", "screwdriver", "carrot", "celery", "corn", "lettuce", "tomato", "airplane", "bicycle", "car", "train", "truck"]
            },
            "feature_desc": {
                "dims": ["feature"],
                "attrs": {},
                "data": ["IS IT AN ANIMAL?", "IS IT A BODY PART?", "IS IT A BUILDING?", "IS IT A BUILDING PART?", "IS IT CLOTHING?", "IS IT FURNITURE?", "IS IT AN INSECT?", "IS IT A KITCHEN ITEM?", "IS IT MANMADE?", "IS IT A TOOL?", "CAN YOU EAT IT?", "IS IT A VEHICLE?", "IS IT A PERSON?", "IS IT A VEGETABLE / PLANT?", "IS IT A FRUIT?", "IS IT MADE OF METAL?", "IS IT MADE OF PLASTIC?", "IS PART OF IT MADE OF GLASS?", "IS IT MADE OF WOOD?", "IS IT SHINY?", "CAN YOU SEE THROUGH IT?", "IS IT COLORFUL?", "DOES IT CHANGE COLOR?", "IS ONE MORE THAN ONE COLORED?", "IS IT ALWAYS THE SAME COLOR(S)?", "IS IT WHITE?", "IS IT RED?", "IS IT ORANGE?", "IS IT FLESH-COLORED?", "IS IT YELLOW?", "IS IT GREEN?", "IS IT BLUE?", "IS IT SILVER?", "IS IT BROWN?", "IS IT BLACK?", "IS IT CURVED?", "IS IT STRAIGHT?", "IS IT FLAT?", "DOES IT HAVE A FRONT AND A BACK?", "DOES IT HAVE A FLAT / STRAIGHT TOP?", "DOES IT HAVE FLAT / STRAIGHT SIDES?", "IS TALLER THAN IT IS WIDE/LONG?", "IS IT LONG?", "IS IT POINTED / SHARP?", "IS IT TAPERED?", "IS IT ROUND?", "DOES IT HAVE CORNERS?", "IS IT SYMMETRICAL?", "IS IT HAIRY?", "IS IT FUZZY?", "IS IT CLEAR?", "IS IT SMOOTH?", "IS IT SOFT?", "IS IT HEAVY?", "IS IT LIGHTWEIGHT?", "IS IT DENSE?", "IS IT SLIPPERY?", "CAN IT CHANGE SHAPE?", "CAN IT BEND?", "CAN IT STRETCH?", "CAN IT BREAK?", "IS IT FRAGILE?", "DOES IT HAVE PARTS?", "DOES IT HAVE MOVING PARTS?", "DOES IT COME IN PAIRS?", "DOES IT COME IN A BUNCH/PACK?", "DOES IT LIVE IN GROUPS?", "IS IT PART OF SOMETHING LARGER?", "DOES IT CONTAIN SOMETHING ELSE?", "DOES IT HAVE INTERNAL STRUCTURE?", "DOES IT OPEN?", "IS IT HOLLOW?", "DOES IT HAVE A HARD INSIDE?", "DOES IT HAVE A HARD OUTER SHELL?", "DOES IT HAVE AT LEAST ONE HOLE?", "IS IT ALIVE?", "WAS IT EVER ALIVE?", "IS IT A SPECIFIC GENDER?", "IS IT MANUFACTURED?", "WAS IT INVENTED?", "WAS IT AROUND 100 YEARS AGO?", "ARE THERE MANY VARIETIES OF IT?", "DOES IT COME IN DIFFERENT SIZES?", "DOES IT GROW?", "IS IT SMALLER THAN A GOLFBALL?", "IS IT BIGGER THAN A LOAF OF BREAD?", "IS IT BIGGER THAN A MICROWAVE OVEN?", "IS IT BIGGER THAN A BED?", "IS IT BIGGER THAN A CAR?", "IS IT BIGGER THAN A HOUSE?", "IS IT TALLER THAN A PERSON?", "DOES IT HAVE A TAIL?", "DOES IT HAVE LEGS?", "DOES IT HAVE FOUR LEGS?", "DOES IT HAVE FEET?", "DOES IT HAVE PAWS?", "DOES IT HAVE CLAWS?", "DOES IT HAVE HORNS / THORNS / SPIKES?", "DOES IT HAVE HOOVES?", "DOES IT HAVE A FACE?", "DOES IT HAVE A BACKBONE?", "DOES IT HAVE WINGS?", "DOES IT HAVE EARS?", "DOES IT HAVE ROOTS?", "DOES IT HAVE SEEDS?", "DOES IT HAVE LEAVES?", "DOES IT COME FROM A PLANT?", "DOES IT HAVE FEATHERS?", "DOES IT HAVE SOME SORT OF NOSE?", "DOES IT HAVE A HARD NOSE/BEAK?", "DOES IT CONTAIN LIQUID?", "DOES IT HAVE WIRES OR A CORD?", "DOES IT HAVE WRITING ON IT?", "DOES IT HAVE WHEELS?", "DOES IT MAKE A SOUND?", "DOES IT MAKE A NICE SOUND?", "DOES IT MAKE SOUND CONTINUOUSLY WHEN ACTIVE?", "IS ITS JOB TO MAKE SOUNDS?", "DOES IT ROLL?", "CAN IT RUN?", "IS IT FAST?", "CAN IT FLY?", "CAN IT JUMP?", "CAN IT FLOAT?", "CAN IT SWIM?", "CAN IT DIG?", "CAN IT CLIMB TREES?", "CAN IT CAUSE YOU PAIN?", "CAN IT BITE OR STING?", "DOES IT STAND ON TWO LEGS?", "IS IT WILD?", "IS IT A HERBIVORE?", "IS IT A PREDATOR?", "IS IT WARM BLOODED?", "IS IT A MAMMAL?", "IS IT NOCTURNAL?", "DOES IT LAY EGGS?", "IS IT CONSCIOUS?", "DOES IT HAVE FEELINGS?", "IS IT SMART?", "IS IT MECHANICAL?", "IS IT ELECTRONIC?", "DOES IT USE ELECTRICITY?", "CAN IT KEEP YOU DRY?", "DOES IT PROVIDE PROTECTION?", "DOES IT PROVIDE SHADE?", "DOES IT CAST A SHADOW?", "DO YOU SEE IT DAILY?", "IS IT HELPFUL?", "DO YOU INTERACT WITH IT?", "CAN YOU TOUCH IT?", "WOULD YOU AVOID TOUCHING IT?", "CAN YOU HOLD IT?", "CAN YOU HOLD IT IN ONE HAND?", "DO YOU HOLD IT TO USE IT?", "CAN YOU PLAY IT?", "CAN YOU PLAY WITH IT?", "CAN YOU PET IT?", "CAN YOU USE IT?", "DO YOU USE IT DAILY?", "CAN YOU USE IT UP?", "DO YOU USE IT WHEN COOKING?", "IS IT USED TO CARRY THINGS?", "CAN YOU PICK IT UP?", "CAN YOU CONTROL IT?", "CAN YOU SIT ON IT?", "CAN YOU RIDE ON/IN IT?", "IS IT USED FOR TRANSPORTATION?", "COULD YOU FIT INSIDE IT?", "IS IT USED IN SPORTS?", "DO YOU WEAR IT?", "CAN IT BE WASHED?", "IS IT COLD?", "IS IT COOL?", "IS IT WARM?", "IS IT HOT?", "IS IT UNHEALTHY?", "IS IT HARD TO CATCH?", "CAN YOU PEEL IT?", "CAN YOU WALK ON IT?", "CAN YOU SWITCH IT ON AND OFF?", "CAN IT BE EASILY MOVED?", "DO YOU DRINK FROM IT?", "DOES IT GO IN YOUR MOUTH?", "IS IT TASTY?", "IS IT USED DURING MEALS?", "DOES IT HAVE A STRONG SMELL?", "DOES IT SMELL GOOD?", "DOES IT SMELL BAD?", "IS IT USUALLY INSIDE?", "IS IT USUALLY OUTSIDE?", "WOULD YOU FIND IT ON A FARM?", "WOULD YOU FIND IT IN A SCHOOL?", "WOULD YOU FIND IT IN A ZOO?", "WOULD YOU FIND IT IN AN OFFICE?", "WOULD YOU FIND IT IN A RESTAURANT?", "WOULD YOU FIND IN THE BATHROOM?", "WOULD YOU FIND IT IN A HOUSE?", "WOULD YOU FIND IT NEAR A ROAD?", "WOULD YOU FIND IT IN A DUMP/LANDFILL?", "WOULD YOU FIND IT IN THE FOREST?", "WOULD YOU FIND IT IN A GARDEN?", "WOULD YOU FIND IT IN THE SKY?", "DO YOU FIND IT IN SPACE?", "DOES IT LIVE ABOVE GROUND?", "DOES IT GET WET?", "DOES IT LIVE IN WATER?", "CAN IT LIVE OUT OF WATER?", "DO YOU TAKE CARE OF IT?", "DOES IT MAKE YOU HAPPY?", "DO YOU LOVE IT?", "WOULD YOU MISS IT IF IT WERE GONE?", "IS IT SCARY?", "IS IT DANGEROUS?", "IS IT FRIENDLY?", "IS IT RARE?", "CAN YOU BUY IT?", "IS IT VALUABLE?"]
            },
            "group_membership": {
                "dims": ["noun"],
                "attrs": {},
                "data": ["animals", "animals", "animals", "animals", "animals", "body_parts", "body_parts", "body_parts", "body_parts", "body_parts", "buildings", "buildings", "buildings", "buildings", "buildings", "building_parts", "building_parts", "building_parts", "building_parts", "building_parts", "clothing", "clothing", "clothing", "clothing", "clothing", "furniture", "furniture", "furniture", "furniture", "furniture", "insects", "insects", "insects", "insects", "insects", "kitchen_utensils", "kitchen_utensils", "kitchen_utensils", "kitchen_utensils", "kitchen_utensils", "man_made_objects", "man_made_objects", "man_made_objects", "man_made_objects", "man_made_objects", "tools", "tools", "tools", "tools", "tools", "vegetables", "vegetables", "vegetables", "vegetables", "vegetables", "vehicles", "vehicles", "vehicles", "vehicles", "vehicles"]
            }
        }
    */
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Payload Fields">
    private SemanticData noun;
    private SemanticData feature_desc;
    private SemanticData group_membership;
    //</editor-fold>

    public SemanticCoords() {
        this.messageType = TYPESTRING;
    }

    public static boolean isFeatureVector(String messageBody) {
        return messageBody.contains("noun") && messageBody.contains("feature_desc")
            && messageBody.contains("group_membership");
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">

    /**
     * @return the noun
     */
    public SemanticData getNoun() {
        return noun;
    }

    /**
     * @param noun the noun to set
     */
    public void setNoun(SemanticData noun) {
        this.noun = noun;
    }

    /**
     * @return the feature_desc
     */
    public SemanticData getFeature_desc() {
        return feature_desc;
    }

    /**
     * @param feature_desc the feature_desc to set
     */
    public void setFeature_desc(SemanticData feature_desc) {
        this.feature_desc = feature_desc;
    }

    /**
     * @return the group_membership
     */
    public SemanticData getGroup_membership() {
        return group_membership;
    }

    /**
     * @param group_membership the group_membership to set
     */
    public void setGroup_membership(SemanticData group_membership) {
        this.group_membership = group_membership;
    }
    //</editor-fold>
}

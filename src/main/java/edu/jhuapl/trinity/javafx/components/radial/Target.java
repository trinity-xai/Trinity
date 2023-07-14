package edu.jhuapl.trinity.javafx.components.radial;

//import com.fasterxml.jackson.annotation.JsonTypeInfo;

/*-
 * #%L
 * trinity
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

/**
 * Enforces a common interface for objects that can be targeted. Targeting
 * implies an act of "activation" (current target for user) and an act of
 * "engagement" (user has interacted with the target sufficiently).
 * These states can be defined as any combination of logic.
 *
 * @author Sean Phillips
 */
//@JsonTypeInfo(
//    use = JsonTypeInfo.Id.MINIMAL_CLASS,
//    include = JsonTypeInfo.As.PROPERTY,
//    property = "@type")
public interface Target {
    /**
     * provide the unique id for the entity this Target represents.
     *
     * @return String representation of the unique id
     */
    public String getEntityId();

    /**
     * Function to determine if the current tracker has been activated.
     *
     * @return value, true if selected.
     */
    public boolean isActivated();

    /**
     * Set the activated state.
     *
     * @param activated true or false if activated.
     */
    public void setActivated(boolean activated);

    /**
     * test the engaged state.
     *
     * @return true or false if engagement was successful.
     */
    public boolean isEngaged();

    /**
     * Action to engage this target.
     *
     * @return boolean true if engagement successful
     */
    public boolean engage();

    /**
     * Action to engage this target.
     *
     * @return boolean true if engagement successful
     */
    public long getEngagementStart();

    /**
     * time in milliseconds that the engagement was completed.
     *
     * @return longtrue if engagement successful
     */
    public long getEngagementEnd();

    /**
     * Action to clear both activation and engaged flags along with any graphic
     * updates.
     */
    public void reset();


}

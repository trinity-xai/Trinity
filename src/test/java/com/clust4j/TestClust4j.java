/*******************************************************************************
 *    Original Copyright 2015, 2016 Taylor G Smith
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/
package com.clust4j;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestClust4j {
    final static Clust4j c4j = new Clust4j() {
        private static final long serialVersionUID = 1L;
    };

    @Test
    public void testSaveNPE() throws IOException {
        assertThrows(NullPointerException.class, () -> {
            c4j.saveObject(null);
        });
    }

    @Test
    public void testLoadNPE() throws IOException, ClassNotFoundException {
        assertThrows(NullPointerException.class, () -> {
            Clust4j.loadObject(null);
        });
    }
}

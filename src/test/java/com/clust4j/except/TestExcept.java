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
package com.clust4j.except;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestExcept {

    @Test
    public void testICSE1() {
        assertThrows(IllegalClusterStateException.class, () -> {
            throw new IllegalClusterStateException();
        });
    }

    @Test
    public void testICSE2() {
        assertThrows(IllegalClusterStateException.class, () -> {
            throw new IllegalClusterStateException("asdf");
        });
    }

    @Test
    public void testICSE3() {
        assertThrows(IllegalClusterStateException.class, () -> {
            throw new IllegalClusterStateException(new Exception());
        });
    }

    @Test
    public void testICSE4() {
        assertThrows(IllegalClusterStateException.class, () -> {
            throw new IllegalClusterStateException("asdf", new Exception());
        });
    }


    @Test
    public void testMPE1() {
        assertThrows(MatrixParseException.class, () -> {
            throw new MatrixParseException();
        });
    }

    @Test
    public void testMPE2() {
        assertThrows(MatrixParseException.class, () -> {
            throw new MatrixParseException("asdf");
        });
    }

    @Test
    public void testMPE3() {
        assertThrows(MatrixParseException.class, () -> {
            throw new MatrixParseException(new Exception());
        });
    }

    @Test
    public void testMPE4() {
        assertThrows(MatrixParseException.class, () -> {
            throw new MatrixParseException("asdf", new Exception());
        });
    }


    @Test
    public void testMNFE1() {
        assertThrows(ModelNotFitException.class, () -> {
            throw new ModelNotFitException();
        });
    }

    @Test
    public void testMNFE2() {
        assertThrows(ModelNotFitException.class, () -> {
            throw new ModelNotFitException("asdf");
        });
    }

    @Test
    public void testMNFE3() {
        assertThrows(ModelNotFitException.class, () -> {
            throw new ModelNotFitException(new Exception());
        });
    }

    @Test
    public void testMNFE4() {
        assertThrows(ModelNotFitException.class, () -> {
            throw new ModelNotFitException("asdf", new Exception());
        });
    }


    @Test
    public void testNaN1() {
        assertThrows(NaNException.class, () -> {
            throw new NaNException();
        });
    }

    @Test
    public void testNaN2() {
        assertThrows(NaNException.class, () -> {
            throw new NaNException("asdf");
        });
    }

    @Test
    public void testNaN3() {
        assertThrows(NaNException.class, () -> {
            throw new NaNException(new Exception());
        });
    }

    @Test
    public void testNaN4() {
        assertThrows(NaNException.class, () -> {
            throw new NaNException("asdf", new Exception());
        });
    }


    @Test
    public void testNUME1() {
        assertThrows(NonUniformMatrixException.class, () -> {
            throw new NonUniformMatrixException(1, 2);
        });
    }
}

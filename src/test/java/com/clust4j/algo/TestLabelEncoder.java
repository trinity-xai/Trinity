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
package com.clust4j.algo;

import com.clust4j.TestSuite;
import com.clust4j.except.ModelNotFitException;
import com.clust4j.utils.VecUtils;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLabelEncoder implements BaseModelTest {

    @Test
    public void test1() {
        int[] labels = new int[]{1, 2, 2, 6};
        LabelEncoder le = new LabelEncoder(labels).fit();

        // Test that fit just returns immediately after already fit
        le.fit();

        int[] expectedClasses = new int[]{1, 2, 6};
        assertTrue(VecUtils.equalsExactly(le.getClasses(), expectedClasses));
        assertTrue(le.getNumClasses() == 3);
        assertTrue(le.encodeOrNull(1) == 0);

        int[] expectedEncodings = new int[]{0, 1, 1, 2};
        assertTrue(VecUtils.equalsExactly(expectedEncodings, le.getEncodedLabels()));
        assertTrue(VecUtils.equalsExactly(labels, le.getRawLabels()));

        assertTrue(le.encodeOrNull(0) == null);
        assertTrue(le.reverseEncodeOrNull(0) == 1);

        assertTrue(le.reverseEncodeOrNull(12) == null);
        assertTrue(le.encodeOrNull(2) == 1);

        assertTrue(VecUtils.equalsExactly(le.reverseTransform(le.getEncodedLabels()), labels));
        assertTrue(VecUtils.equalsExactly(le.transform(le.getRawLabels()), expectedEncodings));
    }

    @Test
    public void test2() {
        assertThrows(ModelNotFitException.class, () -> {
            int[] labels = new int[]{1, 2, 2, 6};
            LabelEncoder le = new LabelEncoder(labels);
            le.encodeOrNull(1);
        });
    }

    @Test
    public void test3() {
        assertThrows(ModelNotFitException.class, () -> {
            int[] labels = new int[]{1, 2, 2, 6};
            LabelEncoder le = new LabelEncoder(labels);
            le.reverseEncodeOrNull(1);
        });
    }

    @Test
    public void test4() {
        assertThrows(ModelNotFitException.class, () -> {
            int[] labels = new int[]{1, 2, 2, 6};
            LabelEncoder le = new LabelEncoder(labels);
            le.getEncodedLabels();
        });
    }

    @Test
    public void test5() {
        assertThrows(ModelNotFitException.class, () -> {
            int[] labels = new int[]{1, 2, 2, 6};
            LabelEncoder le = new LabelEncoder(labels);
            le.reverseTransform(new int[]{0, 1, 1, 2});
        });
    }

    @Test
    public void test6() {
        assertThrows(ModelNotFitException.class, () -> {
            int[] labels = new int[]{1, 2, 2, 6};
            LabelEncoder le = new LabelEncoder(labels);
            le.transform(labels);
        });
    }

    @Test
    public void test7() {
        assertThrows(IllegalArgumentException.class, () -> {
            int[] labels = new int[]{1, 2, 2, 6};
            LabelEncoder le = new LabelEncoder(labels).fit();
            le.transform(new int[]{7, 6, 5});
        });
    }

    @Test
    public void test8() {
        assertThrows(IllegalArgumentException.class, () -> {
            int[] labels = new int[]{1, 2, 2, 6};
            LabelEncoder le = new LabelEncoder(labels).fit();
            le.reverseTransform(new int[]{3, 2, 1});
        });
    }


    // ====== test unique classes with less than 2
    @Test
    public void test9() {
        assertThrows(IllegalArgumentException.class, () -> {
            int[] labels = new int[]{};
            new LabelEncoder(labels);
        });
    }

    @Test
    public void test10() {
        assertThrows(IllegalArgumentException.class, () -> {
            int[] labels = new int[]{1, 1, 1, 1, 1, 1, 1};
            new LabelEncoder(labels);
        });
    }

    @Test
    public void test11() {
        assertThrows(IllegalArgumentException.class, () -> {
            int[] labels = new int[]{1};
            new LabelEncoder(labels);
        });
    }

    @Test
    @Override
    public void testSerialization() throws IOException, ClassNotFoundException {
        LabelEncoder encoder = new LabelEncoder(new int[]{
            0, 1, 5, 1, 2, 2, 2, 0, 1, 1, 5
        }).fit();

        final int[] mappings = encoder.getEncodedLabels();
        encoder.saveObject(new FileOutputStream(TestSuite.tmpSerPath));
        assertTrue(TestSuite.file.exists());

        LabelEncoder encoder2 = (LabelEncoder) LabelEncoder.loadObject(new FileInputStream(TestSuite.tmpSerPath));
        assertTrue(VecUtils.equalsExactly(mappings, encoder2.getEncodedLabels()));
        Files.delete(TestSuite.path);
    }

    @Test
    public void testNotFitException1() {
        assertThrows(ModelNotFitException.class, () -> {
            LabelEncoder encoder = new LabelEncoder(new int[]{0, 1, 5, 1, 2, 2, 2, 0, 1, 1, 5});
            encoder.encodeOrNull(2);
        });
    }

    @Test
    public void testNotFitException2() {
        assertThrows(ModelNotFitException.class, () -> {
            LabelEncoder encoder = new LabelEncoder(new int[]{0, 1, 5, 1, 2, 2, 2, 0, 1, 1, 5});
            encoder.reverseEncodeOrNull(2);
        });
    }

    @Test
    public void testNotFitException3() {
        assertThrows(ModelNotFitException.class, () -> {
            LabelEncoder encoder = new LabelEncoder(new int[]{0, 1, 5, 1, 2, 2, 2, 0, 1, 1, 5});
            encoder.getEncodedLabels();
        });
    }

    @Test
    public void testNotFitException4() {
        assertThrows(ModelNotFitException.class, () -> {
            LabelEncoder encoder = new LabelEncoder(new int[]{0, 1, 5, 1, 2, 2, 2, 0, 1, 1, 5});
            encoder.transform(new int[]{1, 2, 0});
        });
    }

    @Test
    public void testNotFitException5() {
        assertThrows(ModelNotFitException.class, () -> {
            LabelEncoder encoder = new LabelEncoder(new int[]{0, 1, 5, 1, 2, 2, 2, 0, 1, 1, 5});
            encoder.reverseTransform(new int[]{1, 2, 0});
        });
    }

    @Test
    public void testNoiseyLabelEncoder() {
        NoiseyLabelEncoder encoder = new NoiseyLabelEncoder(
            new int[]{0, -1, 5, -1, 2, 2, 2, 0, 1, 1, 5}).fit();
        assertTrue(encoder.encodeOrNull(-1) == -1);
        assertTrue(encoder.reverseEncodeOrNull(-1) == -1);

        final int[] exp = new int[]{0, -1, 1, -1, 2, 2, 2, 0, 3, 3, 1};
        assertTrue(VecUtils.equalsExactly(encoder.getEncodedLabels(), exp));

        // test extra fit, make sure nothing changes...
        assertTrue(VecUtils.equalsExactly(encoder.fit().getEncodedLabels(), exp));
    }
}

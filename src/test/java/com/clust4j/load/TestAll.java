package com.clust4j.load;

import com.clust4j.TestSuite;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;


@Suite
@SelectClasses({
    TestSuite.class,
    LoadTests.class
})

/**
 * Secondary test suite for clust4j. Runs all production
 * tests as well as some larger tests we don't want TravisCI to run
 * @author Taylor G Smith
 */
public class TestAll {
}

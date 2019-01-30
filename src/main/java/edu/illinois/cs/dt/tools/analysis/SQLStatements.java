package edu.illinois.cs.dt.tools.analysis;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SQLStatements {
    public static final Path INSERT_RAW_SUBJECT = Paths.get("src/main/sql/build/subject_raw_insert.sql");
    public static final Path UPDATE_SUBJECT_RAW_LOC = Paths.get("src/main/sql/build/subject_raw_update_loc.sql");

    public static final Path CREATE_TABLES = Paths.get("src/main/sql/build/create_tables.sql");
    public static final Path POST_SETUP = Paths.get("src/main/sql/build/post_setup.sql");

    public static final Path INSERT_SUBJECT = Paths.get("src/main/sql/build/subject_insert.sql");
    public static final Path INSERT_ORIGINAL_ORDER = Paths.get("src/main/sql/build/original_order_insert.sql");

    public static final Path INSERT_TEST_RUN_RESULT = Paths.get("src/main/sql/build/test_run_result_insert.sql");
    public static final Path UPDATE_TEST_RUN_RESULT_COUNT = Paths.get("src/main/sql/build/test_run_result_count_update.sql");
    public static final Path INSERT_TEST_RESULT = Paths.get("src/main/sql/build/test_result_insert.sql");

    public static final Path INSERT_FLAKY_TEST_LIST = Paths.get("src/main/sql/build/flaky_test_list_insert.sql");
    public static final Path INSERT_FLAKY_TEST = Paths.get("src/main/sql/build/flaky_test_insert.sql");
    public static final Path INSERT_DETECTION_ROUND = Paths.get("src/main/sql/build/detection_round_insert.sql");
    public static final Path INSERT_DETECTION_ROUND_TEST_RUN = Paths.get("src/main/sql/build/detection_round_insert_test_run.sql");

    public static final Path INSERT_VERIFICATION_ROUND = Paths.get("src/main/sql/build/verify_round_insert.sql");

    public static final Path INSERT_MODULE_TEST_TIME = Paths.get("src/main/sql/build/module_test_time_insert.sql");

    public static final Path INSERT_OPERATION_TIME = Paths.get("src/main/sql/build/operation_time_insert.sql");

    public static final Path COUNT_TESTS = Paths.get("src/main/sql/figures/count_tests.sql");

    public static final Path COUNT_MODULES_RESULTS = Paths.get("src/main/sql/figures/count_modules_results.sql");
    public static final Path COUNT_PROJECTS_RESULTS = Paths.get("src/main/sql/figures/count_projects_results.sql");
    public static final Path COUNT_MODULES = Paths.get("src/main/sql/figures/count_modules.sql");

    public static final Path COUNT_PROJECTS_WITH_FLAKY = Paths.get("src/main/sql/figures/count_projects_with_flaky.sql");
    public static final Path COUNT_MODULES_WITH_FLAKY = Paths.get("src/main/sql/figures/count_modules_with_flaky.sql");

    public static final Path COUNT_PROJECTS_WITH_ODNO = Paths.get("src/main/sql/figures/count_projects_with_odno.sql");
    public static final Path COUNT_MODULES_WITH_ODNO = Paths.get("src/main/sql/figures/count_modules_with_odno.sql");
    public static final Path COUNT_FLAKY = Paths.get("src/main/sql/figures/count_flaky.sql");
    public static final Path PROBABILITY_FAILURE = Paths.get("src/main/sql/figures/failure_frequency.sql");
    public static final Path TOTAL_NO_ORIG_AND_RANDOM = Paths.get("src/main/sql/figures/total_no_original_and_random.sql");
    public static final Path COUNT_TESTS_BY_ROUND_TYPE = Paths.get("src/main/sql/figures/count_tests_by_round_type.sql");
    public static final Path PROBABILITY_FIND_FLAKY = Paths.get("src/main/sql/figures/probability_find_flaky.sql");
    public static final Path PROBABILITY_BEST_RANDOM = Paths.get("src/main/sql/figures/probability_best_random.sql");
    public static final Path PROBABILITY_BEST_FLAKY = Paths.get("src/main/sql/figures/probability_best_flaky.sql");

    public static final Path COUNT_ODNO_TESTS = Paths.get("src/main/sql/figures/count_odno_tests.sql");

    public static final Path SUBJECT_INFO_TABLE = Paths.get("src/main/sql/tables/subject_information_table.sql");

    public static final Path COUNT_PROJECT_FLAKY_TESTS = Paths.get("src/main/sql/figures/count_project_flaky_test.sql") ;
    public static final Path COUNT_MODULE_FLAKY_TESTS = Paths.get("src/main/sql/figures/count_module_flaky_test.sql");
    public static final Path FLAKY_TEST_BY_TECHNIQUE = Paths.get("src/main/sql/tables/flaky_test_by_technique.sql");
    public static final Path FAILURE_PROB_PER_TEST_PER_RUN = Paths.get("src/main/sql/tables/failure_prob_per_test_per_run.sql");
    public static final Path FAILURE_PROB_BY_ROUND = Paths.get("src/main/sql/tables/failure_prob_by_round.sql");

    public static final Path PERC_RUN_FAIL_OD = Paths.get("src/main/sql/figures/perc_run_fail_od.sql");
    public static final Path PERC_RUN_FAIL_NO = Paths.get("src/main/sql/figures/perc_run_fail_no.sql");

    public static final Path PERC_FAIL_FLAKY_TESTS = Paths.get("src/main/sql/figures/perc_fail_flaky_tests.sql");

    static {
        ensureExists(INSERT_RAW_SUBJECT);

        ensureExists(CREATE_TABLES);
        ensureExists(POST_SETUP);

        ensureExists(INSERT_SUBJECT);
        ensureExists(INSERT_ORIGINAL_ORDER);

        ensureExists(INSERT_TEST_RUN_RESULT);
        ensureExists(UPDATE_TEST_RUN_RESULT_COUNT);
        ensureExists(INSERT_TEST_RESULT);

        ensureExists(INSERT_FLAKY_TEST_LIST);
        ensureExists(INSERT_FLAKY_TEST);
        ensureExists(INSERT_DETECTION_ROUND);
        ensureExists(INSERT_DETECTION_ROUND_TEST_RUN);

        ensureExists(INSERT_VERIFICATION_ROUND);

        ensureExists(INSERT_MODULE_TEST_TIME);

        ensureExists(INSERT_OPERATION_TIME);

        ensureExists(COUNT_TESTS);
        ensureExists(COUNT_MODULES_RESULTS);
        ensureExists(COUNT_PROJECTS_RESULTS);
        ensureExists(COUNT_MODULES);
        ensureExists(COUNT_PROJECTS_WITH_FLAKY);
        ensureExists(COUNT_MODULES_WITH_FLAKY);
        ensureExists(COUNT_PROJECTS_WITH_ODNO);
        ensureExists(COUNT_MODULES_WITH_ODNO);
        ensureExists(COUNT_FLAKY);
        ensureExists(PROBABILITY_FAILURE);
        ensureExists(TOTAL_NO_ORIG_AND_RANDOM);
        ensureExists(COUNT_TESTS_BY_ROUND_TYPE);
        ensureExists(PROBABILITY_BEST_RANDOM);
        ensureExists(PROBABILITY_BEST_FLAKY);
        ensureExists(PROBABILITY_FIND_FLAKY);
        ensureExists(COUNT_ODNO_TESTS);

        ensureExists(SUBJECT_INFO_TABLE);

        ensureExists(UPDATE_SUBJECT_RAW_LOC);
        ensureExists(COUNT_PROJECT_FLAKY_TESTS);
        ensureExists(COUNT_MODULE_FLAKY_TESTS);
        ensureExists(FAILURE_PROB_PER_TEST_PER_RUN);
        ensureExists(FAILURE_PROB_BY_ROUND);

        ensureExists(PERC_RUN_FAIL_OD);
        ensureExists(PERC_RUN_FAIL_NO);

        ensureExists(PERC_FAIL_FLAKY_TESTS);
    }

    private static void ensureExists(final Path p) {
        if (!Files.exists(p)) {
            throw new IllegalStateException(p + " does not exist!");
        }
    }
}
